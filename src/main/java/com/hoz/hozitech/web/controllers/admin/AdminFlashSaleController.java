package com.hoz.hozitech.web.controllers.admin;

import com.hoz.hozitech.application.constant.PaginationConstant;
import com.hoz.hozitech.web.base.RestAPI;
import com.hoz.hozitech.web.base.RoleAdmin;
import com.hoz.hozitech.application.services.flashsale.FlashSaleService;
import com.hoz.hozitech.config.utils.LocalizedApiResponseFactory;
import com.hoz.hozitech.domain.dtos.request.FlashSaleRequest;
import com.hoz.hozitech.domain.dtos.response.ApiResponse;
import com.hoz.hozitech.domain.dtos.response.FlashSaleResponse;
import com.hoz.hozitech.domain.dtos.response.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestAPI("${api.prefix-admin}/flash-sales")
@RoleAdmin
@RequiredArgsConstructor
public class AdminFlashSaleController {

    private final FlashSaleService flashSaleService;
    private final LocalizedApiResponseFactory responseFactory;

    @PostMapping
    public ResponseEntity<ApiResponse<FlashSaleResponse>> create(@Valid @RequestBody FlashSaleRequest request) {
        return ResponseEntity.ok(responseFactory.success("response.admin_flash_sale.created",
                flashSaleService.createFlashSale(request)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<FlashSaleResponse>>> getAll(
            @RequestParam(defaultValue = PaginationConstant.PAGE_DEFAULT_STR) int page,
            @RequestParam(defaultValue = PaginationConstant.PAGE_SIZE_MEDIUM_STR) int size) {
        return ResponseEntity.ok(responseFactory.success("response.admin_flash_sale.list_fetched",
                flashSaleService.getAllFlashSales(page, size)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<FlashSaleResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(responseFactory.success("response.admin_flash_sale.fetched",
                flashSaleService.getFlashSaleById(id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<FlashSaleResponse>> update(@PathVariable UUID id, @Valid @RequestBody FlashSaleRequest request) {
        return ResponseEntity.ok(responseFactory.success("response.admin_flash_sale.updated",
                flashSaleService.updateFlashSale(id, request)));
    }
    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<FlashSaleResponse>> updateStatus(
            @PathVariable UUID id, 
            @RequestParam com.hoz.hozitech.domain.enums.FlashSaleStatus status) {
        return ResponseEntity.ok(responseFactory.success("response.admin_flash_sale.status_updated",
                flashSaleService.updateFlashSaleStatus(id, status)));
    }


    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        flashSaleService.deleteFlashSale(id);
        return ResponseEntity.ok(responseFactory.success("response.admin_flash_sale.deleted"));
    }
}
