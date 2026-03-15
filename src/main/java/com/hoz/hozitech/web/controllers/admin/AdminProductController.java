package com.hoz.hozitech.web.controllers.admin;

import com.hoz.hozitech.web.base.RestAPI;
import com.hoz.hozitech.web.base.RoleAdmin;
import com.hoz.hozitech.application.services.ProductService;
import com.hoz.hozitech.domain.dtos.request.ProductRequest;
import com.hoz.hozitech.domain.dtos.response.ApiResponse;
import com.hoz.hozitech.domain.dtos.response.PageResponse;
import com.hoz.hozitech.domain.dtos.response.ProductResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestAPI("${api.prefix-admin}/products")
@RoleAdmin
@RequiredArgsConstructor
public class AdminProductController {

    private final ProductService productService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ProductResponse>>> getAdminProducts(
            @RequestParam(required = false, defaultValue = "") String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir) {
        return ResponseEntity.ok(ApiResponse.success("Fetch admin products successfully",
                productService.getAdminProducts(keyword, status, page, size, sortBy, sortDir)));
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
}
