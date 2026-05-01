package com.hoz.hozitech.web.controllers.admin;

import com.hoz.hozitech.application.constant.PaginationConstant;
import com.hoz.hozitech.application.services.storage.FileStorageService;
import com.hoz.hozitech.config.utils.LocalizedApiResponseFactory;
import com.hoz.hozitech.web.base.RestAPI;
import com.hoz.hozitech.web.base.RoleAdmin;
import com.hoz.hozitech.application.services.article.ArticleService;
import com.hoz.hozitech.application.services.banner.BannerService;
import com.hoz.hozitech.security.CustomUserDetails;
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
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestAPI("${api.prefix-admin}/cms")
@RoleAdmin
@RequiredArgsConstructor
public class AdminCmsController {

    private final BannerService bannerService;
    private final ArticleService articleService;
    private final FileStorageService fileStorageService;
    private final LocalizedApiResponseFactory responseFactory;

    // --- BANNERS ---

    @GetMapping("/banners")
    public ResponseEntity<ApiResponse<List<BannerResponse>>> getAllBanners() {
        return ResponseEntity.ok(responseFactory.success("response.admin_cms.banners_fetched",
                bannerService.getAllAdminBanners()));
    }

    @PostMapping("/banners")
    public ResponseEntity<ApiResponse<BannerResponse>> createBanner(@Valid @RequestBody BannerRequest request) {
        return ResponseEntity.ok(responseFactory.success("response.admin_cms.banner_created",
                bannerService.createBanner(request)));
    }

    @PutMapping("/banners/{id}")
    public ResponseEntity<ApiResponse<BannerResponse>> updateBanner(
            @PathVariable UUID id, 
            @Valid @RequestBody BannerRequest request) {
        return ResponseEntity.ok(responseFactory.success("response.admin_cms.banner_updated",
                bannerService.updateBanner(id, request)));
    }

    @DeleteMapping("/banners/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteBanner(@PathVariable UUID id) {
        bannerService.deleteBanner(id);
        return ResponseEntity.ok(responseFactory.success("response.admin_cms.banner_deleted"));
    }

    @PostMapping("/banners/upload-image")
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadBannerImage(@RequestParam("file") MultipartFile file) {
        String imageUrl = fileStorageService.uploadFile(file, "banners");
        return ResponseEntity.ok(responseFactory.success("response.admin_cms.banner_image_uploaded",
                Map.of("imageUrl", imageUrl)));
    }

    // --- ARTICLES ---

    @GetMapping("/articles")
    public ResponseEntity<ApiResponse<PageResponse<ArticleResponse>>> getAllArticles(
            @RequestParam(defaultValue = PaginationConstant.PAGE_DEFAULT_STR) int page,
            @RequestParam(defaultValue = PaginationConstant.PAGE_SIZE_MEDIUM_STR) int size) {
        return ResponseEntity.ok(responseFactory.success("response.admin_cms.articles_fetched",
                articleService.getAdminArticles(page, size)));
    }

    @PostMapping("/articles")
    public ResponseEntity<ApiResponse<ArticleResponse>> createArticle(
            @Valid @RequestBody ArticleRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(responseFactory.success("response.admin_cms.article_created",
                articleService.createArticle(request, userDetails.getUser().getId())));
    }

    @PutMapping("/articles/{id}")
    public ResponseEntity<ApiResponse<ArticleResponse>> updateArticle(
            @PathVariable UUID id, 
            @Valid @RequestBody ArticleRequest request) {
        return ResponseEntity.ok(responseFactory.success("response.admin_cms.article_updated",
                articleService.updateArticle(id, request)));
    }

    @DeleteMapping("/articles/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteArticle(@PathVariable UUID id) {
        articleService.deleteArticle(id);
        return ResponseEntity.ok(responseFactory.success("response.admin_cms.article_deleted"));
    }

    @PostMapping("/articles/upload-thumbnail")
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadArticleThumbnail(@RequestParam("file") MultipartFile file) {
        String imageUrl = fileStorageService.uploadFile(file, "articles");
        return ResponseEntity.ok(responseFactory.success("response.admin_cms.article_thumbnail_uploaded",
                Map.of("imageUrl", imageUrl)));
    }
}
