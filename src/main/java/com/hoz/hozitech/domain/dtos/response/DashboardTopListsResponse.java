package com.hoz.hozitech.domain.dtos.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardTopListsResponse {
    private List<DashboardStatsResponse.TopProductItem> topProducts;
    private List<DashboardStatsResponse.TopCategoryItem> topCategories;
    private List<DashboardStatsResponse.TopCustomerItem> topCustomers;
}
