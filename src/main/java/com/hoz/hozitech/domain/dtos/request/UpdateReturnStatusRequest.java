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

    @NotBlank(message = "{validation.status_is_required}")
    private String status;

    @Size(max = 1000, message = "{validation.note_must_be_at_most_1000_characters}")
    private String note;
}
