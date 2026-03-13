package com.hoz.hozitech.domain.dtos.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardStatsResponse {

    // --- Stat Cards ---
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

    // --- Charts ---
    private List<RevenueChartItem> revenueChart;

    // --- Top Lists ---
    private List<TopProductItem> topProducts;
    private List<TopCategoryItem> topCategories;
    private List<TopCustomerItem> topCustomers;
    private List<RecentOrderItem> recentOrders;

    // --- Review Analytics ---
    private Map<Integer, Long> ratingDistribution;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RevenueChartItem {
        private String label;
        private BigDecimal revenue;
        private long orders;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TopProductItem {
        private String id;
        private String name;
        private String imageUrl;
        private long totalSold;
        private BigDecimal revenue;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TopCategoryItem {
        private String id;
        private String name;
        private long totalSold;
        private BigDecimal revenue;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TopCustomerItem {
        private String id;
        private String name;
        private String email;
        private long totalOrders;
        private BigDecimal totalSpent;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RecentOrderItem {
        private String orderNumber;
        private String customerName;
        private BigDecimal totalAmount;
        private String status;
        private LocalDateTime createdAt;
    }
}
