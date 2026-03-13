package com.hoz.hozitech.web.base;

import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Đánh dấu method/class chỉ cho phép SHIPPER truy cập.
 * (Mở rộng so với HAVU — Hozitech có thêm role Shipper)
 *
 * <p>Thay vì viết: @PreAuthorize("hasRole('SHIPPER')")
 * <br>Chỉ cần viết: @RoleShipper
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@PreAuthorize("hasRole('SHIPPER')")
public @interface RoleShipper {
}
