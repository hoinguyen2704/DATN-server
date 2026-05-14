package com.hoz.hozitech.application.constant;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE) // Không cho phép tạo instance
public final class PaginationConstant {

    // Giá trị số
    public static final int DEFAULT_PAGE = 0;
    public static final int DEFAULT_PAGE_SIZE = 10;
    public static final int MAX_PAGE_SIZE = 100;

    // String dùng cho @RequestParam defaultValue
    public static final String PAGE_DEFAULT_STR = "1";
    public static final String PAGE_SIZE_LARGE_STR = "20";
    public static final String PAGE_SIZE_MEDIUM_STR = "10";

    // Sort mặc định
    public static final String DEFAULT_SORT_FIELD = "createdAt";
    public static final String SORT_ASC = "asc";
    public static final String SORT_DESC = "desc";

    // Tiện ích dùng chung

    /**
     * Đảm bảo size nằm trong khoảng [1, MAX_PAGE_SIZE].
     */
    public static int validateSize(int size) {
        if (size <= 0)
            return DEFAULT_PAGE_SIZE;
        return Math.min(size, MAX_PAGE_SIZE);
    }

    /**
     * Tạo Pageable với sort mặc định (createdAt DESC, id DESC).
     * page: 1-based (từ client), tự chuyển sang 0-based.
     */
    public static Pageable of(int page, int size) {
        return PageRequest.of(
                Math.max(page - 1, 0),
                validateSize(size),
                Sort.by(
                        Sort.Order.desc(DEFAULT_SORT_FIELD),
                        Sort.Order.desc("id")));
    }

    /**
     * Tạo Pageable với Sort tùy chọn.
     * page: 1-based (từ client), tự chuyển sang 0-based.
     */
    public static Pageable of(int page, int size, Sort sort) {
        return PageRequest.of(
                Math.max(page - 1, 0),
                validateSize(size),
                sort);
    }

    /**
     * Tạo Pageable với sortBy + sortDir dạng String.
     * page: 1-based (từ client), tự chuyển sang 0-based.
     */
    public static Pageable of(int page, int size, String sortBy, String sortDir) {
        Sort sort = SORT_ASC.equalsIgnoreCase(sortDir)
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        return of(page, size, sort);
    }
}
