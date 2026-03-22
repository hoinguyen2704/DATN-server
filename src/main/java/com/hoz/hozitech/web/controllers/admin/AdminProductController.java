package com.hoz.hozitech.web.controllers.admin;

import com.hoz.hozitech.web.base.RestAPI;
import com.hoz.hozitech.web.base.RoleAdmin;
import com.hoz.hozitech.application.services.product.ProductService;
import com.hoz.hozitech.application.services.storage.FileStorageService;
import com.hoz.hozitech.application.repositories.ProductImageRepository;
import com.hoz.hozitech.application.repositories.ProductRepository;
import com.hoz.hozitech.domain.dtos.request.ProductRequest;
import com.hoz.hozitech.domain.dtos.response.ApiResponse;
import com.hoz.hozitech.domain.dtos.response.PageResponse;
import com.hoz.hozitech.domain.dtos.response.ProductResponse;
import com.hoz.hozitech.domain.entities.Product;
import com.hoz.hozitech.domain.entities.ProductImage;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@RestAPI("${api.prefix-admin}/products")
@RoleAdmin
@RequiredArgsConstructor
public class AdminProductController {

    private final ProductService productService;
    private final FileStorageService fileStorageService;
    private final ProductImageRepository productImageRepository;
    private final ProductRepository productRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ProductResponse>>> getAdminProducts(
            @RequestParam(required = false, defaultValue = "") String keyword,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir) {
        return ResponseEntity.ok(ApiResponse.success("Fetch admin products successfully",
                productService.getAdminProducts(keyword, categoryId, status, page, size, sortBy, sortDir)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> getProductById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success("Fetch product successfully",
                productService.getProductById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(@Valid @RequestBody ProductRequest request) {
        return ResponseEntity
                .ok(ApiResponse.success("Product created successfully", productService.createProduct(request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> updateProduct(
            @PathVariable UUID id,
            @Valid @RequestBody ProductRequest request) {
        return ResponseEntity
                .ok(ApiResponse.success("Product updated successfully", productService.updateProduct(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(@PathVariable UUID id) {
        productService.deleteProduct(id);
        return ResponseEntity.ok(ApiResponse.success("Product deleted successfully"));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<ProductResponse>> toggleStatus(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success("Product status toggled", productService.toggleProductStatus(id)));
    }

    // ─── Image Upload ────────────────────────────────────────────
    @PostMapping("/{id}/images")
    public ResponseEntity<ApiResponse<List<Map<String, String>>>> uploadImages(
            @PathVariable UUID id,
            @RequestParam("files") MultipartFile[] files) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found: " + id));

        int currentMaxOrder = productImageRepository.findByProductIdAndVariantIsNullOrderBySortOrder(id)
                .stream().mapToInt(ProductImage::getSortOrder).max().orElse(-1);

        List<Map<String, String>> uploaded = new ArrayList<>();
        for (MultipartFile file : files) {
            String url = fileStorageService.storeProductImage(file);
            ProductImage image = ProductImage.builder()
                    .imageUrl(url)
                    .altText(product.getName())
                    .sortOrder(++currentMaxOrder)
                    .isPrimary(currentMaxOrder == 0)
                    .product(product)
                    .build();
            productImageRepository.save(image);
            uploaded.add(Map.of("id", image.getId().toString(), "imageUrl", url));
        }
        return ResponseEntity.ok(ApiResponse.success("Images uploaded successfully", uploaded));
    }

    @DeleteMapping("/{productId}/images/{imageId}")
    public ResponseEntity<ApiResponse<Void>> deleteImage(
            @PathVariable UUID productId,
            @PathVariable UUID imageId) {
        ProductImage image = productImageRepository.findById(imageId)
                .orElseThrow(() -> new RuntimeException("Image not found: " + imageId));
        fileStorageService.deleteFile(image.getImageUrl());
        productImageRepository.delete(image);
        return ResponseEntity.ok(ApiResponse.success("Image deleted successfully"));
    }
}
