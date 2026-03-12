package com.eason.ecom.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.eason.ecom.dto.OrderInternalNoteRequest;
import com.eason.ecom.dto.OrderInternalNoteResponse;
import com.eason.ecom.entity.CustomerOrder;
import com.eason.ecom.entity.OrderInternalNote;
import com.eason.ecom.repository.CustomerOrderRepository;
import com.eason.ecom.repository.OrderInternalNoteRepository;

@ExtendWith(MockitoExtension.class)
class OrderInternalNoteServiceTest {

    @Mock
    private OrderInternalNoteRepository orderInternalNoteRepository;

    @Mock
    private CustomerOrderRepository customerOrderRepository;

    @Mock
    private AuditLogService auditLogService;

    private OrderInternalNoteService orderInternalNoteService;

    @BeforeEach
    void setUp() {
        orderInternalNoteService = new OrderInternalNoteService(
                orderInternalNoteRepository,
                customerOrderRepository,
                auditLogService);
    }

    @Test
    void addNotePersistsOperationalComment() {
        CustomerOrder customerOrder = new CustomerOrder();
        customerOrder.setId(55L);
        customerOrder.setOrderNo("ORD-202603120001-1001");
        when(customerOrderRepository.findWithDetailsById(55L)).thenReturn(Optional.of(customerOrder));
        when(orderInternalNoteRepository.save(any(OrderInternalNote.class))).thenAnswer(invocation -> {
            OrderInternalNote note = invocation.getArgument(0);
            note.setId(3L);
            note.setCreatedAt(LocalDateTime.of(2026, 3, 12, 16, 20));
            return note;
        });

        OrderInternalNoteResponse response = orderInternalNoteService.addNote(
                55L,
                new OrderInternalNoteRequest("Customer asked support to hold shipment until Friday."),
                1L,
                "admin");

        assertEquals(3L, response.id());
        assertEquals("ORD-202603120001-1001", response.orderNo());
        assertEquals("admin", response.createdByUsername());
        verify(auditLogService).recordUserAction(any(), any(), any(), any(), any(), any(), any());
    }
}
