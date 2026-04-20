package com.hoz.hozitech.web.controllers.admin;

import com.hoz.hozitech.application.constant.PaginationConstant;
import com.hoz.hozitech.application.services.category.CategoryService;
import com.hoz.hozitech.domain.dtos.request.CategoryRequest;
import com.hoz.hozitech.domain.dtos.request.CreateVariantOptionRequest;
import com.hoz.hozitech.domain.dtos.response.ApiResponse;
import com.hoz.hozitech.domain.dtos.response.CategoryResponse;
import com.hoz.hozitech.domain.dtos.response.PageResponse;
import com.hoz.hozitech.web.base.RestAPI;
import com.hoz.hozitech.web.base.RoleAdmin;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestAPI("${api.prefix-admin}/categories")
@RoleAdmin
@RequiredArgsConstructor
public class AdminCategoryController {

    private final CategoryService categoryService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<CategoryResponse>>> getAdminCategories(
            @RequestParam(required = false, defaultValue = "") String keyword,
            @RequestParam(required = false) UUID brandId,
            @RequestParam(defaultValue = PaginationConstant.PAGE_DEFAULT_STR) int page,
            @RequestParam(defaultValue = PaginationConstant.PAGE_SIZE_MEDIUM_STR) int size) {
        return ResponseEntity.ok(ApiResponse.success("Fetch admin categories successfully",
                categoryService.getAdminCategories(keyword, brandId, page, size)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CategoryResponse>> createCategory(@Valid @RequestBody CategoryRequest request) {
        return ResponseEntity
                .ok(ApiResponse.success("Category created successfully", categoryService.createCategory(request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CategoryResponse>> updateCategory(
            @PathVariable UUID id,
            @Valid @RequestBody CategoryRequest request) {
        return ResponseEntity
                .ok(ApiResponse.success("Category updated successfully", categoryService.updateCategory(id, request)));
    }

    @GetMapping("/{id}/schema")
    public ResponseEntity<ApiResponse<CategoryResponse>> getCategorySchema(@PathVariable UUID id) {
        return ResponseEntity
                .ok(ApiResponse.success("Fetch category schema successfully", categoryService.getCategorySchema(id)));
    }

    @PostMapping("/{categoryId}/variant-attributes/{attributeId}/options")
    public ResponseEntity<ApiResponse<CategoryResponse.VariantOptionResponse>> createVariantAttributeOption(
            @PathVariable UUID categoryId,
            @PathVariable UUID attributeId,
            @Valid @RequestBody CreateVariantOptionRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Variant option upserted successfully",
                        categoryService.upsertVariantOption(categoryId, attributeId, request.getLabel())));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCategory(@PathVariable UUID id) {
        categoryService.deleteCategory(id);
        return ResponseEntity.ok(ApiResponse.success("Category deleted successfully"));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<CategoryResponse>> toggleStatus(@PathVariable UUID id) {
        return ResponseEntity
                .ok(ApiResponse.success("Category status updated", categoryService.toggleActiveStatus(id)));
    }
}
