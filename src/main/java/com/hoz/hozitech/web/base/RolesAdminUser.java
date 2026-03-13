package com.hoz.hozitech.web.base;

import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Đánh dấu method/class cho phép cả ADMIN và USER truy cập.
 *
 * <p>Thay vì viết: @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
 * <br>Chỉ cần viết: @RolesAdminUser
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@PreAuthorize("hasAnyRole('ADMIN', 'USER')")
public @interface RolesAdminUser {
}
