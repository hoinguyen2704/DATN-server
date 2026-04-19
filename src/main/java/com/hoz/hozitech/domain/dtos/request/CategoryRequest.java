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

    private UUID parentId;

    private Boolean active;

    private List<VariantAttributeItem> variantAttributes;

    private List<SpecAttributeItem> specAttributes;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VariantAttributeItem {
        private UUID attributeId;
        private String name;
        private String code;
        private Integer sortOrder;
        private List<VariantOptionItem> options;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VariantOptionItem {
        private UUID id;
        private String label;
        private String code;
        private Integer sortOrder;
        private Boolean active;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SpecAttributeItem {
        private UUID attributeId;
        private String name;
        private String code;
        private String hint;
        private Integer sortOrder;
    }
}
