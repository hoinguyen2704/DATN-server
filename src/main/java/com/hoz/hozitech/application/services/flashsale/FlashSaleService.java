package com.hoz.hozitech.application.services.flashsale;

import com.hoz.hozitech.domain.dtos.request.FlashSaleRequest;
import com.hoz.hozitech.domain.dtos.response.FlashSaleResponse;
import com.hoz.hozitech.domain.dtos.response.PageResponse;
import com.hoz.hozitech.domain.enums.FlashSaleStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface FlashSaleService {

    FlashSaleResponse createFlashSale(FlashSaleRequest request);

    FlashSaleResponse updateFlashSale(UUID id, FlashSaleRequest request);
    FlashSaleResponse updateFlashSaleStatus(UUID id, FlashSaleStatus status);

    void deleteFlashSale(UUID id);

    FlashSaleResponse getFlashSaleById(UUID id);

    PageResponse<FlashSaleResponse> getAllFlashSales(int page, int size);

    FlashSaleResponse getActiveFlashSale();
    List<FlashSaleResponse> getActiveFlashSales();

    /**
     * Checks if variant is in active flash sale with enough stock. 
     * If so, reduces flash stock and returns flash price. Otherwise returns null.
     */
    BigDecimal applyFlashSaleAndReduceStock(UUID variantId, int quantity);
}
