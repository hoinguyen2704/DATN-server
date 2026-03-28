package com.hoz.hozitech.domain.dtos.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CategoryRequest {
    @NotBlank(message = "Category name is required")
    private String name;

    private String description;

    private String imageUrl;

    private UUID parentId;

    private Boolean active;

    private List<SpecTemplateItem> specTemplates;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SpecTemplateItem {
        private String specKey;
        private String hint;
        private Integer sortOrder;
    }
}
