package com.hoz.hozitech.domain.dtos.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardSummaryResponse {
    private BigDecimal totalRevenue;
    private long totalOrders;
    private long newOrders;
    private long totalCustomers;
    private long newCustomers;
    private long productsSold;
    private long cancelledOrders;
    private long returnedOrders;
    private long totalFeedbacks;
    private long newFeedbacks;
}
