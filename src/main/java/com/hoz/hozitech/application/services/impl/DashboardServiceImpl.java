package com.hoz.hozitech.application.services.impl;

import com.hoz.hozitech.application.repositories.*;
import com.hoz.hozitech.application.services.DashboardService;
import com.hoz.hozitech.domain.dtos.response.DashboardStatsResponse;
import com.hoz.hozitech.domain.dtos.response.DashboardStatsResponse.*;
import com.hoz.hozitech.domain.entities.Order;
import com.hoz.hozitech.domain.enums.OrderStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardServiceImpl implements DashboardService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final UserRepository userRepository;
    private final FeedbackRepository feedbackRepository;

    @Override
    public DashboardStatsResponse getDashboardStats(String period) {
        LocalDateTime[] range = getDateRange(period);
        LocalDateTime from = range[0];
        LocalDateTime to = range[1];

        return DashboardStatsResponse.builder()
                // Stat Cards
                .totalRevenue(orderRepository.sumRevenueByDateRange(from, to))
                .totalOrders(orderRepository.count())
                .newOrders(orderRepository.countOrdersByDateRange(from, to))
                .totalCustomers(userRepository.count())
                .newCustomers(userRepository.countNewCustomers(from, to))
                .productsSold(orderItemRepository.sumProductsSoldByDateRange(from, to))
                .cancelledOrders(orderRepository.countOrdersByStatusAndDateRange(OrderStatus.CANCELLED, from, to))
                .returnedOrders(orderRepository.countOrdersByStatusAndDateRange(OrderStatus.RETURNED, from, to))
                .totalFeedbacks(feedbackRepository.count())
                .newFeedbacks(feedbackRepository.countNewFeedbacks(from, to))
                // Charts
                .revenueChart(buildRevenueChart(period, from, to))
                // Top Lists
                .topProducts(buildTopProducts(from, to))
                .topCategories(buildTopCategories(from, to))
                .topCustomers(buildTopCustomers(from, to))
                .recentOrders(buildRecentOrders())
                // Review Analytics
                .ratingDistribution(buildRatingDistribution())
                .build();
    }

    private List<RevenueChartItem> buildRevenueChart(String period, LocalDateTime from, LocalDateTime to) {
        if ("YEAR".equalsIgnoreCase(period)) {
            // Group by month for yearly view
            int year = LocalDate.now().getYear();
            List<Object[]> rows = orderRepository.findRevenueGroupedByMonth(year);
            return rows.stream().map(row -> RevenueChartItem.builder()
                    .label("Tháng " + ((Number) row[0]).intValue())
                    .revenue((BigDecimal) row[1])
                    .orders(((Number) row[2]).longValue())
                    .build()
            ).collect(Collectors.toList());
        } else {
            // Group by date for day/week/month view
            List<Object[]> rows = orderRepository.findRevenueGroupedByDate(from, to);
            return rows.stream().map(row -> RevenueChartItem.builder()
                    .label(row[0].toString())
                    .revenue((BigDecimal) row[1])
                    .orders(((Number) row[2]).longValue())
                    .build()
            ).collect(Collectors.toList());
        }
    }

    private List<TopProductItem> buildTopProducts(LocalDateTime from, LocalDateTime to) {
        List<Object[]> rows = orderItemRepository.findTopSellingProducts(from, to, PageRequest.of(0, 10));
        return rows.stream().map(row -> TopProductItem.builder()
                .id(row[0].toString())
                .name((String) row[1])
                .imageUrl((String) row[2])
                .totalSold(((Number) row[3]).longValue())
                .revenue((BigDecimal) row[4])
                .build()
        ).collect(Collectors.toList());
    }

    private List<TopCategoryItem> buildTopCategories(LocalDateTime from, LocalDateTime to) {
        List<Object[]> rows = orderItemRepository.findTopSellingCategories(from, to, PageRequest.of(0, 10));
        return rows.stream().map(row -> TopCategoryItem.builder()
                .id(row[0].toString())
                .name((String) row[1])
                .totalSold(((Number) row[2]).longValue())
                .revenue((BigDecimal) row[3])
                .build()
        ).collect(Collectors.toList());
    }

    private List<TopCustomerItem> buildTopCustomers(LocalDateTime from, LocalDateTime to) {
        List<Object[]> rows = orderRepository.findTopCustomers(from, to, PageRequest.of(0, 10));
        return rows.stream().map(row -> TopCustomerItem.builder()
                .id(row[0].toString())
                .name((String) row[1])
                .email((String) row[2])
                .totalOrders(((Number) row[3]).longValue())
                .totalSpent((BigDecimal) row[4])
                .build()
        ).collect(Collectors.toList());
    }

    private List<RecentOrderItem> buildRecentOrders() {
        List<Order> orders = orderRepository.findRecentOrders(PageRequest.of(0, 10));
        return orders.stream().map(o -> RecentOrderItem.builder()
                .orderNumber(o.getOrderNumber())
                .customerName(o.getUser().getFullName())
                .totalAmount(o.getTotalAmount())
                .status(o.getOrderStatus().name())
                .createdAt(o.getCreatedAt())
                .build()
        ).collect(Collectors.toList());
    }

    private Map<Integer, Long> buildRatingDistribution() {
        // Initialize with 0 for all ratings 1-5
        Map<Integer, Long> distribution = new LinkedHashMap<>();
        for (int i = 1; i <= 5; i++) {
            distribution.put(i, 0L);
        }
        List<Object[]> rows = feedbackRepository.getRatingDistribution();
        for (Object[] row : rows) {
            int rating = ((Number) row[0]).intValue();
            long count = ((Number) row[1]).longValue();
            distribution.put(rating, count);
        }
        return distribution;
    }

    private LocalDateTime[] getDateRange(String period) {
        LocalDate today = LocalDate.now();
        LocalDateTime from;
        LocalDateTime to = today.atTime(LocalTime.MAX);

        switch (period != null ? period.toUpperCase() : "MONTH") {
            case "DAY":
                from = today.atStartOfDay();
                break;
            case "WEEK":
                from = today.minusDays(6).atStartOfDay();
                break;
            case "YEAR":
                from = today.with(TemporalAdjusters.firstDayOfYear()).atStartOfDay();
                break;
            case "MONTH":
            default:
                from = today.with(TemporalAdjusters.firstDayOfMonth()).atStartOfDay();
                break;
        }

        return new LocalDateTime[]{from, to};
    }
}
