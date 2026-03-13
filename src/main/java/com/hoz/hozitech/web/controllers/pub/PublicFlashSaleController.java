package com.hoz.hozitech.web.controllers.pub;

import com.hoz.hozitech.web.base.RestAPI;
import com.hoz.hozitech.application.services.FlashSaleService;
import com.hoz.hozitech.domain.dtos.response.ApiResponse;
import com.hoz.hozitech.domain.dtos.response.FlashSaleResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;

@RestAPI("${api.prefix-client}/flash-sales")
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
