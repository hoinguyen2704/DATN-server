package com.hoz.hozitech.application.services;

import com.hoz.hozitech.domain.dtos.response.PageResponse;
import com.hoz.hozitech.domain.dtos.response.WishlistResponse;

import java.util.UUID;

public interface WishlistService {
    void addProductToWishlist(UUID userId, UUID productId);
    void removeProductFromWishlist(UUID userId, UUID productId);
    PageResponse<WishlistResponse> getUserWishlist(UUID userId, int page, int size);
}
