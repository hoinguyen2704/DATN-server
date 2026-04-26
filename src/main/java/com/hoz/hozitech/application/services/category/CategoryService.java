package com.hoz.hozitech.application.services.category;

import com.hoz.hozitech.domain.dtos.request.CategoryRequest;
import com.hoz.hozitech.domain.dtos.response.CategoryResponse;
import com.hoz.hozitech.domain.dtos.response.PageResponse;

import java.util.List;
import java.util.UUID;

public interface CategoryService {

    // Public operations
    CategoryResponse getCategoryBySlug(String slug);

    CategoryResponse getCategoryById(UUID id);

    CategoryResponse getCategorySchema(UUID id);

    List<CategoryResponse> getAllActiveCategories();

    // Admin operations
    PageResponse<CategoryResponse> getAdminCategories(
            String keyword,
            UUID brandId,
            int page,
            int size,
            String sortBy,
            String sortDir);

    CategoryResponse createCategory(CategoryRequest request);

    CategoryResponse updateCategory(UUID id, CategoryRequest request);

    CategoryResponse.VariantAttributeSchemaResponse upsertVariantAttribute(
            UUID categoryId,
            String name,
            String optionLabelsText);

    CategoryResponse.VariantOptionResponse upsertVariantOption(UUID categoryId, UUID attributeId, String label);

    void deleteCategory(UUID id);

    CategoryResponse toggleActiveStatus(UUID id);
}
