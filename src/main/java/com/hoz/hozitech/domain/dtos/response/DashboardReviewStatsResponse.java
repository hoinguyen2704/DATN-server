package com.hoz.hozitech.domain.dtos.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardReviewStatsResponse {
    private long totalFeedbacks;
    private long newFeedbacks;
    private Map<Integer, Long> ratingDistribution;
}
