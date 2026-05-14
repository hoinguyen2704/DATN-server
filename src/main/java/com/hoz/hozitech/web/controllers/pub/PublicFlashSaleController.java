package com.hoz.hozitech.web.controllers.pub;

import com.hoz.hozitech.application.constant.PaginationConstant;
import com.hoz.hozitech.web.base.RestAPI;
import com.hoz.hozitech.application.services.flashsale.FlashSaleService;
import com.hoz.hozitech.config.utils.LocalizedApiResponseFactory;
import com.hoz.hozitech.domain.dtos.response.ApiResponse;
import com.hoz.hozitech.domain.dtos.response.FlashSaleResponse;
import com.hoz.hozitech.domain.dtos.response.FlashSaleResponse.FlashSaleItemResponse;
import com.hoz.hozitech.domain.dtos.response.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@RestAPI("${api.prefix-client}/flash-sales")
@RequiredArgsConstructor
public class PublicFlashSaleController {

    private final FlashSaleService flashSaleService;
    private final LocalizedApiResponseFactory responseFactory;

    @GetMapping("/active")
    public ResponseEntity<ApiResponse<FlashSaleResponse>> getActiveFlashSale() {
        FlashSaleResponse response = flashSaleService.getActiveFlashSale();
        if (response == null) {
            return ResponseEntity.ok(responseFactory.success("response.flash_sale.no_active", null));
        }
        return ResponseEntity.ok(responseFactory.success("response.flash_sale.active_fetched", response));
    }

    @GetMapping("/active-list")
    public ResponseEntity<ApiResponse<List<FlashSaleResponse>>> getActiveFlashSales() {
        List<FlashSaleResponse> responses = flashSaleService.getActiveFlashSales();
        return ResponseEntity.ok(responseFactory.success("response.flash_sale.active_list_fetched", responses));
    }

    @GetMapping("/active-page")
    public ResponseEntity<ApiResponse<PageResponse<FlashSaleResponse>>> getActiveFlashSalePage(
            @RequestParam(defaultValue = PaginationConstant.PAGE_DEFAULT_STR) int page,
            @RequestParam(defaultValue = "2") int size) {
        PageResponse<FlashSaleResponse> responses = flashSaleService.getActiveFlashSales(page, size);
        return ResponseEntity.ok(responseFactory.success("response.flash_sale.active_list_fetched", responses));
    }

    @GetMapping("/{flashSaleId}/items")
    public ResponseEntity<ApiResponse<PageResponse<FlashSaleItemResponse>>> getActiveFlashSaleItems(
            @PathVariable UUID flashSaleId,
            @RequestParam(defaultValue = PaginationConstant.PAGE_DEFAULT_STR) int page,
            @RequestParam(defaultValue = PaginationConstant.PAGE_SIZE_MEDIUM_STR) int size) {
        PageResponse<FlashSaleItemResponse> responses = flashSaleService.getActiveFlashSaleItems(flashSaleId, page, size);
        return ResponseEntity.ok(responseFactory.success("response.flash_sale.active_list_fetched", responses));
    }

    @GetMapping("/active-items")
    public ResponseEntity<ApiResponse<List<FlashSaleItemResponse>>> getActiveFlashSaleItemsByVariants(
            @RequestParam(required = false, defaultValue = "") String variantIds) {
        List<UUID> ids = Arrays.stream(variantIds.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .map(UUID::fromString)
                .distinct()
                .toList();
        return ResponseEntity.ok(responseFactory.success(
                "response.flash_sale.active_list_fetched",
                flashSaleService.getActiveFlashSaleItemsByVariantIds(ids)));
    }
}
