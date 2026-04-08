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

    @NotNull(message = "approved flag is required")
    private Boolean approved;

    @DecimalMin(value = "0.01", message = "approvedAmount must be greater than 0")
    private BigDecimal approvedAmount;

    @Size(max = 1000, message = "note must be at most 1000 characters")
    private String note;
}
