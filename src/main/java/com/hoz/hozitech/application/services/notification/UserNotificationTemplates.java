package com.hoz.hozitech.application.services.notification;

import com.hoz.hozitech.domain.entities.Order;
import com.hoz.hozitech.domain.entities.ReturnRequest;
import com.hoz.hozitech.domain.entities.Ticket;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class UserNotificationTemplates {

    public static NotificationPayload orderCreated(Order order) {
        return base(NotificationTypes.ORDER, "ORDER_CREATED",
                "Đặt hàng thành công",
                "Đơn hàng " + order.getOrderNumber() + " đã được tạo thành công.",
                "/user/orders/" + order.getOrderNumber(),
                "ORDER",
                order.getId().toString());
    }

    public static NotificationPayload orderCancelled(Order order) {
        return base(NotificationTypes.ORDER, "ORDER_CANCELLED",
                "Đơn hàng đã bị hủy",
                "Đơn hàng " + order.getOrderNumber() + " đã được hủy theo yêu cầu của bạn.",
                "/user/orders/" + order.getOrderNumber(),
                "ORDER",
                order.getId().toString());
    }

    public static NotificationPayload orderStatusChanged(Order order) {
        return base(NotificationTypes.ORDER, "ORDER_STATUS_CHANGED",
                "Cập nhật đơn hàng",
                "Đơn hàng " + order.getOrderNumber() + " đã chuyển sang trạng thái " + order.getOrderStatus().getLabel() + ".",
                "/user/orders/" + order.getOrderNumber(),
                "ORDER",
                order.getId().toString());
    }

    public static NotificationPayload orderAutoCancelled(Order order) {
        return base(NotificationTypes.ORDER, "ORDER_AUTO_CANCELLED",
                "Đơn hàng đã bị hủy",
                "Đơn hàng " + order.getOrderNumber() + " đã bị tự động hủy do không hoàn tất thanh toán trong thời gian quy định.",
                "/user/orders/" + order.getOrderNumber(),
                "ORDER",
                order.getId().toString());
    }

    public static NotificationPayload paymentSuccess(Order order) {
        return base(NotificationTypes.PAYMENT, "PAYMENT_SUCCESS",
                "Thanh toán thành công",
                "Đơn hàng " + order.getOrderNumber() + " đã được thanh toán.",
                "/user/orders/" + order.getOrderNumber(),
                "ORDER",
                order.getId().toString());
    }

    public static NotificationPayload paymentFailed(Order order) {
        return base(NotificationTypes.PAYMENT, "PAYMENT_FAILED",
                "Thanh toán thất bại",
                "Đơn hàng " + order.getOrderNumber() + " thanh toán không thành công.",
                "/user/orders/" + order.getOrderNumber(),
                "ORDER",
                order.getId().toString());
    }

    public static NotificationPayload paymentRefunded(Order order) {
        return base(NotificationTypes.PAYMENT, "PAYMENT_REFUNDED",
                "Đã hoàn tiền",
                "Đơn hàng " + order.getOrderNumber() + " đã được hoàn tiền.",
                "/user/orders/" + order.getOrderNumber(),
                "ORDER",
                order.getId().toString());
    }

    public static NotificationPayload returnCreated(ReturnRequest returnRequest) {
        return base(NotificationTypes.RETURN, "RETURN_CREATED",
                "Yêu cầu trả hàng đã tạo",
                "Yêu cầu " + returnRequest.getReturnNumber() + " cho đơn " + returnRequest.getOrder().getOrderNumber() + " đã được gửi.",
                "/user/returns/" + returnRequest.getReturnNumber(),
                "RETURN",
                returnRequest.getReturnNumber());
    }

    public static NotificationPayload returnApproved(ReturnRequest returnRequest) {
        return base(NotificationTypes.RETURN, "RETURN_APPROVED",
                "Yêu cầu trả hàng đã duyệt",
                "Yêu cầu " + returnRequest.getReturnNumber() + " đã được duyệt.",
                "/user/returns/" + returnRequest.getReturnNumber(),
                "RETURN",
                returnRequest.getReturnNumber());
    }

    public static NotificationPayload returnRejected(ReturnRequest returnRequest) {
        return base(NotificationTypes.RETURN, "RETURN_REJECTED",
                "Yêu cầu trả hàng bị từ chối",
                "Yêu cầu " + returnRequest.getReturnNumber() + " đã bị từ chối.",
                "/user/returns/" + returnRequest.getReturnNumber(),
                "RETURN",
                returnRequest.getReturnNumber());
    }

    public static NotificationPayload returnStatusChanged(ReturnRequest returnRequest) {
        return base(NotificationTypes.RETURN, "RETURN_STATUS_CHANGED",
                "Cập nhật yêu cầu trả hàng",
                "Yêu cầu " + returnRequest.getReturnNumber() + " chuyển sang trạng thái " + returnRequest.getStatus().getDescription() + ".",
                "/user/returns/" + returnRequest.getReturnNumber(),
                "RETURN",
                returnRequest.getReturnNumber());
    }

    public static NotificationPayload refundSuccess(ReturnRequest returnRequest) {
        return base(NotificationTypes.RETURN, "REFUND_SUCCESS",
                "Hoàn tiền thành công",
                "Yêu cầu " + returnRequest.getReturnNumber() + " đã được hoàn tiền thành công.",
                "/user/returns/" + returnRequest.getReturnNumber(),
                "RETURN",
                returnRequest.getReturnNumber());
    }

    public static NotificationPayload supportAdminReplied(Ticket ticket) {
        return base(NotificationTypes.SUPPORT, "SUPPORT_ADMIN_REPLIED",
                "Bạn có phản hồi hỗ trợ mới",
                "Yêu cầu " + ticket.getTicketNumber() + " đã được admin phản hồi.",
                "/user/support?ticketId=" + ticket.getId(),
                "TICKET",
                ticket.getId().toString());
    }

    public static NotificationPayload supportStatusChanged(Ticket ticket) {
        return base(NotificationTypes.SUPPORT, "SUPPORT_STATUS_CHANGED",
                "Cập nhật trạng thái hỗ trợ",
                "Yêu cầu " + ticket.getTicketNumber() + " đã chuyển sang trạng thái " + ticket.getStatus().getDescription() + ".",
                "/user/support?ticketId=" + ticket.getId(),
                "TICKET",
                ticket.getId().toString());
    }

    private static NotificationPayload base(String type,
                                            String eventCode,
                                            String title,
                                            String content,
                                            String targetUrl,
                                            String targetType,
                                            String targetId) {
        return NotificationPayload.builder()
                .type(type)
                .eventCode(eventCode)
                .title(title)
                .content(content)
                .targetUrl(targetUrl)
                .targetType(targetType)
                .targetId(targetId)
                .build();
    }
}
