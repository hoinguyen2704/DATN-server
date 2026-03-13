package com.hoz.hozitech.application.services;

import com.hoz.hozitech.domain.dtos.request.BrandRequest;
import com.hoz.hozitech.domain.dtos.response.BrandResponse;

import java.util.List;
import java.util.UUID;

public interface BrandService {

    // Public
    List<BrandResponse> getAllBrands();

    BrandResponse getBrandBySlug(String slug);

    // Admin
    BrandResponse createBrand(BrandRequest request);

    BrandResponse updateBrand(UUID id, BrandRequest request);

    void deleteBrand(UUID id);
}
