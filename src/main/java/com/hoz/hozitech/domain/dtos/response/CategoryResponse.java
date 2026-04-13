package com.hoz.hozitech.domain.dtos.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CategoryResponse {
    private UUID id;
    private String name;
    private String slug;
    private String description;
    private String imageUrl;
    private Boolean active;
    private Long productCount;
    private LocalDateTime createdAt;

    // For Tree View
    private List<CategoryResponse> children;

    // Spec templates for this category
    private List<SpecTemplateResponse> specTemplates;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SpecTemplateResponse {
        private UUID id;
        private String specKey;
        private String hint;
        private Integer sortOrder;
    }
}
