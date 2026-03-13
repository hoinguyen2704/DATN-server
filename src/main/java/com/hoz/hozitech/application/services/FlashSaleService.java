package com.hoz.hozitech.application.services;

import com.hoz.hozitech.domain.dtos.request.FlashSaleRequest;
import com.hoz.hozitech.domain.dtos.response.FlashSaleResponse;
import com.hoz.hozitech.domain.dtos.response.PageResponse;

import java.math.BigDecimal;
import java.util.UUID;

public interface FlashSaleService {

    FlashSaleResponse createFlashSale(FlashSaleRequest request);

    FlashSaleResponse updateFlashSale(UUID id, FlashSaleRequest request);

    void deleteFlashSale(UUID id);

    FlashSaleResponse getFlashSaleById(UUID id);

    PageResponse<FlashSaleResponse> getAllFlashSales(int page, int size);

    FlashSaleResponse getActiveFlashSale();

    /**
     * Checks if variant is in active flash sale with enough stock. 
     * If so, reduces flash stock and returns flash price. Otherwise returns null.
     */
    BigDecimal applyFlashSaleAndReduceStock(UUID variantId, int quantity);
}
