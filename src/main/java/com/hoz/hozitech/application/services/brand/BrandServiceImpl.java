package com.hoz.hozitech.application.services.brand;

import com.hoz.hozitech.application.repositories.BrandRepository;
import com.hoz.hozitech.application.services.brand.BrandService;
import com.hoz.hozitech.domain.dtos.request.BrandRequest;
import com.hoz.hozitech.domain.dtos.response.BrandResponse;
import com.hoz.hozitech.domain.dtos.response.PageResponse;
import com.hoz.hozitech.domain.entities.Brand;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BrandServiceImpl implements BrandService {

    private final BrandRepository brandRepository;

    private static final Pattern NONLATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITE_SPACE = Pattern.compile("[\\s]");

    @Override
    public List<BrandResponse> getAllBrands() {
        return brandRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public BrandResponse getBrandBySlug(String slug) {
        Brand brand = brandRepository.findBySlug(slug)
                .orElseThrow(() -> new IllegalArgumentException("Brand not found"));
        return mapToResponse(brand);
    }

    @Override
    public BrandResponse getBrandById(UUID id) {
        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Brand not found with id: " + id));
        return mapToResponse(brand);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<BrandResponse> getAdminBrands(String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by("createdAt").descending());
        Page<Brand> brands;
        if (keyword != null && !keyword.isBlank()) {
            brands = brandRepository.findByNameContainingIgnoreCase(keyword, pageable);
        } else {
            brands = brandRepository.findAll(pageable);
        }
        return PageResponse.of(brands.map(this::mapToResponse));
    }

    @Override
    @Transactional
    public BrandResponse createBrand(BrandRequest request) {
        String slug = toSlug(request.getName());
        if (brandRepository.findBySlug(slug).isPresent()) {
            throw new IllegalArgumentException("Brand with this name already exists");
        }

        Brand brand = Brand.builder()
                .name(request.getName())
                .slug(slug)
                .logoUrl(request.getLogoUrl())
                .build();

        return mapToResponse(brandRepository.save(brand));
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

        return mapToResponse(brandRepository.save(brand));
    }

    @Override
    @Transactional
    public void deleteBrand(UUID id) {
        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Brand not found"));

        if (brand.getProducts() != null && !brand.getProducts().isEmpty()) {
            throw new IllegalArgumentException("Cannot delete brand with associated products");
        }

        brandRepository.delete(brand);
    }

    private BrandResponse mapToResponse(Brand brand) {
        return BrandResponse.builder()
                .id(brand.getId())
                .name(brand.getName())
                .slug(brand.getSlug())
                .logoUrl(brand.getLogoUrl())
                .productCount(brand.getProducts() != null ? brand.getProducts().size() : 0)
                .build();
    }

    private String toSlug(String input) {
        if (input == null) return "";
        String nowhitespace = WHITE_SPACE.matcher(input).replaceAll("-");
        String normalized = Normalizer.normalize(nowhitespace, Normalizer.Form.NFD);
        String slug = NONLATIN.matcher(normalized).replaceAll("");
        return slug.toLowerCase(Locale.ENGLISH).replaceAll("-{2,}", "-").replaceAll("^-|-$", "");
    }
}
