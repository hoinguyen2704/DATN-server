package com.hoz.hozitech.application.services.impl;

import com.hoz.hozitech.application.repositories.ArticleRepository;
import com.hoz.hozitech.application.repositories.UserRepository;
import com.hoz.hozitech.application.services.ArticleService;
import com.hoz.hozitech.domain.dtos.request.ArticleRequest;
import com.hoz.hozitech.domain.dtos.response.ArticleResponse;
import com.hoz.hozitech.domain.dtos.response.PageResponse;
import com.hoz.hozitech.domain.entities.Article;
import com.hoz.hozitech.domain.entities.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ArticleServiceImpl implements ArticleService {

    private final ArticleRepository articleRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ArticleResponse> getPublicArticles(int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Article> articlePage = articleRepository.findByIsPublishedTrue(pageable);
        return mapToPageResponse(articlePage);
    }

    @Override
    @Transactional(readOnly = true)
    public ArticleResponse getPublicArticleBySlug(String slug) {
        Article article = articleRepository.findBySlugAndIsPublishedTrue(slug)
                .orElseThrow(() -> new IllegalArgumentException("Article not found or not published"));
        return mapToResponse(article);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ArticleResponse> getAdminArticles(int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Article> articlePage = articleRepository.findAll(pageable);
        return mapToPageResponse(articlePage);
    }

    @Override
    @Transactional
    public ArticleResponse createArticle(ArticleRequest request, UUID authorId) {
        User author = userRepository.findById(authorId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        String slug = generateSlug(request.getTitle());
        if (articleRepository.existsBySlug(slug)) {
            slug += "-" + System.currentTimeMillis();
        }

        Article article = Article.builder()
                .title(request.getTitle())
                .slug(slug)
                .content(request.getContent())
                .thumbnailUrl(request.getThumbnailUrl())
                .isPublished(request.getIsPublished() != null ? request.getIsPublished() : false)
                .author(author)
                .build();

        return mapToResponse(articleRepository.save(article));
    }

    @Override
    @Transactional
    public ArticleResponse updateArticle(UUID id, ArticleRequest request) {
        Article article = articleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Article not found"));

        if (request.getTitle() != null && !request.getTitle().equals(article.getTitle())) {
            article.setTitle(request.getTitle());
            String slug = generateSlug(request.getTitle());
            if (articleRepository.existsBySlug(slug)) {
                slug += "-" + System.currentTimeMillis();
            }
            article.setSlug(slug);
        }
        
        if (request.getContent() != null) article.setContent(request.getContent());
        if (request.getThumbnailUrl() != null) article.setThumbnailUrl(request.getThumbnailUrl());
        if (request.getIsPublished() != null) article.setIsPublished(request.getIsPublished());

        return mapToResponse(articleRepository.save(article));
    }

    @Override
    @Transactional
    public void deleteArticle(UUID id) {
        Article article = articleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Article not found"));
        articleRepository.delete(article);
    }

    private String generateSlug(String title) {
        String normalized = Normalizer.normalize(title, Normalizer.Form.NFD);
        String slug = normalized.replaceAll("[\\p{InCombiningDiacriticalMarks}]", "");
        slug = slug.toLowerCase().replaceAll("[^a-z0-9\\s-]", "").replaceAll("\\s+", "-");
        return slug;
    }

    private ArticleResponse mapToResponse(Article article) {
        return ArticleResponse.builder()
                .id(article.getId())
                .title(article.getTitle())
                .slug(article.getSlug())
                .content(article.getContent())
                .thumbnailUrl(article.getThumbnailUrl())
                .isPublished(article.getIsPublished())
                .authorName(article.getAuthor().getFullName())
                .createdAt(article.getCreatedAt())
                .updatedAt(article.getUpdatedAt())
                .build();
    }

    private PageResponse<ArticleResponse> mapToPageResponse(Page<Article> page) {
        return PageResponse.<ArticleResponse>builder()
                .data(page.getContent().stream().map(this::mapToResponse).collect(Collectors.toList()))
                .page(page.getNumber() + 1)
                .perPage(page.getSize())
                .total(page.getTotalElements())
                .lastPage(page.getTotalPages())
                .build();
    }
}
