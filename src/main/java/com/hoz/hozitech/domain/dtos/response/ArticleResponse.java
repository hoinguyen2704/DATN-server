package com.hoz.hozitech.domain.dtos.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ArticleResponse {
    private UUID id;
    private String title;
    private String slug;
    private String content;
    private String thumbnailUrl;
    private Boolean isPublished;
    private String authorName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
