package com.hoz.hozitech.application.constant;

/**
 * Hằng số liên quan đến Security / JWT Authentication.
 * Tập trung tại đây để tránh "magic string" rải rác trong code.
 */
public final class SecurityConstant {

    private SecurityConstant() {} // Không cho khởi tạo

    public static final String TOKEN_PREFIX = "Bearer ";
    public static final String HEADER_AUTHORIZATION = "Authorization";
    public static final String HEADER_CONTENT_TYPE = "Content-Type";
    public static final String HEADER_ACCEPT = "Accept";
    public static final String HEADER_X_REQUESTED_WITH = "X-Requested-With";
}
