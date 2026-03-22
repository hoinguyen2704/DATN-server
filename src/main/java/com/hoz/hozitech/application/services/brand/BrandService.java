package com.hoz.hozitech.application.services.brand;

import com.hoz.hozitech.domain.dtos.request.BrandRequest;
import com.hoz.hozitech.domain.dtos.response.BrandResponse;
import com.hoz.hozitech.domain.dtos.response.PageResponse;

import java.util.List;
import java.util.UUID;

public interface BrandService {

    // Public
    List<BrandResponse> getAllBrands();

    BrandResponse getBrandBySlug(String slug);

    BrandResponse getBrandById(UUID id);

    // Admin
    PageResponse<BrandResponse> getAdminBrands(String keyword, int page, int size);

    BrandResponse createBrand(BrandRequest request);

    BrandResponse updateBrand(UUID id, BrandRequest request);

    void deleteBrand(UUID id);
}
