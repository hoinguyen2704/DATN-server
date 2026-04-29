package com.hoz.hozitech.domain.dtos.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewReturnRequest {

    @NotNull(message = "{validation.approved_flag_is_required}")
    private Boolean approved;

    @DecimalMin(value = "0.01", message = "{validation.approvedamount_must_be_greater_than_0}")
    private BigDecimal approvedAmount;

    @Size(max = 1000, message = "{validation.note_must_be_at_most_1000_characters}")
    private String note;
}
