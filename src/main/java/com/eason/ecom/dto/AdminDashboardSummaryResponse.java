package com.eason.ecom.dto;

import java.math.BigDecimal;
import java.util.List;

public record AdminDashboardSummaryResponse(
        long totalOrders,
        long ordersCreatedToday,
        long fulfillmentInFlight,
        BigDecimal capturedRevenue30Days,
        BigDecimal averageOrderValue30Days,
        long activeRefundCases,
        long openSupportTickets,
        long urgentSupportTickets,
        long activeCatalogProducts,
        long featuredProducts,
        long lowStockProducts,
        List<MetricBreakdown> orderStatusBreakdown,
        List<MetricBreakdown> supportStatusBreakdown,
        List<LowStockAlert> lowStockAlerts) {

    public record MetricBreakdown(String code, String label, long count) {
    }

    public record LowStockAlert(
            Long productId,
            String sku,
            String name,
            String category,
            int stock,
            int safetyStock,
            int shortage) {
    }
}
