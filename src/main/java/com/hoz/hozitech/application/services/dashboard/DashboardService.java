package com.hoz.hozitech.application.services.dashboard;

import com.hoz.hozitech.domain.dtos.response.DashboardStatsResponse;

import java.util.List;

public interface DashboardService {

    DashboardStatsResponse getDashboardStats(String period);

    List<DashboardStatsResponse.TopVariantItem> getTopVariants(String period, int limit);
}
