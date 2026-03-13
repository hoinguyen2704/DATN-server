package com.hoz.hozitech.application.constant;

/**
 * Hằng số trạng thái chung dùng cho các entity (Product, Coupon, FlashSale, Feedback...).
 * Tránh hardcode chuỗi trạng thái rải rác trong service/controller.
 */
public final class StatusConstant {

    private StatusConstant() {}

    // ─── Product Status ───
    public static final String PRODUCT_ACTIVE = "ACTIVE";
    public static final String PRODUCT_INACTIVE = "INACTIVE";
    public static final String PRODUCT_DRAFT = "DRAFT";

    // ─── Coupon Status ───
    public static final String COUPON_ACTIVE = "ACTIVE";
    public static final String COUPON_EXPIRED = "EXPIRED";
    public static final String COUPON_PAUSED = "PAUSED";

    // ─── Coupon Discount Type ───
    public static final String DISCOUNT_PERCENTAGE = "PERCENTAGE";
    public static final String DISCOUNT_FIXED = "FIXED_AMOUNT";

    // ─── Flash Sale Status ───
    public static final String FLASH_SCHEDULED = "SCHEDULED";
    public static final String FLASH_ACTIVE = "ACTIVE";
    public static final String FLASH_ENDED = "ENDED";

    // ─── Feedback Status ───
    public static final String FEEDBACK_APPROVED = "APPROVED";
    public static final String FEEDBACK_HIDDEN = "HIDDEN";
    public static final String FEEDBACK_SPAM = "SPAM";

    // ─── Ticket Status ───
    public static final String TICKET_OPEN = "OPEN";
    public static final String TICKET_IN_PROGRESS = "IN_PROGRESS";
    public static final String TICKET_RESOLVED = "RESOLVED";
    public static final String TICKET_CLOSED = "CLOSED";
}
