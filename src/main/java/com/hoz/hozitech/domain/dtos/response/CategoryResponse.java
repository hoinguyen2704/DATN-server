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
    private Boolean active;
    private Long productCount;
    private Integer specCount;
    private LocalDateTime createdAt;

    // New schema for dynamic variant/spec rendering.
    private List<VariantAttributeSchemaResponse> variantAttributes;
    private List<SpecSchemaResponse> specAttributes;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VariantAttributeSchemaResponse {
        private UUID id;
        private String name;
        private String code;
        private Integer sortOrder;
        private List<VariantOptionResponse> options;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VariantOptionResponse {
        private UUID id;
        private String label;
        private String code;
        private Integer sortOrder;
        private Boolean active;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SpecSchemaResponse {
        private UUID id;
        private String name;
        private String code;
        private String hint;
        private Integer sortOrder;
    }
}
