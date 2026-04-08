package com.hoz.hozitech.application.services.order;

import com.hoz.hozitech.application.constant.PaginationConstant;
import com.hoz.hozitech.application.repositories.OrderRepository;
import com.hoz.hozitech.application.repositories.OrderStatusHistoryRepository;
import com.hoz.hozitech.application.repositories.RefundTransactionRepository;
import com.hoz.hozitech.application.repositories.ReturnItemRepository;
import com.hoz.hozitech.application.repositories.ReturnRequestRepository;
import com.hoz.hozitech.application.services.notification.NotificationService;
import com.hoz.hozitech.application.services.setting.SettingService;
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
import com.hoz.hozitech.domain.enums.BusinessErrorCode;
import com.hoz.hozitech.domain.enums.OrderStatus;
import com.hoz.hozitech.domain.enums.PaymentStatus;
import com.hoz.hozitech.domain.enums.RefundStatus;
import com.hoz.hozitech.domain.enums.ReturnRequestStatus;
import com.hoz.hozitech.web.exceptions.BusinessException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReturnServiceImpl implements ReturnService {

    private static final int MONEY_SCALE = 2;
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
    private final NotificationService notificationService;
    private final SettingService settingService;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional
    public ReturnRequestResponse createReturnRequest(UUID userId, CreateReturnRequest request, String idempotencyKey) {
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

        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new BusinessException(BusinessErrorCode.ORDER_NOT_FOUND, "Order not found"));

        if (!order.getUser().getId().equals(userId)) {
            throw new BusinessException(BusinessErrorCode.RETURN_NOT_OWNED, "Return request does not belong to user");
        }

        if (order.getOrderStatus() != OrderStatus.SHIPPED || order.getPaymentStatus() != PaymentStatus.COMPLETED) {
            throw new BusinessException(
                    BusinessErrorCode.ORDER_NOT_ELIGIBLE_FOR_RETURN,
                    "Order is not eligible for return. Only shipped & paid orders can be returned");
        }

        Map<UUID, OrderItem> orderItemsById = order.getOrderItems().stream()
                .collect(Collectors.toMap(OrderItem::getId, item -> item));

        List<ReturnItem> returnItems = new ArrayList<>();
        BigDecimal requestedAmount = ZERO;

        for (CreateReturnRequest.ReturnItemRequest reqItem : request.getItems()) {
            OrderItem orderItem = orderItemsById.get(reqItem.getOrderItemId());
            if (orderItem == null) {
                throw new BusinessException(
                        BusinessErrorCode.RETURN_ITEM_NOT_BELONG_TO_ORDER,
                        "Order item does not belong to order: " + reqItem.getOrderItemId());
            }

            if (reqItem.getQuantity() <= 0 || reqItem.getQuantity() > orderItem.getQuantity()) {
                throw new BusinessException(
                        BusinessErrorCode.RETURN_QUANTITY_INVALID,
                        "Invalid return quantity for order item: " + reqItem.getOrderItemId());
            }

            if (returnItemRepository.existsInNonRejectedRequest(reqItem.getOrderItemId())) {
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

        ReturnRequest returnRequest = ReturnRequest.builder()
                .returnNumber(generateReturnNumber())
                .idempotencyKey(normalizedKey)
                .reason(request.getReason())
                .evidenceNote(trimToNull(request.getEvidenceNote()))
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

        notificationService.createForUser(
                userId,
                "Yêu cầu trả hàng đã tạo",
                "Yêu cầu " + saved.getReturnNumber() + " cho đơn " + order.getOrderNumber() + " đã được gửi.",
                "RETURN",
                order.getId());

        return mapToResponse(saved);
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
    public ReturnRequestResponse cancelReturnRequest(UUID userId, UUID returnRequestId) {
        ReturnRequest rr = returnRequestRepository.findByIdForUpdate(returnRequestId)
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
        return mapToResponse(saved);
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

            notificationService.createForUser(
                    rr.getUser().getId(),
                    "Yêu cầu trả hàng đã duyệt",
                    "Yêu cầu " + rr.getReturnNumber() + " đã được duyệt.",
                    "RETURN",
                    rr.getOrder().getId());
        } else {
            rr.setStatus(ReturnRequestStatus.REJECTED);
            rr.setRefundStatus(RefundStatus.FAILED);
            rr.setApprovedAmount(ZERO);
            rr.setResolvedAt(LocalDateTime.now());
            rr.setAdminNote(note);

            for (ReturnItem item : rr.getItems()) {
                item.setApprovedQuantity(0);
            }

            notificationService.createForUser(
                    rr.getUser().getId(),
                    "Yêu cầu trả hàng bị từ chối",
                    "Yêu cầu " + rr.getReturnNumber() + " đã bị từ chối.",
                    "RETURN",
                    rr.getOrder().getId());
        }

        ReturnRequest saved = returnRequestRepository.save(rr);
        return mapToResponse(saved);
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

        notificationService.createForUser(
                saved.getUser().getId(),
                "Cập nhật yêu cầu trả hàng",
                "Yêu cầu " + saved.getReturnNumber() + " chuyển sang trạng thái " + saved.getStatus().getDescription() + ".",
                "RETURN",
                saved.getOrder().getId());

        return mapToResponse(saved);
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
                && rr.getStatus() != ReturnRequestStatus.REFUND_PENDING
                && rr.getStatus() != ReturnRequestStatus.APPROVED) {
            throw new BusinessException(
                    BusinessErrorCode.REFUND_NOT_ALLOWED,
                    "Refund is allowed only for approved/QC-passed return requests");
        }

        BigDecimal baseAmount = rr.getApprovedAmount() != null ? rr.getApprovedAmount() : rr.getRequestedAmount();
        baseAmount = money(baseAmount);

        BigDecimal refundAmount = request.getAmount() != null ? money(request.getAmount()) : baseAmount;
        if (refundAmount.compareTo(ZERO) <= 0 || refundAmount.compareTo(baseAmount) > 0) {
            throw new BusinessException(BusinessErrorCode.REFUND_AMOUNT_INVALID, "Refund amount is invalid");
        }

        String provider = normalizeProvider(request.getProvider());
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
                .transactionId(trimToNull(request.getTransactionId()))
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
        rr.setRefundAmount(refundAmount);
        rr.setResolvedAt(LocalDateTime.now());
        if (rr.getApprovedAmount() == null) {
            rr.setApprovedAmount(baseAmount);
        }

        markOrderAsRefunded(rr.getOrder(), "Hoàn tiền thành công cho yêu cầu " + rr.getReturnNumber());
        ReturnRequest saved = returnRequestRepository.save(rr);

        notificationService.createForUser(
                saved.getUser().getId(),
                "Hoàn tiền thành công",
                "Yêu cầu " + saved.getReturnNumber() + " đã được hoàn tiền thành công.",
                "RETURN",
                saved.getOrder().getId());

        return mapToResponse(saved);
    }

    private ReturnRequestResponse mapToResponse(ReturnRequest rr) {
        List<ReturnRequestResponse.ReturnItemData> itemResponses = rr.getItems().stream()
                .map(item -> ReturnRequestResponse.ReturnItemData.builder()
                        .id(item.getId())
                        .orderItemId(item.getOrderItem() != null ? item.getOrderItem().getId() : null)
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
                .adminNote(rr.getAdminNote())
                .requestedAmount(rr.getRequestedAmount())
                .approvedAmount(rr.getApprovedAmount())
                .refundAmount(rr.getRefundAmount())
                .createdAt(rr.getCreatedAt())
                .updatedAt(rr.getUpdatedAt())
                .resolvedAt(rr.getResolvedAt())
                .items(itemResponses)
                .refunds(refundResponses)
                .build();
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
                    "Unsupported return status: " + rawStatus);
        }
    }

    private void validateTransition(ReturnRequestStatus currentStatus, ReturnRequestStatus nextStatus) {
        Set<ReturnRequestStatus> allowedStatuses = ALLOWED_STATUS_TRANSITIONS.getOrDefault(
                currentStatus,
                EnumSet.noneOf(ReturnRequestStatus.class));
        if (!allowedStatuses.contains(nextStatus)) {
            throw new BusinessException(
                    BusinessErrorCode.RETURN_STATUS_TRANSITION_NOT_ALLOWED,
                    "Cannot transition return status from " + currentStatus.name() + " to " + nextStatus.name());
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
        if (normalized == null) {
            return "MANUAL";
        }
        return normalized.toUpperCase(Locale.ROOT);
    }

    private String normalizeCurrency(String currency) {
        String normalized = trimToNull(currency);
        if (normalized == null) {
            return null;
        }
        return normalized.toUpperCase(Locale.ROOT);
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
