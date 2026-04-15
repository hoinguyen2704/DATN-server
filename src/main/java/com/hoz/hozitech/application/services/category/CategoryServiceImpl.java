package com.hoz.hozitech.application.services.category;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hoz.hozitech.application.constant.PaginationConstant;
import com.hoz.hozitech.application.repositories.CategoryRepository;
import com.hoz.hozitech.application.repositories.SpecAttributeRepository;
import com.hoz.hozitech.application.repositories.VariantAttributeRepository;
import com.hoz.hozitech.config.exceptions.ConflictException;
import com.hoz.hozitech.config.exceptions.InvalidParamException;
import com.hoz.hozitech.domain.dtos.request.CategoryRequest;
import com.hoz.hozitech.domain.dtos.response.CategoryResponse;
import com.hoz.hozitech.domain.dtos.response.PageResponse;
import com.hoz.hozitech.domain.entities.Category;
import com.hoz.hozitech.domain.entities.CategorySpecAttribute;
import com.hoz.hozitech.domain.entities.CategoryVariantAttribute;
import com.hoz.hozitech.domain.entities.SpecAttribute;
import com.hoz.hozitech.domain.entities.VariantAttribute;
import com.hoz.hozitech.domain.entities.VariantAttributeOption;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final SpecAttributeRepository specAttributeRepository;
    private final VariantAttributeRepository variantAttributeRepository;
    private static final Pattern NONLATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITE_SPACE = Pattern.compile("[\\s]");

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getCategoryTree() {
        List<Category> allCategories = categoryRepository.findAll();
        Map<UUID, CategoryResponse> dtoMap = allCategories.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toMap(CategoryResponse::getId, Function.identity()));

        for (Category category : allCategories) {
            if (category.getParentCategory() != null) {
                CategoryResponse childDto = dtoMap.get(category.getId());
                CategoryResponse parentDto = dtoMap.get(category.getParentCategory().getId());
                if (parentDto != null && childDto != null) {
                    parentDto.getChildren().add(childDto);
                }
            }
        }

        return allCategories.stream()
                .filter(c -> c.getParentCategory() == null)
                .map(c -> dtoMap.get(c.getId()))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponse getCategoryBySlug(String slug) {
        Category category = categoryRepository.findBySlug(slug)
                .orElseThrow(() -> new IllegalArgumentException("Category not found with slug: " + slug));
        return mapToResponse(category);
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponse getCategoryById(UUID id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Category not found with id: " + id));
        return mapToResponse(category);
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponse getCategorySchema(UUID id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Category not found with id: " + id));
        return mapToResponse(category);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllActiveCategories() {
        return categoryRepository.findByStatusTrue().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<CategoryResponse> getAdminCategories(String keyword, UUID brandId, int page, int size) {
        Pageable pageable = PaginationConstant.of(page, size);
        Page<Category> categories;
        boolean hasKeyword = keyword != null && !keyword.isBlank();
        if (brandId != null) {
            if (hasKeyword) {
                categories = categoryRepository.findByKeywordAndBrandId(keyword, brandId, pageable);
            } else {
                categories = categoryRepository.findByBrandId(brandId, pageable);
            }
        } else if (hasKeyword) {
            categories = categoryRepository.findByNameContainingIgnoreCase(keyword, pageable);
        } else {
            categories = categoryRepository.findAll(pageable);
        }
        return PageResponse.of(categories.map(this::mapToResponse));
    }

    @Override
    @Transactional
    public CategoryResponse createCategory(CategoryRequest request) {
        validateSchemaRequest(request);

        Category parent = null;
        if (request.getParentId() != null) {
            parent = categoryRepository.findById(request.getParentId())
                    .orElseThrow(() -> new IllegalArgumentException("Parent category not found"));
        }

        String slug = toSlug(request.getName());
        if (categoryRepository.existsBySlug(slug)) {
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

        applyVariantAttributes(category, request.getVariantAttributes());
        applySpecAttributes(category, request.getSpecAttributes());

        return mapToResponse(categoryRepository.save(category));
    }

    @Override
    @Transactional
    public CategoryResponse updateCategory(UUID id, CategoryRequest request) {
        validateSchemaRequest(request);

        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Category not found"));

        category.setName(request.getName());
        category.setDescription(request.getDescription());
        category.setImageUrl(request.getImageUrl());

        if (request.getActive() != null) {
            category.setStatus(request.getActive());
        }

        if (request.getParentId() != null) {
            if (request.getParentId().equals(id)) {
                throw new InvalidParamException("A category cannot be its own parent");
            }
            Category parent = categoryRepository.findById(request.getParentId())
                    .orElseThrow(() -> new IllegalArgumentException("Parent category not found"));
            category.setParentCategory(parent);
        } else {
            category.setParentCategory(null);
        }

        String newSlug = toSlug(request.getName());
        if (!newSlug.equals(category.getSlug()) && !categoryRepository.existsBySlug(newSlug)) {
            category.setSlug(newSlug);
        }

        applyVariantAttributes(category, request.getVariantAttributes());
        applySpecAttributes(category, request.getSpecAttributes());

        return mapToResponse(categoryRepository.save(category));
    }

    @Override
    @Transactional
    public void deleteCategory(UUID id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Category not found"));
        if (!category.getChildren().isEmpty()) {
            throw new InvalidParamException(
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
        List<CategoryVariantAttribute> variantMappings = category.getCategoryVariantAttributes() == null
                ? List.of()
                : category.getCategoryVariantAttributes().stream()
                        .sorted(Comparator.comparing(CategoryVariantAttribute::getSortOrder))
                        .toList();

        List<CategoryResponse.VariantAttributeSchemaResponse> variantAttributes = variantMappings.stream()
                .map(mapping -> {
                    VariantAttribute attr = mapping.getVariantAttribute();
                    List<CategoryResponse.VariantOptionResponse> options = attr.getOptions() == null
                            ? List.of()
                            : attr.getOptions().stream()
                                    .sorted(Comparator.comparing(VariantAttributeOption::getSortOrder))
                                    .map(opt -> CategoryResponse.VariantOptionResponse.builder()
                                            .id(opt.getId())
                                            .label(opt.getLabel())
                                            .code(opt.getCode())
                                            .sortOrder(opt.getSortOrder())
                                            .active(opt.getActive())
                                            .build())
                                    .collect(Collectors.toList());

                    return CategoryResponse.VariantAttributeSchemaResponse.builder()
                            .id(attr.getId())
                            .name(attr.getName())
                            .code(attr.getCode())
                            .sortOrder(mapping.getSortOrder())
                            .options(options)
                            .build();
                })
                .collect(Collectors.toList());

        List<CategorySpecAttribute> specMappings = category.getCategorySpecAttributes() == null
                ? List.of()
                : category.getCategorySpecAttributes().stream()
                        .sorted(Comparator.comparing(CategorySpecAttribute::getSortOrder))
                        .toList();

        List<CategoryResponse.SpecSchemaResponse> specAttributes = specMappings.stream()
                .map(mapping -> CategoryResponse.SpecSchemaResponse.builder()
                        .id(mapping.getSpecAttribute().getId())
                        .name(mapping.getSpecAttribute().getName())
                        .code(mapping.getSpecAttribute().getCode())
                        .hint(mapping.getEffectiveHint())
                        .sortOrder(mapping.getSortOrder())
                        .build())
                .collect(Collectors.toList());

        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .slug(category.getSlug())
                .description(category.getDescription())
                .imageUrl(category.getImageUrl())
                .active(category.getStatus())
                .productCount(category.getProducts() != null ? (long) category.getProducts().size() : 0L)
                .createdAt(category.getCreatedAt())
                .children(new ArrayList<>())
                .variantAttributes(variantAttributes)
                .specAttributes(specAttributes)
                .build();
    }

    private void applyVariantAttributes(Category category, List<CategoryRequest.VariantAttributeItem> items) {
        List<CategoryVariantAttribute> mappings = category.getCategoryVariantAttributes();
        if (items == null) {
            mappings.clear();
            return;
        }

        Map<UUID, CategoryVariantAttribute> existingByAttributeId = mappings.stream()
                .collect(Collectors.toMap(
                        mapping -> mapping.getVariantAttribute().getId(),
                        Function.identity(),
                        (first, second) -> first));
        Set<String> seenCodes = new HashSet<>();

        for (int i = 0; i < items.size(); i++) {
            CategoryRequest.VariantAttributeItem item = items.get(i);
            if (item.getName() == null || item.getName().isBlank()) {
                continue;
            }

            String normalizedCode = normalizeCode(
                    item.getCode() != null && !item.getCode().isBlank() ? item.getCode() : item.getName());
            if (normalizedCode.isBlank()) {
                throw new InvalidParamException("Variant attribute code is invalid");
            }
            if (!seenCodes.add(normalizedCode)) {
                throw new ConflictException("Duplicate variant attribute code in category schema: " + normalizedCode);
            }

            VariantAttribute attr = resolveVariantAttribute(item, normalizedCode);
            applyVariantOptions(attr, item.getOptions(), normalizedCode);

            CategoryVariantAttribute mapping = existingByAttributeId.remove(attr.getId());
            if (mapping == null) {
                mapping = CategoryVariantAttribute.builder()
                        .category(category)
                        .variantAttribute(attr)
                        .build();
                mappings.add(mapping);
            }
            mapping.setCategory(category);
            mapping.setVariantAttribute(attr);
            mapping.setSortOrder(item.getSortOrder() != null ? item.getSortOrder() : i);
        }

        mappings.removeIf(mapping -> existingByAttributeId.containsKey(mapping.getVariantAttribute().getId()));
    }

    private VariantAttribute resolveVariantAttribute(CategoryRequest.VariantAttributeItem item, String normalizedCode) {
        VariantAttribute attr;
        if (item.getAttributeId() != null) {
            attr = variantAttributeRepository.findById(item.getAttributeId())
                    .orElseThrow(() -> new IllegalArgumentException("Variant attribute not found: " + item.getAttributeId()));
        } else if (!normalizedCode.isBlank()) {
            attr = variantAttributeRepository.findByCodeIgnoreCase(normalizedCode)
                    .orElseGet(() -> VariantAttribute.builder().build());
        } else {
            attr = variantAttributeRepository.findByNameIgnoreCase(item.getName().trim())
                    .orElseGet(() -> VariantAttribute.builder().build());
        }

        attr.setName(item.getName().trim());
        attr.setCode(normalizedCode);
        attr.setActive(Boolean.TRUE);
        return variantAttributeRepository.save(attr);
    }

    private void applyVariantOptions(VariantAttribute attr, List<CategoryRequest.VariantOptionItem> options, String attributeCode) {
        List<VariantAttributeOption> entities = attr.getOptions();
        if (options == null) {
            throw new InvalidParamException("Variant attribute " + attributeCode + " must have at least one option");
        }

        Map<String, VariantAttributeOption> existingByCode = entities.stream()
                .collect(Collectors.toMap(
                        option -> normalizeCode(option.getCode()),
                        Function.identity(),
                        (first, second) -> first));
        Set<String> optionCodes = new HashSet<>();
        boolean hasActiveOption = false;
        int appended = 0;
        for (int i = 0; i < options.size(); i++) {
            CategoryRequest.VariantOptionItem option = options.get(i);
            if (option.getLabel() == null || option.getLabel().isBlank()) {
                continue;
            }

            String normalizedCode = normalizeCode(
                    option.getCode() != null && !option.getCode().isBlank() ? option.getCode() : option.getLabel());
            if (normalizedCode.isBlank()) {
                throw new InvalidParamException("Variant option code is invalid for attribute " + attributeCode);
            }
            if (!optionCodes.add(normalizedCode)) {
                throw new ConflictException("Duplicate option code in attribute " + attributeCode + ": " + normalizedCode);
            }

            boolean isActive = option.getActive() != null ? option.getActive() : Boolean.TRUE;
            hasActiveOption = hasActiveOption || isActive;

            VariantAttributeOption entity = existingByCode.remove(normalizedCode);
            if (entity == null) {
                entity = VariantAttributeOption.builder()
                        .variantAttribute(attr)
                        .build();
                entities.add(entity);
            }
            entity.setVariantAttribute(attr);
            entity.setLabel(option.getLabel().trim());
            entity.setCode(normalizedCode);
            entity.setSortOrder(option.getSortOrder() != null ? option.getSortOrder() : appended);
            entity.setActive(isActive);
            appended++;
        }

        existingByCode.values().forEach(staleOption -> staleOption.setActive(Boolean.FALSE));

        if (appended == 0) {
            throw new InvalidParamException("Variant attribute " + attributeCode + " must have at least one option");
        }
        if (!hasActiveOption) {
            throw new InvalidParamException("Variant attribute " + attributeCode + " must have at least one active option");
        }

    }

    private void applySpecAttributes(Category category, List<CategoryRequest.SpecAttributeItem> items) {
        List<CategorySpecAttribute> mappings = category.getCategorySpecAttributes();
        if (items == null) {
            mappings.clear();
            return;
        }

        Map<UUID, CategorySpecAttribute> existingBySpecId = mappings.stream()
                .collect(Collectors.toMap(
                        mapping -> mapping.getSpecAttribute().getId(),
                        Function.identity(),
                        (first, second) -> first));
        Set<String> seenCodes = new HashSet<>();
        for (int i = 0; i < items.size(); i++) {
            CategoryRequest.SpecAttributeItem item = items.get(i);
            if (item.getName() == null || item.getName().isBlank()) {
                continue;
            }

            String normalizedCode = normalizeCode(
                    item.getCode() != null && !item.getCode().isBlank() ? item.getCode() : item.getName());
            if (normalizedCode.isBlank()) {
                throw new InvalidParamException("Spec attribute code is invalid");
            }
            if (!seenCodes.add(normalizedCode)) {
                throw new ConflictException("Duplicate spec attribute code in category schema: " + normalizedCode);
            }

            SpecAttribute attr = resolveSpecAttribute(item, normalizedCode);
            boolean isNewSpecAttribute = attr.getId() == null;
            boolean hasExplicitCode = item.getCode() != null && !item.getCode().isBlank();
            String normalizedNameCode = normalizeCode(item.getName());
            boolean codeLooksAutoFromName = !hasExplicitCode || normalizedCode.equals(normalizedNameCode);

            attr.setName(item.getName().trim());
            if (isNewSpecAttribute
                    || item.getAttributeId() != null
                    || !codeLooksAutoFromName
                    || attr.getCode() == null
                    || attr.getCode().isBlank()) {
                attr.setCode(normalizedCode);
            }
            attr.setDefaultHint(item.getHint());
            attr.setSortOrder(item.getSortOrder() != null ? item.getSortOrder() : i);
            attr.setActive(Boolean.TRUE);
            attr = specAttributeRepository.save(attr);

            CategorySpecAttribute mapping = existingBySpecId.remove(attr.getId());
            if (mapping == null) {
                mapping = CategorySpecAttribute.builder()
                        .category(category)
                        .specAttribute(attr)
                        .build();
                mappings.add(mapping);
            }
            mapping.setCategory(category);
            mapping.setSpecAttribute(attr);
            mapping.setCustomHint(item.getHint());
            mapping.setSortOrder(item.getSortOrder() != null ? item.getSortOrder() : i);
        }

        mappings.removeIf(mapping -> existingBySpecId.containsKey(mapping.getSpecAttribute().getId()));
    }

    private SpecAttribute resolveSpecAttribute(CategoryRequest.SpecAttributeItem item, String normalizedCode) {
        if (item.getAttributeId() != null) {
            return specAttributeRepository.findById(item.getAttributeId())
                    .orElseThrow(() -> new IllegalArgumentException("Spec attribute not found: " + item.getAttributeId()));
        }

        String trimmedName = item.getName() == null ? "" : item.getName().trim();
        if (!trimmedName.isBlank()) {
            SpecAttribute byName = specAttributeRepository.findByNameIgnoreCase(trimmedName).orElse(null);
            if (byName != null) {
                return byName;
            }
        }

        if (!normalizedCode.isBlank()) {
            SpecAttribute byCode = specAttributeRepository.findByCodeIgnoreCase(normalizedCode).orElse(null);
            if (byCode != null) {
                return byCode;
            }
        }

        return SpecAttribute.builder().build();
    }

    private String toSlug(String input) {
        if (input == null) return "";
        String nowhitespace = WHITE_SPACE.matcher(input).replaceAll("-");
        String normalized = Normalizer.normalize(nowhitespace, Normalizer.Form.NFD);
        String slug = NONLATIN.matcher(normalized).replaceAll("");
        return slug.toLowerCase(Locale.ENGLISH).replaceAll("-{2,}", "-").replaceAll("^-|-$", "");
    }

    private String normalizeCode(String input) {
        if (input == null) {
            return "";
        }
        String noWhitespace = WHITE_SPACE.matcher(input.trim()).replaceAll("-");
        String normalized = Normalizer.normalize(noWhitespace, Normalizer.Form.NFD);
        String ascii = NONLATIN.matcher(normalized).replaceAll("");
        return ascii.toUpperCase(Locale.ENGLISH)
                .replaceAll("-{2,}", "-")
                .replaceAll("^-|-$", "");
    }

    private void validateSchemaRequest(CategoryRequest request) {
        if (request.getVariantAttributes() != null) {
            Set<String> seenAttributeCodes = new HashSet<>();
            for (CategoryRequest.VariantAttributeItem item : request.getVariantAttributes()) {
                if (item.getName() == null || item.getName().isBlank()) {
                    continue;
                }

                String attributeCode = normalizeCode(
                        item.getCode() != null && !item.getCode().isBlank() ? item.getCode() : item.getName());
                if (attributeCode.isBlank()) {
                    throw new InvalidParamException("Variant attribute code is invalid");
                }
                if (!seenAttributeCodes.add(attributeCode)) {
                    throw new ConflictException("Duplicate variant attribute code in category schema: " + attributeCode);
                }

                if (item.getOptions() == null || item.getOptions().isEmpty()) {
                    throw new InvalidParamException("Variant attribute " + attributeCode + " must have at least one option");
                }

                Set<String> seenOptionCodes = new HashSet<>();
                boolean hasActiveOption = false;
                for (CategoryRequest.VariantOptionItem option : item.getOptions()) {
                    if (option.getLabel() == null || option.getLabel().isBlank()) {
                        continue;
                    }
                    String optionCode = normalizeCode(
                            option.getCode() != null && !option.getCode().isBlank() ? option.getCode() : option.getLabel());
                    if (optionCode.isBlank()) {
                        throw new InvalidParamException("Variant option code is invalid for attribute " + attributeCode);
                    }
                    if (!seenOptionCodes.add(optionCode)) {
                        throw new ConflictException("Duplicate option code in attribute " + attributeCode + ": " + optionCode);
                    }
                    if (option.getActive() == null || option.getActive()) {
                        hasActiveOption = true;
                    }
                }

                if (!hasActiveOption) {
                    throw new InvalidParamException(
                            "Variant attribute " + attributeCode + " must have at least one active option");
                }
            }
        }

        if (request.getSpecAttributes() != null) {
            Set<String> seenSpecCodes = new HashSet<>();
            for (CategoryRequest.SpecAttributeItem item : request.getSpecAttributes()) {
                if (item.getName() == null || item.getName().isBlank()) {
                    continue;
                }
                String specCode = normalizeCode(
                        item.getCode() != null && !item.getCode().isBlank() ? item.getCode() : item.getName());
                if (specCode.isBlank()) {
                    throw new InvalidParamException("Spec attribute code is invalid");
                }
                if (!seenSpecCodes.add(specCode)) {
                    throw new ConflictException("Duplicate spec attribute code in category schema: " + specCode);
                }
            }
        }
    }
}
