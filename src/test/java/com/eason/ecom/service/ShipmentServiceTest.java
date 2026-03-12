package com.eason.ecom.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.eason.ecom.dto.ShipmentCreateRequest;
import com.eason.ecom.dto.ShipmentResponse;
import com.eason.ecom.entity.CustomerOrder;
import com.eason.ecom.entity.OrderStatus;
import com.eason.ecom.entity.Shipment;
import com.eason.ecom.entity.ShipmentStatus;
import com.eason.ecom.entity.UserAccount;
import com.eason.ecom.entity.UserRole;
import com.eason.ecom.messaging.OrderLifecycleEventFactory;
import com.eason.ecom.messaging.OrderLifecycleEventPublisher;
import com.eason.ecom.repository.CustomerOrderRepository;
import com.eason.ecom.repository.ShipmentRepository;
import com.eason.ecom.support.ShipmentNumberGenerator;

@ExtendWith(MockitoExtension.class)
class ShipmentServiceTest {

    @Mock
    private ShipmentRepository shipmentRepository;

    @Mock
    private CustomerOrderRepository customerOrderRepository;

    @Mock
    private OrderService orderService;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private CommerceMetricsService commerceMetricsService;

    @Mock
    private ShipmentNumberGenerator shipmentNumberGenerator;

    @Mock
    private OrderLifecycleEventFactory orderLifecycleEventFactory;

    @Mock
    private OrderLifecycleEventPublisher orderLifecycleEventPublisher;

    private ShipmentService shipmentService;

    @BeforeEach
    void setUp() {
        shipmentService = new ShipmentService(
                shipmentRepository,
                customerOrderRepository,
                orderService,
                auditLogService,
                commerceMetricsService,
                shipmentNumberGenerator,
                orderLifecycleEventFactory,
                orderLifecycleEventPublisher);
    }

    @Test
    void createShipmentMovesPaidOrderIntoShipmentFlow() {
        CustomerOrder customerOrder = buildOrder(55L, OrderStatus.PAID);
        when(customerOrderRepository.findWithDetailsById(55L)).thenReturn(Optional.of(customerOrder));
        when(shipmentNumberGenerator.next()).thenReturn("SHP-20260312153000000-2222");
        when(shipmentRepository.save(any(Shipment.class))).thenAnswer(invocation -> {
            Shipment shipment = invocation.getArgument(0);
            shipment.setId(12L);
            shipment.setCreatedAt(LocalDateTime.of(2026, 3, 12, 15, 30));
            shipment.setUpdatedAt(LocalDateTime.of(2026, 3, 12, 15, 30));
            return shipment;
        });

        ShipmentResponse response = shipmentService.createShipment(
                55L,
                new ShipmentCreateRequest("CANADA_POST", "CP123456789CA", "Packed and dispatched"),
                1L,
                "admin");

        assertEquals("IN_TRANSIT", response.shipmentStatus());
        assertEquals("SHP-20260312153000000-2222", response.shipmentNo());
        verify(orderService).updateOrderStatusForAdmin(
                55L,
                OrderStatus.ALLOCATED.name(),
                "Order allocated to warehouse fulfillment",
                1L,
                "admin");
        verify(orderService).updateOrderStatusForAdmin(
                55L,
                OrderStatus.SHIPPED.name(),
                "Shipment dispatched to carrier CANADA_POST",
                1L,
                "admin");
    }

    @Test
    void markDeliveredCompletesShippedOrder() {
        CustomerOrder customerOrder = buildOrder(55L, OrderStatus.SHIPPED);
        Shipment shipment = buildShipment(customerOrder);
        when(shipmentRepository.findWithDetailsById(12L)).thenReturn(Optional.of(shipment));
        when(shipmentRepository.save(any(Shipment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ShipmentResponse response = shipmentService.markDelivered(12L, "Delivered at front desk", 1L, "admin");

        assertEquals("DELIVERED", response.shipmentStatus());
        verify(orderService).updateOrderStatusForAdmin(
                55L,
                OrderStatus.COMPLETED.name(),
                "Shipment delivered to customer",
                1L,
                "admin");
    }

    private CustomerOrder buildOrder(Long id, OrderStatus status) {
        CustomerOrder customerOrder = new CustomerOrder();
        customerOrder.setId(id);
        customerOrder.setOrderNo("ORD-202603120001-1001");
        customerOrder.setStatus(status);
        customerOrder.setUser(buildUser());
        customerOrder.setSubtotalAmount(BigDecimal.valueOf(129.99));
        customerOrder.setTaxAmount(BigDecimal.ZERO);
        customerOrder.setShippingAmount(BigDecimal.ZERO);
        customerOrder.setDiscountAmount(BigDecimal.ZERO);
        customerOrder.setTotalAmount(BigDecimal.valueOf(129.99));
        customerOrder.setCreatedAt(LocalDateTime.of(2026, 3, 12, 14, 0));
        customerOrder.setUpdatedAt(LocalDateTime.of(2026, 3, 12, 14, 0));
        customerOrder.setStatusUpdatedAt(LocalDateTime.of(2026, 3, 12, 14, 0));
        return customerOrder;
    }

    private UserAccount buildUser() {
        UserAccount userAccount = new UserAccount();
        userAccount.setId(2L);
        userAccount.setUsername("demo");
        userAccount.setEmail("demo@ecom.local");
        userAccount.setPassword("encoded");
        userAccount.setRole(UserRole.CUSTOMER);
        userAccount.setCreatedAt(LocalDateTime.of(2026, 3, 12, 12, 0));
        return userAccount;
    }

    private Shipment buildShipment(CustomerOrder customerOrder) {
        Shipment shipment = new Shipment();
        shipment.setId(12L);
        shipment.setOrder(customerOrder);
        shipment.setShipmentNo("SHP-20260312153000000-2222");
        shipment.setCarrierCode("CANADA_POST");
        shipment.setTrackingNo("CP123456789CA");
        shipment.setShipmentStatus(ShipmentStatus.IN_TRANSIT);
        shipment.setCreatedAt(LocalDateTime.of(2026, 3, 12, 15, 30));
        shipment.setShippedAt(LocalDateTime.of(2026, 3, 12, 15, 30));
        shipment.setUpdatedAt(LocalDateTime.of(2026, 3, 12, 15, 30));
        return shipment;
    }
}
