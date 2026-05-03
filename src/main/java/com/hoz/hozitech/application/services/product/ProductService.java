package com.hoz.hozitech.application.services.product;

import com.hoz.hozitech.domain.dtos.request.ProductBasicRequest;
import com.hoz.hozitech.domain.dtos.request.ProductRequest;
import com.hoz.hozitech.domain.dtos.request.ProductVariantsUpdateRequest;
import com.hoz.hozitech.domain.dtos.response.AdminProductDeleteResultResponse;
import com.hoz.hozitech.domain.dtos.response.AdminProductListItemResponse;
import com.hoz.hozitech.domain.dtos.response.AdminProductPickerItemResponse;
import com.hoz.hozitech.domain.dtos.response.AdminProductVariantSummaryResponse;
import com.hoz.hozitech.domain.dtos.response.PageResponse;
import com.hoz.hozitech.domain.dtos.response.ProductResponse;

import java.util.List;
import java.util.UUID;

public interface ProductService {

    // Public API
    PageResponse<ProductResponse> searchProducts(String keyword, String categorySlug, String brand, int page, int size,
            String sortBy, String sortDir);

    ProductResponse getProductBySlug(String slug);

    ProductResponse getProductById(UUID id);

    // Admin API
    PageResponse<AdminProductListItemResponse> getAdminProducts(String keyword, java.util.UUID categoryId, String status, int page, int size, String sortBy, String sortDir);

    PageResponse<AdminProductPickerItemResponse> getAdminProductPickerItems(String keyword, java.util.UUID categoryId, java.util.UUID brandId, int page, int size, String sortBy, String sortDir);

    List<AdminProductVariantSummaryResponse> getAdminProductVariantSummaries(UUID productId);

    ProductResponse createProduct(ProductRequest request);

    ProductResponse updateProduct(UUID id, ProductRequest request);

    ProductResponse updateProductBasic(UUID id, ProductBasicRequest request);

    ProductResponse updateProductVariants(UUID id, ProductVariantsUpdateRequest request);

    AdminProductDeleteResultResponse deleteProduct(UUID id);

    ProductResponse toggleProductStatus(UUID id);

    // Homepage API
    java.util.List<ProductResponse> getFeaturedProducts(int limit);

    java.util.List<ProductResponse> getNewArrivals(int limit);

    java.util.List<ProductResponse> getTopRatedProducts(int limit);
}
