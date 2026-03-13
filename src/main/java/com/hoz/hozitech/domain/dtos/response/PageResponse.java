package com.hoz.hozitech.domain.dtos.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PageResponse<T> {
    private List<T> data;
    private int page;
    private int perPage;
    private long total;
    private int lastPage;

    public static <T> PageResponse<T> of(Page<T> pageData) {
        return PageResponse.<T>builder()
                .data(pageData.getContent())
                .page(pageData.getNumber() + 1) // Spring Data JPA pages are 0-indexed, but FE expects 1-indexed
                .perPage(pageData.getSize())
                .total(pageData.getTotalElements())
                .lastPage(pageData.getTotalPages())
                .build();
    }
}
