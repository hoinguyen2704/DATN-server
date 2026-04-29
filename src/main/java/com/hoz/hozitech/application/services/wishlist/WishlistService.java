package com.hoz.hozitech.application.services.wishlist;

import com.hoz.hozitech.domain.dtos.response.PageResponse;
import com.hoz.hozitech.domain.dtos.response.WishlistResponse;

import java.util.UUID;

public interface WishlistService {
    void addProductToWishlist(UUID userId, String productSlug);
    void removeProductFromWishlist(UUID userId, String productSlug);
    PageResponse<WishlistResponse> getUserWishlist(UUID userId, int page, int size);
    boolean isProductInWishlist(UUID userId, String productSlug);
    long getWishlistCount(UUID userId);
}
