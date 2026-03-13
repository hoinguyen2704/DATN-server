package com.hoz.hozitech.application.services;

import com.hoz.hozitech.domain.dtos.request.ProductRequest;
import com.hoz.hozitech.domain.dtos.response.PageResponse;
import com.hoz.hozitech.domain.dtos.response.ProductResponse;

import java.util.UUID;

public interface ProductService {

    // Public API
    PageResponse<ProductResponse> searchProducts(String keyword, String categorySlug, String brand, int page, int size,
            String sortBy, String sortDir);

    ProductResponse getProductBySlug(String slug);

    ProductResponse getProductById(UUID id);

    // Admin API
    ProductResponse createProduct(ProductRequest request);

    ProductResponse updateProduct(UUID id, ProductRequest request);

    void deleteProduct(UUID id);

    ProductResponse toggleProductStatus(UUID id);
}
