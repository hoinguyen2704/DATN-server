package com.hoz.hozitech.application.services.brand;

import java.text.Normalizer;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hoz.hozitech.application.constant.PaginationConstant;
import com.hoz.hozitech.application.repositories.BrandRepository;
import com.hoz.hozitech.application.repositories.ProductRepository;
import com.hoz.hozitech.application.services.notification.AdminNotificationService;
import com.hoz.hozitech.application.services.notification.AdminNotificationTemplates;
import com.hoz.hozitech.config.exceptions.ConflictException;
import com.hoz.hozitech.config.exceptions.InvalidParamException;
import com.hoz.hozitech.domain.dtos.request.BrandRequest;
import com.hoz.hozitech.domain.dtos.response.BrandResponse;
import com.hoz.hozitech.domain.dtos.response.PageResponse;
import com.hoz.hozitech.domain.entities.Brand;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BrandServiceImpl implements BrandService {

    private final BrandRepository brandRepository;
    private final ProductRepository productRepository;
    private final AdminNotificationService adminNotificationService;

    private static final Pattern NONLATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITE_SPACE = Pattern.compile("[\\s]");

    @Override
    public List<BrandResponse> getAllBrands() {
        List<Brand> brands = brandRepository.findAll();
        Map<UUID, Long> productCountByBrandId = loadProductCountByBrandIds(brands.stream()
                .map(Brand::getId)
                .toList());
        return brands.stream()
                .map(brand -> mapToResponse(brand, productCountByBrandId.getOrDefault(brand.getId(), 0L)))
                .collect(Collectors.toList());
    }

    @Override
    public BrandResponse getBrandBySlug(String slug) {
        Brand brand = brandRepository.findBySlug(slug)
                .orElseThrow(() -> new IllegalArgumentException("Brand not found"));
        return mapToResponse(brand, loadProductCountByBrandIds(List.of(brand.getId())).getOrDefault(brand.getId(), 0L));
    }

    @Override
    public BrandResponse getBrandById(UUID id) {
        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> new InvalidParamException("Brand not found with id: " + id)
                        .withMessageKey("error.brand_not_found_with_id", id));
        return mapToResponse(brand, loadProductCountByBrandIds(List.of(brand.getId())).getOrDefault(brand.getId(), 0L));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<BrandResponse> getAdminBrands(String keyword, UUID categoryId, int page, int size) {
        Pageable pageable = PaginationConstant.of(page, size);
        Page<Brand> brands;
        boolean hasKeyword = keyword != null && !keyword.isBlank();
        if (categoryId != null) {
            if (hasKeyword) {
                brands = brandRepository.findByKeywordAndCategoryId(keyword, categoryId, pageable);
            } else {
                brands = brandRepository.findByCategoryId(categoryId, pageable);
            }
        } else if (hasKeyword) {
            brands = brandRepository.findByNameContainingIgnoreCase(keyword, pageable);
        } else {
            brands = brandRepository.findAll(pageable);
        }
        Map<UUID, Long> productCountByBrandId = loadProductCountByBrandIds(brands.getContent().stream()
                .map(Brand::getId)
                .toList());
        return PageResponse.of(brands.map(brand -> mapToResponse(brand, productCountByBrandId.getOrDefault(brand.getId(), 0L))));
    }

    @Override
    @Transactional
    public BrandResponse createBrand(BrandRequest request) {
        String slug = toSlug(request.getName());
        if (brandRepository.findBySlug(slug).isPresent()) {
            throw new ConflictException("Brand with this name already exists");
        }

        Brand brand = Brand.builder()
                .name(request.getName())
                .slug(slug)
                .logoUrl(request.getLogoUrl())
                .build();

        Brand saved = brandRepository.save(brand);
        adminNotificationService.createShared(AdminNotificationTemplates.brandCreated(saved), true);
        return mapToResponse(saved, 0L);
    }

    @Override
    @Transactional
    public BrandResponse updateBrand(UUID id, BrandRequest request) {
        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Brand not found"));

        brand.setName(request.getName());
        brand.setSlug(toSlug(request.getName()));
        if (request.getLogoUrl() != null) {
            brand.setLogoUrl(request.getLogoUrl());
        }

        Brand saved = brandRepository.save(brand);
        adminNotificationService.createShared(AdminNotificationTemplates.brandUpdated(saved), true);
        return mapToResponse(saved, loadProductCountByBrandIds(List.of(saved.getId())).getOrDefault(saved.getId(), 0L));
    }

    @Override
    @Transactional
    public void deleteBrand(UUID id) {
        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Brand not found"));

        if (productRepository.existsByBrandId(id)) {
            throw new InvalidParamException("Cannot delete brand with associated products");
        }

        brandRepository.delete(brand);
    }

    private BrandResponse mapToResponse(Brand brand, long productCount) {
        return BrandResponse.builder()
                .id(brand.getId())
                .name(brand.getName())
                .slug(brand.getSlug())
                .logoUrl(brand.getLogoUrl())
                .productCount(productCount)
                .build();
    }

    private Map<UUID, Long> loadProductCountByBrandIds(Collection<UUID> brandIds) {
        if (brandIds == null || brandIds.isEmpty()) {
            return Map.of();
        }
        return brandRepository.countProductsByBrandIds(brandIds).stream()
                .collect(Collectors.toMap(
                        row -> (UUID) row[0],
                        row -> ((Number) row[1]).longValue()));
    }

    private String toSlug(String input) {
        if (input == null) return "";
        String nowhitespace = WHITE_SPACE.matcher(input).replaceAll("-");
        String normalized = Normalizer.normalize(nowhitespace, Normalizer.Form.NFD)
                .replace("\u0111", "d")
                .replace("\u0110", "D");
        String slug = NONLATIN.matcher(normalized).replaceAll("");
        return slug.toLowerCase(Locale.ENGLISH).replaceAll("-{2,}", "-").replaceAll("^-|-$", "");
    }
}
