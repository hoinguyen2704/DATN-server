package com.hoz.hozitech.application.services;

import com.hoz.hozitech.domain.dtos.response.DashboardStatsResponse;

public interface DashboardService {

    DashboardStatsResponse getDashboardStats(String period);
}
