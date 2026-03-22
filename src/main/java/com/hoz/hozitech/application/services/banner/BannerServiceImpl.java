package com.hoz.hozitech.application.services.banner;

import com.hoz.hozitech.application.repositories.BannerRepository;
import com.hoz.hozitech.application.services.banner.BannerService;
import com.hoz.hozitech.domain.dtos.request.BannerRequest;
import com.hoz.hozitech.domain.dtos.response.BannerResponse;
import com.hoz.hozitech.domain.entities.Banner;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BannerServiceImpl implements BannerService {

    private final BannerRepository bannerRepository;

    @Override
    @Transactional(readOnly = true)
    public List<BannerResponse> getAllPublicBanners() {
        return bannerRepository.findByIsActiveTrue(Sort.by(Sort.Direction.ASC, "sortOrder"))
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<BannerResponse> getAllAdminBanners() {
        return bannerRepository.findAll(Sort.by(Sort.Direction.ASC, "sortOrder"))
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public BannerResponse createBanner(BannerRequest request) {
        Banner banner = Banner.builder()
                .title(request.getTitle())
                .imageUrl(request.getImageUrl())
                .targetUrl(request.getTargetUrl())
                .sortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0)
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .build();
        return mapToResponse(bannerRepository.save(banner));
    }

    @Override
    @Transactional
    public BannerResponse updateBanner(UUID id, BannerRequest request) {
        Banner banner = bannerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Banner not found"));

        if (request.getTitle() != null) banner.setTitle(request.getTitle());
        if (request.getImageUrl() != null) banner.setImageUrl(request.getImageUrl());
        if (request.getTargetUrl() != null) banner.setTargetUrl(request.getTargetUrl());
        if (request.getSortOrder() != null) banner.setSortOrder(request.getSortOrder());
        if (request.getIsActive() != null) banner.setIsActive(request.getIsActive());

        return mapToResponse(bannerRepository.save(banner));
    }

    @Override
    @Transactional
    public void deleteBanner(UUID id) {
        Banner banner = bannerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Banner not found"));
        bannerRepository.delete(banner);
    }

    private BannerResponse mapToResponse(Banner banner) {
        return BannerResponse.builder()
                .id(banner.getId())
                .title(banner.getTitle())
                .imageUrl(banner.getImageUrl())
                .targetUrl(banner.getTargetUrl())
                .sortOrder(banner.getSortOrder())
                .isActive(banner.getIsActive())
                .createdAt(banner.getCreatedAt())
                .build();
    }
}
