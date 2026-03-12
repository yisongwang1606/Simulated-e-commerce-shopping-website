package com.eason.ecom.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.eason.ecom.dto.AdminDashboardSummaryResponse;
import com.eason.ecom.entity.OrderStatus;
import com.eason.ecom.entity.Product;
import com.eason.ecom.entity.ProductStatus;
import com.eason.ecom.entity.RefundStatus;
import com.eason.ecom.entity.SupportTicketPriority;
import com.eason.ecom.entity.SupportTicketStatus;
import com.eason.ecom.repository.CustomerOrderRepository;
import com.eason.ecom.repository.ProductRepository;
import com.eason.ecom.repository.RefundRequestRepository;
import com.eason.ecom.repository.SupportTicketRepository;

@Service
public class AdminDashboardService {

    private static final List<OrderStatus> REVENUE_STATUSES = List.of(
            OrderStatus.PAID,
            OrderStatus.ALLOCATED,
            OrderStatus.SHIPPED,
            OrderStatus.COMPLETED,
            OrderStatus.REFUND_PENDING,
            OrderStatus.REFUNDED);

    private static final List<OrderStatus> FULFILLMENT_STATUSES = List.of(
            OrderStatus.PAYMENT_PENDING,
            OrderStatus.PAID,
            OrderStatus.ALLOCATED,
            OrderStatus.SHIPPED,
            OrderStatus.REFUND_PENDING);

    private static final List<RefundStatus> ACTIVE_REFUND_STATUSES = List.of(
            RefundStatus.REQUESTED,
            RefundStatus.APPROVED);

    private static final List<SupportTicketStatus> OPEN_SUPPORT_STATUSES = List.of(
            SupportTicketStatus.OPEN,
            SupportTicketStatus.IN_PROGRESS,
            SupportTicketStatus.WAITING_ON_CUSTOMER);

    private final CustomerOrderRepository customerOrderRepository;
    private final ProductRepository productRepository;
    private final RefundRequestRepository refundRequestRepository;
    private final SupportTicketRepository supportTicketRepository;

    public AdminDashboardService(
            CustomerOrderRepository customerOrderRepository,
            ProductRepository productRepository,
            RefundRequestRepository refundRequestRepository,
            SupportTicketRepository supportTicketRepository) {
        this.customerOrderRepository = customerOrderRepository;
        this.productRepository = productRepository;
        this.refundRequestRepository = refundRequestRepository;
        this.supportTicketRepository = supportTicketRepository;
    }

