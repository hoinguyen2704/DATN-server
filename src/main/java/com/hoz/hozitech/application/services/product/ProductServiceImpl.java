package com.hoz.hozitech.application.services.product;

import com.hoz.hozitech.application.constant.StatusConstant;
import com.hoz.hozitech.application.constant.PaginationConstant;
import com.hoz.hozitech.application.repositories.BrandRepository;
import com.hoz.hozitech.application.repositories.CategoryRepository;
import com.hoz.hozitech.application.repositories.ProductRepository;
import com.hoz.hozitech.application.specifications.ProductSpecification;
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
import com.hoz.hozitech.domain.entities.Product;
import com.hoz.hozitech.domain.entities.ProductImage;
import com.hoz.hozitech.domain.entities.ProductVariant;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;

import java.text.Normalizer;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;
    private final EntityManager entityManager;
    
    private static final Pattern NONLATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITE_SPACE = Pattern.compile("[\\s]");

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> searchProducts(String keyword, String categorySlug, String brand, int page, int size, String sortBy, String sortDir) {
        
        Sort sort;
        if ("popular".equalsIgnoreCase(sortBy)) {
            sort = Sort.by(
                Sort.Order.desc("hasStock"),
                Sort.Order.desc("totalSold"),
                Sort.Order.desc("createdAt")
            );
        } else {
            Sort.Direction direction = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name()) ? Sort.Direction.ASC : Sort.Direction.DESC;
            sort = Sort.by(
                Sort.Order.desc("hasStock"),
                new Sort.Order(direction, sortBy)
            );
        }

        Pageable pageable = PaginationConstant.of(page, size, sort);

        UUID categoryId = null;
        if (categorySlug != null && !categorySlug.isBlank()) {
            Category category = categoryRepository.findBySlug(categorySlug)
                    .orElseThrow(() -> new IllegalArgumentException("Category not found"));
            categoryId = category.getId();
        }

        Specification<Product> spec = ProductSpecification.filter(
                keyword, categoryId, brand, null, null, null, true);

        Page<Product> products = productRepository.findAll(spec, pageable);
        return PageResponse.of(products.map(this::mapToResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getProductBySlug(String slug) {
        Product product = productRepository.findBySlug(slug)
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));
        return mapToDetailedResponse(product);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getProductById(UUID id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));
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
            throw new IllegalArgumentException("Product name already exists");
        }

        String slug = toSlug(request.getName());
        if (productRepository.existsBySlug(slug)) {
            slug += "-" + UUID.randomUUID().toString().substring(0, 6);
        }

        Product product = Product.builder()
                .name(request.getName())
                .slug(slug)
                .description(request.getDescription())
                .brand(brand)
                .originPrice(request.getOriginPrice())
                .specsJson(request.getSpecsJson())
                .status(request.getStatus() != null ? request.getStatus() : StatusConstant.PRODUCT_ACTIVE)
                .isFeatured(request.getIsFeatured() != null ? request.getIsFeatured() : false)
                .category(category)
                .variants(new ArrayList<>())
                .images(new ArrayList<>())
                .build();

        if (request.getImages() != null) {
            for (ProductImageRequest imgReq : request.getImages()) {
                ProductImage img = ProductImage.builder()
                        .imageUrl(imgReq.getImageUrl())
                        .isPrimary(imgReq.getIsPrimary() != null ? imgReq.getIsPrimary() : false)
                        .product(product)
                        .build();
                product.getImages().add(img);
            }
        }

        if (request.getVariants() != null) {
            for (ProductVariantRequest varReq : request.getVariants()) {
                ProductVariant variant = ProductVariant.builder()
                        .sku(varReq.getSku())
                        .variantName(varReq.getVariantName())
                        .price(varReq.getPrice())
                        .compareAtPrice(varReq.getCompareAtPrice())
                        .stock(varReq.getStock() != null ? varReq.getStock() : 0)
                        .active(varReq.getActive() != null ? varReq.getActive() : true)
                        .product(product)
                        .images(new ArrayList<>())
                        .build();

                if (varReq.getImages() != null) {
                    for (ProductImageRequest vImgReq : varReq.getImages()) {
                        ProductImage vImg = ProductImage.builder()
                                .imageUrl(vImgReq.getImageUrl())
                                .isPrimary(vImgReq.getIsPrimary() != null ? vImgReq.getIsPrimary() : false)
                                .variant(variant)
                                .build();
                        variant.getImages().add(vImg);
                    }
                }
                product.getVariants().add(variant);
            }
        }

        return mapToDetailedResponse(productRepository.save(product));
    }

    @Override
    @Transactional
    public ProductResponse updateProduct(UUID id, ProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new IllegalArgumentException("Category not found"));

        if (!product.getName().equals(request.getName()) && productRepository.existsByName(request.getName())) {
             throw new IllegalArgumentException("Product name already exists");
        }

        product.setName(request.getName());
        product.setDescription(request.getDescription());
        Brand brand = brandRepository.findById(request.getBrandId())
                .orElseThrow(() -> new IllegalArgumentException("Brand not found"));
        product.setBrand(brand);
        product.setOriginPrice(request.getOriginPrice());
        product.setSpecsJson(request.getSpecsJson());
        product.setCategory(category);
        if (request.getStatus() != null) product.setStatus(request.getStatus());
        if (request.getIsFeatured() != null) product.setIsFeatured(request.getIsFeatured());

        // Do NOT clear variants completely, this breaks historical order_items foreign keys!
        // Instead, merge variants based on SKU.
        if (request.getVariants() != null) {
            Map<String, ProductVariant> existingVariantsMap = product.getVariants().stream()
                    .collect(Collectors.toMap(ProductVariant::getSku, v -> v, (v1, v2) -> v1)); // handle duplicate skus

            for (ProductVariantRequest varReq : request.getVariants()) {
                ProductVariant variant = existingVariantsMap.get(varReq.getSku());

                if (variant != null) {
                    // 1. Update existing variant gracefully
                    variant.setVariantName(varReq.getVariantName());
                    variant.setPrice(varReq.getPrice());
                    variant.setCompareAtPrice(varReq.getCompareAtPrice());
                    variant.setStock(varReq.getStock() != null ? varReq.getStock() : 0);
                    variant.setActive(varReq.getActive() != null ? varReq.getActive() : true);
                    
                    variant.getImages().clear(); // Safe to replace images as they have no incoming foreign keys
                    if (varReq.getImages() != null) {
                        for (ProductImageRequest vImgReq : varReq.getImages()) {
                            ProductImage vImg = ProductImage.builder()
                                    .imageUrl(vImgReq.getImageUrl())
                                    .isPrimary(vImgReq.getIsPrimary() != null ? vImgReq.getIsPrimary() : false)
                                    .variant(variant)
                                    .build();
                            variant.getImages().add(vImg);
                        }
                    }
                    existingVariantsMap.remove(varReq.getSku()); // Mark as processed
                } else {
                    // 2. Create entirely new variant
                    ProductVariant newVariant = ProductVariant.builder()
                            .sku(varReq.getSku())
                            .variantName(varReq.getVariantName())
                            .price(varReq.getPrice())
                            .compareAtPrice(varReq.getCompareAtPrice())
                            .stock(varReq.getStock() != null ? varReq.getStock() : 0)
                            .active(varReq.getActive() != null ? varReq.getActive() : true)
                            .product(product)
                            .images(new ArrayList<>())
                            .build();

                    if (varReq.getImages() != null) {
                        for (ProductImageRequest vImgReq : varReq.getImages()) {
                            ProductImage vImg = ProductImage.builder()
                                    .imageUrl(vImgReq.getImageUrl())
                                    .isPrimary(vImgReq.getIsPrimary() != null ? vImgReq.getIsPrimary() : false)
                                    .variant(newVariant)
                                    .build();
                            newVariant.getImages().add(vImg);
                        }
                    }
                    product.getVariants().add(newVariant);
                }
            }

            // 3. For any variant that remains in the map, it means the Admin removed it from the UI.
            // We MUST NOT delete it from product.getVariants() to prevent ForeignKey violations on old Orders.
            // Soft-deactivate it instead.
            for (ProductVariant removedVariant : existingVariantsMap.values()) {
                removedVariant.setActive(false);
                removedVariant.setStock(0);
            }
        }
        return mapToDetailedResponse(productRepository.save(product));
    }

    @Override
    @Transactional
    public void deleteProduct(UUID id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));

        // Critical Check: Prevent deletion if product is tied to historical orders
        Number orderItemCount = (Number) entityManager.createNativeQuery(
                "SELECT COUNT(*) FROM order_items WHERE variant_id IN (SELECT id FROM product_variants WHERE product_id = :pid)")
                .setParameter("pid", id)
                .getSingleResult();

        if (orderItemCount.longValue() > 0) {
            throw new IllegalArgumentException("Sản phẩm này đã phát sinh đơn hàng, không thể xoá cứng. Vui lòng chuyển trạng thái thành Bản Nháp hoặc Đã Ẩn!");
        }

        // Preemptively wipe carts containing any variants of this product to bypass strict postgres FK restrictions
        entityManager.createNativeQuery("DELETE FROM carts WHERE variant_id IN (SELECT id FROM product_variants WHERE product_id = :pid)")
                .setParameter("pid", id)
                .executeUpdate();

        // Also purge any wishlist containing this product
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
        
        if (StatusConstant.PRODUCT_ACTIVE.equalsIgnoreCase(product.getStatus())) {
            product.setStatus(StatusConstant.PRODUCT_DRAFT);
        } else {
            product.setStatus(StatusConstant.PRODUCT_ACTIVE);
        }
        return mapToDetailedResponse(productRepository.save(product));
    }

    private ProductResponse mapToResponse(Product product) {
        String mainImageUrl = product.getImages().stream()
                .filter(img -> Boolean.TRUE.equals(img.getIsPrimary()))
                .map(ProductImage::getImageUrl)
                .findFirst()
                .orElse(product.getImages().isEmpty() ? null : product.getImages().get(0).getImageUrl());

        int totalStock = product.getVariants().stream().mapToInt(ProductVariant::getStock).sum();

        java.math.BigDecimal lowestPrice = product.getVariants().stream()
                .map(ProductVariant::getPrice)
                .filter(java.util.Objects::nonNull)
                .min(java.math.BigDecimal::compareTo)
                .orElse(product.getOriginPrice());

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
                .originPrice(product.getOriginPrice())
                .lowestPrice(lowestPrice)
                .status(product.getStatus())
                .isFeatured(product.getIsFeatured())
                .specsJson(product.getSpecsJson())
                .totalSold(product.getTotalSold() != null ? product.getTotalSold() : 0)
                .createdAt(product.getCreatedAt())
                .mainImageUrl(mainImageUrl)
                .outOfStock(totalStock <= 0)
                .build();
    }

    private ProductResponse mapToDetailedResponse(Product product) {
        ProductResponse response = mapToResponse(product);
        
        List<ProductVariantResponse> variants = product.getVariants().stream().map((ProductVariant v) -> {
            List<ProductImageResponse> vImages = v.getImages().stream().map((ProductImage img) ->
                    ProductImageResponse.builder()
                            .id(img.getId())
                            .imageUrl(img.getImageUrl())
                            .isPrimary(img.getIsPrimary())
                            .build()
            ).collect(Collectors.toList());

            String vName = v.getVariantName() != null ? v.getVariantName() : "";
            
            return ProductVariantResponse.builder()
                    .id(v.getId())
                    .sku(v.getSku())
                    .variantName(vName)
                    .color(vName.contains("-") ? vName.split("-")[0].trim() : null)
                    .storageCapacity(vName.contains("-") && vName.split("-").length > 1 ? vName.split("-")[1].trim() : null)
                    .price(v.getPrice())
                    .compareAtPrice(v.getCompareAtPrice())
                    .stockQuantity(v.getStock())
                    .active(v.getActive())
                    .images(vImages)
                    .build();
        }).collect(Collectors.toList());
        
        response.setVariants(variants);

        // Map product-level images (not tied to any variant)
        List<ProductImageResponse> productImages = product.getImages().stream()
                .filter(img -> img.getVariant() == null)
                .map(img -> ProductImageResponse.builder()
                        .id(img.getId())
                        .imageUrl(img.getImageUrl())
                        .isPrimary(img.getIsPrimary())
                        .build())
                .collect(Collectors.toList());
        response.setImages(productImages);

        return response;
    }

    private String toSlug(String input) {
        if (input == null) return "";
        String nowhitespace = WHITE_SPACE.matcher(input).replaceAll("-");
        String normalized = Normalizer.normalize(nowhitespace, Normalizer.Form.NFD);
        String slug = NONLATIN.matcher(normalized).replaceAll("");
        return slug.toLowerCase(Locale.ENGLISH).replaceAll("-{2,}", "-").replaceAll("^-|-$", "");
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> getAdminProducts(String keyword, java.util.UUID categoryId, String status, int page, int size, String sortBy, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name()) ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PaginationConstant.of(page, size, sort);

        // Map status param to Boolean: null = all, ACTIVE = true, INACTIVE/DRAFT = false
        Boolean active = null;
        if (status != null && !status.isBlank()) {
            active = StatusConstant.PRODUCT_ACTIVE.equalsIgnoreCase(status);
        }

        Specification<Product> spec = ProductSpecification.filter(keyword, categoryId, null, null, null, null, active);
        Page<Product> products = productRepository.findAll(spec, pageable);
        return PageResponse.of(products.map(this::mapToDetailedResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponse> getFeaturedProducts(int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return productRepository.findByStatusAndIsFeaturedTrue(StatusConstant.PRODUCT_ACTIVE, pageable)
                .stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponse> getNewArrivals(int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return productRepository.findByStatusOrderByCreatedAtDesc(StatusConstant.PRODUCT_ACTIVE, pageable)
                .stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponse> getTopRatedProducts(int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return productRepository.findTopRatedProducts(pageable)
                .stream().map(this::mapToResponse).collect(Collectors.toList());
    }
}
