package com.hoz.hozitech.web.base;

import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Đánh dấu method/class chỉ cho phép USER truy cập.
 *
 * <p>Thay vì viết: @PreAuthorize("hasRole('USER')")
 * <br>Chỉ cần viết: @RoleUser
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@PreAuthorize("hasRole('USER')")
public @interface RoleUser {
}
