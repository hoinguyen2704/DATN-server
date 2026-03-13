package com.hoz.hozitech.web.controllers;

import com.hoz.hozitech.application.services.FlashSaleService;
import com.hoz.hozitech.domain.dtos.response.ApiResponse;
import com.hoz.hozitech.domain.dtos.response.FlashSaleResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${api.prefix-client}/flash-sales")
@RequiredArgsConstructor
public class PublicFlashSaleController {

    private final FlashSaleService flashSaleService;

    @GetMapping("/active")
    public ResponseEntity<ApiResponse<FlashSaleResponse>> getActiveFlashSale() {
        FlashSaleResponse response = flashSaleService.getActiveFlashSale();
        if (response == null) {
            return ResponseEntity.ok(new ApiResponse<>(true, "No active flash sale", null, java.time.LocalDateTime.now()));
        }
        return ResponseEntity.ok(ApiResponse.success("Active flash sale fetched", response));
    }
}
