package com.hoz.hozitech.web.controllers;

import com.hoz.hozitech.application.services.CartService;
import com.hoz.hozitech.config.security.CustomUserDetails;
import com.hoz.hozitech.domain.dtos.request.CartRequest;
import com.hoz.hozitech.domain.dtos.response.ApiResponse;
import com.hoz.hozitech.domain.dtos.response.CartResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("${api.prefix-client}/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<CartResponse>>> getCart(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success("Cart fetched",
                cartService.getCartByUser(userDetails.getUser().getId())));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CartResponse>> addToCart(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody CartRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Item added to cart",
                cartService.addToCart(userDetails.getUser().getId(), request)));
    }

    @PutMapping("/{cartItemId}")
    public ResponseEntity<ApiResponse<CartResponse>> updateCartItem(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID cartItemId,
            @RequestBody Map<String, Integer> body) {
        return ResponseEntity.ok(ApiResponse.success("Cart item updated",
                cartService.updateCartItem(userDetails.getUser().getId(), cartItemId, body.get("quantity"))));
    }

    @DeleteMapping("/{cartItemId}")
    public ResponseEntity<ApiResponse<Void>> removeCartItem(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID cartItemId) {
        cartService.removeCartItem(userDetails.getUser().getId(), cartItemId);
        return ResponseEntity.ok(ApiResponse.success("Cart item removed"));
    }

    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> clearCart(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        cartService.clearCart(userDetails.getUser().getId());
        return ResponseEntity.ok(ApiResponse.success("Cart cleared"));
    }

    @GetMapping("/count")
    public ResponseEntity<ApiResponse<Long>> getCartCount(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success("Cart count",
                cartService.getCartCount(userDetails.getUser().getId())));
    }
}
