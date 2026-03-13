package com.hoz.hozitech.application.services;

import com.hoz.hozitech.domain.dtos.request.ArticleRequest;
import com.hoz.hozitech.domain.dtos.response.ArticleResponse;
import com.hoz.hozitech.domain.dtos.response.PageResponse;

import java.util.UUID;

public interface ArticleService {
    PageResponse<ArticleResponse> getPublicArticles(int page, int size);
    ArticleResponse getPublicArticleBySlug(String slug);
    
    PageResponse<ArticleResponse> getAdminArticles(int page, int size);
    ArticleResponse createArticle(ArticleRequest request, UUID authorId);
    ArticleResponse updateArticle(UUID id, ArticleRequest request);
    void deleteArticle(UUID id);
}
