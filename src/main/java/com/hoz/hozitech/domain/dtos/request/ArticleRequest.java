package com.hoz.hozitech.domain.dtos.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ArticleRequest {
    @NotBlank(message = "{validation.title_is_required}")
    private String title;

    @NotBlank(message = "{validation.content_is_required}")
    private String content;

    private String thumbnailUrl;
    private Boolean isPublished;
}
