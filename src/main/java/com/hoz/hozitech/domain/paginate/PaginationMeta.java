package com.hoz.hozitech.domain.paginate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PaginationMeta {
    private int page;
    private int perPage;
    private int lastPage;
    private long total;
}
