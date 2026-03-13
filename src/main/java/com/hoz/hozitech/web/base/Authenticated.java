package com.hoz.hozitech.web.base;

import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Cho phép tất cả user đã đăng nhập (ADMIN, USER, SHIPPER) truy cập.
 *
 * <p>Thay vì viết: @PreAuthorize("isAuthenticated()")
 * <br>Chỉ cần viết: @Authenticated
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@PreAuthorize("isAuthenticated()")
public @interface Authenticated {
}
