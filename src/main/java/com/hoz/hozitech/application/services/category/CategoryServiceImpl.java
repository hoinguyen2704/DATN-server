package com.hoz.hozitech.application.services.category;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collection;
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
import org.springframework.data.domain.Sort;

import com.hoz.hozitech.application.constant.PaginationConstant;
import com.hoz.hozitech.application.repositories.CategoryRepository;
import com.hoz.hozitech.application.repositories.CategorySpecAttributeRepository;
import com.hoz.hozitech.application.repositories.CategoryVariantAttributeRepository;
import com.hoz.hozitech.application.repositories.ProductRepository;
import com.hoz.hozitech.application.repositories.SpecAttributeRepository;
import com.hoz.hozitech.application.repositories.VariantAttributeRepository;
import com.hoz.hozitech.application.repositories.VariantAttributeOptionRepository;
import com.hoz.hozitech.application.services.notification.AdminNotificationService;
import com.hoz.hozitech.application.services.notification.AdminNotificationTemplates;
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
import com.hoz.hozitech.web.exceptions.ResourceNotFoundException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final CategoryVariantAttributeRepository categoryVariantAttributeRepository;
    private final CategorySpecAttributeRepository categorySpecAttributeRepository;
    private final SpecAttributeRepository specAttributeRepository;
    private final VariantAttributeRepository variantAttributeRepository;
    private final VariantAttributeOptionRepository variantAttributeOptionRepository;
    private final AdminNotificationService adminNotificationService;
    private static final Pattern NONLATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITE_SPACE = Pattern.compile("[\\s]");

    @Override
    @Transactional(readOnly = true)
    public CategoryResponse getCategoryBySlug(String slug) {
        Category category = categoryRepository.findBySlug(slug)
                .orElseThrow(() -> new InvalidParamException("Category not found with slug: " + slug)
                        .withMessageKey("error.category_not_found_with_slug", slug));
        return mapToResponse(category);
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponse getCategoryById(UUID id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new InvalidParamException("Category not found with id: " + id)
                        .withMessageKey("error.category_not_found_with_id", id));
        return mapToResponse(category);
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponse getCategorySchema(UUID id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new InvalidParamException("Category not found with id: " + id)
                        .withMessageKey("error.category_not_found_with_id", id));
        List<CategoryVariantAttribute> variantMappings = categoryVariantAttributeRepository.findSchemaByCategoryId(id);
        List<CategorySpecAttribute> specMappings = categorySpecAttributeRepository.findSchemaByCategoryId(id);
        long productCount = loadProductCountByCategoryIds(List.of(id)).getOrDefault(id, 0L);
        return mapToSchemaResponse(category, variantMappings, specMappings, productCount);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllActiveCategories() {
        List<Category> categories = categoryRepository.findByStatusTrue();
        Map<UUID, Long> productCountByCategoryId = loadProductCountByCategoryIds(categories.stream()
                .map(Category::getId)
                .toList());
        Map<UUID, Integer> specCountByCategoryId = loadSpecCountByCategoryIds(categories.stream()
                .map(Category::getId)
                .toList());
        return categories.stream()
                .map(category -> mapToAdminSummaryResponse(
                        category,
                        productCountByCategoryId.getOrDefault(category.getId(), 0L),
                        specCountByCategoryId.getOrDefault(category.getId(), 0)))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<CategoryResponse> getAdminCategories(
            String keyword,
            UUID brandId,
            int page,
            int size,
            String sortBy,
            String sortDir) {
        if (requiresComputedAdminCategorySort(sortBy)) {
            return getAdminCategoriesWithComputedSort(keyword, brandId, page, size, sortBy, sortDir);
        }

        Pageable pageable = PaginationConstant.of(page, size, resolveAdminCategorySort(sortBy, sortDir));
        Page<Category> categories = queryAdminCategories(keyword, brandId, pageable);
        Map<UUID, Long> productCountByCategoryId = loadProductCountByCategoryIds(categories.getContent().stream()
                .map(Category::getId)
                .toList());
        Map<UUID, Integer> specCountByCategoryId = loadSpecCountByCategoryIds(categories.getContent().stream()
                .map(Category::getId)
                .toList());

        List<CategoryResponse> content = categories.getContent().stream()
                .map(category -> mapToAdminSummaryResponse(
                        category,
                        productCountByCategoryId.getOrDefault(category.getId(), 0L),
                        specCountByCategoryId.getOrDefault(category.getId(), 0)))
                .collect(Collectors.toList());

        return PageResponse.<CategoryResponse>builder()
                .data(content)
                .page(page)
                .perPage(size)
                .total(categories.getTotalElements())
                .lastPage(categories.getTotalPages())
                .build();
    }

    private Sort resolveAdminCategorySort(String sortBy, String sortDir) {
        Sort.Direction direction = "ASC".equalsIgnoreCase(sortDir)
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        return switch (sortBy == null ? "" : sortBy) {
            case "name" -> Sort.by(
                    new Sort.Order(direction, "name"),
                    Sort.Order.desc("createdAt"),
                    Sort.Order.desc("id"));
            case "slug" -> Sort.by(
                    new Sort.Order(direction, "slug"),
                    Sort.Order.desc("createdAt"),
                    Sort.Order.desc("id"));
            case "status" -> Sort.by(
                    new Sort.Order(direction, "status"),
                    Sort.Order.desc("createdAt"),
                    Sort.Order.desc("id"));
            case "createdAt" -> Sort.by(
                    new Sort.Order(direction, "createdAt"),
                    Sort.Order.desc("id"));
            default -> Sort.by(
                    Sort.Order.desc("createdAt"),
                    Sort.Order.desc("id"));
        };
    }

    private boolean requiresComputedAdminCategorySort(String sortBy) {
        return "specCount".equalsIgnoreCase(sortBy) || "productCount".equalsIgnoreCase(sortBy);
    }

    private Page<Category> queryAdminCategories(String keyword, UUID brandId, Pageable pageable) {
        boolean hasKeyword = keyword != null && !keyword.isBlank();
        if (brandId != null) {
            if (hasKeyword) {
                return categoryRepository.findByKeywordAndBrandId(keyword, brandId, pageable);
            }
            return categoryRepository.findByBrandId(brandId, pageable);
        }
        if (hasKeyword) {
            return categoryRepository.findByNameContainingIgnoreCase(keyword, pageable);
        }
        return categoryRepository.findAll(pageable);
    }

    private PageResponse<CategoryResponse> getAdminCategoriesWithComputedSort(
            String keyword,
            UUID brandId,
            int page,
            int size,
            String sortBy,
            String sortDir) {
        List<Category> categories = loadAllAdminCategories(keyword, brandId);
        Map<UUID, Long> productCountByCategoryId = loadProductCountByCategoryIds(categories.stream()
                .map(Category::getId)
                .toList());
        Map<UUID, Integer> specCountByCategoryId = loadSpecCountByCategoryIds(categories.stream()
                .map(Category::getId)
                .toList());
        List<Category> sortedCategories = new ArrayList<>(categories);
        sortedCategories.sort(buildComputedCategoryComparator(sortBy, sortDir, productCountByCategoryId, specCountByCategoryId));

        int safePage = Math.max(page, 1);
        int safeSize = PaginationConstant.validateSize(size);
        int fromIndex = Math.min((safePage - 1) * safeSize, sortedCategories.size());
        int toIndex = Math.min(fromIndex + safeSize, sortedCategories.size());

        List<CategoryResponse> content = sortedCategories.subList(fromIndex, toIndex).stream()
                .map(category -> mapToAdminSummaryResponse(
                        category,
                        productCountByCategoryId.getOrDefault(category.getId(), 0L),
                        specCountByCategoryId.getOrDefault(category.getId(), 0)))
                .collect(Collectors.toList());

        long total = sortedCategories.size();
        int totalPages = total == 0 ? 0 : (int) Math.ceil((double) total / safeSize);

        return PageResponse.<CategoryResponse>builder()
                .data(content)
                .page(safePage)
                .perPage(safeSize)
                .total(total)
                .lastPage(totalPages)
                .build();
    }

    private List<Category> loadAllAdminCategories(String keyword, UUID brandId) {
        List<Category> categories = new ArrayList<>();
        int currentPage = 1;
        Page<Category> batch;

        do {
            Pageable pageable = PaginationConstant.of(
                    currentPage,
                    PaginationConstant.MAX_PAGE_SIZE,
                    Sort.by(
                            Sort.Order.desc("createdAt"),
                            Sort.Order.desc("id")));
            batch = queryAdminCategories(keyword, brandId, pageable);
            categories.addAll(batch.getContent());
            currentPage++;
        } while (batch.hasNext());

        return categories;
    }

    private Comparator<Category> buildComputedCategoryComparator(
            String sortBy,
            String sortDir,
            Map<UUID, Long> productCountByCategoryId,
            Map<UUID, Integer> specCountByCategoryId) {
        Comparator<Category> comparator = switch (sortBy == null ? "" : sortBy) {
            case "specCount" -> Comparator.comparingInt(category -> specCountByCategoryId.getOrDefault(category.getId(), 0));
            case "productCount" -> Comparator.comparingLong(category -> productCountByCategoryId.getOrDefault(category.getId(), 0L));
            default -> Comparator.comparing(Category::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()));
        };

        if (!"ASC".equalsIgnoreCase(sortDir)) {
            comparator = comparator.reversed();
        }

        return comparator
                .thenComparing(Category::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(Category::getId, Comparator.nullsLast(Comparator.reverseOrder()));
    }

    @Override
    @Transactional
    public CategoryResponse createCategory(CategoryRequest request) {
        validateSchemaRequest(request);

        String slug = toSlug(request.getName());
        if (categoryRepository.existsBySlug(slug)) {
            slug = slug + "-" + UUID.randomUUID().toString().substring(0, 8);
        }

        Category category = Category.builder()
                .name(request.getName())
                .slug(slug)
                .status(request.getActive() != null ? request.getActive() : true)
                .build();

        applyVariantAttributes(category, request.getVariantAttributes());
        applySpecAttributes(category, request.getSpecAttributes());

        Category saved = categoryRepository.save(category);
        adminNotificationService.createShared(AdminNotificationTemplates.categoryCreated(saved), true);
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public CategoryResponse updateCategory(UUID id, CategoryRequest request) {
        validateSchemaRequest(request);

        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Category not found"));

        category.setName(request.getName());

        if (request.getActive() != null) {
            category.setStatus(request.getActive());
        }

        String newSlug = toSlug(request.getName());
        if (!newSlug.equals(category.getSlug()) && !categoryRepository.existsBySlug(newSlug)) {
            category.setSlug(newSlug);
        }

        applyVariantAttributes(category, request.getVariantAttributes());
        applySpecAttributes(category, request.getSpecAttributes());

        Category saved = categoryRepository.save(category);
        adminNotificationService.createShared(AdminNotificationTemplates.categoryUpdated(saved), true);
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public CategoryResponse.VariantAttributeSchemaResponse upsertVariantAttribute(
            UUID categoryId,
            String name,
            String optionLabelsText) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category", categoryId));

        String trimmedName = name == null ? "" : name.trim();
        if (trimmedName.isBlank()) {
            throw new InvalidParamException("Variant attribute name is required");
        }

        List<CategoryRequest.VariantOptionItem> initialOptions = parseVariantOptionItems(optionLabelsText);
        if (initialOptions.isEmpty()) {
            throw new InvalidParamException("At least one variant option label is required");
        }

        String normalizedCode = normalizeCode(trimmedName);
        if (normalizedCode.isBlank()) {
            throw new InvalidParamException("Variant attribute code is invalid");
        }

        boolean attributeExistsInCategory = category.getCategoryVariantAttributes().stream()
                .anyMatch(mapping -> mapping.getVariantAttribute() != null
                        && normalizedCode.equalsIgnoreCase(mapping.getVariantAttribute().getCode()));
        if (attributeExistsInCategory) {
            throw new ConflictException("Variant attribute already exists in category schema");
        }

        CategoryRequest.VariantAttributeItem item = new CategoryRequest.VariantAttributeItem(
                null,
                trimmedName,
                normalizedCode,
                null,
                initialOptions);

        VariantAttribute attribute = resolveVariantAttribute(item, normalizedCode);
        applyVariantOptions(attribute, item.getOptions(), normalizedCode);

        int nextSortOrder = category.getCategoryVariantAttributes().stream()
                .map(mapping -> mapping.getSortOrder() == null ? 0 : mapping.getSortOrder())
                .max(Integer::compareTo)
                .orElse(-1) + 1;

        CategoryVariantAttribute mapping = CategoryVariantAttribute.builder()
                .category(category)
                .variantAttribute(attribute)
                .sortOrder(nextSortOrder)
                .build();
        category.getCategoryVariantAttributes().add(mapping);

        Category savedCategory = categoryRepository.save(category);
        CategoryVariantAttribute savedMapping = savedCategory.getCategoryVariantAttributes().stream()
                .filter(existingMapping -> existingMapping.getVariantAttribute() != null
                        && attribute.getId().equals(existingMapping.getVariantAttribute().getId()))
                .findFirst()
                .orElse(mapping);
        return mapToVariantAttributeSchemaResponse(savedMapping);
    }

    @Override
    @Transactional
    public CategoryResponse.VariantOptionResponse upsertVariantOption(UUID categoryId, UUID attributeId, String label) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category", categoryId));
        String trimmedLabel = label == null ? "" : label.trim();
        if (trimmedLabel.isBlank()) {
            throw new InvalidParamException("Variant option label is required");
        }

        CategoryVariantAttribute mapping = category.getCategoryVariantAttributes().stream()
                .filter(item -> item.getVariantAttribute() != null
                        && attributeId.equals(item.getVariantAttribute().getId()))
                .findFirst()
                .orElseThrow(() -> new InvalidParamException("Variant attribute does not belong to category"));
        VariantAttribute attribute = mapping.getVariantAttribute();
        String normalizedCode = normalizeCode(trimmedLabel);
        if (normalizedCode.isBlank()) {
            throw new InvalidParamException("Variant option code is invalid");
        }

        VariantAttributeOption existing = variantAttributeOptionRepository
                .findByVariantAttributeIdAndCodeIgnoreCase(attributeId, normalizedCode)
                .orElse(null);
        if (existing != null) {
            if (Boolean.TRUE.equals(existing.getActive())) {
                throw new ConflictException("Variant option already exists and is active");
            }

            existing.setLabel(trimmedLabel);
            existing.setCode(normalizedCode);
            existing.setActive(Boolean.TRUE);
            return mapToVariantOptionResponse(variantAttributeOptionRepository.save(existing));
        }

        int nextSortOrder = attribute.getOptions().stream()
                .map(option -> option.getSortOrder() == null ? 0 : option.getSortOrder())
                .max(Integer::compareTo)
                .orElse(-1) + 1;
        VariantAttributeOption created = VariantAttributeOption.builder()
                .variantAttribute(attribute)
                .label(trimmedLabel)
                .code(normalizedCode)
                .sortOrder(nextSortOrder)
                .active(Boolean.TRUE)
                .build();
        created = variantAttributeOptionRepository.save(created);
        attribute.getOptions().add(created);
        return mapToVariantOptionResponse(created);
    }

    @Override
    @Transactional
    public void deleteCategory(UUID id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Category not found"));

        if (productRepository.existsByCategoryId(id)) {
            throw new ConflictException(
                    "Không thể xóa danh mục vì vẫn còn sản phẩm thuộc danh mục này",
                    "Cannot delete category because products still reference it");
        }

        categoryRepository.delete(category);
    }

    @Override
    @Transactional
    public CategoryResponse toggleActiveStatus(UUID id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Category not found"));
        category.setStatus(!category.getStatus());
        Category saved = categoryRepository.save(category);
        adminNotificationService.createShared(AdminNotificationTemplates.categoryUpdated(saved), true);
        return mapToResponse(saved);
    }

    private CategoryResponse mapToResponse(Category category) {
        List<CategoryVariantAttribute> variantMappings = category.getCategoryVariantAttributes() == null
                ? List.of()
                : category.getCategoryVariantAttributes().stream()
                .sorted(Comparator.comparing(CategoryVariantAttribute::getSortOrder))
                .toList();

        List<CategorySpecAttribute> specMappings = category.getCategorySpecAttributes() == null
                ? List.of()
                : category.getCategorySpecAttributes().stream()
                .sorted(Comparator.comparing(CategorySpecAttribute::getSortOrder))
                .toList();

        long productCount = category.getProducts() != null ? (long) category.getProducts().size() : 0L;
        return mapToSchemaResponse(category, variantMappings, specMappings, productCount);
    }

    private CategoryResponse mapToSchemaResponse(
            Category category,
            List<CategoryVariantAttribute> variantMappings,
            List<CategorySpecAttribute> specMappings,
            long productCount) {
        List<CategoryResponse.VariantAttributeSchemaResponse> variantAttributes = variantMappings.stream()
                .map(this::mapToVariantAttributeSchemaResponse)
                .collect(Collectors.toList());

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
                .active(category.getStatus())
                .productCount(productCount)
                .specCount(specAttributes.size())
                .createdAt(category.getCreatedAt())
                .variantAttributes(variantAttributes)
                .specAttributes(specAttributes)
                .build();
    }

    private CategoryResponse mapToAdminSummaryResponse(Category category, long productCount, int specCount) {
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .slug(category.getSlug())
                .active(category.getStatus())
                .productCount(productCount)
                .specCount(specCount)
                .createdAt(category.getCreatedAt())
                .build();
    }

    private Map<UUID, Long> loadProductCountByCategoryIds(Collection<UUID> categoryIds) {
        if (categoryIds == null || categoryIds.isEmpty()) {
            return Map.of();
        }
        return categoryRepository.countProductsByCategoryIds(categoryIds).stream()
                .collect(Collectors.toMap(
                        row -> (UUID) row[0],
                        row -> ((Number) row[1]).longValue()));
    }

    private Map<UUID, Integer> loadSpecCountByCategoryIds(Collection<UUID> categoryIds) {
        if (categoryIds == null || categoryIds.isEmpty()) {
            return Map.of();
        }
        return categoryRepository.countSpecAttributesByCategoryIds(categoryIds).stream()
                .collect(Collectors.toMap(
                        row -> (UUID) row[0],
                        row -> ((Number) row[1]).intValue()));
    }

    private CategoryResponse.VariantOptionResponse mapToVariantOptionResponse(VariantAttributeOption option) {
        return CategoryResponse.VariantOptionResponse.builder()
                .id(option.getId())
                .label(option.getLabel())
                .code(option.getCode())
                .sortOrder(option.getSortOrder())
                .active(option.getActive())
                .build();
    }

    private List<CategoryRequest.VariantOptionItem> parseVariantOptionItems(String optionLabelsText) {
        if (optionLabelsText == null || optionLabelsText.isBlank()) {
            return List.of();
        }

        String[] rawLabels = optionLabelsText.split("[,，]");
        List<CategoryRequest.VariantOptionItem> optionItems = new ArrayList<>();
        for (String rawLabel : rawLabels) {
            String trimmedLabel = rawLabel == null ? "" : rawLabel.trim();
            if (trimmedLabel.isBlank()) {
                continue;
            }

            optionItems.add(new CategoryRequest.VariantOptionItem(
                    null,
                    trimmedLabel,
                    null,
                    optionItems.size(),
                    Boolean.TRUE));
        }
        return optionItems;
    }

    private CategoryResponse.VariantAttributeSchemaResponse mapToVariantAttributeSchemaResponse(
            CategoryVariantAttribute mapping) {
        VariantAttribute attribute = mapping.getVariantAttribute();
        List<CategoryResponse.VariantOptionResponse> options = attribute.getOptions() == null
                ? List.of()
                : attribute.getOptions().stream()
                .sorted(Comparator.comparing(VariantAttributeOption::getSortOrder))
                .map(this::mapToVariantOptionResponse)
                .collect(Collectors.toList());

        return CategoryResponse.VariantAttributeSchemaResponse.builder()
                .id(attribute.getId())
                .name(attribute.getName())
                .code(attribute.getCode())
                .sortOrder(mapping.getSortOrder())
                .options(options)
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
                throw new ConflictException("Duplicate variant attribute code in category schema: " + normalizedCode)
                        .withMessageKey("error.duplicate_variant_attribute_code", normalizedCode);
            }

            VariantAttribute attribute = resolveVariantAttribute(item, normalizedCode);
            applyVariantOptions(attribute, item.getOptions(), normalizedCode);

            CategoryVariantAttribute mapping = existingByAttributeId.remove(attribute.getId());
            if (mapping == null) {
                mapping = CategoryVariantAttribute.builder()
                        .category(category)
                        .variantAttribute(attribute)
                        .build();
                mappings.add(mapping);
            }
            mapping.setCategory(category);
            mapping.setVariantAttribute(attribute);
            mapping.setSortOrder(item.getSortOrder() != null ? item.getSortOrder() : i);
        }

        mappings.removeIf(mapping -> existingByAttributeId.containsKey(mapping.getVariantAttribute().getId()));
    }

    private VariantAttribute resolveVariantAttribute(CategoryRequest.VariantAttributeItem item, String normalizedCode) {
        VariantAttribute attribute;
        if (item.getAttributeId() != null) {
            attribute = variantAttributeRepository.findById(item.getAttributeId())
                    .orElseThrow(() -> new ResourceNotFoundException("Variant attribute", item.getAttributeId()));
        } else if (!normalizedCode.isBlank()) {
            attribute = variantAttributeRepository.findByCodeIgnoreCase(normalizedCode)
                    .orElseGet(() -> VariantAttribute.builder().build());
        } else {
            attribute = variantAttributeRepository.findByNameIgnoreCase(item.getName().trim())
                    .orElseGet(() -> VariantAttribute.builder().build());
        }

        attribute.setName(item.getName().trim());
        attribute.setCode(normalizedCode);
        attribute.setActive(Boolean.TRUE);
        return variantAttributeRepository.save(attribute);
    }

    private void applyVariantOptions(
            VariantAttribute attribute,
            List<CategoryRequest.VariantOptionItem> options,
            String attributeCode
    ) {
        List<VariantAttributeOption> entities = attribute.getOptions();
        if (options == null) {
            throw new InvalidParamException("Variant attribute " + attributeCode + " must have at least one option")
                    .withMessageKey("error.variant_attribute_requires_options", attributeCode);
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
                throw new InvalidParamException("Variant option code is invalid for attribute " + attributeCode)
                        .withMessageKey("error.variant_option_code_invalid_for_attribute", attributeCode);
            }
            if (!optionCodes.add(normalizedCode)) {
                throw new ConflictException("Duplicate option code in attribute " + attributeCode + ": " + normalizedCode)
                        .withMessageKey("error.duplicate_option_code_in_attribute", attributeCode, normalizedCode);
            }

            boolean isActive = option.getActive() != null ? option.getActive() : Boolean.TRUE;
            hasActiveOption = hasActiveOption || isActive;

            VariantAttributeOption entity = existingByCode.remove(normalizedCode);
            if (entity == null) {
                entity = VariantAttributeOption.builder()
                        .variantAttribute(attribute)
                        .build();
                entities.add(entity);
            }
            entity.setVariantAttribute(attribute);
            entity.setLabel(option.getLabel().trim());
            entity.setCode(normalizedCode);
            entity.setSortOrder(option.getSortOrder() != null ? option.getSortOrder() : appended);
            entity.setActive(isActive);
            appended++;
        }

        existingByCode.values().forEach(staleOption -> staleOption.setActive(Boolean.FALSE));

        if (appended == 0) {
            throw new InvalidParamException("Variant attribute " + attributeCode + " must have at least one option")
                    .withMessageKey("error.variant_attribute_requires_options", attributeCode);
        }
        if (!hasActiveOption) {
            throw new InvalidParamException("Variant attribute " + attributeCode + " must have at least one active option")
                    .withMessageKey("error.variant_attribute_requires_active_option", attributeCode);
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
                throw new ConflictException("Duplicate spec attribute code in category schema: " + normalizedCode)
                        .withMessageKey("error.duplicate_spec_attribute_code", normalizedCode);
            }

            SpecAttribute attribute = resolveSpecAttribute(item, normalizedCode);
            boolean isNewSpecAttribute = attribute.getId() == null;
            boolean hasExplicitCode = item.getCode() != null && !item.getCode().isBlank();
            String normalizedNameCode = normalizeCode(item.getName());
            boolean codeLooksAutoFromName = !hasExplicitCode || normalizedCode.equals(normalizedNameCode);

            attribute.setName(item.getName().trim());
            if (isNewSpecAttribute
                    || item.getAttributeId() != null
                    || !codeLooksAutoFromName
                    || attribute.getCode() == null
                    || attribute.getCode().isBlank()) {
                attribute.setCode(normalizedCode);
            }
            attribute.setDefaultHint(item.getHint());
            attribute.setSortOrder(item.getSortOrder() != null ? item.getSortOrder() : i);
            attribute.setActive(Boolean.TRUE);
            attribute = specAttributeRepository.save(attribute);

            CategorySpecAttribute mapping = existingBySpecId.remove(attribute.getId());
            if (mapping == null) {
                mapping = CategorySpecAttribute.builder()
                        .category(category)
                        .specAttribute(attribute)
                        .build();
                mappings.add(mapping);
            }
            mapping.setCategory(category);
            mapping.setSpecAttribute(attribute);
            mapping.setCustomHint(item.getHint());
            mapping.setSortOrder(item.getSortOrder() != null ? item.getSortOrder() : i);
        }

        mappings.removeIf(mapping -> existingBySpecId.containsKey(mapping.getSpecAttribute().getId()));
    }

    private SpecAttribute resolveSpecAttribute(CategoryRequest.SpecAttributeItem item, String normalizedCode) {
        if (item.getAttributeId() != null) {
            return specAttributeRepository.findById(item.getAttributeId())
                    .orElseThrow(() -> new ResourceNotFoundException("Spec attribute", item.getAttributeId()));
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
        String noWhitespace = WHITE_SPACE.matcher(input).replaceAll("-");
        String normalized = Normalizer.normalize(noWhitespace, Normalizer.Form.NFD)
                .replace("\u0111", "d")
                .replace("\u0110", "D");
        String slug = NONLATIN.matcher(normalized).replaceAll("");
        return slug.toLowerCase(Locale.ENGLISH).replaceAll("-{2,}", "-").replaceAll("^-|-$", "");
    }

    private String normalizeCode(String input) {
        if (input == null) {
            return "";
        }
        String normalized = Normalizer.normalize(input.trim(), Normalizer.Form.NFD);
        String ascii = normalized
                .replace("\u0111", "d")
                .replace("\u0110", "D")
                .replaceAll("[\\p{InCombiningDiacriticalMarks}]", "")
                .replace('_', '-');
        return ascii.toUpperCase(Locale.ENGLISH)
                .replaceAll("[^A-Z0-9-]+", "-")
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
                    throw new ConflictException("Duplicate variant attribute code in category schema: " + attributeCode)
                            .withMessageKey("error.duplicate_variant_attribute_code", attributeCode);
                }

                if (item.getOptions() == null || item.getOptions().isEmpty()) {
                    throw new InvalidParamException("Variant attribute " + attributeCode + " must have at least one option")
                            .withMessageKey("error.variant_attribute_requires_options", attributeCode);
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
                        throw new InvalidParamException("Variant option code is invalid for attribute " + attributeCode)
                                .withMessageKey("error.variant_option_code_invalid_for_attribute", attributeCode);
                    }
                    if (!seenOptionCodes.add(optionCode)) {
                        throw new ConflictException("Duplicate option code in attribute " + attributeCode + ": " + optionCode)
                                .withMessageKey("error.duplicate_option_code_in_attribute", attributeCode, optionCode);
                    }
                    if (option.getActive() == null || option.getActive()) {
                        hasActiveOption = true;
                    }
                }

                if (!hasActiveOption) {
                    throw new InvalidParamException(
                            "Variant attribute " + attributeCode + " must have at least one active option")
                            .withMessageKey("error.variant_attribute_requires_active_option", attributeCode);
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
                    throw new ConflictException("Duplicate spec attribute code in category schema: " + specCode)
                            .withMessageKey("error.duplicate_spec_attribute_code", specCode);
                }
            }
        }
    }
}
