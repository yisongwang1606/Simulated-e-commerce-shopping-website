package com.eason.ecom.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
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

import com.eason.ecom.dto.OrderTagResponse;
import com.eason.ecom.entity.CustomerOrder;
import com.eason.ecom.entity.OrderTag;
import com.eason.ecom.entity.OrderTagAssignment;
import com.eason.ecom.repository.CustomerOrderRepository;
import com.eason.ecom.repository.OrderTagAssignmentRepository;
import com.eason.ecom.repository.OrderTagRepository;

@ExtendWith(MockitoExtension.class)
class OrderTagServiceTest {

    @Mock
    private OrderTagRepository orderTagRepository;

    @Mock
    private OrderTagAssignmentRepository orderTagAssignmentRepository;

    @Mock
    private CustomerOrderRepository customerOrderRepository;

    @Mock
    private AuditLogService auditLogService;

    private OrderTagService orderTagService;

    @BeforeEach
    void setUp() {
        orderTagService = new OrderTagService(
                orderTagRepository,
                orderTagAssignmentRepository,
                customerOrderRepository,
                auditLogService);
    }

    @Test
    void assignTagCreatesAssignmentAndReturnsUpdatedTags() {
        CustomerOrder customerOrder = buildOrder();
        OrderTag orderTag = buildTag(3L, "VIP");
        OrderTagAssignment assignment = buildAssignment(customerOrder, orderTag);

        when(customerOrderRepository.findWithDetailsById(55L)).thenReturn(Optional.of(customerOrder));
        when(orderTagRepository.findById(3L)).thenReturn(Optional.of(orderTag));
        when(orderTagAssignmentRepository.findByOrderIdAndOrderTagId(55L, 3L)).thenReturn(Optional.empty());
        when(orderTagAssignmentRepository.save(any(OrderTagAssignment.class))).thenReturn(assignment);
        when(orderTagAssignmentRepository.findByOrderIdWithTag(55L)).thenReturn(List.of(assignment));

        List<OrderTagResponse> response = orderTagService.assignTag(55L, 3L, 1L, "admin");

        assertEquals(1, response.size());
        assertEquals("VIP", response.getFirst().tagCode());
    }

    @Test
    void assignTagReturnsExistingAssignmentWithoutDuplicating() {
        CustomerOrder customerOrder = buildOrder();
        OrderTag orderTag = buildTag(3L, "VIP");
        OrderTagAssignment assignment = buildAssignment(customerOrder, orderTag);

        when(customerOrderRepository.findWithDetailsById(55L)).thenReturn(Optional.of(customerOrder));
        when(orderTagRepository.findById(3L)).thenReturn(Optional.of(orderTag));
        when(orderTagAssignmentRepository.findByOrderIdAndOrderTagId(55L, 3L)).thenReturn(Optional.of(assignment));
        when(orderTagAssignmentRepository.findByOrderIdWithTag(55L)).thenReturn(List.of(assignment));

        List<OrderTagResponse> response = orderTagService.assignTag(55L, 3L, 1L, "admin");

        assertEquals(1, response.size());
        verify(orderTagAssignmentRepository, never()).save(any(OrderTagAssignment.class));
    }

    private CustomerOrder buildOrder() {
        CustomerOrder customerOrder = new CustomerOrder();
        customerOrder.setId(55L);
        customerOrder.setOrderNo("ORD-202603120001-1001");
        customerOrder.setCreatedAt(LocalDateTime.of(2026, 3, 12, 10, 0));
        customerOrder.setUpdatedAt(LocalDateTime.of(2026, 3, 12, 10, 0));
        customerOrder.setStatusUpdatedAt(LocalDateTime.of(2026, 3, 12, 10, 0));
        return customerOrder;
    }

    private OrderTag buildTag(Long id, String code) {
        OrderTag orderTag = new OrderTag();
        orderTag.setId(id);
        orderTag.setTagCode(code);
        orderTag.setDisplayName("VIP Customer");
        orderTag.setTagGroup("CUSTOMER");
        orderTag.setTone("emerald");
        return orderTag;
    }

    private OrderTagAssignment buildAssignment(CustomerOrder customerOrder, OrderTag orderTag) {
        OrderTagAssignment assignment = new OrderTagAssignment();
        assignment.setId(99L);
        assignment.setOrder(customerOrder);
        assignment.setOrderTag(orderTag);
        assignment.setAssignedByUserId(1L);
        assignment.setAssignedByUsername("admin");
        return assignment;
    }
}
