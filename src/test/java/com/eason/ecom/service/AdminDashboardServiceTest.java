package com.eason.ecom.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import com.eason.ecom.dto.AdminDashboardSummaryResponse;
import com.eason.ecom.entity.OrderStatus;
import com.eason.ecom.entity.Product;
import com.eason.ecom.entity.ProductStatus;
import com.eason.ecom.entity.SupportTicketStatus;
import com.eason.ecom.repository.CustomerOrderRepository;
import com.eason.ecom.repository.ProductRepository;
import com.eason.ecom.repository.RefundRequestRepository;
import com.eason.ecom.repository.SupportTicketRepository;

@ExtendWith(MockitoExtension.class)
class AdminDashboardServiceTest {

    @Mock
    private CustomerOrderRepository customerOrderRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private RefundRequestRepository refundRequestRepository;

    @Mock
    private SupportTicketRepository supportTicketRepository;

    private AdminDashboardService adminDashboardService;

    @BeforeEach
    void setUp() {
        adminDashboardService = new AdminDashboardService(
                customerOrderRepository,
                productRepository,
                refundRequestRepository,
                supportTicketRepository);
    }

    @Test
    void getSummaryBuildsOperationalSnapshot() {
        Product lowStockProduct = new Product();
        lowStockProduct.setId(7L);
        lowStockProduct.setSku("ELEC-007");
        lowStockProduct.setName("Docking Station");
        lowStockProduct.setCategory("Electronics");
        lowStockProduct.setStock(2);
        lowStockProduct.setSafetyStock(8);
        lowStockProduct.setStatus(ProductStatus.ACTIVE);
        lowStockProduct.setCreatedAt(LocalDateTime.of(2026, 3, 12, 9, 0));

        when(customerOrderRepository.count()).thenReturn(40L);
        when(customerOrderRepository.countCreatedSince(any())).thenReturn(5L);
        when(customerOrderRepository.countByStatusIn(any())).thenReturn(11L);
        when(customerOrderRepository.summarizeCommercialWindow(any(), any()))
                .thenReturn(List.<Object[]>of(new Object[] {new BigDecimal("1250.00"), 5L}));
        when(customerOrderRepository.summarizeByStatus()).thenReturn(List.of(
                new Object[] {OrderStatus.CREATED, 4L},
                new Object[] {OrderStatus.SHIPPED, 9L},
                new Object[] {OrderStatus.COMPLETED, 12L}));

        when(refundRequestRepository.countByRefundStatusIn(any())).thenReturn(3L);

        when(supportTicketRepository.countByTicketStatusIn(any())).thenReturn(6L);
        when(supportTicketRepository.countByPriorityAndTicketStatusIn(eq(com.eason.ecom.entity.SupportTicketPriority.URGENT), any()))
                .thenReturn(2L);
        when(supportTicketRepository.summarizeByStatus()).thenReturn(List.of(
                new Object[] {SupportTicketStatus.OPEN, 3L},
                new Object[] {SupportTicketStatus.IN_PROGRESS, 2L},
                new Object[] {SupportTicketStatus.RESOLVED, 1L}));

        when(productRepository.countByStatus(ProductStatus.ACTIVE)).thenReturn(100L);
        when(productRepository.countByFeaturedTrueAndStatus(ProductStatus.ACTIVE)).thenReturn(16L);
        when(productRepository.countLowStockProducts()).thenReturn(7L);
        when(productRepository.findLowStockAlerts(any(Pageable.class))).thenReturn(List.of(lowStockProduct));

        AdminDashboardSummaryResponse response = adminDashboardService.getSummary();

        assertEquals(40L, response.totalOrders());
        assertEquals(5L, response.ordersCreatedToday());
        assertEquals(11L, response.fulfillmentInFlight());
        assertEquals(new BigDecimal("1250.00"), response.capturedRevenue30Days());
        assertEquals(new BigDecimal("250.00"), response.averageOrderValue30Days());
        assertEquals(3L, response.activeRefundCases());
        assertEquals(6L, response.openSupportTickets());
        assertEquals(2L, response.urgentSupportTickets());
        assertEquals(100L, response.activeCatalogProducts());
        assertEquals(16L, response.featuredProducts());
        assertEquals(7L, response.lowStockProducts());
        assertEquals("Created", response.orderStatusBreakdown().getFirst().label());
        assertEquals(1, response.lowStockAlerts().size());
        assertEquals(6, response.lowStockAlerts().getFirst().shortage());
    }
}
