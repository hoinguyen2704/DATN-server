package com.hoz.hozitech.domain.dtos.response;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminProductDeleteResultResponse {
    private UUID id;
    private String action;
    private String status;
}
