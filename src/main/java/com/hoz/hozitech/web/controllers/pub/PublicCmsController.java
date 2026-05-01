package com.hoz.hozitech.web.controllers.pub;

import com.hoz.hozitech.application.constant.PaginationConstant;
import com.hoz.hozitech.web.base.RestAPI;
import com.hoz.hozitech.application.services.article.ArticleService;
import com.hoz.hozitech.application.services.banner.BannerService;
import com.hoz.hozitech.config.utils.LocalizedApiResponseFactory;
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
    private final LocalizedApiResponseFactory responseFactory;

    @GetMapping("/banners")
    public ResponseEntity<ApiResponse<List<BannerResponse>>> getPublicBanners() {
        return ResponseEntity.ok(responseFactory.success("response.cms.public_banners_fetched",
                bannerService.getAllPublicBanners()));
    }

    @GetMapping("/articles")
    public ResponseEntity<ApiResponse<PageResponse<ArticleResponse>>> getPublicArticles(
            @RequestParam(defaultValue = PaginationConstant.PAGE_DEFAULT_STR) int page,
            @RequestParam(defaultValue = PaginationConstant.PAGE_SIZE_MEDIUM_STR) int size) {
        return ResponseEntity.ok(responseFactory.success("response.cms.public_articles_fetched",
                articleService.getPublicArticles(page, size)));
    }

    @GetMapping("/articles/{slug}")
    public ResponseEntity<ApiResponse<ArticleResponse>> getPublicArticleBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(responseFactory.success("response.cms.public_article_fetched",
                articleService.getPublicArticleBySlug(slug)));
    }
}
