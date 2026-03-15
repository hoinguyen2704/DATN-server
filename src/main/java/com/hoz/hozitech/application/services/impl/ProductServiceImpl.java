package com.hoz.hozitech.application.services.impl;

import com.hoz.hozitech.application.repositories.BrandRepository;
import com.hoz.hozitech.application.repositories.CategoryRepository;
import com.hoz.hozitech.application.repositories.ProductRepository;
import com.hoz.hozitech.application.services.ProductService;
import com.hoz.hozitech.application.specifications.ProductSpecification;
import com.hoz.hozitech.domain.dtos.request.ProductImageRequest;
import com.hoz.hozitech.domain.dtos.request.ProductRequest;
import com.hoz.hozitech.domain.dtos.request.ProductVariantRequest;
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
    
    private static final Pattern NONLATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITE_SPACE = Pattern.compile("[\\s]");

    @Override
    public PageResponse<ProductResponse> searchProducts(String keyword, String categorySlug, String brand, int page, int size, String sortBy, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name()) ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(page - 1, size, sort);

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
    public ProductResponse getProductBySlug(String slug) {
        Product product = productRepository.findBySlug(slug)
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));
        return mapToDetailedResponse(product);
    }

    @Override
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
                .status(request.getStatus() != null ? request.getStatus() : "ACTIVE")
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

        product.getImages().clear();
        product.getVariants().clear();

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
    public void deleteProduct(UUID id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));
        productRepository.delete(product);
    }

    @Override
    @Transactional
    public ProductResponse toggleProductStatus(UUID id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));
        
        if ("ACTIVE".equalsIgnoreCase(product.getStatus())) {
            product.setStatus("DRAFT");
        } else {
            product.setStatus("ACTIVE");
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

        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .slug(product.getSlug())
                .description(product.getDescription())
                .brandName(product.getBrand() != null ? product.getBrand().getName() : null)
                .originPrice(product.getOriginPrice())
                .status(product.getStatus())
                .isFeatured(product.getIsFeatured())
                .specsJson(product.getSpecsJson())
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
                    .stockQuantity(v.getStock())
                    .images(vImages)
                    .build();
        }).collect(Collectors.toList());
        
        response.setVariants(variants);
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
    public PageResponse<ProductResponse> getAdminProducts(String keyword, String status, int page, int size, String sortBy, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name()) ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page - 1, size, sort);

        // For admin: no additional status filter, return all products matching keyword
        Specification<Product> spec = ProductSpecification.filter(keyword, null, null, null, null, null, false);
        Page<Product> products = productRepository.findAll(spec, pageable);
        return PageResponse.of(products.map(this::mapToResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponse> getFeaturedProducts(int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return productRepository.findByStatusAndIsFeaturedTrue("ACTIVE", pageable)
                .stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponse> getNewArrivals(int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return productRepository.findByStatusOrderByCreatedAtDesc("ACTIVE", pageable)
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
