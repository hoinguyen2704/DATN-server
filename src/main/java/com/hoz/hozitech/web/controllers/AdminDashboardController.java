package com.hoz.hozitech.web.controllers;

import com.hoz.hozitech.application.services.DashboardService;
import com.hoz.hozitech.domain.dtos.response.DashboardStatsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("${api.prefix-admin}/dashboard")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/stats")
    public ResponseEntity<DashboardStatsResponse> getDashboardStats(
            @RequestParam(value = "period", defaultValue = "MONTH") String period) {
        return ResponseEntity.ok(dashboardService.getDashboardStats(period));
    }
}
