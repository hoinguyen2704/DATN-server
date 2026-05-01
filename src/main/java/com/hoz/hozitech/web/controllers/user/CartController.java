package com.hoz.hozitech.web.controllers.user;

import com.hoz.hozitech.application.services.cart.CartService;
import com.hoz.hozitech.config.utils.LocalizedApiResponseFactory;
import com.hoz.hozitech.security.CustomUserDetails;
import com.hoz.hozitech.domain.dtos.request.CartRequest;
import com.hoz.hozitech.domain.dtos.response.ApiResponse;
import com.hoz.hozitech.domain.dtos.response.CartResponse;
import com.hoz.hozitech.web.base.RestAPI;
import com.hoz.hozitech.web.base.Authenticated;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestAPI("${api.prefix-client}/cart")
@Authenticated
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;
    private final LocalizedApiResponseFactory responseFactory;

    @GetMapping
    public ResponseEntity<ApiResponse<List<CartResponse>>> getCart(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(responseFactory.success(
                "response.cart.fetched",
                cartService.getCartByUser(userDetails.getUser().getId())));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CartResponse>> addToCart(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody CartRequest request) {
        return ResponseEntity.ok(responseFactory.success(
                "response.cart.item_added",
                cartService.addToCart(userDetails.getUser().getId(), request)));
    }

    @PutMapping("/items/{variantSku}")
    public ResponseEntity<ApiResponse<CartResponse>> updateCartItem(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable String variantSku,
            @RequestBody Map<String, Integer> body) {
        return ResponseEntity.ok(responseFactory.success(
                "response.cart.item_updated",
                cartService.updateCartItem(userDetails.getUser().getId(), variantSku, body.get("quantity"))));
    }

    @DeleteMapping("/items/{variantSku}")
    public ResponseEntity<ApiResponse<Void>> removeCartItem(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable String variantSku) {
        cartService.removeCartItem(userDetails.getUser().getId(), variantSku);
        return ResponseEntity.ok(responseFactory.success("response.cart.item_removed"));
    }

    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> clearCart(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        cartService.clearCart(userDetails.getUser().getId());
        return ResponseEntity.ok(responseFactory.success("response.cart.cleared"));
    }

    @GetMapping("/count")
    public ResponseEntity<ApiResponse<Long>> getCartCount(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(responseFactory.success(
                "response.cart.count_fetched",
                cartService.getCartCount(userDetails.getUser().getId())));
    }
}
