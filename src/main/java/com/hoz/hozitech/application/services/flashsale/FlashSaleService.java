package com.hoz.hozitech.application.services.flashsale;

import com.hoz.hozitech.domain.dtos.request.FlashSaleRequest;
import com.hoz.hozitech.domain.dtos.response.FlashSaleResponse;
import com.hoz.hozitech.domain.dtos.response.PageResponse;
import com.hoz.hozitech.domain.enums.FlashSaleStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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

    BigDecimal getActiveFlashSalePrice(UUID variantId, int quantity);

    BigDecimal applyFlashSaleAndReduceStock(UUID variantId, int quantity);

    void restoreFlashSaleSoldCount(UUID variantId, BigDecimal soldUnitPrice, int quantity, LocalDateTime soldAt);
}
