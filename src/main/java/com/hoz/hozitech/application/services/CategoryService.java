package com.hoz.hozitech.application.services;

import com.hoz.hozitech.domain.dtos.request.CategoryRequest;
import com.hoz.hozitech.domain.dtos.response.CategoryResponse;

import java.util.List;
import java.util.UUID;

public interface CategoryService {

    // Public operations
    List<CategoryResponse> getCategoryTree();

    CategoryResponse getCategoryBySlug(String slug);

    List<CategoryResponse> getAllActiveCategories();

    // Admin operations
    CategoryResponse createCategory(CategoryRequest request);

    CategoryResponse updateCategory(UUID id, CategoryRequest request);

    void deleteCategory(UUID id);

    CategoryResponse toggleActiveStatus(UUID id);
}
