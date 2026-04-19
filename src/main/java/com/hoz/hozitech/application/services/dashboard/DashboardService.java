package com.hoz.hozitech.application.services.dashboard;

import com.hoz.hozitech.domain.dtos.response.DashboardRevenueResponse;
import com.hoz.hozitech.domain.dtos.response.DashboardReviewStatsResponse;
import com.hoz.hozitech.domain.dtos.response.DashboardStatsResponse;
import com.hoz.hozitech.domain.dtos.response.DashboardSummaryResponse;
import com.hoz.hozitech.domain.dtos.response.DashboardTopListsResponse;

import java.util.List;

public interface DashboardService {

    DashboardStatsResponse getDashboardStats(String period);

    DashboardSummaryResponse getDashboardSummary(String period);

    DashboardRevenueResponse getDashboardRevenue(String period);

    DashboardTopListsResponse getDashboardTopLists(String period);

    List<DashboardStatsResponse.RecentOrderItem> getRecentOrders();

    DashboardReviewStatsResponse getDashboardReviewStats(String period);

    List<DashboardStatsResponse.TopVariantItem> getTopVariants(String period, int limit);
}
