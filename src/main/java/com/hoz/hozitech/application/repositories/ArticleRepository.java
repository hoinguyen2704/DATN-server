package com.hoz.hozitech.application.repositories;

import com.hoz.hozitech.domain.entities.Article;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ArticleRepository extends JpaRepository<Article, UUID> {
    Page<Article> findByIsPublishedTrue(Pageable pageable);
    Optional<Article> findBySlugAndIsPublishedTrue(String slug);
    Optional<Article> findBySlug(String slug);
    boolean existsBySlug(String slug);
}