    @Transactional(readOnly = true)
    public AdminDashboardSummaryResponse getSummary() {
        long totalOrders = customerOrderRepository.count();
        long ordersCreatedToday = customerOrderRepository.countCreatedSince(LocalDate.now().atStartOfDay());
        long fulfillmentInFlight = customerOrderRepository.countByStatusIn(FULFILLMENT_STATUSES);
        long activeRefundCases = refundRequestRepository.countByRefundStatusIn(ACTIVE_REFUND_STATUSES);
        long openSupportTickets = supportTicketRepository.countByTicketStatusIn(OPEN_SUPPORT_STATUSES);
        long urgentSupportTickets = supportTicketRepository.countByPriorityAndTicketStatusIn(
                SupportTicketPriority.URGENT,
                OPEN_SUPPORT_STATUSES);
        long activeCatalogProducts = productRepository.countByStatus(ProductStatus.ACTIVE);
        long featuredProducts = productRepository.countByFeaturedTrueAndStatus(ProductStatus.ACTIVE);
        long lowStockProducts = productRepository.countLowStockProducts();

        List<Object[]> commercialWindowRows = customerOrderRepository.summarizeCommercialWindow(
                LocalDate.now().minusDays(30).atStartOfDay(),
                REVENUE_STATUSES);
        Object[] commercialWindow = commercialWindowRows.isEmpty()
                ? new Object[] {BigDecimal.ZERO, 0L}
                : commercialWindowRows.getFirst();
        BigDecimal capturedRevenue30Days = commercialWindow[0] instanceof BigDecimal bigDecimal
                ? bigDecimal
                : BigDecimal.valueOf(((Number) commercialWindow[0]).doubleValue());
        long orderCountInWindow = commercialWindow[1] == null ? 0L : ((Number) commercialWindow[1]).longValue();
        BigDecimal averageOrderValue30Days = orderCountInWindow == 0
                ? BigDecimal.ZERO
                : capturedRevenue30Days.divide(BigDecimal.valueOf(orderCountInWindow), 2, RoundingMode.HALF_UP);

        List<AdminDashboardSummaryResponse.MetricBreakdown> orderStatusBreakdown =
                toOrderBreakdown(customerOrderRepository.summarizeByStatus());
        List<AdminDashboardSummaryResponse.MetricBreakdown> supportStatusBreakdown =
                toSupportBreakdown(supportTicketRepository.summarizeByStatus());
        List<AdminDashboardSummaryResponse.LowStockAlert> lowStockAlerts = productRepository.findLowStockAlerts(
                PageRequest.of(0, 5)).stream()
                .map(this::toLowStockAlert)
                .toList();

        return new AdminDashboardSummaryResponse(
                totalOrders,
                ordersCreatedToday,
                fulfillmentInFlight,
                capturedRevenue30Days,
                averageOrderValue30Days,
                activeRefundCases,
                openSupportTickets,
                urgentSupportTickets,
                activeCatalogProducts,
                featuredProducts,
                lowStockProducts,
                orderStatusBreakdown,
                supportStatusBreakdown,
                lowStockAlerts);
    }

    private List<AdminDashboardSummaryResponse.MetricBreakdown> toOrderBreakdown(List<Object[]> rows) {
        Map<OrderStatus, Long> counts = new EnumMap<>(OrderStatus.class);
        Arrays.stream(OrderStatus.values()).forEach(status -> counts.put(status, 0L));
        for (Object[] row : rows) {
            counts.put((OrderStatus) row[0], ((Number) row[1]).longValue());
        }
        return Arrays.stream(OrderStatus.values())
                .map(status -> new AdminDashboardSummaryResponse.MetricBreakdown(
                        status.name(),
                        toLabel(status.name()),
                        counts.getOrDefault(status, 0L)))
                .toList();
    }

    private List<AdminDashboardSummaryResponse.MetricBreakdown> toSupportBreakdown(List<Object[]> rows) {
        Map<SupportTicketStatus, Long> counts = new EnumMap<>(SupportTicketStatus.class);
        Arrays.stream(SupportTicketStatus.values()).forEach(status -> counts.put(status, 0L));
        for (Object[] row : rows) {
            counts.put((SupportTicketStatus) row[0], ((Number) row[1]).longValue());
        }
        return Arrays.stream(SupportTicketStatus.values())
                .map(status -> new AdminDashboardSummaryResponse.MetricBreakdown(
                        status.name(),
                        toLabel(status.name()),
                        counts.getOrDefault(status, 0L)))
                .toList();
    }

    private AdminDashboardSummaryResponse.LowStockAlert toLowStockAlert(Product product) {
        int shortage = Math.max(product.getSafetyStock() - product.getStock(), 0);
        return new AdminDashboardSummaryResponse.LowStockAlert(
                product.getId(),
                product.getSku(),
                product.getName(),
                product.getCategory(),
                product.getStock(),
                product.getSafetyStock(),
                shortage);
    }

    private String toLabel(String code) {
        String[] parts = code.toLowerCase().split("_");
        StringBuilder labelBuilder = new StringBuilder();
        for (String part : parts) {
            if (labelBuilder.length() > 0) {
                labelBuilder.append(' ');
            }
            labelBuilder.append(Character.toUpperCase(part.charAt(0)));
            labelBuilder.append(part.substring(1));
        }
        return labelBuilder.toString();
    }
}
