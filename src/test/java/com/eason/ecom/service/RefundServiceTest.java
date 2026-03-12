package com.eason.ecom.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;

import com.eason.ecom.dto.PagedResponse;
import com.eason.ecom.dto.RefundRequestCreateRequest;
import com.eason.ecom.dto.RefundRequestResponse;
import com.eason.ecom.dto.RefundSummaryResponse;
import com.eason.ecom.dto.RefundReviewRequest;
import com.eason.ecom.entity.CustomerOrder;
import com.eason.ecom.entity.OrderStatus;
import com.eason.ecom.entity.RefundRequest;
import com.eason.ecom.entity.RefundStatus;
import com.eason.ecom.entity.UserAccount;
import com.eason.ecom.entity.UserRole;
import com.eason.ecom.exception.BadRequestException;
import com.eason.ecom.messaging.OrderLifecycleEventFactory;
import com.eason.ecom.messaging.OrderLifecycleEventPublisher;
import com.eason.ecom.repository.CustomerOrderRepository;
import com.eason.ecom.repository.RefundRequestRepository;

@ExtendWith(MockitoExtension.class)
class RefundServiceTest {

    @Mock
    private RefundRequestRepository refundRequestRepository;

    @Mock
    private CustomerOrderRepository customerOrderRepository;

    @Mock
    private OrderService orderService;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private CommerceMetricsService commerceMetricsService;

    @Mock
    private OrderLifecycleEventFactory orderLifecycleEventFactory;

    @Mock
    private OrderLifecycleEventPublisher orderLifecycleEventPublisher;

    private RefundService refundService;

    @BeforeEach
    void setUp() {
        refundService = new RefundService(
                refundRequestRepository,
                customerOrderRepository,
                orderService,
                auditLogService,
                commerceMetricsService,
                orderLifecycleEventFactory,
                orderLifecycleEventPublisher);
    }

    @Test
    void createRefundRequestForCompletedOrder() {
        CustomerOrder customerOrder = buildOrder(OrderStatus.COMPLETED);
        when(customerOrderRepository.findByIdAndUserId(55L, 2L)).thenReturn(Optional.of(customerOrder));
        when(refundRequestRepository.existsByOrderIdAndRefundStatusIn(55L, List.of(RefundStatus.REQUESTED, RefundStatus.APPROVED)))
                .thenReturn(false);
        when(refundRequestRepository.save(any(RefundRequest.class))).thenAnswer(invocation -> {
            RefundRequest refundRequest = invocation.getArgument(0);
            refundRequest.setId(4L);
            refundRequest.setRequestedAt(LocalDateTime.of(2026, 3, 12, 17, 0));
            return refundRequest;
        });

        RefundRequestResponse response = refundService.createRefundRequest(
                2L,
                "demo",
                55L,
                new RefundRequestCreateRequest("Item arrived damaged."));

        assertEquals("REQUESTED", response.refundStatus());
        assertEquals("demo", response.requestedByUsername());
    }

    @Test
    void reviewRefundRequestApprovesAndMovesOrderToRefundPending() {
        RefundRequest refundRequest = buildRefundRequest();
        when(refundRequestRepository.findById(4L)).thenReturn(Optional.of(refundRequest));
        when(refundRequestRepository.save(any(RefundRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RefundRequestResponse response = refundService.reviewRefundRequest(
                4L,
                new RefundReviewRequest("APPROVED", "Damage evidence confirmed."),
                1L,
                "admin");

        assertEquals("APPROVED", response.refundStatus());
        verify(orderService).updateOrderStatusForAdmin(
                55L,
                OrderStatus.REFUND_PENDING.name(),
                "Refund approved for order ORD-202603120001-1001",
                1L,
                "admin");
    }

    @Test
    void createRefundRequestRejectsWrongOrderState() {
        CustomerOrder customerOrder = buildOrder(OrderStatus.PAID);
        when(customerOrderRepository.findByIdAndUserId(55L, 2L)).thenReturn(Optional.of(customerOrder));

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> refundService.createRefundRequest(
                        2L,
                        "demo",
                        55L,
                        new RefundRequestCreateRequest("No longer needed")));

        assertEquals("Refund can only be requested for shipped or completed orders", exception.getMessage());
    }

    @Test
    void adminRefundListReturnsPagedPayload() {
        RefundRequest refundRequest = buildRefundRequest();
        when(refundRequestRepository.findByRefundStatusOrderByRequestedAtDesc(eq(RefundStatus.REQUESTED), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(refundRequest), PageRequest.of(0, 20), 1));

        PagedResponse<RefundRequestResponse> response = refundService.getRefundRequestsForAdmin("REQUESTED", 0, 20);

        assertEquals(1, response.totalElements());
        assertEquals("REQUESTED", response.items().getFirst().refundStatus());
    }

    @Test
    void refundSummaryAggregatesCountsAndAmounts() {
        when(refundRequestRepository.summarizeByStatus(any(), any())).thenReturn(List.of(
                new Object[] {RefundStatus.REQUESTED, 2L, new java.math.BigDecimal("145.00")},
                new Object[] {RefundStatus.SETTLED, 1L, new java.math.BigDecimal("72.50")}));

        RefundSummaryResponse response = refundService.getRefundSummary(null, null);

        assertEquals(3L, response.totalRequests());
        assertEquals(2L, response.requestedCount());
        assertEquals(new java.math.BigDecimal("145.00"), response.requestedAmount());
        assertEquals(1L, response.settledCount());
        assertEquals(new java.math.BigDecimal("72.50"), response.settledAmount());
    }

    private CustomerOrder buildOrder(OrderStatus status) {
        CustomerOrder customerOrder = new CustomerOrder();
        customerOrder.setId(55L);
        customerOrder.setOrderNo("ORD-202603120001-1001");
        customerOrder.setStatus(status);
        UserAccount user = new UserAccount();
        user.setId(2L);
        user.setUsername("demo");
        user.setEmail("demo@ecom.local");
        user.setRole(UserRole.CUSTOMER);
        customerOrder.setUser(user);
        customerOrder.setCreatedAt(LocalDateTime.of(2026, 3, 12, 14, 0));
        customerOrder.setUpdatedAt(LocalDateTime.of(2026, 3, 12, 14, 0));
        customerOrder.setStatusUpdatedAt(LocalDateTime.of(2026, 3, 12, 14, 0));
        return customerOrder;
    }

    private RefundRequest buildRefundRequest() {
        RefundRequest refundRequest = new RefundRequest();
        refundRequest.setId(4L);
        refundRequest.setOrder(buildOrder(OrderStatus.COMPLETED));
        refundRequest.setRequestedByUserId(2L);
        refundRequest.setRequestedByUsername("demo");
        refundRequest.setRefundStatus(RefundStatus.REQUESTED);
        refundRequest.setReason("Item arrived damaged.");
        refundRequest.setRequestedAt(LocalDateTime.of(2026, 3, 12, 17, 0));
        return refundRequest;
    }
}
