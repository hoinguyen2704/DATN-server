package com.hoz.hozitech.application.services;

import com.hoz.hozitech.domain.dtos.request.CartRequest;
import com.hoz.hozitech.domain.dtos.response.CartResponse;

import java.util.List;
import java.util.UUID;

public interface CartService {

    List<CartResponse> getCartByUser(UUID userId);

    CartResponse addToCart(UUID userId, CartRequest request);

    CartResponse updateCartItem(UUID userId, UUID cartItemId, Integer quantity);

    void removeCartItem(UUID userId, UUID cartItemId);

    void clearCart(UUID userId);

    long getCartCount(UUID userId);
}
