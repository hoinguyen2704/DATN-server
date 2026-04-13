package com.hoz.hozitech.application.services.order;

import static com.hoz.hozitech.application.services.order.OrderUtils.formatPrice;
import static com.hoz.hozitech.application.services.order.OrderUtils.nz;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.hoz.hozitech.application.constant.MailTemplate;
import com.hoz.hozitech.application.services.email.EmailService;
import com.hoz.hozitech.domain.entities.ReturnItem;
import com.hoz.hozitech.domain.entities.ReturnRequest;
import com.hoz.hozitech.domain.entities.User;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
class ReturnEmailSender {

    private final EmailService emailService;

    @Value("${link.frontend}")
    private String frontendUrl;

    void sendReturnUpdatedEmail(ReturnRequest returnRequest, String eventTitle, String eventMessage) {
        try {
            User user = returnRequest.getUser();
            String customerEmail = user != null ? user.getEmail() : null;
            if (customerEmail == null || customerEmail.isBlank()) return;

            Map<String, Object> variables = new HashMap<>();
            variables.put("CUSTOMER_NAME", user.getFullName());
            variables.put("EVENT_TITLE", eventTitle);
            variables.put("EVENT_MESSAGE", eventMessage);
            variables.put("RETURN_NUMBER", returnRequest.getReturnNumber());
            variables.put("ORDER_NUMBER", returnRequest.getOrder() != null ? returnRequest.getOrder().getOrderNumber() : null);
            variables.put("RETURN_STATUS", returnRequest.getStatus() != null ? returnRequest.getStatus().getDescription() : "Không xác định");
            variables.put("REFUND_STATUS", returnRequest.getRefundStatus() != null ? returnRequest.getRefundStatus().getDescription() : "Không xác định");
            variables.put("UPDATED_AT", resolveUpdatedAt(returnRequest));
            variables.put("RETURN_REASON", returnRequest.getReason());
            variables.put("REQUESTED_AMOUNT", formatPrice(nz(returnRequest.getRequestedAmount())));
            variables.put("APPROVED_AMOUNT", returnRequest.getApprovedAmount() != null ? formatPrice(nz(returnRequest.getApprovedAmount())) : null);
            variables.put("REFUND_AMOUNT", returnRequest.getRefundAmount() != null ? formatPrice(nz(returnRequest.getRefundAmount())) : null);
            variables.put("ADMIN_NOTE", returnRequest.getAdminNote());
            variables.put("RETURN_ITEMS", buildItemVariables(returnRequest.getItems()));
            variables.put("RETURN_LINK", frontendUrl + "/user/returns/" + returnRequest.getReturnNumber());

            emailService.sendTemplateMail(
                    customerEmail,
                    eventTitle + " - " + returnRequest.getReturnNumber() + " - HoziTech",
                    MailTemplate.RETURN_UPDATED,
                    variables);
        } catch (Exception e) {
            log.error("Failed to send return update email for {}", returnRequest.getReturnNumber(), e);
        }
    }

    private String resolveUpdatedAt(ReturnRequest returnRequest) {
        LocalDateTime timestamp = returnRequest.getUpdatedAt();
        if (timestamp == null) timestamp = returnRequest.getResolvedAt();
        if (timestamp == null) timestamp = returnRequest.getCreatedAt();
        if (timestamp == null) timestamp = LocalDateTime.now();
        return timestamp.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
    }

    private List<Map<String, Object>> buildItemVariables(List<ReturnItem> items) {
        if (items == null || items.isEmpty()) return List.of();
        return items.stream().map(item -> {
            Map<String, Object> row = new HashMap<>();
            row.put("productName", item.getProductName());
            row.put("variantName", item.getVariantName());
            row.put("requestedQuantity", item.getRequestedQuantity());
            row.put("approvedQuantity", item.getApprovedQuantity());
            row.put("lineAmount", formatPrice(nz(item.getLineAmount())));
            return row;
        }).toList();
    }
}
