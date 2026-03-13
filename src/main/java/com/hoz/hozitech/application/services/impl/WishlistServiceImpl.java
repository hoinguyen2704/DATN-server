package com.hoz.hozitech.application.services.impl;

import com.hoz.hozitech.application.repositories.ProductRepository;
import com.hoz.hozitech.application.repositories.UserRepository;
import com.hoz.hozitech.application.repositories.WishlistRepository;
import com.hoz.hozitech.application.services.WishlistService;
import com.hoz.hozitech.domain.dtos.response.PageResponse;
import com.hoz.hozitech.domain.dtos.response.WishlistResponse;
import com.hoz.hozitech.domain.entities.Product;
import com.hoz.hozitech.domain.entities.User;
import com.hoz.hozitech.domain.entities.Wishlist;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WishlistServiceImpl implements WishlistService {

    private final WishlistRepository wishlistRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    @Override
    @Transactional
    public void addProductToWishlist(UUID userId, UUID productId) {
        if (wishlistRepository.existsByUserIdAndProductId(userId, productId)) {
            // Already in wishlist, silently return or throw exception
            return; 
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));

        Wishlist wishlist = Wishlist.builder()
                .user(user)
                .product(product)
                .build();
        
        wishlistRepository.save(wishlist);
    }

    @Override
    @Transactional
    public void removeProductFromWishlist(UUID userId, UUID productId) {
        Wishlist wishlist = wishlistRepository.findByUserIdAndProductId(userId, productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found in user wishlist"));
        wishlistRepository.delete(wishlist);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<WishlistResponse> getUserWishlist(UUID userId, int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Wishlist> wishlistPage = wishlistRepository.findByUserId(userId, pageable);

        return PageResponse.<WishlistResponse>builder()
                .data(wishlistPage.getContent().stream().map(this::mapToResponse).collect(Collectors.toList()))
                .page(wishlistPage.getNumber() + 1)
                .perPage(wishlistPage.getSize())
                .total(wishlistPage.getTotalElements())
                .lastPage(wishlistPage.getTotalPages())
                .build();
    }

    private WishlistResponse mapToResponse(Wishlist wishlist) {
        Product product = wishlist.getProduct();
        
        // Find main thumbnail or use first variant image
        String thumbnailUrl = null;
        if (product.getImages() != null && !product.getImages().isEmpty()) {
            thumbnailUrl = product.getImages().stream()
                    .filter(img -> Boolean.TRUE.equals(img.getIsPrimary()))
                    .findFirst()
                    .map(img -> img.getImageUrl())
                    .orElse(product.getImages().get(0).getImageUrl());
        }

        BigDecimal price = product.getOriginPrice();
        BigDecimal comparePrice = product.getOriginPrice();
        
        if (product.getVariants() != null && !product.getVariants().isEmpty()) {
            price = product.getVariants().get(0).getPrice();
            comparePrice = product.getVariants().get(0).getCompareAtPrice();
        }

        return WishlistResponse.builder()
                .id(wishlist.getId())
                .productId(product.getId())
                .productName(product.getName())
                .productSlug(product.getSlug())
                .productPrice(price)
                .productCompareAtPrice(comparePrice)
                .productThumbnailUrl(thumbnailUrl)
                .addedAt(wishlist.getCreatedAt())
                .build();
    }
}
