package com.hoz.hozitech.application.services.flashsale;

import com.hoz.hozitech.application.constant.PaginationConstant;
import com.hoz.hozitech.application.repositories.FlashSaleItemRepository;
import com.hoz.hozitech.application.repositories.FlashSaleRepository;
import com.hoz.hozitech.application.repositories.ProductVariantRepository;
import com.hoz.hozitech.domain.dtos.request.FlashSaleRequest;
import com.hoz.hozitech.domain.dtos.response.FlashSaleResponse;
import com.hoz.hozitech.domain.dtos.response.FlashSaleResponse.FlashSaleItemResponse;
import com.hoz.hozitech.domain.dtos.response.PageResponse;
import com.hoz.hozitech.domain.entities.FlashSale;
import com.hoz.hozitech.domain.entities.FlashSaleItem;
import com.hoz.hozitech.domain.entities.ProductVariant;
import com.hoz.hozitech.domain.enums.FlashSaleStatus;
import com.hoz.hozitech.web.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FlashSaleServiceImpl implements FlashSaleService {

    private final FlashSaleRepository flashSaleRepository;
    private final FlashSaleItemRepository flashSaleItemRepository;
    private final ProductVariantRepository productVariantRepository;

    @Override
    @Transactional
    public FlashSaleResponse createFlashSale(FlashSaleRequest request) {
        FlashSale flashSale = FlashSale.builder()
                .name(request.getName())
                .description(request.getDescription())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .status(FlashSaleStatus.SCHEDULED)
                .build();

        flashSale = flashSaleRepository.save(flashSale);

        if (request.getItems() != null) {
            for (FlashSaleRequest.FlashSaleItemRequest itemReq : request.getItems()) {
                ProductVariant variant = productVariantRepository.findById(itemReq.getVariantId())
                        .orElseThrow(() -> new ResourceNotFoundException("Variant", itemReq.getVariantId()));

                FlashSaleItem item = FlashSaleItem.builder()
                        .flashSale(flashSale)
                        .variant(variant)
                        .flashPrice(itemReq.getFlashPrice())
                        .flashStock(itemReq.getFlashStock())
                        .soldCount(0)
                        .build();
                flashSaleItemRepository.save(item);
                flashSale.getItems().add(item);
            }
        }

        return toResponse(flashSale);
    }

    @Override
    @Transactional
    public FlashSaleResponse updateFlashSale(UUID id, FlashSaleRequest request) {
        FlashSale flashSale = flashSaleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Flash sale", id));

        flashSale.setName(request.getName());
        flashSale.setDescription(request.getDescription());
        flashSale.setStartTime(request.getStartTime());
        flashSale.setEndTime(request.getEndTime());
        flashSale.getItems().clear();
        flashSaleRepository.flush();
        
        if (request.getItems() != null) {
            for (FlashSaleRequest.FlashSaleItemRequest itemReq : request.getItems()) {
                ProductVariant variant = productVariantRepository.findById(itemReq.getVariantId())
                        .orElseThrow(() -> new ResourceNotFoundException("Variant", itemReq.getVariantId()));

                FlashSaleItem item = FlashSaleItem.builder()
                        .flashSale(flashSale)
                        .variant(variant)
                        .flashPrice(itemReq.getFlashPrice())
                        .flashStock(itemReq.getFlashStock())
                        .soldCount(0)
                        .build();
                flashSale.getItems().add(item);
            }
        }

        flashSale = flashSaleRepository.save(flashSale);
        return toResponse(flashSale);
    }

    @Override
    @Transactional
    public FlashSaleResponse updateFlashSaleStatus(UUID id, FlashSaleStatus status) {
        FlashSale flashSale = flashSaleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Flash sale", id));
        flashSale.setStatus(status);
        flashSale = flashSaleRepository.save(flashSale);
        return toResponse(flashSale);
    }

    @Override
    @Transactional
    public void deleteFlashSale(UUID id) {
        flashSaleRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public FlashSaleResponse getFlashSaleById(UUID id) {
        FlashSale flashSale = flashSaleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Flash sale", id));
        return toResponse(flashSale);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<FlashSaleResponse> getAllFlashSales(int page, int size) {
        Page<FlashSale> pageResult = flashSaleRepository.findAllByOrderByCreatedAtDesc(
                PaginationConstant.of(page, size));
        Page<FlashSaleResponse> mapped = pageResult.map(this::toResponse);
        return PageResponse.of(mapped);
    }

    @Override
    @Transactional(readOnly = true)
    public FlashSaleResponse getActiveFlashSale() {
        return getActiveFlashSales().stream()
                .findFirst()
                .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FlashSaleResponse> getActiveFlashSales() {
        return flashSaleRepository.findActiveFlashSales().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public BigDecimal applyFlashSaleAndReduceStock(UUID variantId, int quantity) {
        return flashSaleItemRepository.findActiveFlashSaleItemByVariantIdForUpdate(variantId)
                .map(item -> {
                    if (item.getFlashStock() - item.getSoldCount() >= quantity) {
                        item.setSoldCount(item.getSoldCount() + quantity);
                        flashSaleItemRepository.save(item);
                        return item.getFlashPrice();
                    }
                    return null;
                })
                .orElse(null);
    }

    @Override
    @Transactional
    public void restoreFlashSaleSoldCount(UUID variantId, BigDecimal soldUnitPrice, int quantity, LocalDateTime soldAt) {
        if (variantId == null || soldUnitPrice == null || soldAt == null || quantity <= 0) {
            return;
        }

        flashSaleItemRepository.findRollbackCandidatesForUpdate(variantId, soldUnitPrice, soldAt)
                .stream()
                .filter(item -> item.getSoldCount() != null && item.getSoldCount() > 0)
                .findFirst()
                .ifPresent(item -> {
                    int restored = Math.max(0, item.getSoldCount() - quantity);
                    item.setSoldCount(restored);
                    flashSaleItemRepository.save(item);
                });
    }

    // --- Mapper ---

    private FlashSaleResponse toResponse(FlashSale fs) {
        List<FlashSaleItemResponse> itemResponses = fs.getItems().stream().map(item -> {
            ProductVariant v = item.getVariant();
            String productId = v.getProduct() != null ? v.getProduct().getId().toString() : "";
            String productSlug = v.getProduct() != null ? v.getProduct().getSlug() : "";
            String productName = v.getProduct() != null ? v.getProduct().getName() : "";
            String imageUrl = "";
            if (v.getProduct() != null && v.getProduct().getImages() != null && !v.getProduct().getImages().isEmpty()) {
                imageUrl = v.getProduct().getImages().stream()
                        .filter(img -> Boolean.TRUE.equals(img.getIsPrimary()))
                        .findFirst()
                        .map(img -> img.getImageUrl())
                        .orElse(v.getProduct().getImages().getFirst().getImageUrl());
            }

            return FlashSaleItemResponse.builder()
                    .id(item.getId().toString())
                    .productId(productId)
                    .productSlug(productSlug)
                    .variantId(v.getId().toString())
                    .productName(productName)
                    .variantName(v.getVariantName())
                    .imageUrl(imageUrl)
                    .originalPrice(v.getPrice())
                    .flashPrice(item.getFlashPrice())
                    .flashStock(item.getFlashStock())
                    .soldCount(item.getSoldCount())
                    .remainingStock(item.getFlashStock() - item.getSoldCount())
                    .build();
        }).collect(Collectors.toList());

        return FlashSaleResponse.builder()
                .id(fs.getId().toString())
                .name(fs.getName())
                .description(fs.getDescription())
                .startTime(fs.getStartTime())
                .endTime(fs.getEndTime())
                .status(fs.getStatus().name())
                .items(itemResponses)
                .createdAt(fs.getCreatedAt())
                .build();
    }
}
