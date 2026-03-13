package com.hoz.hozitech.web.controllers;

import com.hoz.hozitech.application.services.ArticleService;
import com.hoz.hozitech.application.services.BannerService;
import com.hoz.hozitech.config.security.CustomUserDetails;
import com.hoz.hozitech.domain.dtos.request.ArticleRequest;
import com.hoz.hozitech.domain.dtos.request.BannerRequest;
import com.hoz.hozitech.domain.dtos.response.ApiResponse;
import com.hoz.hozitech.domain.dtos.response.ArticleResponse;
import com.hoz.hozitech.domain.dtos.response.BannerResponse;
import com.hoz.hozitech.domain.dtos.response.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("${api.prefix-admin}/cms")
@RequiredArgsConstructor
public class AdminCmsController {

    private final BannerService bannerService;
    private final ArticleService articleService;

    // --- BANNERS ---

    @GetMapping("/banners")
    public ResponseEntity<ApiResponse<List<BannerResponse>>> getAllBanners() {
        return ResponseEntity.ok(ApiResponse.success("All Banners retrieved successfully", bannerService.getAllAdminBanners()));
    }

    @PostMapping("/banners")
    public ResponseEntity<ApiResponse<BannerResponse>> createBanner(@Valid @RequestBody BannerRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Banner created", bannerService.createBanner(request)));
    }

    @PutMapping("/banners/{id}")
    public ResponseEntity<ApiResponse<BannerResponse>> updateBanner(
            @PathVariable UUID id, 
            @Valid @RequestBody BannerRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Banner updated", bannerService.updateBanner(id, request)));
    }

    @DeleteMapping("/banners/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteBanner(@PathVariable UUID id) {
        bannerService.deleteBanner(id);
        return ResponseEntity.ok(ApiResponse.success("Banner deleted successfully"));
    }

    // --- ARTICLES ---

    @GetMapping("/articles")
    public ResponseEntity<ApiResponse<PageResponse<ArticleResponse>>> getAllArticles(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success("All Articles retrieved successfully", articleService.getAdminArticles(page, size)));
    }

    @PostMapping("/articles")
    public ResponseEntity<ApiResponse<ArticleResponse>> createArticle(
            @Valid @RequestBody ArticleRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success("Article created", articleService.createArticle(request, userDetails.getUser().getId())));
    }

    @PutMapping("/articles/{id}")
    public ResponseEntity<ApiResponse<ArticleResponse>> updateArticle(
            @PathVariable UUID id, 
            @Valid @RequestBody ArticleRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Article updated", articleService.updateArticle(id, request)));
    }

    @DeleteMapping("/articles/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteArticle(@PathVariable UUID id) {
        articleService.deleteArticle(id);
        return ResponseEntity.ok(ApiResponse.success("Article deleted successfully"));
    }
}
