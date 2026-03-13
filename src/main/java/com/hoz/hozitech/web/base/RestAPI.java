package com.hoz.hozitech.web.base;

import org.springframework.core.annotation.AliasFor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Gộp @RestController + @RequestMapping thành 1 annotation duy nhất.
 *
 * <p>Thay vì viết:
 * <pre>
 *   @RestController
 *   @RequestMapping("${api.prefix-admin}/brands")
 * </pre>
 * Chỉ cần viết:
 * <pre>
 *   @RestAPI("${api.prefix-admin}/brands")
 * </pre>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@RestController
@RequestMapping
public @interface RestAPI {
    @AliasFor(annotation = RequestMapping.class, attribute = "path")
    String value() default "";
}
