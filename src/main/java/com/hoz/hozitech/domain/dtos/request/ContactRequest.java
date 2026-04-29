package com.hoz.hozitech.domain.dtos.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ContactRequest {
    
    @NotBlank(message = "{validation.name_is_required}")
    private String name;

    @NotBlank(message = "{validation.email_is_required}")
    @Email(message = "{validation.invalid_email_format}")
    private String email;

    private String phone;

    @NotBlank(message = "{validation.subject_is_required}")
    private String subject;

    @NotBlank(message = "{validation.message_is_required}")
    private String message;
}
