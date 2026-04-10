package com.hoz.hozitech.config.web;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import jakarta.servlet.Filter;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;

/**
 * Fix lỗi: "No converter for ApiResponse with preset Content-Type application/json;charset=ISO-8859-1"
 *
 * Nguyên nhân: Tomcat mặc định set charset=ISO-8859-1 khi response.getWriter() được gọi trước khi
 * Content-Type được set rõ ràng. Jackson không nhận ra media type này.
 *
 * Giải pháp: Filter sửa Content-Type response từ ISO-8859-1 → UTF-8 trước khi Jackson xử lý.
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
        configurer
                .favorParameter(false)
                .defaultContentType(MediaType.APPLICATION_JSON);
    }


    /**
     * Filter ưu tiên cao nhất — chặn và sửa Content-Type trước khi nó đi qua Spring MVC.
     * Wrap response để khi bất kỳ thứ gì set charset=ISO-8859-1, ta override thành UTF-8.
     */
    @Bean
    public FilterRegistrationBean<Filter> charsetFixFilter() {
        FilterRegistrationBean<Filter> registration = new FilterRegistrationBean<>();
        registration.setFilter((request, response, chain) -> {
            HttpServletResponse httpResponse = (HttpServletResponse) response;
            HttpServletResponseWrapper wrapper = new HttpServletResponseWrapper(httpResponse) {
                @Override
                public void setContentType(String type) {
                    if (type != null && type.contains("ISO-8859-1")) {
                        type = type.replace("ISO-8859-1", "UTF-8");
                    }
                    super.setContentType(type);
                }
            };
            chain.doFilter(request, wrapper);
        });
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        registration.addUrlPatterns("/*");
        return registration;
    }
}
