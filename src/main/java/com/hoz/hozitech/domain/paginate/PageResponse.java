package com.hoz.hozitech.domain.paginate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PageResponse<T> {
    private List<T> data;
    private PaginationMeta pagination;

    /**
     * Tạo PageResponse từ Spring Page<T>
     * Dùng trực tiếp khi không cần map entity → DTO
     */
    public static <T> PageResponse<T> of(Page<T> page) {
        return PageResponse.<T>builder()
                .data(page.getContent())
                .pagination(PaginationMeta.builder()
                        .page(page.getNumber() + 1) // Spring page 0-indexed → FE 1-indexed
                        .perPage(page.getSize())
                        .lastPage(page.getTotalPages())
                        .total(page.getTotalElements())
                        .build())
                .build();
    }

    /**
     * Tạo PageResponse với data đã được map sang DTO
     * Dùng khi cần convert entity → response DTO
     */
    public static <T> PageResponse<T> of(List<T> content, Page<?> page) {
        return PageResponse.<T>builder()
                .data(content)
                .pagination(PaginationMeta.builder()
                        .page(page.getNumber() + 1)
                        .perPage(page.getSize())
                        .lastPage(page.getTotalPages())
                        .total(page.getTotalElements())
                        .build())
                .build();
    }
}
