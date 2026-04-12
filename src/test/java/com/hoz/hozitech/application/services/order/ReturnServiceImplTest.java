package com.hoz.hozitech.application.services.order;

import com.hoz.hozitech.application.repositories.OrderRepository;
import com.hoz.hozitech.application.repositories.OrderStatusHistoryRepository;
import com.hoz.hozitech.application.repositories.RefundTransactionRepository;
import com.hoz.hozitech.application.repositories.ReturnItemRepository;
import com.hoz.hozitech.application.repositories.ReturnRequestRepository;
import com.hoz.hozitech.application.repositories.ReturnStatusHistoryRepository;
import com.hoz.hozitech.application.services.notification.NotificationService;
import com.hoz.hozitech.application.services.setting.SettingService;
import com.hoz.hozitech.domain.dtos.request.CreateReturnRequest;
import com.hoz.hozitech.domain.dtos.request.ProcessRefundRequest;
import com.hoz.hozitech.domain.dtos.request.UpdateReturnStatusRequest;
import com.hoz.hozitech.domain.dtos.response.ReturnRequestResponse;
import com.hoz.hozitech.domain.entities.Order;
import com.hoz.hozitech.domain.entities.OrderItem;
import com.hoz.hozitech.domain.entities.RefundTransaction;
import com.hoz.hozitech.domain.entities.ReturnRequest;
import com.hoz.hozitech.domain.entities.ReturnStatusHistory;
import com.hoz.hozitech.domain.entities.User;
import com.hoz.hozitech.domain.enums.BusinessErrorCode;
import com.hoz.hozitech.domain.enums.OrderStatus;
import com.hoz.hozitech.domain.enums.PaymentStatus;
import com.hoz.hozitech.domain.enums.RefundStatus;
import com.hoz.hozitech.domain.enums.ReturnRequestStatus;
import com.hoz.hozitech.web.exceptions.BusinessException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class ReturnServiceImplTest {

    @Mock
    private ReturnRequestRepository returnRequestRepository;
    @Mock
    private ReturnItemRepository returnItemRepository;
    @Mock
    private RefundTransactionRepository refundTransactionRepository;
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private OrderStatusHistoryRepository orderStatusHistoryRepository;
    @Mock
    private NotificationService notificationService;
    @Mock
    private SettingService settingService;
    @Mock
    private ReturnStatusHistoryRepository returnStatusHistoryRepository;
    @Mock
    private EntityManager entityManager;
    @Mock
    private Query lockQuery;

    private ReturnServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ReturnServiceImpl(
                returnRequestRepository,
                returnItemRepository,
                refundTransactionRepository,
                orderRepository,
                orderStatusHistoryRepository,
                returnStatusHistoryRepository,
                notificationService,
                settingService);

        ReflectionTestUtils.setField(service, "entityManager", entityManager);

        lenient().when(entityManager.createNativeQuery(anyString())).thenReturn(lockQuery);
        lenient().when(lockQuery.setParameter(eq("lockKey"), any())).thenReturn(lockQuery);
        lenient().when(lockQuery.getSingleResult()).thenReturn(1);
        lenient().when(returnStatusHistoryRepository.save(any(ReturnStatusHistory.class))).thenAnswer(invocation -> {
            ReturnStatusHistory h = invocation.getArgument(0);
            h.setId(UUID.randomUUID());
            h.setCreatedAt(LocalDateTime.now());
            return h;
        });
    }

    @Test
    void createReturnRequest_shouldThrowWhenMissingIdempotencyKey() {
        UUID userId = UUID.randomUUID();
        CreateReturnRequest request = CreateReturnRequest.builder()
                .orderId(UUID.randomUUID())
                .reason("Khong dung nhu mo ta")
                .items(List.of(
                        CreateReturnRequest.ReturnItemRequest.builder()
                                .orderItemId(UUID.randomUUID())
                                .quantity(1)
                                .build()))
                .build();

        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> service.createReturnRequest(userId, request, " "));

        assertEquals(BusinessErrorCode.RETURN_IDEMPOTENCY_KEY_REQUIRED, ex.getErrorCode());
    }

    @Test
    void createReturnRequest_shouldThrowWhenOrderNotEligibleForReturn() {
        UUID userId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        UUID orderItemId = UUID.randomUUID();

        User user = buildUser(userId);
        OrderItem orderItem = buildOrderItem(orderItemId, 2, "200000");
        Order order = buildOrder(orderId, "ORD-001", user, OrderStatus.PENDING, PaymentStatus.PENDING, List.of(orderItem));

        CreateReturnRequest request = CreateReturnRequest.builder()
                .orderId(orderId)
                .reason("Muon tra hang")
                .items(List.of(
                        CreateReturnRequest.ReturnItemRequest.builder()
                                .orderItemId(orderItemId)
                                .quantity(1)
                                .build()))
                .build();

        when(returnRequestRepository.findByUserIdAndIdempotencyKey(userId, "idemp-1")).thenReturn(Optional.empty());
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> service.createReturnRequest(userId, request, "idemp-1"));

        assertEquals(BusinessErrorCode.ORDER_NOT_ELIGIBLE_FOR_RETURN, ex.getErrorCode());
    }

    @Test
    void createReturnRequest_shouldCreateReturnSuccessfully() {
        UUID userId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        UUID orderItemId = UUID.randomUUID();

        User user = buildUser(userId);
        OrderItem orderItem = buildOrderItem(orderItemId, 2, "100000");
        Order order = buildOrder(orderId, "ORD-2026-001", user, OrderStatus.SHIPPED, PaymentStatus.COMPLETED, List.of(orderItem));

        CreateReturnRequest request = CreateReturnRequest.builder()
                .orderId(orderId)
                .reason("Loi san pham")
                .evidenceNote("Video loi")
                .items(List.of(
                        CreateReturnRequest.ReturnItemRequest.builder()
                                .orderItemId(orderItemId)
                                .quantity(1)
                                .build()))
                .build();

        when(returnRequestRepository.findByUserIdAndIdempotencyKey(userId, "idemp-create")).thenReturn(Optional.empty());
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(returnItemRepository.existsInNonRejectedRequest(orderItemId)).thenReturn(false);
        when(returnRequestRepository.existsByReturnNumber(anyString())).thenReturn(false);
        when(returnRequestRepository.save(any(ReturnRequest.class))).thenAnswer(invocation -> {
            ReturnRequest rr = invocation.getArgument(0);
            rr.setId(UUID.randomUUID());
            rr.setCreatedAt(LocalDateTime.now());
            return rr;
        });

        ReturnRequestResponse response = service.createReturnRequest(userId, request, "idemp-create");

        assertEquals("REQUESTED", response.getStatus());
        assertEquals("PENDING", response.getRefundStatus());
        assertEquals("100000.00", response.getRequestedAmount().toPlainString());
        assertTrue(response.getReturnNumber().startsWith("RET-"));

        verify(returnRequestRepository).save(any(ReturnRequest.class));
        verify(notificationService).createForUser(eq(userId), anyString(), anyString(), eq("RETURN"), eq(orderId));
    }

    @Test
    void processRefund_shouldReturnExistingResultWhenIdempotencyKeyReused() {
        UUID returnRequestId = UUID.randomUUID();
        String idempotencyKey = "refund-key-1";

        User user = buildUser(UUID.randomUUID());
        Order order = buildOrder(UUID.randomUUID(), "ORD-LOCK-01", user, OrderStatus.SHIPPED, PaymentStatus.COMPLETED, List.of());
        ReturnRequest returnRequest = ReturnRequest.builder()
                .returnNumber("RET-20260408-AAA111")
                .status(ReturnRequestStatus.REFUND_PENDING)
                .refundStatus(RefundStatus.PROCESSING)
                .reason("Loi")
                .requestedAmount(new BigDecimal("100000.00"))
                .approvedAmount(new BigDecimal("100000.00"))
                .refundAmount(new BigDecimal("0.00"))
                .user(user)
                .order(order)
                .items(new ArrayList<>())
                .refundTransactions(new ArrayList<>())
                .build();

        RefundTransaction existingTx = RefundTransaction.builder()
                .idempotencyKey(idempotencyKey)
                .provider("VNPAY")
                .status(RefundStatus.SUCCESS)
                .amount(new BigDecimal("100000.00"))
                .currency("VND")
                .returnRequest(returnRequest)
                .order(order)
                .build();

        when(refundTransactionRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.of(existingTx));

        ReturnRequestResponse response = service.processRefund(
                returnRequestId,
                ProcessRefundRequest.builder().amount(new BigDecimal("100000.00")).build(),
                idempotencyKey);

        assertEquals("RET-20260408-AAA111", response.getReturnNumber());
        verify(returnRequestRepository, never()).findByIdForUpdate(any());
    }

    @Test
    void processRefund_shouldMarkOrderAsRefunded() {
        UUID returnRequestId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        String idempotencyKey = "refund-key-2";

        User user = buildUser(userId);
        Order order = buildOrder(UUID.randomUUID(), "ORD-2026-REF", user, OrderStatus.SHIPPED, PaymentStatus.COMPLETED, List.of());
        ReturnRequest returnRequest = ReturnRequest.builder()
                .returnNumber("RET-20260408-BBB222")
                .status(ReturnRequestStatus.APPROVED)
                .refundStatus(RefundStatus.PENDING)
                .reason("Khach yeu cau tra")
                .requestedAmount(new BigDecimal("200000.00"))
                .approvedAmount(new BigDecimal("150000.00"))
                .refundAmount(new BigDecimal("0.00"))
                .user(user)
                .order(order)
                .items(new ArrayList<>())
                .refundTransactions(new ArrayList<>())
                .build();
        returnRequest.setId(returnRequestId);

        when(refundTransactionRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());
        when(returnRequestRepository.findByIdForUpdate(returnRequestId)).thenReturn(Optional.of(returnRequest));
        when(settingService.getSettingValue("CURRENCY")).thenReturn("VND");
        when(refundTransactionRepository.save(any(RefundTransaction.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(returnRequestRepository.save(any(ReturnRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ReturnRequestResponse response = service.processRefund(
                returnRequestId,
                ProcessRefundRequest.builder()
                        .amount(new BigDecimal("120000"))
                        .provider("VNPAY")
                        .transactionId("TX-001")
                        .build(),
                idempotencyKey);

        assertEquals("REFUNDED", response.getStatus());
        assertEquals("SUCCESS", response.getRefundStatus());
        assertEquals("120000.00", response.getRefundAmount().toPlainString());
        assertEquals(OrderStatus.RETURNED, order.getOrderStatus());
        assertEquals(PaymentStatus.REFUNDED, order.getPaymentStatus());

        ArgumentCaptor<RefundTransaction> refundCaptor = ArgumentCaptor.forClass(RefundTransaction.class);
        verify(refundTransactionRepository).save(refundCaptor.capture());
        assertEquals(idempotencyKey, refundCaptor.getValue().getIdempotencyKey());
        assertEquals("VNPAY", refundCaptor.getValue().getProvider());
        assertEquals("120000.00", refundCaptor.getValue().getAmount().toPlainString());

        verify(orderStatusHistoryRepository).save(any());
    }

    @Test
    void updateReturnStatus_shouldRejectInvalidTransition() {
        UUID rrId = UUID.randomUUID();
        User user = buildUser(UUID.randomUUID());
        Order order = buildOrder(UUID.randomUUID(), "ORD-2026-TRANS", user, OrderStatus.SHIPPED, PaymentStatus.COMPLETED, List.of());
        ReturnRequest returnRequest = ReturnRequest.builder()
                .returnNumber("RET-TRANS")
                .status(ReturnRequestStatus.REQUESTED)
                .refundStatus(RefundStatus.PENDING)
                .reason("Loi")
                .requestedAmount(new BigDecimal("50000.00"))
                .refundAmount(new BigDecimal("0.00"))
                .user(user)
                .order(order)
                .items(new ArrayList<>())
                .refundTransactions(new ArrayList<>())
                .build();
        returnRequest.setId(rrId);

        when(returnRequestRepository.findByIdForUpdate(rrId)).thenReturn(Optional.of(returnRequest));

        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> service.updateReturnStatus(
                        rrId,
                        UpdateReturnStatusRequest.builder().status("REFUNDED").build()));

        assertEquals(BusinessErrorCode.RETURN_STATUS_TRANSITION_NOT_ALLOWED, ex.getErrorCode());
    }

    private User buildUser(UUID userId) {
        User user = User.builder()
                .userName("u_" + userId.toString().substring(0, 8))
                .fullName("Test User")
                .email("test@example.com")
                .phoneNumber("0123456789")
                .build();
        user.setId(userId);
        return user;
    }

    private Order buildOrder(UUID orderId, String orderNumber, User user, OrderStatus orderStatus, PaymentStatus paymentStatus, List<OrderItem> items) {
        Order order = Order.builder()
                .orderNumber(orderNumber)
                .orderStatus(orderStatus)
                .paymentStatus(paymentStatus)
                .paymentMethod(com.hoz.hozitech.domain.enums.PaymentMethod.VNPAY)
                .subtotal(new BigDecimal("0.00"))
                .shippingFee(new BigDecimal("0.00"))
                .discountAmount(new BigDecimal("0.00"))
                .shippingDiscountAmount(new BigDecimal("0.00"))
                .totalAmount(new BigDecimal("0.00"))
                .shippingAddressJson("{}")
                .user(user)
                .orderItems(new ArrayList<>(items))
                .statusHistories(new ArrayList<>())
                .build();
        order.setId(orderId);
        for (OrderItem item : items) {
            item.setOrder(order);
        }
        return order;
    }

    private OrderItem buildOrderItem(UUID orderItemId, int quantity, String unitPrice) {
        OrderItem item = OrderItem.builder()
                .productName("Product A")
                .variantName("Variant 1")
                .unitPrice(new BigDecimal(unitPrice))
                .quantity(quantity)
                .subtotal(new BigDecimal(unitPrice).multiply(BigDecimal.valueOf(quantity)))
                .build();
        item.setId(orderItemId);
        return item;
    }
}
