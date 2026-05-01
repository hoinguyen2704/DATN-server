package com.hoz.hozitech.web.controllers.user;

import com.hoz.hozitech.application.constant.PaginationConstant;
import com.hoz.hozitech.application.services.wishlist.WishlistService;
import com.hoz.hozitech.config.utils.LocalizedApiResponseFactory;
import com.hoz.hozitech.web.base.RestAPI;
import com.hoz.hozitech.web.base.Authenticated;
import com.hoz.hozitech.security.CustomUserDetails;
import com.hoz.hozitech.domain.dtos.response.ApiResponse;
import com.hoz.hozitech.domain.dtos.response.PageResponse;
import com.hoz.hozitech.domain.dtos.response.WishlistResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestAPI("${api.prefix-client}/wishlists")
@Authenticated
@RequiredArgsConstructor
public class WishlistController {

    private final WishlistService wishlistService;
    private final LocalizedApiResponseFactory responseFactory;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<WishlistResponse>>> getUserWishlist(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(defaultValue = PaginationConstant.PAGE_DEFAULT_STR) int page,
            @RequestParam(defaultValue = PaginationConstant.PAGE_SIZE_MEDIUM_STR) int size) {
        return ResponseEntity.ok(responseFactory.success(
                "response.wishlist.fetched",
                wishlistService.getUserWishlist(userDetails.getUser().getId(), page, size)));
    }

    @PostMapping("/{productSlug}")
    public ResponseEntity<ApiResponse<Void>> addProductToWishlist(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable String productSlug) {
        wishlistService.addProductToWishlist(userDetails.getUser().getId(), productSlug);
        return ResponseEntity.ok(responseFactory.success("response.wishlist.added"));
    }

    @DeleteMapping("/{productSlug}")
    public ResponseEntity<ApiResponse<Void>> removeProductFromWishlist(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable String productSlug) {
        wishlistService.removeProductFromWishlist(userDetails.getUser().getId(), productSlug);
        return ResponseEntity.ok(responseFactory.success("response.wishlist.removed"));
    }

    @GetMapping("/check/{productSlug}")
    public ResponseEntity<ApiResponse<Boolean>> isProductInWishlist(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable String productSlug) {
        return ResponseEntity.ok(responseFactory.success(
                "response.wishlist.status_checked",
                wishlistService.isProductInWishlist(userDetails.getUser().getId(), productSlug)));
    }

    @GetMapping("/count")
    public ResponseEntity<ApiResponse<Long>> getWishlistCount(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(responseFactory.success(
                "response.wishlist.count_fetched",
                wishlistService.getWishlistCount(userDetails.getUser().getId())));
    }
}
