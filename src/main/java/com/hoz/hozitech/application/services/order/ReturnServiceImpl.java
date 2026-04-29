package com.hoz.hozitech.application.services.order;

import com.hoz.hozitech.application.constant.PaginationConstant;
import com.hoz.hozitech.application.repositories.OrderRepository;
import com.hoz.hozitech.application.repositories.OrderStatusHistoryRepository;
import com.hoz.hozitech.application.repositories.RefundTransactionRepository;
import com.hoz.hozitech.application.repositories.ReturnItemRepository;
import com.hoz.hozitech.application.repositories.ReturnRequestRepository;
import com.hoz.hozitech.application.repositories.ReturnStatusHistoryRepository;
import com.hoz.hozitech.application.services.notification.AdminNotificationService;
import com.hoz.hozitech.application.services.notification.AdminNotificationTemplates;
import com.hoz.hozitech.application.services.notification.NotificationService;
import com.hoz.hozitech.application.services.notification.UserNotificationTemplates;
import com.hoz.hozitech.application.services.setting.SettingService;
import com.hoz.hozitech.application.services.storage.FileStorageService;
import com.hoz.hozitech.application.specifications.ReturnRequestSpecification;
import com.hoz.hozitech.domain.dtos.request.CreateReturnRequest;
import com.hoz.hozitech.domain.dtos.request.ProcessRefundRequest;
import com.hoz.hozitech.domain.dtos.request.ReviewReturnRequest;
import com.hoz.hozitech.domain.dtos.request.UpdateReturnStatusRequest;
import com.hoz.hozitech.domain.dtos.response.PageResponse;
import com.hoz.hozitech.domain.dtos.response.ReturnRequestResponse;
import com.hoz.hozitech.domain.entities.Order;
import com.hoz.hozitech.domain.entities.OrderItem;
import com.hoz.hozitech.domain.entities.OrderStatusHistory;
import com.hoz.hozitech.domain.entities.RefundTransaction;
import com.hoz.hozitech.domain.entities.ReturnItem;
import com.hoz.hozitech.domain.entities.ReturnRequest;
import com.hoz.hozitech.domain.entities.ReturnStatusHistory;
import com.hoz.hozitech.domain.enums.BusinessErrorCode;
import com.hoz.hozitech.domain.enums.OrderStatus;
import com.hoz.hozitech.domain.enums.PaymentStatus;
import com.hoz.hozitech.domain.enums.RefundStatus;
import com.hoz.hozitech.domain.enums.ReturnRequestStatus;
import com.hoz.hozitech.web.exceptions.BusinessException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ReturnServiceImpl implements ReturnService {

    private static final int MONEY_SCALE = 2;
    private static final int MAX_EVIDENCE_IMAGES = 5;
    private static final String RETURN_EVIDENCE_FOLDER = "returns";
    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP);

    private static final Map<ReturnRequestStatus, Set<ReturnRequestStatus>> ALLOWED_STATUS_TRANSITIONS = Map.ofEntries(
            Map.entry(ReturnRequestStatus.REQUESTED, EnumSet.of(ReturnRequestStatus.APPROVED, ReturnRequestStatus.REJECTED, ReturnRequestStatus.CANCELLED)),
            Map.entry(ReturnRequestStatus.APPROVED, EnumSet.of(ReturnRequestStatus.IN_TRANSIT, ReturnRequestStatus.RECEIVED, ReturnRequestStatus.REJECTED, ReturnRequestStatus.CANCELLED)),
            Map.entry(ReturnRequestStatus.IN_TRANSIT, EnumSet.of(ReturnRequestStatus.RECEIVED, ReturnRequestStatus.CANCELLED)),
            Map.entry(ReturnRequestStatus.RECEIVED, EnumSet.of(ReturnRequestStatus.QC_PASSED, ReturnRequestStatus.QC_FAILED)),
            Map.entry(ReturnRequestStatus.QC_PASSED, EnumSet.of(ReturnRequestStatus.REFUND_PENDING)),
            Map.entry(ReturnRequestStatus.REFUND_PENDING, EnumSet.of(ReturnRequestStatus.REFUNDED)),
            Map.entry(ReturnRequestStatus.REFUNDED, EnumSet.of(ReturnRequestStatus.CLOSED)),
            Map.entry(ReturnRequestStatus.REJECTED, EnumSet.of(ReturnRequestStatus.CLOSED)),
            Map.entry(ReturnRequestStatus.QC_FAILED, EnumSet.of(ReturnRequestStatus.CLOSED)),
            Map.entry(ReturnRequestStatus.CANCELLED, EnumSet.noneOf(ReturnRequestStatus.class)),
            Map.entry(ReturnRequestStatus.CLOSED, EnumSet.noneOf(ReturnRequestStatus.class))
    );

    private final ReturnRequestRepository returnRequestRepository;
    private final ReturnItemRepository returnItemRepository;
    private final RefundTransactionRepository refundTransactionRepository;
    private final OrderRepository orderRepository;
    private final OrderStatusHistoryRepository orderStatusHistoryRepository;
    private final ReturnStatusHistoryRepository returnStatusHistoryRepository;
    private final NotificationService notificationService;
    private final AdminNotificationService adminNotificationService;
    private final SettingService settingService;
    private final ReturnEmailSender returnEmailSender;
    private final FileStorageService fileStorageService;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional
    public ReturnRequestResponse createReturnRequest(
            UUID userId,
            CreateReturnRequest request,
            List<MultipartFile> evidenceFiles,
            String idempotencyKey) {
        String normalizedKey = normalizeIdempotencyKey(idempotencyKey);
        if (normalizedKey == null) {
            throw new BusinessException(
                    BusinessErrorCode.RETURN_IDEMPOTENCY_KEY_REQUIRED,
                    "Idempotency key is required for creating return request");
        }

        acquirePgAdvisoryTransactionLock("return:create:" + userId + ":" + normalizedKey);
        ReturnRequest existing = returnRequestRepository.findByUserIdAndIdempotencyKey(userId, normalizedKey)
                .orElse(null);
        if (existing != null) {
            return mapToResponse(existing);
        }

        String normalizedOrderNumber = trimToNull(request.getOrderNumber());
        Order order = orderRepository.findByOrderNumber(normalizedOrderNumber)
                .orElseThrow(() -> new BusinessException(BusinessErrorCode.ORDER_NOT_FOUND, "Order not found"));

        if (!order.getUser().getId().equals(userId)) {
            throw new BusinessException(BusinessErrorCode.RETURN_NOT_OWNED, "Return request does not belong to user");
        }

        if (order.getOrderStatus() != OrderStatus.SHIPPED || order.getPaymentStatus() != PaymentStatus.COMPLETED) {
            throw new BusinessException(
                    BusinessErrorCode.ORDER_NOT_ELIGIBLE_FOR_RETURN,
                    "Order is not eligible for return. Only shipped & paid orders can be returned");
        }

        Map<String, List<OrderItem>> orderItemsBySku = order.getOrderItems().stream()
                .filter(item -> item.getVariant() != null)
                .filter(item -> trimToNull(item.getVariant().getSku()) != null)
                .collect(Collectors.groupingBy(
                        item -> normalizeSku(item.getVariant().getSku()),
                        LinkedHashMap::new,
                        Collectors.toList()));
        List<MultipartFile> normalizedEvidenceFiles = normalizeEvidenceFiles(evidenceFiles);
        validateEvidenceFiles(normalizedEvidenceFiles);

        List<ReturnItem> returnItems = new ArrayList<>();
        BigDecimal requestedAmount = ZERO;
        Set<String> requestedSkus = new HashSet<>();

        for (CreateReturnRequest.ReturnItemRequest reqItem : request.getItems()) {
            String normalizedSku = normalizeSku(reqItem.getSku());
            if (!requestedSkus.add(normalizedSku)) {
                throw new BusinessException(
                        BusinessErrorCode.RETURN_ITEM_NOT_BELONG_TO_ORDER,
                        "Duplicate SKU in return request: " + reqItem.getSku());
            }

            List<OrderItem> matchedItems = orderItemsBySku.getOrDefault(normalizedSku, List.of());
            if (matchedItems.isEmpty()) {
                throw new BusinessException(
                        BusinessErrorCode.RETURN_ITEM_NOT_BELONG_TO_ORDER,
                        "Order item does not belong to order for SKU: " + reqItem.getSku());
            }
            if (matchedItems.size() > 1) {
                throw new BusinessException(
                        BusinessErrorCode.RETURN_ITEM_NOT_BELONG_TO_ORDER,
                        "Multiple order items found for SKU: " + reqItem.getSku());
            }
            OrderItem orderItem = matchedItems.get(0);

            if (reqItem.getQuantity() <= 0 || reqItem.getQuantity() > orderItem.getQuantity()) {
                throw new BusinessException(
                        BusinessErrorCode.RETURN_QUANTITY_INVALID,
                        "Invalid return quantity for SKU: " + reqItem.getSku());
            }

            if (returnItemRepository.existsInNonRejectedRequest(orderItem.getId())) {
                throw new BusinessException(
                        BusinessErrorCode.RETURN_ITEM_ALREADY_REQUESTED,
                        "This order item already has a return request");
            }

            BigDecimal lineAmount = money(orderItem.getUnitPrice().multiply(BigDecimal.valueOf(reqItem.getQuantity())));
            requestedAmount = requestedAmount.add(lineAmount);

            ReturnItem returnItem = ReturnItem.builder()
                    .orderItem(orderItem)
                    .productName(orderItem.getProductName())
                    .variantName(orderItem.getVariantName())
                    .unitPrice(money(orderItem.getUnitPrice()))
                    .requestedQuantity(reqItem.getQuantity())
                    .approvedQuantity(null)
                    .lineAmount(lineAmount)
                    .build();
            returnItems.add(returnItem);
        }

        if (returnItems.isEmpty()) {
            throw new BusinessException(BusinessErrorCode.RETURN_ITEM_NOT_FOUND, "No valid return item found");
        }

        List<String> evidenceImageUrls = List.of();
        try {
            evidenceImageUrls = uploadEvidenceImages(normalizedEvidenceFiles);

            ReturnRequest returnRequest = ReturnRequest.builder()
                    .returnNumber(generateReturnNumber())
                    .idempotencyKey(normalizedKey)
                    .reason(request.getReason())
                    .evidenceNote(trimToNull(request.getEvidenceNote()))
                    .evidenceImageUrls(new ArrayList<>(evidenceImageUrls))
                    .status(ReturnRequestStatus.REQUESTED)
                    .refundStatus(RefundStatus.PENDING)
                    .requestedAmount(money(requestedAmount))
                    .approvedAmount(null)
                    .refundAmount(ZERO)
                    .order(order)
                    .user(order.getUser())
                    .items(new ArrayList<>())
                    .build();

            for (ReturnItem item : returnItems) {
                item.setReturnRequest(returnRequest);
                returnRequest.getItems().add(item);
            }

            ReturnRequest saved = returnRequestRepository.save(returnRequest);

            appendReturnStatusHistory(saved, ReturnRequestStatus.REQUESTED, "Yêu cầu trả hàng mới đã được tạo");

            notificationService.createForUser(userId, UserNotificationTemplates.returnCreated(saved));
            adminNotificationService.createShared(AdminNotificationTemplates.returnCreated(saved), false);

            returnEmailSender.sendReturnUpdatedEmail(
                    saved,
                    "Yêu cầu trả hàng đã được tiếp nhận",
                    "Yêu cầu " + saved.getReturnNumber() + " của bạn đã được tạo thành công.");

            return mapToFreshResponse(saved);
        } catch (RuntimeException ex) {
            cleanupEvidenceImages(evidenceImageUrls);
            throw ex;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ReturnRequestResponse> getMyReturnRequests(UUID userId, String status, String keyword, int page, int size) {
        var pageable = PaginationConstant.of(page, size);
        ReturnRequestStatus parsedStatus = parseNullableStatus(status);

        Specification<ReturnRequest> spec = ReturnRequestSpecification.filter(userId, parsedStatus, keyword);
        Page<ReturnRequest> result = returnRequestRepository.findAll(spec, pageable);
        return PageResponse.of(result.map(this::mapToResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public ReturnRequestResponse getReturnByNumberForUser(UUID userId, String returnNumber) {
        ReturnRequest rr = returnRequestRepository.findByReturnNumber(returnNumber)
                .orElseThrow(() -> new BusinessException(BusinessErrorCode.RETURN_NOT_FOUND, "Return request not found"));

        if (!rr.getUser().getId().equals(userId)) {
            throw new BusinessException(BusinessErrorCode.RETURN_NOT_OWNED, "Return request does not belong to user");
        }

        return mapToResponse(rr);
    }

    @Override
    @Transactional
    public ReturnRequestResponse cancelReturnRequest(UUID userId, String returnNumber) {
        ReturnRequest rr = returnRequestRepository.findByReturnNumberForUpdate(returnNumber == null ? null : returnNumber.trim())
                .orElseThrow(() -> new BusinessException(BusinessErrorCode.RETURN_NOT_FOUND, "Return request not found"));

        if (!rr.getUser().getId().equals(userId)) {
            throw new BusinessException(BusinessErrorCode.RETURN_NOT_OWNED, "Return request does not belong to user");
        }

        if (rr.getStatus() != ReturnRequestStatus.REQUESTED) {
            throw new BusinessException(
                    BusinessErrorCode.RETURN_CANCELLATION_NOT_ALLOWED,
                    "Only requested return can be cancelled");
        }

        rr.setStatus(ReturnRequestStatus.CANCELLED);
        rr.setResolvedAt(LocalDateTime.now());

        ReturnRequest saved = returnRequestRepository.save(rr);
        appendReturnStatusHistory(saved, ReturnRequestStatus.CANCELLED, "Khách hàng tự hủy yêu cầu trả hàng");

        returnEmailSender.sendReturnUpdatedEmail(
                saved,
                "Yêu cầu trả hàng đã được hủy",
                "Bạn đã hủy yêu cầu " + saved.getReturnNumber() + " thành công.");

        return mapToFreshResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ReturnRequestResponse> getAllReturnRequests(String status, String keyword, int page, int size) {
        var pageable = PaginationConstant.of(page, size);
        ReturnRequestStatus parsedStatus = parseNullableStatus(status);

        Specification<ReturnRequest> spec = ReturnRequestSpecification.filter(null, parsedStatus, keyword);
        Page<ReturnRequest> result = returnRequestRepository.findAll(spec, pageable);
        return PageResponse.of(result.map(this::mapToResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public ReturnRequestResponse getReturnByNumberForAdmin(String returnNumber) {
        ReturnRequest rr = returnRequestRepository.findByReturnNumber(returnNumber)
                .orElseThrow(() -> new BusinessException(BusinessErrorCode.RETURN_NOT_FOUND, "Return request not found"));
        return mapToResponse(rr);
    }

    @Override
    @Transactional
    public ReturnRequestResponse reviewReturnRequest(UUID returnRequestId, ReviewReturnRequest request) {
        ReturnRequest rr = returnRequestRepository.findByIdForUpdate(returnRequestId)
                .orElseThrow(() -> new BusinessException(BusinessErrorCode.RETURN_NOT_FOUND, "Return request not found"));

        if (rr.getStatus() != ReturnRequestStatus.REQUESTED) {
            throw new BusinessException(
                    BusinessErrorCode.RETURN_REVIEW_NOT_ALLOWED,
                    "Only requested returns can be reviewed");
        }

        String note = trimToNull(request.getNote());

        if (Boolean.TRUE.equals(request.getApproved())) {
            BigDecimal approvedAmount = request.getApprovedAmount() != null
                    ? money(request.getApprovedAmount())
                    : money(rr.getRequestedAmount());
            if (approvedAmount.compareTo(ZERO) <= 0 || approvedAmount.compareTo(money(rr.getRequestedAmount())) > 0) {
                throw new BusinessException(BusinessErrorCode.REFUND_AMOUNT_INVALID, "Approved amount is invalid");
            }

            rr.setStatus(ReturnRequestStatus.APPROVED);
            rr.setRefundStatus(RefundStatus.PENDING);
            rr.setApprovedAmount(approvedAmount);
            rr.setAdminNote(note);

            for (ReturnItem item : rr.getItems()) {
                item.setApprovedQuantity(item.getRequestedQuantity());
            }

            notificationService.createForUser(rr.getUser().getId(), UserNotificationTemplates.returnApproved(rr));
            adminNotificationService.createShared(AdminNotificationTemplates.returnApproved(rr), true);
        } else {
            rr.setStatus(ReturnRequestStatus.REJECTED);
            rr.setRefundStatus(RefundStatus.FAILED);
            rr.setApprovedAmount(ZERO);
            rr.setResolvedAt(LocalDateTime.now());
            rr.setAdminNote(note);

            for (ReturnItem item : rr.getItems()) {
                item.setApprovedQuantity(0);
            }

            notificationService.createForUser(rr.getUser().getId(), UserNotificationTemplates.returnRejected(rr));
            adminNotificationService.createShared(AdminNotificationTemplates.returnRejected(rr), true);
        }

        ReturnRequest saved = returnRequestRepository.save(rr);

        if (Boolean.TRUE.equals(request.getApproved())) {
            appendReturnStatusHistory(saved, ReturnRequestStatus.APPROVED, "Admin đã duyệt yêu cầu trả hàng");
            returnEmailSender.sendReturnUpdatedEmail(
                    saved,
                    "Yêu cầu trả hàng đã được duyệt",
                    "Yêu cầu " + saved.getReturnNumber() + " đã được admin duyệt.");
        } else {
            appendReturnStatusHistory(saved, ReturnRequestStatus.REJECTED, "Admin đã từ chối yêu cầu trả hàng" + (note != null ? ": " + note : ""));
            returnEmailSender.sendReturnUpdatedEmail(
                    saved,
                    "Yêu cầu trả hàng bị từ chối",
                    "Yêu cầu " + saved.getReturnNumber() + " đã bị từ chối.");
        }

        return mapToFreshResponse(saved);
    }

    @Override
    @Transactional
    public ReturnRequestResponse updateReturnStatus(UUID returnRequestId, UpdateReturnStatusRequest request) {
        ReturnRequest rr = returnRequestRepository.findByIdForUpdate(returnRequestId)
                .orElseThrow(() -> new BusinessException(BusinessErrorCode.RETURN_NOT_FOUND, "Return request not found"));

        ReturnRequestStatus targetStatus = parseStatus(request.getStatus());
        ReturnRequestStatus currentStatus = rr.getStatus();

        if (currentStatus == targetStatus) {
            return mapToResponse(rr);
        }

        validateTransition(currentStatus, targetStatus);

        rr.setStatus(targetStatus);
        if (trimToNull(request.getNote()) != null) {
            rr.setAdminNote(trimToNull(request.getNote()));
        }

        switch (targetStatus) {
            case REFUND_PENDING -> rr.setRefundStatus(RefundStatus.PROCESSING);
            case REFUNDED -> {
                rr.setRefundStatus(RefundStatus.SUCCESS);
                if (rr.getRefundAmount() == null || rr.getRefundAmount().compareTo(ZERO) <= 0) {
                    BigDecimal fallback = rr.getApprovedAmount() != null ? rr.getApprovedAmount() : rr.getRequestedAmount();
                    rr.setRefundAmount(money(fallback));
                }
                rr.setResolvedAt(LocalDateTime.now());
                markOrderAsRefunded(rr.getOrder(), "Hoàn tiền cho yêu cầu " + rr.getReturnNumber());
            }
            case QC_FAILED -> {
                rr.setRefundStatus(RefundStatus.FAILED);
                rr.setResolvedAt(LocalDateTime.now());
            }
            case REJECTED, CANCELLED, CLOSED -> {
                if (rr.getResolvedAt() == null) {
                    rr.setResolvedAt(LocalDateTime.now());
                }
            }
            default -> {
                // No-op
            }
        }

        ReturnRequest saved = returnRequestRepository.save(rr);

        appendReturnStatusHistory(saved, targetStatus, targetStatus.getDescription());

        notificationService.createForUser(saved.getUser().getId(), UserNotificationTemplates.returnStatusChanged(saved));
        adminNotificationService.createShared(AdminNotificationTemplates.returnStatusChanged(saved), true);

        returnEmailSender.sendReturnUpdatedEmail(
                saved,
                "Trạng thái yêu cầu trả hàng đã cập nhật",
                "Yêu cầu " + saved.getReturnNumber() + " chuyển sang trạng thái " + saved.getStatus().getDescription() + ".");

        return mapToFreshResponse(saved);
    }

    @Override
    @Transactional
    public ReturnRequestResponse processRefund(UUID returnRequestId, ProcessRefundRequest request, String idempotencyKey) {
        String normalizedKey = normalizeIdempotencyKey(idempotencyKey);
        if (normalizedKey == null) {
            throw new BusinessException(
                    BusinessErrorCode.REFUND_IDEMPOTENCY_KEY_REQUIRED,
                    "Idempotency key is required for refund processing");
        }

        acquirePgAdvisoryTransactionLock("return:refund:" + returnRequestId + ":" + normalizedKey);

        RefundTransaction existingRefund = refundTransactionRepository.findByIdempotencyKey(normalizedKey).orElse(null);
        if (existingRefund != null) {
            return mapToResponse(existingRefund.getReturnRequest());
        }

        ReturnRequest rr = returnRequestRepository.findByIdForUpdate(returnRequestId)
                .orElseThrow(() -> new BusinessException(BusinessErrorCode.RETURN_NOT_FOUND, "Return request not found"));

        if (rr.getStatus() != ReturnRequestStatus.QC_PASSED
                && rr.getStatus() != ReturnRequestStatus.REFUND_PENDING) {
            throw new BusinessException(
                    BusinessErrorCode.REFUND_NOT_ALLOWED,
                    "Refund is allowed only for QC-passed/refund-pending return requests");
        }

        BigDecimal baseAmount = rr.getApprovedAmount() != null ? rr.getApprovedAmount() : rr.getRequestedAmount();
        baseAmount = money(baseAmount);

        if (request.getAmount() == null) {
            throw new BusinessException(BusinessErrorCode.REFUND_AMOUNT_INVALID, "Refund amount is required");
        }
        BigDecimal refundAmount = money(request.getAmount());
        if (refundAmount.compareTo(ZERO) <= 0 || refundAmount.compareTo(baseAmount) > 0) {
            throw new BusinessException(BusinessErrorCode.REFUND_AMOUNT_INVALID, "Refund amount is invalid");
        }

        String provider = normalizeProvider(request.getProvider());
        if (provider == null) {
            throw new BusinessException(BusinessErrorCode.REFUND_NOT_ALLOWED, "Refund provider is required");
        }

        String transactionId = trimToNull(request.getTransactionId());
        if (transactionId == null) {
            throw new BusinessException(BusinessErrorCode.REFUND_NOT_ALLOWED, "Refund transactionId is required");
        }

        String adminNote = trimToNull(request.getAdminNote());
        if (adminNote == null) {
            throw new BusinessException(BusinessErrorCode.REFUND_NOT_ALLOWED, "Refund admin note is required");
        }

        String currency = normalizeCurrency(request.getCurrency());
        if (currency == null) {
            currency = normalizeCurrency(settingService.getSettingValue("CURRENCY"));
        }
        if (currency == null) {
            currency = "VND";
        }

        RefundTransaction refundTx = RefundTransaction.builder()
                .idempotencyKey(normalizedKey)
                .provider(provider)
                .transactionId(transactionId)
                .status(RefundStatus.SUCCESS)
                .amount(refundAmount)
                .currency(currency)
                .rawPayload(trimToNull(request.getRawPayload()))
                .returnRequest(rr)
                .order(rr.getOrder())
                .build();
        refundTransactionRepository.save(refundTx);

        rr.setStatus(ReturnRequestStatus.REFUNDED);
        rr.setRefundStatus(RefundStatus.SUCCESS);
        rr.setAdminNote(adminNote);
        rr.setRefundAmount(refundAmount);
        rr.setResolvedAt(LocalDateTime.now());
        if (rr.getApprovedAmount() == null) {
            rr.setApprovedAmount(baseAmount);
        }

        markOrderAsRefunded(rr.getOrder(), "Hoàn tiền thành công cho yêu cầu " + rr.getReturnNumber());
        ReturnRequest saved = returnRequestRepository.save(rr);

        appendReturnStatusHistory(saved, ReturnRequestStatus.REFUNDED, "Hoàn tiền thành công, mã giao dịch: " + transactionId);

        notificationService.createForUser(saved.getUser().getId(), UserNotificationTemplates.refundSuccess(saved));
        adminNotificationService.createShared(AdminNotificationTemplates.refundSuccess(saved), true);

        returnEmailSender.sendReturnUpdatedEmail(
                saved,
                "Hoàn tiền thành công",
                "Yêu cầu " + saved.getReturnNumber() + " đã được hoàn tiền thành công.");

        return mapToFreshResponse(saved);
    }

    private ReturnRequestResponse mapToResponse(ReturnRequest rr) {
        List<ReturnRequestResponse.ReturnItemData> itemResponses = rr.getItems().stream()
                .map(item -> ReturnRequestResponse.ReturnItemData.builder()
                        .id(item.getId())
                        .orderItemId(item.getOrderItem() != null ? item.getOrderItem().getId() : null)
                        .productSlug(item.getOrderItem() != null
                                && item.getOrderItem().getVariant() != null
                                && item.getOrderItem().getVariant().getProduct() != null
                                ? item.getOrderItem().getVariant().getProduct().getSlug()
                                : null)
                        .sku(item.getOrderItem() != null && item.getOrderItem().getVariant() != null
                                ? item.getOrderItem().getVariant().getSku()
                                : null)
                        .productName(item.getProductName())
                        .variantName(item.getVariantName())
                        .unitPrice(item.getUnitPrice())
                        .requestedQuantity(item.getRequestedQuantity())
                        .approvedQuantity(item.getApprovedQuantity())
                        .lineAmount(item.getLineAmount())
                        .build())
                .collect(Collectors.toList());

        List<ReturnRequestResponse.RefundTransactionData> refundResponses = rr.getRefundTransactions().stream()
                .map(tx -> ReturnRequestResponse.RefundTransactionData.builder()
                        .id(tx.getId())
                        .idempotencyKey(tx.getIdempotencyKey())
                        .provider(tx.getProvider())
                        .transactionId(tx.getTransactionId())
                        .status(tx.getStatus().name())
                        .amount(tx.getAmount())
                        .currency(tx.getCurrency())
                        .failureReason(tx.getFailureReason())
                        .createdAt(tx.getCreatedAt())
                        .build())
                .collect(Collectors.toList());

        List<ReturnRequestResponse.ReturnStatusHistoryData> historyResponses = rr.getStatusHistories().stream()
                .map(h -> ReturnRequestResponse.ReturnStatusHistoryData.builder()
                        .id(h.getId())
                        .status(h.getStatus().name())
                        .description(h.getDescription())
                        .createdAt(h.getCreatedAt())
                        .build())
                .collect(Collectors.toList());

        return ReturnRequestResponse.builder()
                .id(rr.getId())
                .returnNumber(rr.getReturnNumber())
                .orderId(rr.getOrder() != null ? rr.getOrder().getId() : null)
                .orderNumber(rr.getOrder() != null ? rr.getOrder().getOrderNumber() : null)
                .userId(rr.getUser() != null ? rr.getUser().getId() : null)
                .userName(rr.getUser() != null ? rr.getUser().getFullName() : null)
                .userEmail(rr.getUser() != null ? rr.getUser().getEmail() : null)
                .status(rr.getStatus().name())
                .refundStatus(rr.getRefundStatus().name())
                .reason(rr.getReason())
                .evidenceNote(rr.getEvidenceNote())
                .evidenceImageUrls(rr.getEvidenceImageUrls() == null ? List.of() : new ArrayList<>(rr.getEvidenceImageUrls()))
                .adminNote(rr.getAdminNote())
                .requestedAmount(rr.getRequestedAmount())
                .approvedAmount(rr.getApprovedAmount())
                .refundAmount(rr.getRefundAmount())
                .createdAt(rr.getCreatedAt())
                .updatedAt(rr.getUpdatedAt())
                .resolvedAt(rr.getResolvedAt())
                .items(itemResponses)
                .refunds(refundResponses)
                .statusHistories(historyResponses)
                .build();
    }

    private ReturnRequestResponse mapToFreshResponse(ReturnRequest rr) {
        entityManager.flush();
        entityManager.refresh(rr);
        return mapToResponse(rr);
    }

    private List<MultipartFile> normalizeEvidenceFiles(List<MultipartFile> evidenceFiles) {
        if (evidenceFiles == null || evidenceFiles.isEmpty()) {
            return List.of();
        }

        return evidenceFiles.stream()
                .filter(Objects::nonNull)
                .filter(file -> !file.isEmpty())
                .collect(Collectors.toList());
    }

    private void validateEvidenceFiles(List<MultipartFile> evidenceFiles) {
        if (evidenceFiles.size() > MAX_EVIDENCE_IMAGES) {
            throw new BusinessException(
                    BusinessErrorCode.RETURN_EVIDENCE_IMAGE_LIMIT_EXCEEDED,
                    "A maximum of " + MAX_EVIDENCE_IMAGES + " evidence images is allowed")
                    .withMessageKey("error.return_evidence_image_limit_exceeded", MAX_EVIDENCE_IMAGES);
        }

        for (MultipartFile file : evidenceFiles) {
            String contentType = trimToNull(file.getContentType());
            if (contentType == null || !contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
                throw new BusinessException(
                        BusinessErrorCode.RETURN_EVIDENCE_IMAGE_INVALID,
                        "Only image files can be uploaded as return evidence");
            }
        }
    }

    private List<String> uploadEvidenceImages(List<MultipartFile> evidenceFiles) {
        if (evidenceFiles.isEmpty()) {
            return List.of();
        }

        List<String> uploadedUrls = new ArrayList<>(evidenceFiles.size());
        try {
            for (MultipartFile file : evidenceFiles) {
                uploadedUrls.add(fileStorageService.uploadFile(file, RETURN_EVIDENCE_FOLDER));
            }
            return uploadedUrls;
        } catch (RuntimeException ex) {
            cleanupEvidenceImages(uploadedUrls);
            throw ex;
        }
    }

    private void cleanupEvidenceImages(List<String> evidenceImageUrls) {
        if (evidenceImageUrls == null || evidenceImageUrls.isEmpty()) {
            return;
        }

        for (String imageUrl : evidenceImageUrls) {
            try {
                fileStorageService.deleteFile(imageUrl);
            } catch (RuntimeException cleanupEx) {
                log.warn("return_evidence_cleanup_failed imageUrl={}", imageUrl, cleanupEx);
            }
        }
    }

    private void appendReturnStatusHistory(ReturnRequest rr, ReturnRequestStatus status, String description) {
        returnStatusHistoryRepository.save(ReturnStatusHistory.builder()
                .returnRequest(rr)
                .status(status)
                .description(description)
                .build());
    }

    private void markOrderAsRefunded(Order order, String historyDescription) {
        if (order.getPaymentStatus() != PaymentStatus.REFUNDED) {
            order.setPaymentStatus(PaymentStatus.REFUNDED);
        }

        if (order.getOrderStatus() != OrderStatus.RETURNED) {
            order.setOrderStatus(OrderStatus.RETURNED);
            orderStatusHistoryRepository.save(OrderStatusHistory.builder()
                    .order(order)
                    .status(OrderStatus.RETURNED)
                    .description(historyDescription)
                    .build());
        }

        orderRepository.save(order);
    }

    private ReturnRequestStatus parseNullableStatus(String rawStatus) {
        String normalized = trimToNull(rawStatus);
        if (normalized == null) {
            return null;
        }
        return parseStatus(normalized);
    }

    private ReturnRequestStatus parseStatus(String rawStatus) {
        String normalized = trimToNull(rawStatus);
        if (normalized == null) {
            throw new BusinessException(BusinessErrorCode.RETURN_INVALID_STATUS, "Return status is required");
        }
        try {
            return ReturnRequestStatus.valueOf(normalized.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(
                    BusinessErrorCode.RETURN_INVALID_STATUS,
                    "Unsupported return status: " + rawStatus)
                    .withMessageKey("error.unsupported_return_status", rawStatus);
        }
    }

    private void validateTransition(ReturnRequestStatus currentStatus, ReturnRequestStatus nextStatus) {
        Set<ReturnRequestStatus> allowedStatuses = ALLOWED_STATUS_TRANSITIONS.getOrDefault(
                currentStatus,
                EnumSet.noneOf(ReturnRequestStatus.class));
        if (!allowedStatuses.contains(nextStatus)) {
            throw new BusinessException(
                    BusinessErrorCode.RETURN_STATUS_TRANSITION_NOT_ALLOWED,
                    "Cannot transition return status from " + currentStatus.name() + " to " + nextStatus.name())
                    .withMessageKey("error.return_status_transition_not_allowed", currentStatus.name(), nextStatus.name());
        }
    }

    private String generateReturnNumber() {
        String datePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        for (int i = 0; i < 5; i++) {
            String candidate = "RET-" + datePart + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase(Locale.ROOT);
            if (!returnRequestRepository.existsByReturnNumber(candidate)) {
                return candidate;
            }
        }
        throw new BusinessException(BusinessErrorCode.RETURN_NUMBER_GENERATION_FAILED, "Cannot generate return number");
    }

    private BigDecimal money(BigDecimal value) {
        if (value == null) {
            return ZERO;
        }
        return value.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    private String normalizeProvider(String provider) {
        String normalized = trimToNull(provider);
        if (normalized == null) return null;
        return normalized.toUpperCase(Locale.ROOT);
    }

    private String normalizeCurrency(String currency) {
        String normalized = trimToNull(currency);
        if (normalized == null) {
            return null;
        }
        return normalized.toUpperCase(Locale.ROOT);
    }

    private String normalizeSku(String sku) {
        String normalized = trimToNull(sku);
        return normalized == null ? null : normalized.toUpperCase(Locale.ROOT);
    }

    private String normalizeIdempotencyKey(String raw) {
        String normalized = trimToNull(raw);
        if (normalized == null) {
            return null;
        }
        if (normalized.length() > 120) {
            normalized = normalized.substring(0, 120);
        }
        return normalized;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private void acquirePgAdvisoryTransactionLock(String lockKey) {
        entityManager.createNativeQuery("SELECT pg_advisory_xact_lock(hashtext(:lockKey)::bigint)")
                .setParameter("lockKey", lockKey)
                .getSingleResult();
    }
}
