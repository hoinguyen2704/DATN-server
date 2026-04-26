package com.hoz.hozitech.application.services.notification;

import com.hoz.hozitech.domain.entities.Article;
import com.hoz.hozitech.domain.entities.Brand;
import com.hoz.hozitech.domain.entities.Category;
import com.hoz.hozitech.domain.entities.Coupon;
import com.hoz.hozitech.domain.entities.Feedback;
import com.hoz.hozitech.domain.entities.FlashSale;
import com.hoz.hozitech.domain.entities.Order;
import com.hoz.hozitech.domain.entities.Product;
import com.hoz.hozitech.domain.entities.ReturnRequest;
import com.hoz.hozitech.domain.entities.Ticket;
import com.hoz.hozitech.domain.entities.User;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class AdminNotificationTemplates {

    public static NotificationPayload supportTicketCreated(Ticket ticket) {
        return base(NotificationTypes.SUPPORT, "SUPPORT_TICKET_CREATED",
                "Yêu cầu hỗ trợ mới",
                "Ticket " + ticket.getTicketNumber() + " vừa được tạo bởi " + resolveTicketOwnerName(ticket) + ".",
                "/admin/tickets?ticketId=" + ticket.getId(),
                "TICKET",
                ticket.getId().toString());
    }

    public static NotificationPayload supportUserReplied(Ticket ticket) {
        return base(NotificationTypes.SUPPORT, "SUPPORT_USER_REPLIED",
                "Khách hàng đã phản hồi ticket",
                "Ticket " + ticket.getTicketNumber() + " vừa có phản hồi mới từ khách hàng.",
                "/admin/tickets?ticketId=" + ticket.getId(),
                "TICKET",
                ticket.getId().toString());
    }

    public static NotificationPayload orderCreated(Order order) {
        return base(NotificationTypes.ORDER, "ORDER_CREATED",
                "Đơn hàng mới",
                "Đơn hàng " + order.getOrderNumber() + " vừa được tạo.",
                "/admin/orders/" + order.getOrderNumber(),
                "ORDER",
                order.getId().toString());
    }

    public static NotificationPayload orderAutoCancelled(Order order) {
        return base(NotificationTypes.ORDER, "ORDER_AUTO_CANCELLED",
                "Đơn hàng tự động hủy",
                "Đơn hàng " + order.getOrderNumber() + " đã bị tự động hủy do quá hạn thanh toán.",
                "/admin/orders/" + order.getOrderNumber(),
                "ORDER",
                order.getId().toString());
    }

    public static NotificationPayload paymentSuccess(Order order) {
        return base(NotificationTypes.PAYMENT, "PAYMENT_SUCCESS",
                "Đơn hàng thanh toán thành công",
                "Đơn hàng " + order.getOrderNumber() + " đã được thanh toán thành công.",
                "/admin/orders/" + order.getOrderNumber(),
                "ORDER",
                order.getId().toString());
    }

    public static NotificationPayload paymentFailed(Order order) {
        return base(NotificationTypes.PAYMENT, "PAYMENT_FAILED",
                "Đơn hàng thanh toán thất bại",
                "Đơn hàng " + order.getOrderNumber() + " thanh toán không thành công.",
                "/admin/orders/" + order.getOrderNumber(),
                "ORDER",
                order.getId().toString());
    }

    public static NotificationPayload paymentRefunded(Order order) {
        return base(NotificationTypes.PAYMENT, "PAYMENT_REFUNDED",
                "Đơn hàng đã hoàn tiền",
                "Đơn hàng " + order.getOrderNumber() + " đã được hoàn tiền.",
                "/admin/orders/" + order.getOrderNumber(),
                "ORDER",
                order.getId().toString());
    }

    public static NotificationPayload returnCreated(ReturnRequest returnRequest) {
        return base(NotificationTypes.RETURN, "RETURN_REQUEST_CREATED",
                "Yêu cầu trả hàng mới",
                "Yêu cầu " + returnRequest.getReturnNumber() + " vừa được tạo cho đơn " + returnRequest.getOrder().getOrderNumber() + ".",
                "/admin/returns/" + returnRequest.getReturnNumber(),
                "RETURN",
                returnRequest.getReturnNumber());
    }

    public static NotificationPayload returnApproved(ReturnRequest returnRequest) {
        return base(NotificationTypes.RETURN, "RETURN_APPROVED",
                "Yêu cầu trả hàng đã duyệt",
                "Yêu cầu " + returnRequest.getReturnNumber() + " đã được duyệt.",
                "/admin/returns/" + returnRequest.getReturnNumber(),
                "RETURN",
                returnRequest.getReturnNumber());
    }

    public static NotificationPayload returnRejected(ReturnRequest returnRequest) {
        return base(NotificationTypes.RETURN, "RETURN_REJECTED",
                "Yêu cầu trả hàng bị từ chối",
                "Yêu cầu " + returnRequest.getReturnNumber() + " đã bị từ chối.",
                "/admin/returns/" + returnRequest.getReturnNumber(),
                "RETURN",
                returnRequest.getReturnNumber());
    }

    public static NotificationPayload returnStatusChanged(ReturnRequest returnRequest) {
        return base(NotificationTypes.RETURN, "RETURN_STATUS_CHANGED",
                "Cập nhật yêu cầu trả hàng",
                "Yêu cầu " + returnRequest.getReturnNumber() + " chuyển sang trạng thái " + returnRequest.getStatus().getDescription() + ".",
                "/admin/returns/" + returnRequest.getReturnNumber(),
                "RETURN",
                returnRequest.getReturnNumber());
    }

    public static NotificationPayload refundSuccess(ReturnRequest returnRequest) {
        return base(NotificationTypes.RETURN, "REFUND_SUCCESS",
                "Hoàn tiền thành công",
                "Yêu cầu " + returnRequest.getReturnNumber() + " đã được hoàn tiền thành công.",
                "/admin/returns/" + returnRequest.getReturnNumber(),
                "RETURN",
                returnRequest.getReturnNumber());
    }

    public static NotificationPayload userStatusChanged(User user) {
        return base(NotificationTypes.USER_ADMIN, "USER_STATUS_CHANGED",
                "Trạng thái khách hàng đã thay đổi",
                "Tài khoản " + resolveUserName(user) + " hiện ở trạng thái " + user.getStatus().name() + ".",
                "/admin/customers/" + user.getId(),
                "USER",
                user.getId().toString());
    }

    public static NotificationPayload userPhoneUpdated(User user) {
        return base(NotificationTypes.USER_ADMIN, "USER_PHONE_UPDATED",
                "Khách hàng đã cập nhật số điện thoại",
                "Thông tin số điện thoại của " + resolveUserName(user) + " vừa được thay đổi.",
                "/admin/customers/" + user.getId(),
                "USER",
                user.getId().toString());
    }

    public static NotificationPayload productCreated(Product product) {
        return base(NotificationTypes.CATALOG, "PRODUCT_CREATED",
                "Sản phẩm mới đã tạo",
                "Sản phẩm " + product.getName() + " vừa được tạo.",
                "/admin/products/" + product.getId(),
                "PRODUCT",
                product.getId().toString());
    }

    public static NotificationPayload productUpdated(Product product) {
        return base(NotificationTypes.CATALOG, "PRODUCT_UPDATED",
                "Sản phẩm đã cập nhật",
                "Sản phẩm " + product.getName() + " vừa được cập nhật.",
                "/admin/products/" + product.getId(),
                "PRODUCT",
                product.getId().toString());
    }

    public static NotificationPayload categoryCreated(Category category) {
        return base(NotificationTypes.CATALOG, "CATEGORY_CREATED",
                "Danh mục mới đã tạo",
                "Danh mục " + category.getName() + " vừa được tạo.",
                "/admin/categories/" + category.getId() + "/edit",
                "CATEGORY",
                category.getId().toString());
    }

    public static NotificationPayload categoryUpdated(Category category) {
        return base(NotificationTypes.CATALOG, "CATEGORY_UPDATED",
                "Danh mục đã cập nhật",
                "Danh mục " + category.getName() + " vừa được cập nhật.",
                "/admin/categories/" + category.getId() + "/edit",
                "CATEGORY",
                category.getId().toString());
    }

    public static NotificationPayload brandCreated(Brand brand) {
        return base(NotificationTypes.CATALOG, "BRAND_CREATED",
                "Thương hiệu mới đã tạo",
                "Thương hiệu " + brand.getName() + " vừa được tạo.",
                "/admin/brands",
                "BRAND",
                brand.getId().toString());
    }

    public static NotificationPayload brandUpdated(Brand brand) {
        return base(NotificationTypes.CATALOG, "BRAND_UPDATED",
                "Thương hiệu đã cập nhật",
                "Thương hiệu " + brand.getName() + " vừa được cập nhật.",
                "/admin/brands",
                "BRAND",
                brand.getId().toString());
    }

    public static NotificationPayload couponCreated(Coupon coupon) {
        return base(NotificationTypes.COUPON, "COUPON_CREATED",
                "Voucher mới đã tạo",
                "Voucher " + coupon.getCode() + " vừa được tạo.",
                "/admin/vouchers",
                "COUPON",
                coupon.getId().toString());
    }

    public static NotificationPayload couponUpdated(Coupon coupon) {
        return base(NotificationTypes.COUPON, "COUPON_UPDATED",
                "Voucher đã cập nhật",
                "Voucher " + coupon.getCode() + " vừa được cập nhật.",
                "/admin/vouchers",
                "COUPON",
                coupon.getId().toString());
    }

    public static NotificationPayload couponStatusChanged(Coupon coupon) {
        return base(NotificationTypes.COUPON, "COUPON_STATUS_CHANGED",
                "Trạng thái voucher đã đổi",
                "Voucher " + coupon.getCode() + " hiện ở trạng thái " + coupon.getStatus().name() + ".",
                "/admin/vouchers",
                "COUPON",
                coupon.getId().toString());
    }

    public static NotificationPayload flashSaleCreated(FlashSale flashSale) {
        return base(NotificationTypes.FLASH_SALE, "FLASH_SALE_CREATED",
                "Flash sale mới đã tạo",
                "Flash sale " + flashSale.getName() + " vừa được tạo.",
                "/admin/flash-sales/" + flashSale.getId() + "/edit",
                "FLASH_SALE",
                flashSale.getId().toString());
    }

    public static NotificationPayload flashSaleUpdated(FlashSale flashSale) {
        return base(NotificationTypes.FLASH_SALE, "FLASH_SALE_UPDATED",
                "Flash sale đã cập nhật",
                "Flash sale " + flashSale.getName() + " vừa được cập nhật.",
                "/admin/flash-sales/" + flashSale.getId() + "/edit",
                "FLASH_SALE",
                flashSale.getId().toString());
    }

    public static NotificationPayload flashSaleStatusChanged(FlashSale flashSale) {
        return base(NotificationTypes.FLASH_SALE, "FLASH_SALE_STATUS_CHANGED",
                "Trạng thái flash sale đã đổi",
                "Flash sale " + flashSale.getName() + " hiện ở trạng thái " + flashSale.getStatus().name() + ".",
                "/admin/flash-sales/" + flashSale.getId() + "/edit",
                "FLASH_SALE",
                flashSale.getId().toString());
    }

    public static NotificationPayload cmsArticleCreated(Article article) {
        return base(NotificationTypes.CONTENT, "CMS_ARTICLE_CREATED",
                "Bài viết mới đã tạo",
                "Bài viết " + article.getTitle() + " vừa được tạo.",
                "/admin/cms",
                "ARTICLE",
                article.getId().toString());
    }

    public static NotificationPayload cmsArticleUpdated(Article article) {
        return base(NotificationTypes.CONTENT, "CMS_ARTICLE_UPDATED",
                "Bài viết đã cập nhật",
                "Bài viết " + article.getTitle() + " vừa được cập nhật.",
                "/admin/cms",
                "ARTICLE",
                article.getId().toString());
    }

    public static NotificationPayload cmsArticlePublished(Article article) {
        return base(NotificationTypes.CONTENT, "CMS_ARTICLE_PUBLISHED",
                "Bài viết đã xuất bản",
                "Bài viết " + article.getTitle() + " vừa được xuất bản.",
                "/admin/cms",
                "ARTICLE",
                article.getId().toString());
    }

    public static NotificationPayload settingUpdated(int count) {
        return base(NotificationTypes.SETTINGS, "SETTING_UPDATED",
                "Cài đặt hệ thống đã cập nhật",
                "Có " + count + " cấu hình hệ thống vừa được cập nhật.",
                "/admin/settings",
                "SETTINGS",
                "system");
    }

    public static NotificationPayload feedbackCreated(Feedback feedback) {
        return base(NotificationTypes.FEEDBACK, "FEEDBACK_CREATED",
                "Đánh giá mới từ khách hàng",
                resolveUserName(feedback.getUser()) + " vừa gửi đánh giá cho " + feedback.getProduct().getName() + ".",
                "/admin/feedbacks",
                "FEEDBACK",
                feedback.getId().toString());
    }

    public static NotificationPayload feedbackStatusChanged(Feedback feedback) {
        return base(NotificationTypes.FEEDBACK, "FEEDBACK_STATUS_CHANGED",
                "Trạng thái đánh giá đã đổi",
                "Đánh giá của " + resolveUserName(feedback.getUser()) + " hiện ở trạng thái " + feedback.getStatus().name() + ".",
                "/admin/feedbacks",
                "FEEDBACK",
                feedback.getId().toString());
    }

    private static String resolveTicketOwnerName(Ticket ticket) {
        if (ticket.getUser() != null) {
            return resolveUserName(ticket.getUser());
        }
        if (ticket.getGuestName() != null && !ticket.getGuestName().isBlank()) {
            return ticket.getGuestName();
        }
        if (ticket.getGuestEmail() != null && !ticket.getGuestEmail().isBlank()) {
            return ticket.getGuestEmail();
        }
        return "khách hàng";
    }

    private static String resolveUserName(User user) {
        if (user == null) {
            return "khách hàng";
        }
        if (user.getFullName() != null && !user.getFullName().isBlank()) {
            return user.getFullName();
        }
        if (user.getUserName() != null && !user.getUserName().isBlank()) {
            return user.getUserName();
        }
        return user.getEmail() != null ? user.getEmail() : "khách hàng";
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
