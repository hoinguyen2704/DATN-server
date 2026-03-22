package com.hoz.hozitech.application.services.banner;

import com.hoz.hozitech.domain.dtos.request.BannerRequest;
import com.hoz.hozitech.domain.dtos.response.BannerResponse;

import java.util.List;
import java.util.UUID;

public interface BannerService {
    List<BannerResponse> getAllPublicBanners();
    List<BannerResponse> getAllAdminBanners();
    BannerResponse createBanner(BannerRequest request);
    BannerResponse updateBanner(UUID id, BannerRequest request);
    void deleteBanner(UUID id);
}
