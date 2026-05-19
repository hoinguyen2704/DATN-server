package com.hoz.hozitech.application.services.flashsale;

import com.hoz.hozitech.application.constant.PaginationConstant;
import com.hoz.hozitech.application.repositories.FlashSaleItemRepository;
import com.hoz.hozitech.application.repositories.FlashSaleItemRepository.StorefrontFlashSaleItemView;
import com.hoz.hozitech.application.repositories.FlashSaleRepository;
import com.hoz.hozitech.application.repositories.FlashSaleRepository.ActiveStorefrontFlashSaleView;
import com.hoz.hozitech.application.repositories.OrderItemRepository;
import com.hoz.hozitech.application.repositories.ProductImageRepository;
import com.hoz.hozitech.application.repositories.ProductVariantRepository;
import com.hoz.hozitech.application.repositories.ReturnItemRepository;
import com.hoz.hozitech.application.services.notification.AdminNotificationService;
import com.hoz.hozitech.application.services.notification.AdminNotificationTemplates;
import com.hoz.hozitech.application.services.promotion.PromotionStatusSyncService;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FlashSaleServiceImpl implements FlashSaleService {

    private static final int STOREFRONT_ITEM_PAGE_SIZE = 10;

    private final FlashSaleRepository flashSaleRepository;
    private final FlashSaleItemRepository flashSaleItemRepository;
    private final ProductImageRepository productImageRepository;
    private final ProductVariantRepository productVariantRepository;
    private final OrderItemRepository orderItemRepository;
    private final ReturnItemRepository returnItemRepository;
    private final AdminNotificationService adminNotificationService;
    private final PromotionStatusSyncService promotionStatusSyncService;

    @Override
    @Transactional
    public FlashSaleResponse createFlashSale(FlashSaleRequest request) {
        FlashSale flashSale = FlashSale.builder()
                .name(request.getName())
                .description(request.getDescription())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .status(resolveStatus(request.getStartTime(), request.getEndTime()))
                .build();

        flashSale = flashSaleRepository.save(flashSale);

        if (request.getItems() != null) {
            for (FlashSaleRequest.FlashSaleItemRequest itemReq : request.getItems()) {
                FlashSaleItem item = buildValidatedFlashSaleItem(flashSale, itemReq);
                flashSaleItemRepository.save(item);
                flashSale.getItems().add(item);
            }
        }

        adminNotificationService.createShared(AdminNotificationTemplates.flashSaleCreated(flashSale), true);
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
        flashSale.setStatus(resolveStatusForExistingFlashSale(
                flashSale.getStatus(),
                request.getStartTime(),
                request.getEndTime()));
        flashSale.getItems().clear();
        flashSaleRepository.flush();
        
        if (request.getItems() != null) {
            for (FlashSaleRequest.FlashSaleItemRequest itemReq : request.getItems()) {
                FlashSaleItem item = buildValidatedFlashSaleItem(flashSale, itemReq);
                flashSale.getItems().add(item);
            }
        }

        flashSale = flashSaleRepository.save(flashSale);
        adminNotificationService.createShared(AdminNotificationTemplates.flashSaleUpdated(flashSale), true);
        return toResponse(flashSale);
    }

    @Override
    @Transactional
    public FlashSaleResponse updateFlashSaleStatus(UUID id, FlashSaleStatus status) {
        FlashSale flashSale = flashSaleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Flash sale", id));
        flashSale.setStatus(status == FlashSaleStatus.HIDDEN
                ? FlashSaleStatus.HIDDEN
                : resolveStatus(flashSale.getStartTime(), flashSale.getEndTime()));
        flashSale = flashSaleRepository.save(flashSale);
        adminNotificationService.createShared(AdminNotificationTemplates.flashSaleStatusChanged(flashSale), true);
        return toResponse(flashSale);
    }

    @Override
    @Transactional
    public void deleteFlashSale(UUID id) {
        FlashSale flashSale = flashSaleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Flash sale", id));
        boolean hasOrders = flashSaleItemRepository.existsByFlashSaleIdAndSoldCountGreaterThan(id, 0);
        if (hasOrders) {
            flashSale.setStatus(FlashSaleStatus.HIDDEN);
            flashSaleRepository.save(flashSale);
            adminNotificationService.createShared(AdminNotificationTemplates.flashSaleStatusChanged(flashSale), true);
            return;
        }

        flashSaleRepository.delete(flashSale);
    }

    private FlashSaleItem buildValidatedFlashSaleItem(
            FlashSale flashSale,
            FlashSaleRequest.FlashSaleItemRequest itemReq
    ) {
        ProductVariant variant = productVariantRepository.findById(itemReq.getVariantId())
                .orElseThrow(() -> new ResourceNotFoundException("Variant", itemReq.getVariantId()));
        validateFlashSaleItemRequest(itemReq, variant);

        return FlashSaleItem.builder()
                .flashSale(flashSale)
                .variant(variant)
                .flashPrice(itemReq.getFlashPrice())
                .flashStock(itemReq.getFlashStock())
                .soldCount(0)
                .build();
    }

    private void validateFlashSaleItemRequest(
            FlashSaleRequest.FlashSaleItemRequest itemReq,
            ProductVariant variant
    ) {
        String variantLabel = resolveVariantLabel(variant);
        BigDecimal flashPrice = itemReq.getFlashPrice();
        BigDecimal originalPrice = resolveOriginalPrice(variant);
        Integer flashStock = itemReq.getFlashStock();
        int stockQuantity = variant.getStock() != null ? Math.max(0, variant.getStock()) : 0;

        if (!Boolean.TRUE.equals(variant.getActive())) {
            throw new IllegalArgumentException("Variant " + variantLabel + " is not active.");
        }

        if (originalPrice == null || originalPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Variant " + variantLabel + " has an invalid original price.");
        }

        if (flashPrice == null || flashPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Flash Sale price for variant " + variantLabel + " must be greater than 0.");
        }

        if (flashPrice.compareTo(originalPrice) > 0) {
            throw new IllegalArgumentException(
                    "Flash Sale price for variant " + variantLabel
                            + " must not exceed original price " + originalPrice + ".");
        }

        if (flashStock == null || flashStock <= 0) {
            throw new IllegalArgumentException("Flash Sale stock for variant " + variantLabel + " must be greater than 0.");
        }

        if (flashStock > stockQuantity) {
            throw new IllegalArgumentException(
                    "Flash Sale stock for variant " + variantLabel
                            + " must not exceed current stock " + stockQuantity + ".");
        }
    }

    private BigDecimal resolveOriginalPrice(ProductVariant variant) {
        if (variant.getCompareAtPrice() != null && variant.getCompareAtPrice().compareTo(BigDecimal.ZERO) > 0) {
            return variant.getCompareAtPrice();
        }
        if (variant.getProduct() != null
                && variant.getProduct().getOriginPrice() != null
                && variant.getProduct().getOriginPrice().compareTo(BigDecimal.ZERO) > 0) {
            return variant.getProduct().getOriginPrice();
        }
        return variant.getPrice();
    }

    private String resolveVariantLabel(ProductVariant variant) {
        if (variant.getSku() != null && !variant.getSku().isBlank()) {
            return variant.getSku();
        }
        if (variant.getVariantName() != null && !variant.getVariantName().isBlank()) {
            return variant.getVariantName();
        }
        return variant.getId() != null ? variant.getId().toString() : "unknown";
    }

    @Override
    @Transactional(readOnly = true)
    public FlashSaleResponse getFlashSaleById(UUID id) {
        promotionStatusSyncService.syncFlashSaleStatuses();
        FlashSale flashSale = flashSaleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Flash sale", id));
        return toResponse(flashSale);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<FlashSaleResponse> getAllFlashSales(int page, int size) {
        promotionStatusSyncService.syncFlashSaleStatuses();
        Page<FlashSale> pageResult = flashSaleRepository.findAll(
                PaginationConstant.of(page, size, Sort.by(
                        Sort.Order.desc("createdAt"),
                        Sort.Order.desc("id"))));
        Map<UUID, Integer> itemCountByFlashSaleId = buildFlashSaleItemCountMap(pageResult.getContent().stream()
                .map(FlashSale::getId)
                .toList());
        List<FlashSaleResponse> content = pageResult.getContent().stream()
                .map(flashSale -> toSummaryResponse(
                        flashSale,
                        itemCountByFlashSaleId.getOrDefault(flashSale.getId(), 0)))
                .toList();
        return PageResponse.<FlashSaleResponse>builder()
                .data(content)
                .page(page)
                .perPage(size)
                .total(pageResult.getTotalElements())
                .lastPage(pageResult.getTotalPages())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public FlashSaleResponse getActiveFlashSale() {
        PageResponse<FlashSaleResponse> activePage = getActiveFlashSales(1, 1);
        return activePage.getData().stream().findFirst().orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FlashSaleResponse> getActiveFlashSales() {
        return buildActiveStorefrontResponses();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<FlashSaleResponse> getActiveFlashSales(int page, int size) {
        return buildActiveStorefrontResponsePage(page, size);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<FlashSaleItemResponse> getActiveFlashSaleItems(UUID flashSaleId, int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(PaginationConstant.validateSize(size), STOREFRONT_ITEM_PAGE_SIZE);
        if (flashSaleId == null) {
            return PageResponse.<FlashSaleItemResponse>builder()
                    .data(List.of())
                    .page(safePage)
                    .perPage(safeSize)
                    .total(0)
                    .lastPage(0)
                    .build();
        }

        Page<StorefrontFlashSaleItemView> itemPage =
                flashSaleItemRepository.findActiveStorefrontItemsByFlashSaleId(
                        flashSaleId,
                        PageRequest.of(safePage - 1, safeSize));
        Map<UUID, String> imageByProductId = buildStorefrontImageMap(itemPage.getContent());
        List<FlashSaleItemResponse> content = itemPage.getContent().stream()
                .map(item -> toStorefrontItemResponse(item, imageByProductId))
                .toList();

        return PageResponse.<FlashSaleItemResponse>builder()
                .data(content)
                .page(itemPage.getNumber() + 1)
                .perPage(itemPage.getSize())
                .total(itemPage.getTotalElements())
                .lastPage(itemPage.getTotalPages())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<FlashSaleItemResponse> getActiveFlashSaleItemsByVariantIds(List<UUID> variantIds) {
        if (variantIds == null || variantIds.isEmpty()) {
            return List.of();
        }

        List<UUID> dedupedVariantIds = variantIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (dedupedVariantIds.isEmpty()) {
            return List.of();
        }

        List<StorefrontFlashSaleItemView> itemViews =
                flashSaleItemRepository.findActiveStorefrontItemsByVariantIds(dedupedVariantIds);
        Map<UUID, String> imageByProductId = buildStorefrontImageMap(itemViews);
        return itemViews.stream()
                .map(item -> toStorefrontItemResponse(item, imageByProductId))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getActiveFlashSalePrice(UUID variantId, int quantity) {
        promotionStatusSyncService.syncFlashSaleStatuses();
        if (variantId == null || quantity <= 0) {
            return null;
        }

        return flashSaleItemRepository.findActiveFlashSaleItemByVariantId(variantId).stream()
                .filter(item -> resolveAvailableFlashSaleStock(item) >= quantity)
                .map(FlashSaleItem::getFlashPrice)
                .findFirst()
                .orElse(null);
    }

    @Override
    @Transactional
    public BigDecimal applyFlashSaleAndReduceStock(UUID variantId, int quantity) {
        promotionStatusSyncService.syncFlashSaleStatuses();
        return flashSaleItemRepository.findActiveFlashSaleItemByVariantIdForUpdate(variantId).stream()
                .filter(item -> resolveAvailableFlashSaleStock(item) >= quantity)
                .findFirst()
                .map(item -> {
                    item.setSoldCount(item.getSoldCount() + quantity);
                    flashSaleItemRepository.save(item);
                    return item.getFlashPrice();
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

    private int resolveAvailableFlashSaleStock(FlashSaleItem item) {
        int flashStock = item.getFlashStock() != null ? item.getFlashStock() : 0;
        int soldCount = item.getSoldCount() != null ? item.getSoldCount() : 0;
        int flashRemaining = Math.max(0, flashStock - soldCount);
        if (item.getVariant() == null || item.getVariant().getStock() == null) {
            return flashRemaining;
        }
        return Math.min(flashRemaining, Math.max(0, item.getVariant().getStock()));
    }

    // --- Mapper ---

    private List<FlashSaleResponse> buildActiveStorefrontResponses() {
        List<ActiveStorefrontFlashSaleView> activeFlashSales = flashSaleRepository.findActiveStorefrontFlashSales();
        if (activeFlashSales.isEmpty()) {
            return List.of();
        }

        List<UUID> flashSaleIds = activeFlashSales.stream()
                .map(ActiveStorefrontFlashSaleView::getId)
                .toList();
        List<StorefrontFlashSaleItemView> itemViews = loadInitialStorefrontItems(activeFlashSales);
        Map<UUID, Integer> itemCountByFlashSaleId = buildFlashSaleItemCountMap(flashSaleIds);
        Map<UUID, List<StorefrontFlashSaleItemView>> itemsByFlashSaleId = itemViews.stream()
                .collect(Collectors.groupingBy(
                        StorefrontFlashSaleItemView::getFlashSaleId,
                        LinkedHashMap::new,
                        Collectors.toCollection(ArrayList::new)));
        Map<UUID, String> imageByProductId = buildStorefrontImageMap(itemViews);

        return activeFlashSales.stream()
                .map(flashSale -> toStorefrontResponse(
                        flashSale,
                        itemsByFlashSaleId.getOrDefault(flashSale.getId(), List.of()),
                        imageByProductId,
                        itemCountByFlashSaleId.getOrDefault(flashSale.getId(), 0)))
                .toList();
    }

    private PageResponse<FlashSaleResponse> buildActiveStorefrontResponsePage(int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = PaginationConstant.validateSize(size);
        Page<ActiveStorefrontFlashSaleView> activeFlashSales = flashSaleRepository.findActiveStorefrontFlashSales(
                PageRequest.of(safePage - 1, safeSize));
        if (activeFlashSales.isEmpty()) {
            return PageResponse.<FlashSaleResponse>builder()
                    .data(List.of())
                    .page(safePage)
                    .perPage(safeSize)
                    .total(activeFlashSales.getTotalElements())
                    .lastPage(activeFlashSales.getTotalPages())
                    .build();
        }

        List<UUID> flashSaleIds = activeFlashSales.getContent().stream()
                .map(ActiveStorefrontFlashSaleView::getId)
                .toList();
        Map<UUID, Integer> itemCountByFlashSaleId = buildFlashSaleItemCountMap(flashSaleIds);
        List<StorefrontFlashSaleItemView> itemViews = loadInitialStorefrontItems(activeFlashSales.getContent());
        Map<UUID, List<StorefrontFlashSaleItemView>> itemsByFlashSaleId = itemViews.stream()
                .collect(Collectors.groupingBy(
                        StorefrontFlashSaleItemView::getFlashSaleId,
                        LinkedHashMap::new,
                        Collectors.toCollection(ArrayList::new)));
        Map<UUID, String> imageByProductId = buildStorefrontImageMap(itemViews);
        List<FlashSaleResponse> content = activeFlashSales.getContent().stream()
                .map(flashSale -> toStorefrontResponse(
                        flashSale,
                        itemsByFlashSaleId.getOrDefault(flashSale.getId(), List.of()),
                        imageByProductId,
                        itemCountByFlashSaleId.getOrDefault(flashSale.getId(), 0)))
                .toList();

        return PageResponse.<FlashSaleResponse>builder()
                .data(content)
                .page(activeFlashSales.getNumber() + 1)
                .perPage(activeFlashSales.getSize())
                .total(activeFlashSales.getTotalElements())
                .lastPage(activeFlashSales.getTotalPages())
                .build();
    }

    private List<StorefrontFlashSaleItemView> loadInitialStorefrontItems(
            List<ActiveStorefrontFlashSaleView> activeFlashSales) {
        if (activeFlashSales == null || activeFlashSales.isEmpty()) {
            return List.of();
        }

        return activeFlashSales.stream()
                .flatMap(flashSale -> flashSaleItemRepository
                        .findActiveStorefrontItemsByFlashSaleId(
                                flashSale.getId(),
                                PageRequest.of(0, STOREFRONT_ITEM_PAGE_SIZE))
                        .getContent()
                        .stream())
                .toList();
    }

    private Map<UUID, String> buildStorefrontImageMap(List<StorefrontFlashSaleItemView> itemViews) {
        List<UUID> productIds = itemViews.stream()
                .map(StorefrontFlashSaleItemView::getProductId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (productIds.isEmpty()) {
            return Map.of();
        }
        return productImageRepository.findPreferredImageMapByProductIds(productIds);
    }

    private FlashSaleResponse toStorefrontResponse(
            ActiveStorefrontFlashSaleView flashSale,
            List<StorefrontFlashSaleItemView> items,
            Map<UUID, String> imageByProductId,
            int itemCount) {
        List<FlashSaleItemResponse> itemResponses = items.stream()
                .map(item -> toStorefrontItemResponse(item, imageByProductId))
                .toList();

        return FlashSaleResponse.builder()
                .id(flashSale.getId().toString())
                .name(flashSale.getName())
                .description(flashSale.getDescription())
                .startTime(flashSale.getStartTime())
                .endTime(flashSale.getEndTime())
                .status(FlashSaleStatus.ACTIVE.name())
                .itemCount(itemCount)
                .items(itemResponses)
                .createdAt(flashSale.getCreatedAt())
                .build();
    }

    private FlashSaleItemResponse toStorefrontItemResponse(
            StorefrontFlashSaleItemView item,
            Map<UUID, String> imageByProductId) {
        UUID productUuid = item.getProductId();
        int flashStock = item.getFlashStock() != null ? item.getFlashStock() : 0;
        int soldCount = item.getSoldCount() != null ? item.getSoldCount() : 0;
        int stockQuantity = item.getStockQuantity() != null ? Math.max(0, item.getStockQuantity()) : 0;
        int remainingStock = Math.min(Math.max(0, flashStock - soldCount), stockQuantity);

        return FlashSaleItemResponse.builder()
                .id(item.getId() != null ? item.getId().toString() : "")
                .productId(productUuid != null ? productUuid.toString() : "")
                .productSlug(item.getProductSlug() != null ? item.getProductSlug() : "")
                .variantId(item.getVariantId() != null ? item.getVariantId().toString() : "")
                .productName(item.getProductName() != null ? item.getProductName() : "")
                .variantName(item.getVariantName())
                .imageUrl(productUuid == null ? "" : imageByProductId.getOrDefault(productUuid, ""))
                .originalPrice(item.getOriginalPrice())
                .flashPrice(item.getFlashPrice())
                .flashStock(flashStock)
                .soldCount(soldCount)
                .remainingStock(remainingStock)
                .stockQuantity(stockQuantity)
                .build();
    }

    private FlashSaleResponse toResponse(FlashSale fs) {
        List<StorefrontFlashSaleItemView> itemViews = flashSaleItemRepository.findStorefrontItemsByFlashSaleIds(List.of(fs.getId()));
        List<UUID> variantIds = itemViews.stream()
                .map(StorefrontFlashSaleItemView::getVariantId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<UUID, Long> grossByVariantId = buildVariantSoldMap(variantIds);
        Map<UUID, Long> returnedByVariantId = buildVariantReturnedMap(variantIds);
        Map<UUID, String> imageByProductId = buildStorefrontImageMap(itemViews);

        List<FlashSaleItemResponse> itemResponses = itemViews.stream()
                .map(item -> toAdminItemResponse(item, imageByProductId, grossByVariantId, returnedByVariantId))
                .collect(Collectors.toList());

        return FlashSaleResponse.builder()
                .id(fs.getId().toString())
                .name(fs.getName())
                .description(fs.getDescription())
                .startTime(fs.getStartTime())
                .endTime(fs.getEndTime())
                .status(fs.getStatus().name())
                .itemCount(itemResponses.size())
                .items(itemResponses)
                .createdAt(fs.getCreatedAt())
                .build();
    }

    private FlashSaleItemResponse toAdminItemResponse(
            StorefrontFlashSaleItemView item,
            Map<UUID, String> imageByProductId,
            Map<UUID, Long> grossByVariantId,
            Map<UUID, Long> returnedByVariantId) {
        UUID productUuid = item.getProductId();
        UUID variantUuid = item.getVariantId();
        int flashStock = item.getFlashStock() != null ? item.getFlashStock() : 0;
        int soldCount = item.getSoldCount() != null ? item.getSoldCount() : 0;
        long grossSoldQty = variantUuid == null ? 0L : grossByVariantId.getOrDefault(variantUuid, 0L);
        long returnedQty = variantUuid == null ? 0L : returnedByVariantId.getOrDefault(variantUuid, 0L);

        return FlashSaleItemResponse.builder()
                .id(item.getId() != null ? item.getId().toString() : "")
                .productId(productUuid != null ? productUuid.toString() : "")
                .productSlug(item.getProductSlug() != null ? item.getProductSlug() : "")
                .variantId(variantUuid != null ? variantUuid.toString() : "")
                .productName(item.getProductName() != null ? item.getProductName() : "")
                .variantName(item.getVariantName())
                .imageUrl(productUuid == null ? "" : imageByProductId.getOrDefault(productUuid, ""))
                .originalPrice(item.getOriginalPrice())
                .flashPrice(item.getFlashPrice())
                .flashStock(flashStock)
                .soldCount(soldCount)
                .remainingStock(flashStock - soldCount)
                .grossSoldQty(grossSoldQty)
                .returnedQty(returnedQty)
                .netSoldQty(Math.max(grossSoldQty - returnedQty, 0L))
                .stockQuantity(item.getStockQuantity())
                .build();
    }

    private FlashSaleResponse toSummaryResponse(FlashSale flashSale, int itemCount) {
        return FlashSaleResponse.builder()
                .id(flashSale.getId().toString())
                .name(flashSale.getName())
                .description(flashSale.getDescription())
                .startTime(flashSale.getStartTime())
                .endTime(flashSale.getEndTime())
                .status(flashSale.getStatus().name())
                .itemCount(itemCount)
                .items(List.of())
                .createdAt(flashSale.getCreatedAt())
                .build();
    }

    private Map<UUID, Integer> buildFlashSaleItemCountMap(List<UUID> flashSaleIds) {
        if (flashSaleIds == null || flashSaleIds.isEmpty()) {
            return Map.of();
        }

        return flashSaleRepository.countItemsByFlashSaleIds(flashSaleIds).stream()
                .collect(Collectors.toMap(
                        row -> (UUID) row[0],
                        row -> ((Number) row[1]).intValue()));
    }

    private Map<UUID, Long> buildVariantSoldMap(List<UUID> variantIds) {
        if (variantIds == null || variantIds.isEmpty()) {
            return Map.of();
        }

        return orderItemRepository.sumSoldQuantityByVariantIds(variantIds).stream()
                .collect(Collectors.toMap(
                        row -> (UUID) row[0],
                        row -> ((Number) row[1]).longValue(),
                        Long::sum));
    }

    private Map<UUID, Long> buildVariantReturnedMap(List<UUID> variantIds) {
        if (variantIds == null || variantIds.isEmpty()) {
            return Map.of();
        }

        return returnItemRepository.sumReturnedQuantityByVariantIds(variantIds).stream()
                .collect(Collectors.toMap(
                        row -> (UUID) row[0],
                        row -> ((Number) row[1]).longValue(),
                        Long::sum));
    }

    private FlashSaleStatus resolveStatus(LocalDateTime startTime, LocalDateTime endTime) {
        LocalDateTime now = promotionStatusSyncService.now();
        if (endTime.isBefore(now)) {
            return FlashSaleStatus.ENDED;
        }
        if (!startTime.isAfter(now)) {
            return FlashSaleStatus.ACTIVE;
        }
        return FlashSaleStatus.SCHEDULED;
    }

    private FlashSaleStatus resolveStatusForExistingFlashSale(
            FlashSaleStatus currentStatus,
            LocalDateTime startTime,
            LocalDateTime endTime
    ) {
        if (currentStatus == FlashSaleStatus.HIDDEN) {
            return FlashSaleStatus.HIDDEN;
        }
        return resolveStatus(startTime, endTime);
    }
}
