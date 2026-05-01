package com.hoz.hozitech.web.controllers.admin;

import com.hoz.hozitech.application.constant.PaginationConstant;
import com.hoz.hozitech.application.services.category.CategoryService;
import com.hoz.hozitech.config.utils.LocalizedApiResponseFactory;
import com.hoz.hozitech.domain.dtos.request.CategoryRequest;
import com.hoz.hozitech.domain.dtos.request.CreateVariantAttributeRequest;
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
    private final LocalizedApiResponseFactory responseFactory;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<CategoryResponse>>> getAdminCategories(
            @RequestParam(required = false, defaultValue = "") String keyword,
            @RequestParam(required = false) UUID brandId,
            @RequestParam(defaultValue = PaginationConstant.PAGE_DEFAULT_STR) int page,
            @RequestParam(defaultValue = PaginationConstant.PAGE_SIZE_MEDIUM_STR) int size,
            @RequestParam(required = false, defaultValue = "createdAt") String sortBy,
            @RequestParam(required = false, defaultValue = "DESC") String sortDir) {
        return ResponseEntity.ok(responseFactory.success("response.admin_category.list_fetched",
                categoryService.getAdminCategories(keyword, brandId, page, size, sortBy, sortDir)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CategoryResponse>> createCategory(@Valid @RequestBody CategoryRequest request) {
        return ResponseEntity
                .ok(responseFactory.success("response.admin_category.created", categoryService.createCategory(request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CategoryResponse>> updateCategory(
            @PathVariable UUID id,
            @Valid @RequestBody CategoryRequest request) {
        return ResponseEntity
                .ok(responseFactory.success("response.admin_category.updated", categoryService.updateCategory(id, request)));
    }

    @GetMapping("/{id}/schema")
    public ResponseEntity<ApiResponse<CategoryResponse>> getCategorySchema(@PathVariable UUID id) {
        return ResponseEntity
                .ok(responseFactory.success("response.admin_category.schema_fetched",
                        categoryService.getCategorySchema(id)));
    }

    @PostMapping("/{categoryId}/variant-attributes")
    public ResponseEntity<ApiResponse<CategoryResponse.VariantAttributeSchemaResponse>> createVariantAttribute(
            @PathVariable UUID categoryId,
            @Valid @RequestBody CreateVariantAttributeRequest request) {
        return ResponseEntity.ok(
                responseFactory.success(
                        "response.admin_category.variant_attribute_upserted",
                        categoryService.upsertVariantAttribute(
                                categoryId,
                                request.getName(),
                                request.getOptionLabelsText())));
    }

    @PostMapping("/{categoryId}/variant-attributes/{attributeId}/options")
    public ResponseEntity<ApiResponse<CategoryResponse.VariantOptionResponse>> createVariantAttributeOption(
            @PathVariable UUID categoryId,
            @PathVariable UUID attributeId,
            @Valid @RequestBody CreateVariantOptionRequest request) {
        return ResponseEntity.ok(
                responseFactory.success(
                        "response.admin_category.variant_option_upserted",
                        categoryService.upsertVariantOption(categoryId, attributeId, request.getLabel())));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCategory(@PathVariable UUID id) {
        categoryService.deleteCategory(id);
        return ResponseEntity.ok(responseFactory.success("response.admin_category.deleted"));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<CategoryResponse>> toggleStatus(@PathVariable UUID id) {
        return ResponseEntity
                .ok(responseFactory.success("response.admin_category.status_updated",
                        categoryService.toggleActiveStatus(id)));
    }
}
