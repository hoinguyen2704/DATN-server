package com.hoz.hozitech.application.services.cart;

import com.hoz.hozitech.application.repositories.CartRepository;
import com.hoz.hozitech.application.repositories.ProductVariantRepository;
import com.hoz.hozitech.application.repositories.UserRepository;

import com.hoz.hozitech.domain.dtos.request.CartRequest;
import com.hoz.hozitech.domain.dtos.response.CartResponse;
import com.hoz.hozitech.domain.entities.Cart;
import com.hoz.hozitech.domain.entities.ProductImage;
import com.hoz.hozitech.domain.entities.ProductVariant;
import com.hoz.hozitech.domain.entities.User;
import com.hoz.hozitech.domain.enums.BusinessErrorCode;
import com.hoz.hozitech.domain.enums.ProductStatus;
import com.hoz.hozitech.web.exceptions.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final ProductVariantRepository variantRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public List<CartResponse> getCartByUser(UUID userId) {
        return cartRepository.findByUserId(userId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public CartResponse addToCart(UUID userId, CartRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(BusinessErrorCode.USER_NOT_FOUND, "User not found"));

        ProductVariant variant = variantRepository.findById(request.getVariantId())
                .orElseThrow(() -> new BusinessException(BusinessErrorCode.VARIANT_NOT_FOUND, "Product variant not found"));

        validateVariantPurchasable(variant);

        if (variant.getStock() < request.getQuantity()) {
            throw new BusinessException(BusinessErrorCode.INSUFFICIENT_STOCK, "Not enough stock available");
        }

        // Check if item already in cart -> update quantity
        var existingCart = cartRepository.findByUserIdAndVariantId(userId, request.getVariantId());
        if (existingCart.isPresent()) {
            Cart cart = existingCart.get();
            int newQty = cart.getQuantity() + request.getQuantity();
            if (newQty > variant.getStock()) {
                throw new BusinessException(BusinessErrorCode.INSUFFICIENT_STOCK, "Total quantity exceeds available stock");
            }
            cart.setQuantity(newQty);
            return mapToResponse(cartRepository.save(cart));
        }

        Cart cart = Cart.builder()
                .user(user)
                .variant(variant)
                .quantity(request.getQuantity())
                .build();

        return mapToResponse(cartRepository.save(cart));
    }

    @Override
    @Transactional
    public CartResponse updateCartItem(UUID userId, UUID cartItemId, Integer quantity) {
        Cart cart = cartRepository.findById(cartItemId)
                .orElseThrow(() -> new BusinessException(BusinessErrorCode.CART_ITEM_NOT_FOUND, "Cart item not found"));

        if (!cart.getUser().getId().equals(userId)) {
            throw new BusinessException(BusinessErrorCode.CART_ITEM_UNAUTHORIZED, "Unauthorized access to cart item");
        }

        if (quantity <= 0) {
            cartRepository.delete(cart);
            return null;
        }

        validateVariantPurchasable(cart.getVariant());
        if (quantity > cart.getVariant().getStock()) {
            throw new BusinessException(BusinessErrorCode.INSUFFICIENT_STOCK, "Quantity exceeds available stock");
        }

        cart.setQuantity(quantity);
        return mapToResponse(cartRepository.save(cart));
    }

    @Override
    @Transactional
    public void removeCartItem(UUID userId, UUID cartItemId) {
        Cart cart = cartRepository.findById(cartItemId)
                .orElseThrow(() -> new BusinessException(BusinessErrorCode.CART_ITEM_NOT_FOUND, "Cart item not found"));

        if (!cart.getUser().getId().equals(userId)) {
            throw new BusinessException(BusinessErrorCode.CART_ITEM_UNAUTHORIZED, "Unauthorized access to cart item");
        }

        cartRepository.delete(cart);
    }

    @Override
    @Transactional
    public void clearCart(UUID userId) {
        cartRepository.deleteAllByUserId(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public long getCartCount(UUID userId) {
        return cartRepository.countByUserId(userId);
    }

    private CartResponse mapToResponse(Cart cart) {
        ProductVariant variant = cart.getVariant();
        String issueCode = null;
        String issueMessage = null;
        boolean available = true;

        if (variant.getProduct().getStatus() != ProductStatus.ACTIVE) {
            issueCode = BusinessErrorCode.PRODUCT_NOT_AVAILABLE.name();
            issueMessage = "Sản phẩm hiện không còn mở bán";
            available = false;
        } else if (!Boolean.TRUE.equals(variant.getActive())) {
            issueCode = BusinessErrorCode.VARIANT_NOT_AVAILABLE.name();
            issueMessage = "Phiên bản sản phẩm hiện không còn mở bán";
            available = false;
        } else if (variant.getStock() <= 0) {
            issueCode = BusinessErrorCode.INSUFFICIENT_STOCK.name();
            issueMessage = "Sản phẩm đã hết hàng";
            available = false;
        } else if (cart.getQuantity() > variant.getStock()) {
            issueCode = BusinessErrorCode.INSUFFICIENT_STOCK.name();
            issueMessage = "Số lượng trong giỏ vượt quá tồn kho hiện tại";
            available = false;
        }

        String imageUrl = variant.getProduct().getImages().stream()
                .filter(img -> Boolean.TRUE.equals(img.getIsPrimary()))
                .map(ProductImage::getImageUrl)
                .findFirst()
                .orElse(null);

        return CartResponse.builder()
                .id(cart.getId())
                .variantId(variant.getId())
                .productName(variant.getProduct().getName())
                .productSlug(variant.getProduct().getSlug())
                .variantName(variant.getVariantName())
                .imageUrl(imageUrl)
                .price(variant.getPrice())
                .quantity(cart.getQuantity())
                .subtotal(variant.getPrice().multiply(BigDecimal.valueOf(cart.getQuantity())))
                .stockQuantity(variant.getStock())
                .available(available)
                .issueCode(issueCode)
                .issueMessage(issueMessage)
                .build();
    }

    private void validateVariantPurchasable(ProductVariant variant) {
        if (variant.getProduct() == null || variant.getProduct().getStatus() != ProductStatus.ACTIVE) {
            throw new BusinessException(BusinessErrorCode.PRODUCT_NOT_AVAILABLE, "Product is not available for purchase");
        }
        if (!Boolean.TRUE.equals(variant.getActive())) {
            throw new BusinessException(BusinessErrorCode.VARIANT_NOT_AVAILABLE, "Product variant is not available for purchase");
        }
        if (variant.getPrice() == null || variant.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(BusinessErrorCode.PRODUCT_NOT_AVAILABLE, "Product price is invalid or requires contact");
        }
    }
}
