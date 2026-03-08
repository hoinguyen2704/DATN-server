package com.hoz.hozitech.utils;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * Utility class để tạo Pageable từ request params.
 * FE gửi page 1-indexed → convert sang Spring 0-indexed.
 */
public final class PaginationUtils {

    private PaginationUtils() {
    }

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 10;
    private static final int MAX_SIZE = 100;

    /**
     * Tạo Pageable cơ bản (không sort)
     */
    public static Pageable of(Integer page, Integer size) {
        int p = (page == null || page < 1) ? DEFAULT_PAGE : page;
        int s = (size == null || size < 1) ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);
        return PageRequest.of(p - 1, s); // FE 1-indexed → Spring 0-indexed
    }

    /**
     * Tạo Pageable có sort (VD: sortBy="price", sortDir="asc")
     */
    public static Pageable of(Integer page, Integer size, String sortBy, String sortDir) {
        int p = (page == null || page < 1) ? DEFAULT_PAGE : page;
        int s = (size == null || size < 1) ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);

        if (sortBy == null || sortBy.isBlank()) {
            return PageRequest.of(p - 1, s);
        }

        Sort.Direction direction = "desc".equalsIgnoreCase(sortDir)
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;

        return PageRequest.of(p - 1, s, Sort.by(direction, sortBy));
    }
}
