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

    PageResponse<FlashSaleResponse> getActiveFlashSales(int page, int size);

    PageResponse<FlashSaleResponse.FlashSaleItemResponse> getActiveFlashSaleItems(UUID flashSaleId, int page, int size);

    List<FlashSaleResponse.FlashSaleItemResponse> getActiveFlashSaleItemsByVariantIds(List<UUID> variantIds);

    BigDecimal getActiveFlashSalePrice(UUID variantId, int quantity);

    BigDecimal applyFlashSaleAndReduceStock(UUID variantId, int quantity);

    void restoreFlashSaleSoldCount(UUID variantId, BigDecimal soldUnitPrice, int quantity, LocalDateTime soldAt);
}
