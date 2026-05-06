package com.hoz.hozitech.application.services.order;

import static com.hoz.hozitech.application.services.order.OrderUtils.formatPrice;
import static com.hoz.hozitech.application.services.order.OrderUtils.nz;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.hoz.hozitech.application.constant.MailTemplate;
import com.hoz.hozitech.application.services.email.EmailService;
import com.hoz.hozitech.domain.dtos.response.OrderResponse;
import com.hoz.hozitech.domain.entities.Address;
import com.hoz.hozitech.domain.entities.Order;
import com.hoz.hozitech.domain.entities.User;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// Sends order-related emails (order created, order shipped).
@Slf4j
@Component
@RequiredArgsConstructor
class OrderEmailSender {

    private final EmailService emailService;
    private final OrderResponseMapper responseMapper;

    @Value("${link.frontend}")
    private String frontendUrl;

    void sendOrderCreatedEmail(Order order, User user, Address address) {
        try {
            String customerEmail = user.getEmail();
            if (customerEmail == null || customerEmail.isBlank()) return;

            Map<String, Object> variables = buildCommonEmailVariables(order, user);
            variables.put("ORDER_DATE", order.getCreatedAt() != null
                    ? order.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                    : LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
            variables.put("CUSTOMER_FULL_NAME", address.getFullName());
            variables.put("CUSTOMER_PHONE", address.getPhoneNumber());
            variables.put("CUSTOMER_ADDRESS", buildFullAddress(address));

            emailService.sendTemplateMail(customerEmail,
                    "Đơn hàng " + order.getOrderNumber() + " đã tạo thành công - HoziTech",
                    MailTemplate.ORDER_CREATED, variables);
        } catch (Exception e) {
            log.error("Failed to send order created email for order {}", order.getOrderNumber(), e);
        }
    }

    void sendOrderShippedEmail(Order order) {
        try {
            User user = order.getUser();
            String customerEmail = user.getEmail();
            if (customerEmail == null || customerEmail.isBlank()) return;

            Map<String, Object> variables = buildCommonEmailVariables(order, user);
            variables.put("SHIPPED_DATE", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));

            emailService.sendTemplateMail(customerEmail,
                    "Đơn hàng " + order.getOrderNumber() + " đã giao thành công - HoziTech",
                    MailTemplate.ORDER_SHIPPED, variables);
        } catch (Exception e) {
            log.error("Failed to send order shipped email for order {}", order.getOrderNumber(), e);
        }
    }

    void sendPaymentRefundedEmail(Order order) {
        try {
            User user = order.getUser();
            String customerEmail = user.getEmail();
            if (customerEmail == null || customerEmail.isBlank()) return;

            Map<String, Object> variables = buildCommonEmailVariables(order, user);
            variables.put("REFUNDED_DATE", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
            variables.put("REFUND_AMOUNT", formatPrice(nz(order.getTotalAmount())));
            variables.put("PAYMENT_METHOD", order.getPaymentMethod() != null ? order.getPaymentMethod().name() : null);

            emailService.sendTemplateMail(customerEmail,
                    "Đơn hàng " + order.getOrderNumber() + " đã được hoàn tiền - HoziTech",
                    MailTemplate.PAYMENT_REFUNDED, variables);
        } catch (Exception e) {
            log.error("Failed to send payment refunded email for order {}", order.getOrderNumber(), e);
        }
    }

    private Map<String, Object> buildCommonEmailVariables(Order order, User user) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("CUSTOMER_NAME", user.getFullName());
        variables.put("ORDER_NUMBER", order.getOrderNumber());

        OrderResponse response = responseMapper.mapToResponse(order);
        variables.put("ORDER_ITEMS", response.getItems());
        variables.put("ORDER_SUBTOTAL", formatPrice(nz(order.getSubtotal())));
        variables.put("ORDER_SHIPPING_FEE", formatPrice(nz(order.getShippingFee())));
        variables.put("ORDER_DISCOUNT_AMOUNT",
                nz(order.getDiscountAmount()).compareTo(BigDecimal.ZERO) > 0
                        ? formatPrice(order.getDiscountAmount()) : null);
        variables.put("ORDER_SHIPPING_DISCOUNT_AMOUNT",
                nz(order.getShippingDiscountAmount()).compareTo(BigDecimal.ZERO) > 0
                        ? formatPrice(order.getShippingDiscountAmount()) : null);
        variables.put("ORDER_TAX_LABEL", responseMapper.buildTaxLabel(order));
        variables.put("ORDER_TAX_AMOUNT",
                nz(order.getTaxAmount()).compareTo(BigDecimal.ZERO) > 0
                        ? formatPrice(order.getTaxAmount()) : null);
        variables.put("ORDER_COUPON_CODE", order.getCouponCode());
        variables.put("ORDER_TOTAL", formatPrice(nz(order.getTotalAmount())));
        variables.put("ORDER_LINK", frontendUrl + "/user/orders/" + order.getOrderNumber());

        return variables;
    }

    private String buildFullAddress(Address address) {
        StringBuilder sb = new StringBuilder();
        if (address.getDetailAddress() != null) sb.append(address.getDetailAddress());
        if (address.getWard() != null) sb.append(", ").append(address.getWard());
        if (address.getDistrict() != null) sb.append(", ").append(address.getDistrict());
        if (address.getProvince() != null) sb.append(", ").append(address.getProvince());
        return sb.toString();
    }
}
