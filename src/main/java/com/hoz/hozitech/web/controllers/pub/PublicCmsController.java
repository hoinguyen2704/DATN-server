package com.hoz.hozitech.web.controllers.pub;

import com.hoz.hozitech.web.base.RestAPI;
import com.hoz.hozitech.application.services.ArticleService;
import com.hoz.hozitech.application.services.BannerService;
import com.hoz.hozitech.domain.dtos.response.ApiResponse;
import com.hoz.hozitech.domain.dtos.response.ArticleResponse;
import com.hoz.hozitech.domain.dtos.response.BannerResponse;
import com.hoz.hozitech.domain.dtos.response.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestAPI("${api.prefix-client}/cms")
@RequiredArgsConstructor
public class PublicCmsController {

    private final BannerService bannerService;
    private final ArticleService articleService;

    @GetMapping("/banners")
    public ResponseEntity<ApiResponse<List<BannerResponse>>> getPublicBanners() {
        return ResponseEntity.ok(ApiResponse.success("Banners retrieved successfully", bannerService.getAllPublicBanners()));
    }

    @GetMapping("/articles")
    public ResponseEntity<ApiResponse<PageResponse<ArticleResponse>>> getPublicArticles(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success("Articles retrieved successfully", articleService.getPublicArticles(page, size)));
    }

    @GetMapping("/articles/{slug}")
    public ResponseEntity<ApiResponse<ArticleResponse>> getPublicArticleBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(ApiResponse.success("Article retrieved successfully", articleService.getPublicArticleBySlug(slug)));
    }
}
