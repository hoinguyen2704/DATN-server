package com.hoz.hozitech.domain.dtos.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateReturnStatusRequest {

    @NotBlank(message = "status is required")
    private String status;

    @Size(max = 1000, message = "note must be at most 1000 characters")
    private String note;
}
