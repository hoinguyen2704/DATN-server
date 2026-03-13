package com.hoz.hozitech.application.services.impl;

import com.hoz.hozitech.application.repositories.CartRepository;
import com.hoz.hozitech.application.repositories.ProductVariantRepository;
import com.hoz.hozitech.application.repositories.UserRepository;
import com.hoz.hozitech.application.services.CartService;
import com.hoz.hozitech.domain.dtos.request.CartRequest;
import com.hoz.hozitech.domain.dtos.response.CartResponse;
import com.hoz.hozitech.domain.entities.Cart;
import com.hoz.hozitech.domain.entities.ProductImage;
import com.hoz.hozitech.domain.entities.ProductVariant;
import com.hoz.hozitech.domain.entities.User;
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
    public List<CartResponse> getCartByUser(UUID userId) {
        return cartRepository.findByUserId(userId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public CartResponse addToCart(UUID userId, CartRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        ProductVariant variant = variantRepository.findById(request.getVariantId())
                .orElseThrow(() -> new IllegalArgumentException("Product variant not found"));

        if (variant.getStock() < request.getQuantity()) {
            throw new IllegalArgumentException("Not enough stock available");
        }

        // Check if item already in cart -> update quantity
        var existingCart = cartRepository.findByUserIdAndVariantId(userId, request.getVariantId());
        if (existingCart.isPresent()) {
            Cart cart = existingCart.get();
            int newQty = cart.getQuantity() + request.getQuantity();
            if (newQty > variant.getStock()) {
                throw new IllegalArgumentException("Total quantity exceeds available stock");
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
                .orElseThrow(() -> new IllegalArgumentException("Cart item not found"));

        if (!cart.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Unauthorized access to cart item");
        }

        if (quantity <= 0) {
            cartRepository.delete(cart);
            return null;
        }

        if (quantity > cart.getVariant().getStock()) {
            throw new IllegalArgumentException("Quantity exceeds available stock");
        }

        cart.setQuantity(quantity);
        return mapToResponse(cartRepository.save(cart));
    }

    @Override
    @Transactional
    public void removeCartItem(UUID userId, UUID cartItemId) {
        Cart cart = cartRepository.findById(cartItemId)
                .orElseThrow(() -> new IllegalArgumentException("Cart item not found"));

        if (!cart.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Unauthorized access to cart item");
        }

        cartRepository.delete(cart);
    }

    @Override
    @Transactional
    public void clearCart(UUID userId) {
        cartRepository.deleteAllByUserId(userId);
    }

    @Override
    public long getCartCount(UUID userId) {
        return cartRepository.countByUserId(userId);
    }

    private CartResponse mapToResponse(Cart cart) {
        ProductVariant variant = cart.getVariant();
        String imageUrl = variant.getProduct().getImages().stream()
                .filter(img -> Boolean.TRUE.equals(img.getIsPrimary()))
                .map(ProductImage::getImageUrl)
                .findFirst()
                .orElse(null);

        return CartResponse.builder()
                .id(cart.getId())
                .variantId(variant.getId())
                .productName(variant.getProduct().getName())
                .variantName(variant.getVariantName())
                .imageUrl(imageUrl)
                .price(variant.getPrice())
                .quantity(cart.getQuantity())
                .subtotal(variant.getPrice().multiply(BigDecimal.valueOf(cart.getQuantity())))
                .stockQuantity(variant.getStock())
                .build();
    }
}
