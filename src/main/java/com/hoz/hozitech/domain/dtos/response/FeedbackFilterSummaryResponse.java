package com.hoz.hozitech.domain.dtos.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FeedbackFilterSummaryResponse {
    private long total;
    private long withContent;
    private Map<Integer, Long> ratingCounts;
}
