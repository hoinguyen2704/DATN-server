package com.hoz.hozitech.web.controllers.user;

import com.hoz.hozitech.application.services.wishlist.WishlistService;
import com.hoz.hozitech.web.base.RestAPI;
import org.springframework.security.access.prepost.PreAuthorize;
import com.hoz.hozitech.security.CustomUserDetails;
import com.hoz.hozitech.domain.dtos.response.ApiResponse;
import com.hoz.hozitech.domain.dtos.response.PageResponse;
import com.hoz.hozitech.domain.dtos.response.WishlistResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestAPI("${api.prefix-client}/wishlists")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class WishlistController {

    private final WishlistService wishlistService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<WishlistResponse>>> getUserWishlist(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success("Wishlist retrieved successfully", 
                wishlistService.getUserWishlist(userDetails.getUser().getId(), page, size)));
    }

    @PostMapping("/{productId}")
    public ResponseEntity<ApiResponse<Void>> addProductToWishlist(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID productId) {
        wishlistService.addProductToWishlist(userDetails.getUser().getId(), productId);
        return ResponseEntity.ok(ApiResponse.success("Product added to wishlist successfully"));
    }

    @DeleteMapping("/{productId}")
    public ResponseEntity<ApiResponse<Void>> removeProductFromWishlist(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID productId) {
        wishlistService.removeProductFromWishlist(userDetails.getUser().getId(), productId);
        return ResponseEntity.ok(ApiResponse.success("Product removed from wishlist successfully"));
    }

    @GetMapping("/check/{productId}")
    public ResponseEntity<ApiResponse<Boolean>> isProductInWishlist(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID productId) {
        return ResponseEntity.ok(ApiResponse.success("Check wishlist success",
                wishlistService.isProductInWishlist(userDetails.getUser().getId(), productId)));
    }

    @GetMapping("/count")
    public ResponseEntity<ApiResponse<Long>> getWishlistCount(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success("Wishlist count retrieved",
                wishlistService.getWishlistCount(userDetails.getUser().getId())));
    }
}
