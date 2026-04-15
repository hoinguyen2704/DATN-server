package com.hoz.hozitech.application.services.product;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hoz.hozitech.application.constant.PaginationConstant;
import com.hoz.hozitech.application.repositories.BrandRepository;
import com.hoz.hozitech.application.repositories.CategoryRepository;
import com.hoz.hozitech.application.repositories.OrderItemRepository;
import com.hoz.hozitech.application.repositories.ProductRepository;
import com.hoz.hozitech.application.repositories.ProductVariantRepository;
import com.hoz.hozitech.application.repositories.ReturnItemRepository;
import com.hoz.hozitech.application.specifications.ProductSpecification;
import com.hoz.hozitech.config.exceptions.ConflictException;
import com.hoz.hozitech.domain.dtos.request.ProductImageRequest;
import com.hoz.hozitech.domain.dtos.request.ProductRequest;
import com.hoz.hozitech.domain.dtos.request.ProductVariantRequest;
import com.hoz.hozitech.domain.dtos.response.CategoryResponse;
import com.hoz.hozitech.domain.dtos.response.PageResponse;
import com.hoz.hozitech.domain.dtos.response.ProductImageResponse;
import com.hoz.hozitech.domain.dtos.response.ProductResponse;
import com.hoz.hozitech.domain.dtos.response.ProductVariantResponse;
import com.hoz.hozitech.domain.entities.Brand;
import com.hoz.hozitech.domain.entities.Category;
import com.hoz.hozitech.domain.entities.CategorySpecAttribute;
import com.hoz.hozitech.domain.entities.CategoryVariantAttribute;
import com.hoz.hozitech.domain.entities.Product;
import com.hoz.hozitech.domain.entities.ProductImage;
import com.hoz.hozitech.domain.entities.ProductSpecValue;
import com.hoz.hozitech.domain.entities.ProductVariant;
import com.hoz.hozitech.domain.entities.ProductVariantAttributeValue;
import com.hoz.hozitech.domain.entities.VariantAttributeOption;
import com.hoz.hozitech.domain.enums.ProductStatus;
import com.hoz.hozitech.web.exceptions.ResourceNotFoundException;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.RandomStringUtils;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final OrderItemRepository orderItemRepository;
    private final ReturnItemRepository returnItemRepository;
    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;
    private final EntityManager entityManager;

    private static final Pattern NONLATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITE_SPACE = Pattern.compile("[\\s]");
    private static final Pattern INVALID_SKU_CHARS = Pattern.compile("[^A-Z0-9-]");

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> searchProducts(String keyword, String categorySlug, String brand, int page, int size, String sortBy, String sortDir) {
        Sort sort;
        if ("popular".equalsIgnoreCase(sortBy)) {
            sort = Sort.by(
                    Sort.Order.desc("hasStock"),
                    Sort.Order.desc("totalSold"),
                    Sort.Order.desc("createdAt"));
        } else {
            Sort.Direction direction = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name()) ? Sort.Direction.ASC : Sort.Direction.DESC;
            sort = Sort.by(
                    Sort.Order.desc("hasStock"),
                    new Sort.Order(direction, sortBy));
        }

        Pageable pageable = PaginationConstant.of(page, size, sort);

        UUID categoryId = null;
        if (categorySlug != null && !categorySlug.isBlank()) {
            Category category = categoryRepository.findBySlug(categorySlug)
                    .orElseThrow(() -> new IllegalArgumentException("Category not found"));
            categoryId = category.getId();
        }

        Specification<Product> spec = ProductSpecification.filter(keyword, categoryId, brand, null, null, null, true);
        Page<Product> products = productRepository.findAll(spec, pageable);
        return PageResponse.of(products.map(this::mapToResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getProductBySlug(String slug) {
        Product product = productRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Product", slug));
        return mapToDetailedResponse(product);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getProductById(UUID id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));
        return mapToDetailedResponse(product);
    }

    @Override
    @Transactional
    public ProductResponse createProduct(ProductRequest request) {
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new IllegalArgumentException("Category not found"));
        Brand brand = brandRepository.findById(request.getBrandId())
                .orElseThrow(() -> new IllegalArgumentException("Brand not found"));

        if (productRepository.existsByName(request.getName())) {
            throw new ConflictException("Product name already exists");
        }

        String slug = uniqueSlug(request.getName(), null);
        String productCode = uniqueProductCode(request.getProductCode(), null);

        Product product = Product.builder()
                .name(request.getName())
                .slug(slug)
                .description(request.getDescription())
                .brand(brand)
                .originPrice(request.getOriginPrice())
                .productCode(productCode)
                .status(request.getStatus() != null ? request.getStatus() : ProductStatus.ACTIVE)
                .isFeatured(request.getIsFeatured() != null ? request.getIsFeatured() : false)
                .category(category)
                .variants(new ArrayList<>())
                .images(new ArrayList<>())
                .specValues(new ArrayList<>())
                .build();

        applyProductImages(product, request.getImages());
        applyProductSpecs(product, request.getSpecs());
        applyVariants(product, request.getVariants(), true);

        return mapToDetailedResponse(productRepository.save(product));
    }

    @Override
    @Transactional
    public ProductResponse updateProduct(UUID id, ProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new IllegalArgumentException("Category not found"));
        Brand brand = brandRepository.findById(request.getBrandId())
                .orElseThrow(() -> new IllegalArgumentException("Brand not found"));

        if (!Objects.equals(product.getName(), request.getName()) && productRepository.existsByName(request.getName())) {
            throw new ConflictException("Product name already exists");
        }

        product.setName(request.getName());
        product.setSlug(uniqueSlug(request.getName(), product.getId()));
        product.setDescription(request.getDescription());
        product.setBrand(brand);
        product.setOriginPrice(request.getOriginPrice());
        product.setCategory(category);
        if (request.getProductCode() != null && !request.getProductCode().isBlank()) {
            String requestedProductCode = sanitizeProductCode(request.getProductCode());
            if (!requestedProductCode.equals(product.getProductCode())) {
                throw new ConflictException("Product code is immutable after first save");
            }
        }
        if (request.getStatus() != null) {
            product.setStatus(request.getStatus());
        }
        if (request.getIsFeatured() != null) {
            product.setIsFeatured(request.getIsFeatured());
        }

        applyProductSpecs(product, request.getSpecs());
        applyVariants(product, request.getVariants(), false);
        return mapToDetailedResponse(productRepository.save(product));
    }

    private void applyProductImages(Product product, List<ProductImageRequest> imageRequests) {
        if (imageRequests == null) {
            return;
        }
        for (ProductImageRequest imgReq : imageRequests) {
            ProductImage img = ProductImage.builder()
                    .imageUrl(imgReq.getImageUrl())
                    .isPrimary(imgReq.getIsPrimary() != null ? imgReq.getIsPrimary() : false)
                    .product(product)
                    .build();
            product.getImages().add(img);
        }
    }

    private void applyProductSpecs(Product product, List<ProductRequest.ProductSpecRequest> specRequests) {
        product.getSpecValues().clear();

        List<CategorySpecAttribute> specSchema = getSpecSchema(product.getCategory());
        Map<UUID, CategorySpecAttribute> schemaBySpecId = specSchema.stream()
                .collect(Collectors.toMap(m -> m.getSpecAttribute().getId(), m -> m));

        if (!schemaBySpecId.isEmpty() && (specRequests == null || specRequests.isEmpty())) {
            throw new IllegalArgumentException("Specs are required for selected category");
        }

        if (specRequests == null) {
            return;
        }

        Map<UUID, ProductRequest.ProductSpecRequest> requestBySpecId = new HashMap<>();
        for (ProductRequest.ProductSpecRequest request : specRequests) {
            if (requestBySpecId.putIfAbsent(request.getSpecAttributeId(), request) != null) {
                throw new ConflictException("Duplicate spec attribute in request");
            }
        }

        if (!schemaBySpecId.isEmpty()) {
            for (UUID specId : schemaBySpecId.keySet()) {
                ProductRequest.ProductSpecRequest request = requestBySpecId.get(specId);
                if (request == null || request.getValue() == null || request.getValue().isBlank()) {
                    throw new IllegalArgumentException("Missing spec value for attribute " + specId);
                }
            }
        }

        for (ProductRequest.ProductSpecRequest request : specRequests) {
            CategorySpecAttribute mapping = schemaBySpecId.get(request.getSpecAttributeId());
            if (mapping == null) {
                throw new IllegalArgumentException("Spec attribute not allowed in selected category: " + request.getSpecAttributeId());
            }
            ProductSpecValue value = ProductSpecValue.builder()
                    .product(product)
                    .specAttribute(mapping.getSpecAttribute())
                    .valueText(request.getValue().trim())
                    .build();
            product.getSpecValues().add(value);
        }
    }

    private void applyVariants(Product product, List<ProductVariantRequest> variantRequests, boolean isCreate) {
        if (variantRequests == null || variantRequests.isEmpty()) {
            throw new IllegalArgumentException("At least one variant is required");
        }

        List<CategoryVariantAttribute> variantSchema = getVariantSchema(product.getCategory());
        if (variantSchema.isEmpty() && variantRequests.size() != 1) {
            throw new IllegalArgumentException("Category without variant schema supports exactly one variant");
        }

        Map<UUID, ProductVariant> existingById = product.getVariants().stream()
                .collect(Collectors.toMap(ProductVariant::getId, v -> v, (a, b) -> a));
        Map<String, ProductVariant> existingBySku = product.getVariants().stream()
                .filter(v -> v.getSku() != null)
                .collect(Collectors.toMap(ProductVariant::getSku, v -> v, (a, b) -> a));

        Set<String> usedSkus = new HashSet<>();
        Set<String> usedSignatures = new HashSet<>();
        Set<UUID> processedIds = new HashSet<>();

        for (ProductVariantRequest request : variantRequests) {
            ProductVariant variant = resolveVariantEntity(request, existingById, existingBySku);
            VariantComputed computed = computeVariantData(product, request, variantSchema, variant.getId(), usedSkus);

            if (!usedSignatures.add(computed.signature())) {
                throw new ConflictException("Duplicate variant combination in request");
            }

            variant.setSku(computed.sku());
            variant.setVariantName(computed.displayName());
            variant.setVariantSignature(computed.signature());
            variant.setPrice(request.getPrice());
            variant.setCompareAtPrice(request.getCompareAtPrice());
            variant.setStock(request.getStock() != null ? request.getStock() : 0);
            variant.setActive(request.getActive() != null ? request.getActive() : true);
            variant.setProduct(product);

            // Replace variant selections.
            variant.getAttributeValues().clear();
            variant.getAttributeValues().addAll(computed.attributeValues(variant));

            if (request.getImages() != null) {
                variant.getImages().clear();
                for (ProductImageRequest imageRequest : request.getImages()) {
                    ProductImage image = ProductImage.builder()
                            .imageUrl(imageRequest.getImageUrl())
                            .isPrimary(imageRequest.getIsPrimary() != null ? imageRequest.getIsPrimary() : false)
                            .product(product)
                            .variant(variant)
                            .build();
                    variant.getImages().add(image);
                }
            }

            if (variant.getId() == null && isCreate) {
                product.getVariants().add(variant);
            } else if (variant.getId() == null && !product.getVariants().contains(variant)) {
                product.getVariants().add(variant);
            }

            if (variant.getId() != null) {
                processedIds.add(variant.getId());
            }
            usedSkus.add(computed.sku());
        }

        if (!isCreate) {
            for (ProductVariant existing : existingById.values()) {
                if (!processedIds.contains(existing.getId())) {
                    existing.setActive(false);
                    existing.setStock(0);
                }
            }
        }
    }

    private ProductVariant resolveVariantEntity(ProductVariantRequest request, Map<UUID, ProductVariant> existingById, Map<String, ProductVariant> existingBySku) {
        if (request.getId() != null && existingById.containsKey(request.getId())) {
            return existingById.get(request.getId());
        }
        if (request.getSku() != null && existingBySku.containsKey(sanitizeSku(request.getSku()))) {
            return existingBySku.get(sanitizeSku(request.getSku()));
        }
        return ProductVariant.builder()
                .images(new ArrayList<>())
                .attributeValues(new ArrayList<>())
                .build();
    }

    private VariantComputed computeVariantData(Product product,
                                              ProductVariantRequest request,
                                              List<CategoryVariantAttribute> variantSchema,
                                              UUID currentVariantId,
                                              Set<String> usedSkus) {
        if (variantSchema.isEmpty()) {
            if (request.getSelections() != null && !request.getSelections().isEmpty()) {
                throw new IllegalArgumentException("Category without variant schema does not accept selections");
            }
            String sku = uniqueSku(request.getSku(), List.of("DEFAULT"), product.getProductCode(), currentVariantId, usedSkus);
            return new VariantComputed(sku, "Mặc định", "DEFAULT", List.of());
        }

        if (request.getSelections() == null || request.getSelections().isEmpty()) {
            throw new IllegalArgumentException("Variant selections are required");
        }

        Map<UUID, UUID> selectedOptionByAttribute = new HashMap<>();
        for (ProductVariantRequest.VariantSelectionRequest selection : request.getSelections()) {
            if (selectedOptionByAttribute.putIfAbsent(selection.getVariantAttributeId(), selection.getOptionId()) != null) {
                throw new ConflictException("Duplicate attribute in variant selections");
            }
        }

        Set<UUID> schemaAttributeIds = variantSchema.stream()
                .map(mapping -> mapping.getVariantAttribute().getId())
                .collect(Collectors.toSet());
        for (UUID selectedAttributeId : selectedOptionByAttribute.keySet()) {
            if (!schemaAttributeIds.contains(selectedAttributeId)) {
                throw new IllegalArgumentException("Selection contains attribute not in category schema: " + selectedAttributeId);
            }
        }
        if (selectedOptionByAttribute.size() != schemaAttributeIds.size()) {
            throw new IllegalArgumentException("Each variant must select exactly one option for every schema attribute");
        }

        List<String> displayTokens = new ArrayList<>();
        List<String> signatureTokens = new ArrayList<>();
        List<String> skuTokens = new ArrayList<>();
        List<AttributeSelectionContext> selectionContexts = new ArrayList<>();

        for (CategoryVariantAttribute mapping : variantSchema) {
            UUID attributeId = mapping.getVariantAttribute().getId();
            UUID optionId = selectedOptionByAttribute.get(attributeId);
            if (optionId == null) {
                throw new IllegalArgumentException("Missing option for attribute " + mapping.getVariantAttribute().getName());
            }

            VariantAttributeOption option = mapping.getVariantAttribute().getOptions().stream()
                    .filter(opt -> opt.getId().equals(optionId))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Option does not belong to attribute: " + optionId));
            if (!Boolean.TRUE.equals(option.getActive())) {
                throw new IllegalArgumentException("Option is inactive and cannot be used: " + optionId);
            }

            displayTokens.add(option.getLabel());
            signatureTokens.add(mapping.getVariantAttribute().getCode() + "=" + option.getCode());
            skuTokens.add(option.getCode());
            selectionContexts.add(new AttributeSelectionContext(mapping.getVariantAttribute().getId(), option));
        }

        String signature = String.join("|", signatureTokens);
        String displayName = String.join(" - ", displayTokens);
        String sku = uniqueSku(request.getSku(), skuTokens, product.getProductCode(), currentVariantId, usedSkus);

        return new VariantComputed(sku, displayName, signature, selectionContexts);
    }

    private String uniqueSku(String requestSku, List<String> tokens, String productCode, UUID currentVariantId, Set<String> usedSkus) {
        String base = requestSku;
        if (base == null || base.isBlank()) {
            base = sanitizeSku(String.join("-", buildSkuTokens(productCode, tokens)));
        } else {
            base = sanitizeSku(base);
        }
        if (base.isBlank()) {
            base = sanitizeSku(productCode + "-SKU");
        }

        String candidate = base;
        int suffix = 2;
        while (usedSkus.contains(candidate) || isSkuUsed(candidate, currentVariantId)) {
            candidate = base + "-" + String.format("%02d", suffix++);
        }
        return candidate;
    }

    private boolean isSkuUsed(String sku, UUID currentVariantId) {
        if (currentVariantId == null) {
            return productVariantRepository.existsBySku(sku);
        }
        return productVariantRepository.existsBySkuAndIdNot(sku, currentVariantId);
    }

    private List<String> buildSkuTokens(String productCode, List<String> optionCodes) {
        List<String> tokens = new ArrayList<>();
        tokens.add(sanitizeSkuToken(productCode));
        tokens.addAll(optionCodes.stream().map(this::sanitizeSkuToken).toList());
        return tokens;
    }

    private String sanitizeSkuToken(String raw) {
        if (raw == null) {
            return "";
        }
        String token = normalizeCode(raw);
        if (token.length() > 12) {
            token = token.substring(0, 12);
        }
        return token;
    }

    private String sanitizeSku(String raw) {
        if (raw == null) {
            return "";
        }
        String value = normalizeCode(raw);
        value = INVALID_SKU_CHARS.matcher(value).replaceAll("");
        value = value.replaceAll("-{2,}", "-").replaceAll("^-|-$", "");
        if (value.length() > 100) {
            return value.substring(0, 100);
        }
        return value;
    }

    private String uniqueProductCode(String requestProductCode, UUID currentProductId) {
        String base = sanitizeProductCode(requestProductCode);
        if (base.isBlank()) {
            base = generateProductCodeBase();
        }

        String candidate = base;
        int suffix = 2;
        while (isProductCodeUsed(candidate, currentProductId)) {
            String suffixToken = String.format("%02d", suffix++);
            int maxBaseLen = Math.max(1, 12 - suffixToken.length());
            String truncatedBase = base.length() > maxBaseLen ? base.substring(0, maxBaseLen) : base;
            candidate = truncatedBase + suffixToken;
        }
        return candidate;
    }

    private String generateProductCodeBase() {
        return "PRD" + RandomStringUtils.random(6, "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789");
    }

    private String sanitizeProductCode(String raw) {
        String normalized = normalizeCode(raw);
        if (normalized.length() > 12) {
            normalized = normalized.substring(0, 12);
        }
        return normalized;
    }

    private boolean isProductCodeUsed(String productCode, UUID currentProductId) {
        if (currentProductId == null) {
            return productRepository.existsByProductCode(productCode);
        }
        return productRepository.existsByProductCodeAndIdNot(productCode, currentProductId);
    }

    private String uniqueSlug(String name, UUID currentProductId) {
        String base = toSlug(name);
        String candidate = base;
        if (candidate.isBlank()) {
            candidate = "product";
        }
        while (isSlugUsed(candidate, currentProductId)) {
            candidate = base + "-" + UUID.randomUUID().toString().substring(0, 6);
        }
        return candidate;
    }

    private boolean isSlugUsed(String slug, UUID currentProductId) {
        return productRepository.findBySlug(slug)
                .map(existing -> currentProductId == null || !existing.getId().equals(currentProductId))
                .orElse(false);
    }

    @Override
    @Transactional
    public void deleteProduct(UUID id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));

        Number orderItemCount = (Number) entityManager.createNativeQuery(
                        "SELECT COUNT(*) FROM order_items WHERE variant_id IN (SELECT id FROM product_variants WHERE product_id = :pid)")
                .setParameter("pid", id)
                .getSingleResult();

        if (orderItemCount.longValue() > 0) {
            throw new ConflictException("Sản phẩm này đã phát sinh đơn hàng, không thể xoá cứng. Vui lòng chuyển trạng thái thành Bản Nháp hoặc Đã Ẩn!");
        }

        entityManager.createNativeQuery("DELETE FROM carts WHERE variant_id IN (SELECT id FROM product_variants WHERE product_id = :pid)")
                .setParameter("pid", id)
                .executeUpdate();

        entityManager.createNativeQuery("DELETE FROM wishlists WHERE product_id = :pid")
                .setParameter("pid", id)
                .executeUpdate();

        productRepository.delete(product);
    }

    @Override
    @Transactional
    public ProductResponse toggleProductStatus(UUID id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));

        if (ProductStatus.ACTIVE == product.getStatus()) {
            product.setStatus(ProductStatus.DRAFT);
        } else {
            product.setStatus(ProductStatus.ACTIVE);
        }
        return mapToDetailedResponse(productRepository.save(product));
    }

    private ProductResponse mapToResponse(Product product) {
        Comparator<ProductImage> imageComparator = Comparator
                .comparing((ProductImage img) -> !Boolean.TRUE.equals(img.getIsPrimary()))
                .thenComparing(img -> img.getSortOrder() == null ? Integer.MAX_VALUE : img.getSortOrder())
                .thenComparing(img -> img.getId() == null ? "" : img.getId().toString());

        String mainImageUrl = product.getImages().stream()
                .filter(img -> img.getVariant() == null)
                .sorted(imageComparator)
                .map(ProductImage::getImageUrl)
                .findFirst()
                .orElseGet(() -> product.getVariants().stream()
                        .flatMap(v -> v.getImages().stream())
                        .sorted(imageComparator)
                        .map(ProductImage::getImageUrl)
                        .findFirst()
                        .orElse(null));

        int totalStock = product.getVariants().stream()
                .filter(v -> Boolean.TRUE.equals(v.getActive()))
                .mapToInt(ProductVariant::getStock)
                .sum();

        BigDecimal lowestPrice = product.getVariants().stream()
                .filter(v -> Boolean.TRUE.equals(v.getActive()))
                .map(ProductVariant::getPrice)
                .filter(Objects::nonNull)
                .min(BigDecimal::compareTo)
                .orElse(product.getOriginPrice());

        List<CategorySpecAttribute> specMappings = getSpecSchema(product.getCategory());
        List<ProductResponse.ProductSpecValueResponse> specs = buildSpecResponses(product, specMappings);

        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .slug(product.getSlug())
                .description(product.getDescription())
                .brandId(product.getBrand() != null ? product.getBrand().getId() : null)
                .brandName(product.getBrand() != null ? product.getBrand().getName() : null)
                .category(product.getCategory() != null ? CategoryResponse.builder()
                        .id(product.getCategory().getId())
                        .name(product.getCategory().getName())
                        .slug(product.getCategory().getSlug())
                        .build() : null)
                .productCode(product.getProductCode())
                .originPrice(product.getOriginPrice())
                .lowestPrice(lowestPrice)
                .averageRating(product.getAverageRating() != null ? product.getAverageRating() : 0.0)
                .totalReviews(product.getFeedbacks() != null ? product.getFeedbacks().size() : 0)
                .status(product.getStatus())
                .isFeatured(product.getIsFeatured())
                .specs(specs)
                .totalSold(product.getTotalSold() != null ? product.getTotalSold() : 0)
                .createdAt(product.getCreatedAt())
                .mainImageUrl(mainImageUrl)
                .outOfStock(totalStock <= 0)
                .build();
    }

    private Map<UUID, Long> buildVariantSoldMap(List<UUID> variantIds) {
        if (variantIds == null || variantIds.isEmpty()) {
            return Map.of();
        }

        List<UUID> dedupedVariantIds = variantIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (dedupedVariantIds.isEmpty()) {
            return Map.of();
        }

        return orderItemRepository.sumSoldQuantityByVariantIds(dedupedVariantIds).stream()
                .collect(Collectors.toMap(
                        row -> (UUID) row[0],
                        row -> ((Number) row[1]).longValue(),
                        Long::sum));
    }

    private Map<UUID, Long> buildVariantReturnedMap(List<UUID> variantIds) {
        if (variantIds == null || variantIds.isEmpty()) {
            return Map.of();
        }

        List<UUID> dedupedVariantIds = variantIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (dedupedVariantIds.isEmpty()) {
            return Map.of();
        }

        return returnItemRepository.sumReturnedQuantityByVariantIds(dedupedVariantIds).stream()
                .collect(Collectors.toMap(
                        row -> (UUID) row[0],
                        row -> ((Number) row[1]).longValue(),
                        Long::sum));
    }

    private ProductResponse mapToDetailedResponse(Product product) {
        List<UUID> variantIds = product.getVariants().stream()
                .map(ProductVariant::getId)
                .toList();
        Map<UUID, Long> soldByVariantId = buildVariantSoldMap(variantIds);
        Map<UUID, Long> returnedByVariantId = buildVariantReturnedMap(variantIds);
        return mapToDetailedResponse(product, soldByVariantId, returnedByVariantId);
    }

    private ProductResponse mapToDetailedResponse(
            Product product,
            Map<UUID, Long> soldByVariantId,
            Map<UUID, Long> returnedByVariantId
    ) {
        ProductResponse response = mapToResponse(product);
        Comparator<ProductImage> imageComparator = Comparator
                .comparing((ProductImage img) -> !Boolean.TRUE.equals(img.getIsPrimary()))
                .thenComparing(img -> img.getSortOrder() == null ? Integer.MAX_VALUE : img.getSortOrder())
                .thenComparing(img -> img.getId() == null ? "" : img.getId().toString());

        List<CategorySpecAttribute> specMappings = getSpecSchema(product.getCategory());
        List<ProductResponse.SpecSchemaResponse> specSchema = specMappings.stream()
                .map(mapping -> ProductResponse.SpecSchemaResponse.builder()
                        .id(mapping.getSpecAttribute().getId())
                        .name(mapping.getSpecAttribute().getName())
                        .code(mapping.getSpecAttribute().getCode())
                        .hint(mapping.getEffectiveHint())
                        .sortOrder(mapping.getSortOrder())
                        .build())
                .collect(Collectors.toList());

        List<CategoryVariantAttribute> variantMappings = getVariantSchema(product.getCategory());
        Map<UUID, Integer> variantSortOrderByAttributeId = variantMappings.stream()
                .collect(Collectors.toMap(
                        mapping -> mapping.getVariantAttribute().getId(),
                        CategoryVariantAttribute::getSortOrder,
                        (first, second) -> first));
        Map<UUID, Set<UUID>> usedOptionIdsByAttributeId = product.getVariants().stream()
                .flatMap(variant -> variant.getAttributeValues().stream())
                .collect(Collectors.groupingBy(
                        value -> value.getVariantAttribute().getId(),
                        Collectors.mapping(value -> value.getOption().getId(), Collectors.toSet())));

        List<ProductResponse.VariantAttributeSchemaResponse> variantSchema = variantMappings.stream()
                .map(mapping -> {
                    UUID attributeId = mapping.getVariantAttribute().getId();
                    Set<UUID> usedOptionIds = usedOptionIdsByAttributeId.get(attributeId);
                    return ProductResponse.VariantAttributeSchemaResponse.builder()
                            .id(attributeId)
                            .name(mapping.getVariantAttribute().getName())
                            .code(mapping.getVariantAttribute().getCode())
                            .sortOrder(mapping.getSortOrder())
                            .options(mapping.getVariantAttribute().getOptions().stream()
                                    .filter(option -> usedOptionIds == null
                                            || usedOptionIds.isEmpty()
                                            || usedOptionIds.contains(option.getId()))
                                    .sorted(Comparator.comparing(VariantAttributeOption::getSortOrder))
                                    .map(option -> ProductResponse.VariantOptionResponse.builder()
                                            .id(option.getId())
                                            .label(option.getLabel())
                                            .code(option.getCode())
                                            .sortOrder(option.getSortOrder())
                                            .active(option.getActive())
                                            .build())
                                    .collect(Collectors.toList()))
                            .build();
                })
                .collect(Collectors.toList());

        List<ProductVariantResponse> variants = product.getVariants().stream()
                .map(variant -> {
                    List<ProductImageResponse> variantImages = variant.getImages().stream()
                            .sorted(imageComparator)
                            .map(img -> ProductImageResponse.builder()
                                    .id(img.getId())
                                    .imageUrl(img.getImageUrl())
                                    .altText(img.getAltText())
                                    .sortOrder(img.getSortOrder())
                                    .isPrimary(img.getIsPrimary())
                                    .variantId(variant.getId())
                                    .build())
                            .collect(Collectors.toList());

                    List<ProductVariantResponse.VariantAttributeValueResponse> attributes = variant.getAttributeValues().stream()
                            .sorted(Comparator.comparing(value ->
                                    variantSortOrderByAttributeId.getOrDefault(value.getVariantAttribute().getId(), Integer.MAX_VALUE)))
                            .map(value -> ProductVariantResponse.VariantAttributeValueResponse.builder()
                                    .variantAttributeId(value.getVariantAttribute().getId())
                                    .attributeName(value.getVariantAttribute().getName())
                                    .attributeCode(value.getVariantAttribute().getCode())
                                    .variantAttributeName(value.getVariantAttribute().getName())
                                    .variantAttributeCode(value.getVariantAttribute().getCode())
                                    .optionId(value.getOption().getId())
                                    .optionLabel(value.getOption().getLabel())
                                    .optionCode(value.getOption().getCode())
                                    .build())
                            .collect(Collectors.toList());

                    return ProductVariantResponse.builder()
                            .id(variant.getId())
                            .sku(variant.getSku())
                            .displayName(variant.getVariantName())
                            .variantName(variant.getVariantName())
                            .variantSignature(variant.getVariantSignature())
                            .price(variant.getPrice())
                            .compareAtPrice(variant.getCompareAtPrice())
                            .stockQuantity(variant.getStock())
                            .grossSoldQty(soldByVariantId.getOrDefault(variant.getId(), 0L))
                            .returnedQty(returnedByVariantId.getOrDefault(variant.getId(), 0L))
                            .netSoldQty(Math.max(
                                    soldByVariantId.getOrDefault(variant.getId(), 0L)
                                            - returnedByVariantId.getOrDefault(variant.getId(), 0L),
                                    0L))
                            .active(variant.getActive())
                            .selections(attributes)
                            .attributes(attributes)
                            .images(variantImages)
                            .build();
                })
                .collect(Collectors.toList());

        List<ProductImageResponse> productImages = product.getImages().stream()
                .filter(img -> img.getVariant() == null)
                .sorted(imageComparator)
                .map(img -> ProductImageResponse.builder()
                        .id(img.getId())
                        .imageUrl(img.getImageUrl())
                        .altText(img.getAltText())
                        .sortOrder(img.getSortOrder())
                        .isPrimary(img.getIsPrimary())
                        .variantId(null)
                        .build())
                .collect(Collectors.toList());

        response.setVariantSchema(variantSchema);
        response.setSpecSchema(specSchema);
        response.setSpecs(buildSpecResponses(product, specMappings));
        response.setVariants(variants);
        response.setImages(productImages);
        return response;
    }

    private List<ProductResponse.ProductSpecValueResponse> buildSpecResponses(
            Product product,
            List<CategorySpecAttribute> specMappings
    ) {
        Map<UUID, CategorySpecAttribute> schemaBySpecId = specMappings.stream()
                .collect(Collectors.toMap(
                        mapping -> mapping.getSpecAttribute().getId(),
                        mapping -> mapping,
                        (first, second) -> first));
        Map<UUID, Integer> specSortOrderBySpecId = specMappings.stream()
                .collect(Collectors.toMap(
                        mapping -> mapping.getSpecAttribute().getId(),
                        CategorySpecAttribute::getSortOrder,
                        (first, second) -> first));

        return product.getSpecValues().stream()
                .sorted(Comparator.comparing(value ->
                        specSortOrderBySpecId.getOrDefault(
                                value.getSpecAttribute().getId(),
                                value.getSpecAttribute().getSortOrder())))
                .map(value -> {
                    CategorySpecAttribute mapping = schemaBySpecId.get(value.getSpecAttribute().getId());
                    Integer sortOrder = mapping != null
                            ? mapping.getSortOrder()
                            : value.getSpecAttribute().getSortOrder();
                    String code = value.getSpecAttribute().getCode();
                    return ProductResponse.ProductSpecValueResponse.builder()
                            .specAttributeId(value.getSpecAttribute().getId())
                            .name(value.getSpecAttribute().getName())
                            .code(code)
                            .specCode(code)
                            .value(value.getValueText())
                            .sortOrder(sortOrder)
                            .build();
                })
                .collect(Collectors.toList());
    }

    private List<CategoryVariantAttribute> getVariantSchema(Category category) {
        if (category == null || category.getCategoryVariantAttributes() == null) {
            return List.of();
        }
        return category.getCategoryVariantAttributes().stream()
                .sorted(Comparator.comparing(CategoryVariantAttribute::getSortOrder))
                .collect(Collectors.toList());
    }

    private List<CategorySpecAttribute> getSpecSchema(Category category) {
        if (category == null || category.getCategorySpecAttributes() == null) {
            return List.of();
        }
        return category.getCategorySpecAttributes().stream()
                .sorted(Comparator.comparing(CategorySpecAttribute::getSortOrder))
                .collect(Collectors.toList());
    }

    private String toSlug(String input) {
        if (input == null) return "";
        String nowhitespace = WHITE_SPACE.matcher(input).replaceAll("-");
        String normalized = Normalizer.normalize(nowhitespace, Normalizer.Form.NFD);
        String slug = NONLATIN.matcher(normalized).replaceAll("");
        return slug.toLowerCase(Locale.ENGLISH).replaceAll("-{2,}", "-").replaceAll("^-|-$", "");
    }

    private String normalizeCode(String input) {
        if (input == null) return "";
        String nowhitespace = WHITE_SPACE.matcher(input.trim()).replaceAll("-");
        String normalized = Normalizer.normalize(nowhitespace, Normalizer.Form.NFD);
        String value = NONLATIN.matcher(normalized).replaceAll("");
        return value.toUpperCase(Locale.ENGLISH).replaceAll("-{2,}", "-").replaceAll("^-|-$", "");
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> getAdminProducts(String keyword, UUID categoryId, String status, int page, int size, String sortBy, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name()) ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PaginationConstant.of(page, size, sort);

        Boolean active = null;
        if (status != null && !status.isBlank()) {
            active = ProductStatus.ACTIVE.name().equalsIgnoreCase(status);
        }

        Specification<Product> spec = ProductSpecification.filter(keyword, categoryId, null, null, null, null, active);
        Page<Product> products = productRepository.findAll(spec, pageable);
        List<UUID> variantIds = products.getContent().stream()
                .flatMap(product -> product.getVariants().stream())
                .map(ProductVariant::getId)
                .filter(Objects::nonNull)
                .toList();
        Map<UUID, Long> soldByVariantId = buildVariantSoldMap(variantIds);
        Map<UUID, Long> returnedByVariantId = buildVariantReturnedMap(variantIds);
        return PageResponse.of(products.map(product -> mapToDetailedResponse(product, soldByVariantId, returnedByVariantId)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponse> getFeaturedProducts(int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return productRepository.findByStatusAndIsFeaturedTrue(ProductStatus.ACTIVE, pageable)
                .stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponse> getNewArrivals(int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return productRepository.findByStatusOrderByCreatedAtDesc(ProductStatus.ACTIVE, pageable)
                .stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponse> getTopRatedProducts(int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return productRepository.findTopRatedProducts(pageable)
                .stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    private record AttributeSelectionContext(UUID variantAttributeId, VariantAttributeOption option) {}

    private record VariantComputed(
            String sku,
            String displayName,
            String signature,
            List<AttributeSelectionContext> selections
    ) {
        List<ProductVariantAttributeValue> attributeValues(ProductVariant variant) {
            return selections.stream()
                    .map(selection -> ProductVariantAttributeValue.builder()
                            .productVariant(variant)
                            .variantAttribute(selection.option().getVariantAttribute())
                            .option(selection.option())
                            .build())
                    .collect(Collectors.toList());
        }
    }
}
