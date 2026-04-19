package com.hoz.hozitech.application.services.dashboard;

import com.hoz.hozitech.application.repositories.FeedbackRepository;
import com.hoz.hozitech.application.repositories.OrderItemRepository;
import com.hoz.hozitech.application.repositories.OrderRepository;
import com.hoz.hozitech.application.repositories.ProductImageRepository;
import com.hoz.hozitech.application.repositories.ReturnItemRepository;
import com.hoz.hozitech.application.repositories.UserRepository;
import com.hoz.hozitech.domain.dtos.response.DashboardRevenueResponse;
import com.hoz.hozitech.domain.dtos.response.DashboardReviewStatsResponse;
import com.hoz.hozitech.domain.dtos.response.DashboardStatsResponse;
import com.hoz.hozitech.domain.dtos.response.DashboardStatsResponse.RecentOrderItem;
import com.hoz.hozitech.domain.dtos.response.DashboardStatsResponse.RevenueChartItem;
import com.hoz.hozitech.domain.dtos.response.DashboardStatsResponse.TopCategoryItem;
import com.hoz.hozitech.domain.dtos.response.DashboardStatsResponse.TopCustomerItem;
import com.hoz.hozitech.domain.dtos.response.DashboardStatsResponse.TopProductItem;
import com.hoz.hozitech.domain.dtos.response.DashboardStatsResponse.TopVariantItem;
import com.hoz.hozitech.domain.dtos.response.DashboardSummaryResponse;
import com.hoz.hozitech.domain.dtos.response.DashboardTopListsResponse;
import com.hoz.hozitech.domain.entities.Order;
import com.hoz.hozitech.domain.enums.OrderStatus;
import com.hoz.hozitech.domain.enums.PaymentStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardServiceImpl implements DashboardService {

    private static final long UI_CACHE_TTL_MS = 30_000L;

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductImageRepository productImageRepository;
    private final ReturnItemRepository returnItemRepository;
    private final UserRepository userRepository;
    private final FeedbackRepository feedbackRepository;

    private final Map<String, CacheEntry<?>> uiCache = new ConcurrentHashMap<>();

    @Override
    public DashboardStatsResponse getDashboardStats(String period) {
        DashboardSummaryResponse summary = getDashboardSummary(period);
        DashboardRevenueResponse revenue = getDashboardRevenue(period);
        DashboardTopListsResponse topLists = getDashboardTopLists(period);
        DashboardReviewStatsResponse reviews = getDashboardReviewStats(period);

        return DashboardStatsResponse.builder()
                .totalRevenue(summary.getTotalRevenue())
                .totalOrders(summary.getTotalOrders())
                .newOrders(summary.getNewOrders())
                .totalCustomers(summary.getTotalCustomers())
                .newCustomers(summary.getNewCustomers())
                .productsSold(summary.getProductsSold())
                .cancelledOrders(summary.getCancelledOrders())
                .returnedOrders(summary.getReturnedOrders())
                .totalFeedbacks(summary.getTotalFeedbacks())
                .newFeedbacks(summary.getNewFeedbacks())
                .revenueChart(revenue.getRevenueChart())
                .topProducts(topLists.getTopProducts())
                .topVariants(getTopVariants(period, 10))
                .topCategories(topLists.getTopCategories())
                .topCustomers(topLists.getTopCustomers())
                .recentOrders(getRecentOrders())
                .ratingDistribution(reviews.getRatingDistribution())
                .build();
    }

    @Override
    public DashboardSummaryResponse getDashboardSummary(String period) {
        String normalizedPeriod = normalizePeriod(period);
        return getCached("dashboard:summary:" + normalizedPeriod, () -> {
            DateRange range = getDateRange(normalizedPeriod);
            return DashboardSummaryResponse.builder()
                    .totalRevenue(orderRepository.sumRevenueByDateRange(range.from(), range.to()))
                    .totalOrders(orderRepository.count())
                    .newOrders(orderRepository.countOrdersByDateRange(range.from(), range.to()))
                    .totalCustomers(userRepository.count())
                    .newCustomers(userRepository.countNewCustomers(range.from(), range.to()))
                    .productsSold(orderItemRepository.sumProductsSoldByDateRange(range.from(), range.to()))
                    .cancelledOrders(
                            orderRepository.countOrdersByStatusAndDateRange(OrderStatus.CANCELLED, range.from(), range.to())
                                    + orderRepository.countOrdersByPaymentStatusAndDateRange(PaymentStatus.FAILED, range.from(), range.to())
                    )
                    .returnedOrders(orderRepository.countOrdersByStatusAndDateRange(OrderStatus.RETURNED, range.from(), range.to()))
                    .totalFeedbacks(feedbackRepository.count())
                    .newFeedbacks(feedbackRepository.countNewFeedbacks(range.from(), range.to()))
                    .build();
        });
    }

    @Override
    public DashboardRevenueResponse getDashboardRevenue(String period) {
        String normalizedPeriod = normalizePeriod(period);
        return getCached("dashboard:revenue:" + normalizedPeriod, () -> {
            DateRange range = getDateRange(normalizedPeriod);
            return DashboardRevenueResponse.builder()
                    .revenueChart(buildRevenueChart(normalizedPeriod, range.from(), range.to()))
                    .build();
        });
    }

    @Override
    public DashboardTopListsResponse getDashboardTopLists(String period) {
        String normalizedPeriod = normalizePeriod(period);
        return getCached("dashboard:top-lists:" + normalizedPeriod, () -> {
            DateRange range = getDateRange(normalizedPeriod);
            return DashboardTopListsResponse.builder()
                    .topProducts(buildTopProducts(range.from(), range.to()))
                    .topCategories(buildTopCategories(range.from(), range.to()))
                    .topCustomers(buildTopCustomers(range.from(), range.to()))
                    .build();
        });
    }

    @Override
    public List<RecentOrderItem> getRecentOrders() {
        return getCached("dashboard:recent-orders", this::buildRecentOrders);
    }

    @Override
    public DashboardReviewStatsResponse getDashboardReviewStats(String period) {
        String normalizedPeriod = normalizePeriod(period);
        return getCached("dashboard:reviews:" + normalizedPeriod, () -> {
            DateRange range = getDateRange(normalizedPeriod);
            return DashboardReviewStatsResponse.builder()
                    .totalFeedbacks(feedbackRepository.count())
                    .newFeedbacks(feedbackRepository.countNewFeedbacks(range.from(), range.to()))
                    .ratingDistribution(buildRatingDistribution())
                    .build();
        });
    }

    @Override
    public List<TopVariantItem> getTopVariants(String period, int limit) {
        String normalizedPeriod = normalizePeriod(period);
        int safeLimit = Math.max(1, Math.min(limit, 200));
        return getCached("dashboard:top-variants:" + normalizedPeriod + ":" + safeLimit, () -> {
            DateRange range = getDateRange(normalizedPeriod);
            return buildTopVariants(range.from(), range.to(), safeLimit);
        });
    }

    private List<RevenueChartItem> buildRevenueChart(String period, LocalDateTime from, LocalDateTime to) {
        if ("YEAR".equalsIgnoreCase(period)) {
            int year = LocalDate.now().getYear();
            List<Object[]> rows = orderRepository.findRevenueGroupedByMonth(year);
            return rows.stream().map(row -> RevenueChartItem.builder()
                    .label("Tháng " + ((Number) row[0]).intValue())
                    .revenue((BigDecimal) row[1])
                    .orders(((Number) row[2]).longValue())
                    .build()
            ).collect(Collectors.toList());
        }

        List<Object[]> rows = orderRepository.findRevenueGroupedByDate(from, to);
        return rows.stream().map(row -> RevenueChartItem.builder()
                .label(row[0].toString())
                .revenue((BigDecimal) row[1])
                .orders(((Number) row[2]).longValue())
                .build()
        ).collect(Collectors.toList());
    }

    private List<TopProductItem> buildTopProducts(LocalDateTime from, LocalDateTime to) {
        List<Object[]> rows = orderItemRepository.findTopSellingProducts(from, to, PageRequest.of(0, 10));
        List<UUID> productIds = rows.stream()
                .map(row -> (UUID) row[0])
                .filter(Objects::nonNull)
                .toList();
        Map<UUID, String> imageByProductId = productImageRepository.findPreferredImageMapByProductIds(productIds);

        return rows.stream().map(row -> {
            UUID productId = (UUID) row[0];
            return TopProductItem.builder()
                    .id(productId.toString())
                    .name((String) row[1])
                    .imageUrl(imageByProductId.get(productId))
                    .totalSold(((Number) row[2]).longValue())
                    .revenue((BigDecimal) row[3])
                    .build();
        }).collect(Collectors.toList());
    }

    private List<TopVariantItem> buildTopVariants(LocalDateTime from, LocalDateTime to, int limit) {
        List<Object[]> rows = orderItemRepository.findTopSellingVariants(from, to, PageRequest.of(0, limit));
        List<UUID> variantIds = rows.stream()
                .map(row -> (UUID) row[0])
                .filter(Objects::nonNull)
                .toList();

        Map<UUID, Long> returnedByVariantId = variantIds.isEmpty()
                ? Map.of()
                : returnItemRepository.sumReturnedQuantityByVariantIdsBetween(variantIds, from, to).stream()
                .collect(Collectors.toMap(
                        row -> (UUID) row[0],
                        row -> ((Number) row[1]).longValue(),
                        Long::sum));

        return rows.stream().map(row -> {
            UUID variantId = (UUID) row[0];
            long grossSoldQty = ((Number) row[4]).longValue();
            long returnedQty = returnedByVariantId.getOrDefault(variantId, 0L);
            return TopVariantItem.builder()
                    .variantId(variantId.toString())
                    .productId(row[1].toString())
                    .productName((String) row[2])
                    .variantName((String) row[3])
                    .totalSold(grossSoldQty)
                    .grossSoldQty(grossSoldQty)
                    .returnedQty(returnedQty)
                    .netSoldQty(Math.max(grossSoldQty - returnedQty, 0L))
                    .revenue((BigDecimal) row[5])
                    .build();
        }).collect(Collectors.toList());
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
        return orders.stream().map(order -> RecentOrderItem.builder()
                .orderNumber(order.getOrderNumber())
                .customerName(order.getUser().getFullName())
                .totalAmount(order.getTotalAmount())
                .status(order.getOrderStatus().name())
                .createdAt(order.getCreatedAt())
                .build()
        ).collect(Collectors.toList());
    }

    private Map<Integer, Long> buildRatingDistribution() {
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

    @SuppressWarnings("unchecked")
    private <T> T getCached(String key, Supplier<T> loader) {
        long now = System.currentTimeMillis();
        CacheEntry<?> cached = uiCache.get(key);
        if (cached != null && cached.expiresAt() > now) {
            return (T) cached.value();
        }

        T value = loader.get();
        uiCache.put(key, new CacheEntry<>(value, now + UI_CACHE_TTL_MS));
        return value;
    }

    private String normalizePeriod(String period) {
        return period == null || period.isBlank() ? "MONTH" : period.toUpperCase(Locale.ROOT);
    }

    private DateRange getDateRange(String period) {
        LocalDate today = LocalDate.now();
        LocalDateTime from;
        LocalDateTime to = today.atTime(LocalTime.MAX);

        switch (period) {
            case "DAY":
                from = today.atStartOfDay();
                break;
            case "WEEK":
                LocalDate monday = today.with(java.time.DayOfWeek.MONDAY);
                from = monday.atStartOfDay();
                to = monday.plusDays(6).atTime(LocalTime.MAX);
                break;
            case "YEAR":
                from = today.with(TemporalAdjusters.firstDayOfYear()).atStartOfDay();
                break;
            case "MONTH":
            default:
                from = today.with(TemporalAdjusters.firstDayOfMonth()).atStartOfDay();
                break;
        }

        return new DateRange(from, to);
    }

    private record CacheEntry<T>(T value, long expiresAt) {
    }

    private record DateRange(LocalDateTime from, LocalDateTime to) {
    }
}
