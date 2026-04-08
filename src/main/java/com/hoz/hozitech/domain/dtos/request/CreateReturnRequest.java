package com.hoz.hozitech.domain.dtos.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateReturnRequest {

    @NotNull(message = "orderId is required")
    private UUID orderId;

    @NotBlank(message = "reason is required")
    @Size(max = 500, message = "reason must be at most 500 characters")
    private String reason;

    @Size(max = 1000, message = "evidenceNote must be at most 1000 characters")
    private String evidenceNote;

    @NotEmpty(message = "items must not be empty")
    private List<@Valid ReturnItemRequest> items;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReturnItemRequest {

        @NotNull(message = "orderItemId is required")
        private UUID orderItemId;

        @NotNull(message = "quantity is required")
        @Min(value = 1, message = "quantity must be greater than 0")
        private Integer quantity;
    }
}
