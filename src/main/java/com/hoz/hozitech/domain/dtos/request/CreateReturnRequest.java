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

    @NotNull(message = "{validation.orderid_is_required}")
    private UUID orderId;

    @NotBlank(message = "{validation.reason_is_required}")
    @Size(max = 500, message = "{validation.reason_must_be_at_most_500_characters}")
    private String reason;

    @Size(max = 1000, message = "{validation.evidencenote_must_be_at_most_1000_characters}")
    private String evidenceNote;

    @NotEmpty(message = "{validation.items_must_not_be_empty}")
    private List<@Valid ReturnItemRequest> items;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReturnItemRequest {

        @NotNull(message = "{validation.orderitemid_is_required}")
        private UUID orderItemId;

        @NotNull(message = "{validation.quantity_is_required}")
        @Min(value = 1, message = "{validation.quantity_must_be_greater_than_0}")
        private Integer quantity;
    }
}
