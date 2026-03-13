package com.hoz.hozitech.web.base;

import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Đánh dấu method/class chỉ cho phép ADMIN truy cập.
 *
 * <p>Thay vì viết: @PreAuthorize("hasRole('ADMIN')")
 * <br>Chỉ cần viết: @RoleAdmin
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@PreAuthorize("hasRole('ADMIN')")
public @interface RoleAdmin {
}
