package com.hoz.hozitech.application.services.impl;

import com.hoz.hozitech.application.repositories.CategoryRepository;
import com.hoz.hozitech.application.services.CategoryService;
import com.hoz.hozitech.domain.dtos.request.CategoryRequest;
import com.hoz.hozitech.domain.dtos.response.CategoryResponse;
import com.hoz.hozitech.domain.entities.Category;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private static final Pattern NONLATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITE_SPACE = Pattern.compile("[\\s]");

    @Override
    public List<CategoryResponse> getCategoryTree() {
        // Fetch all categories
        List<Category> allCategories = categoryRepository.findAll();

        // Map to response DTOs
        Map<UUID, CategoryResponse> dtoMap = allCategories.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toMap(CategoryResponse::getId, Function.identity()));

        // Build tree: Link children to their parents
        for (Category category : allCategories) {
            if (category.getParentCategory() != null) {
                CategoryResponse childDto = dtoMap.get(category.getId());
                CategoryResponse parentDto = dtoMap.get(category.getParentCategory().getId());
                if (parentDto != null && childDto != null) {
                    parentDto.getChildren().add(childDto);
                }
            }
        }

        // Return only root nodes (parents are null)
        return allCategories.stream()
                .filter(c -> c.getParentCategory() == null)
                .map(c -> dtoMap.get(c.getId()))
                .collect(Collectors.toList());
    }

    @Override
    public CategoryResponse getCategoryBySlug(String slug) {
        Category category = categoryRepository.findBySlug(slug)
                .orElseThrow(() -> new IllegalArgumentException("Category not found with slug: " + slug));
        return mapToResponse(category);
    }

    @Override
    public List<CategoryResponse> getAllActiveCategories() {
        return categoryRepository.findByActiveTrue().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public CategoryResponse createCategory(CategoryRequest request) {
        Category parent = null;
        if (request.getParentId() != null) {
            parent = categoryRepository.findById(request.getParentId())
                    .orElseThrow(() -> new IllegalArgumentException("Parent category not found"));
        }

        String slug = toSlug(request.getName());
        if (categoryRepository.existsBySlug(slug)) {
            // Append a random UUID or timestamp to ensure uniqueness in real world
            slug = slug + "-" + UUID.randomUUID().toString().substring(0, 8);
        }

        Category category = Category.builder()
                .name(request.getName())
                .slug(slug)
                .description(request.getDescription())
                .imageUrl(request.getImageUrl())
                .status(request.getActive() != null ? request.getActive() : true)
                .parentCategory(parent)
                .build();

        return mapToResponse(categoryRepository.save(category));
    }

    @Override
    @Transactional
    public CategoryResponse updateCategory(UUID id, CategoryRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Category not found"));

        category.setName(request.getName());
        category.setDescription(request.getDescription());
        category.setImageUrl(request.getImageUrl());

        if (request.getActive() != null) {
            category.setStatus(request.getActive());
        }

        if (request.getParentId() != null) {
            // Prevent setting itself as parent
            if (request.getParentId().equals(id)) {
                throw new IllegalArgumentException("A category cannot be its own parent");
            }
            Category parent = categoryRepository.findById(request.getParentId())
                    .orElseThrow(() -> new IllegalArgumentException("Parent category not found"));
            category.setParentCategory(parent);
        } else {
            category.setParentCategory(null);
        }

        // Update slug if name changed significantly (optional, but good practice)
        String newSlug = toSlug(request.getName());
        if (!newSlug.equals(category.getSlug()) && !categoryRepository.existsBySlug(newSlug)) {
            category.setSlug(newSlug);
        }

        return mapToResponse(categoryRepository.save(category));
    }

    @Override
    @Transactional
    public void deleteCategory(UUID id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Category not found"));

        // Check if it has children
        if (!category.getChildren().isEmpty()) {
            throw new IllegalArgumentException(
                    "Cannot delete category with children. Please reassign or delete children first.");
        }

        categoryRepository.delete(category);
    }

    @Override
    @Transactional
    public CategoryResponse toggleActiveStatus(UUID id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Category not found"));
        category.setStatus(!category.getStatus());
        return mapToResponse(categoryRepository.save(category));
    }

    private CategoryResponse mapToResponse(Category category) {
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .slug(category.getSlug())
                .description(category.getDescription())
                .imageUrl(category.getImageUrl())
                .active(category.getStatus())
                .createdAt(category.getCreatedAt())
                .children(new java.util.ArrayList<>()) // Initialize to avoid null pointer when building tree
                .build();
    }

    private String toSlug(String input) {
        if (input == null)
            return "";
        String nowhitespace = WHITE_SPACE.matcher(input).replaceAll("-");
        String normalized = Normalizer.normalize(nowhitespace, Normalizer.Form.NFD);
        String slug = NONLATIN.matcher(normalized).replaceAll("");
        return slug.toLowerCase(Locale.ENGLISH).replaceAll("-{2,}", "-").replaceAll("^-|-$", "");
    }
}
