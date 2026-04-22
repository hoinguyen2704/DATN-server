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
public class ProductFeedbackPageResponse {
    private List<FeedbackResponse> data;
    private int page;
    private int perPage;
    private long total;
    private int lastPage;
    private FeedbackFilterSummaryResponse summary;

    public static ProductFeedbackPageResponse of(Page<FeedbackResponse> pageData, FeedbackFilterSummaryResponse summary) {
        return ProductFeedbackPageResponse.builder()
                .data(pageData.getContent())
                .page(pageData.getNumber() + 1)
                .perPage(pageData.getSize())
                .total(pageData.getTotalElements())
                .lastPage(pageData.getTotalPages())
                .summary(summary)
                .build();
    }
}
