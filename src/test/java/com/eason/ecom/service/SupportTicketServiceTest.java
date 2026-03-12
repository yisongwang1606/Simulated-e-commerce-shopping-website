package com.eason.ecom.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import com.eason.ecom.dto.PagedResponse;
import com.eason.ecom.dto.SupportTicketCreateRequest;
import com.eason.ecom.dto.SupportTicketResponse;
import com.eason.ecom.dto.SupportTicketUpdateRequest;
import com.eason.ecom.entity.CustomerOrder;
import com.eason.ecom.entity.SupportTicket;
import com.eason.ecom.entity.SupportTicketPriority;
import com.eason.ecom.entity.SupportTicketStatus;
import com.eason.ecom.entity.UserAccount;
import com.eason.ecom.entity.UserRole;
import com.eason.ecom.exception.BadRequestException;
import com.eason.ecom.repository.CustomerOrderRepository;
import com.eason.ecom.repository.SupportTicketRepository;
import com.eason.ecom.support.SupportTicketNumberGenerator;

@ExtendWith(MockitoExtension.class)
class SupportTicketServiceTest {

    @Mock
    private SupportTicketRepository supportTicketRepository;

    @Mock
    private CustomerOrderRepository customerOrderRepository;

    @Mock
    private SupportTicketNumberGenerator supportTicketNumberGenerator;

    @Mock
    private AuditLogService auditLogService;

    private SupportTicketService supportTicketService;

    @BeforeEach
    void setUp() {
        supportTicketService = new SupportTicketService(
                supportTicketRepository,
                customerOrderRepository,
                supportTicketNumberGenerator,
                auditLogService);
    }

    @Test
    void createTicketPersistsSupportCase() {
        CustomerOrder customerOrder = buildOrder();

        when(customerOrderRepository.findByIdAndUserId(55L, 2L)).thenReturn(Optional.of(customerOrder));
        when(supportTicketNumberGenerator.next()).thenReturn("TKT-20260312150000000-1001");
        when(supportTicketRepository.save(any(SupportTicket.class))).thenAnswer(invocation -> {
            SupportTicket supportTicket = invocation.getArgument(0);
            supportTicket.setId(4L);
            supportTicket.setCreatedAt(LocalDateTime.of(2026, 3, 12, 15, 0));
            supportTicket.setUpdatedAt(LocalDateTime.of(2026, 3, 12, 15, 0));
            return supportTicket;
        });

        SupportTicketResponse response = supportTicketService.createTicket(
                2L,
                "demo",
                55L,
                new SupportTicketCreateRequest(
                        "DELIVERY",
                        "HIGH",
                        "Parcel marked delivered but not received",
                        "Nothing arrived at the building concierge."));

        assertEquals("OPEN", response.ticketStatus());
        assertEquals("HIGH", response.priority());
        assertEquals("TKT-20260312150000000-1001", response.ticketNo());
    }

    @Test
    void createTicketRejectsInvalidPriority() {
        CustomerOrder customerOrder = buildOrder();
        when(customerOrderRepository.findByIdAndUserId(55L, 2L)).thenReturn(Optional.of(customerOrder));

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> supportTicketService.createTicket(
                        2L,
                        "demo",
                        55L,
                        new SupportTicketCreateRequest(
                                "DELIVERY",
                                "SEVERE",
                                "Parcel marked delivered but not received",
                                "Nothing arrived at the building concierge.")));

        assertEquals("Unsupported support ticket priority: SEVERE", exception.getMessage());
    }

    @Test
    void adminTicketListReturnsPagedPayload() {
        SupportTicket supportTicket = buildTicket();
        when(supportTicketRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(supportTicket), PageRequest.of(0, 20), 1));

        PagedResponse<SupportTicketResponse> response = supportTicketService.getTicketsForAdmin(
                "OPEN",
                "HIGH",
                "Customer Support",
                0,
                20);

        assertEquals(1, response.totalElements());
        assertEquals("OPEN", response.items().getFirst().ticketStatus());
    }

    @Test
    void updateTicketStoresAssignmentAndResolution() {
        SupportTicket supportTicket = buildTicket();
        when(supportTicketRepository.findWithDetailsById(4L)).thenReturn(Optional.of(supportTicket));
        when(supportTicketRepository.save(any(SupportTicket.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SupportTicketResponse response = supportTicketService.updateTicket(
                4L,
                new SupportTicketUpdateRequest(
                        "RESOLVED",
                        "Customer Support",
                        "support.alex",
                        "Carrier investigation completed.",
                        "Carrier confirmed delivery to the correct concierge desk."),
                1L,
                "admin");

        assertEquals("RESOLVED", response.ticketStatus());
        assertEquals("Customer Support", response.assignedTeam());
        assertEquals("support.alex", response.assignedToUsername());
    }

    private CustomerOrder buildOrder() {
        CustomerOrder customerOrder = new CustomerOrder();
        customerOrder.setId(55L);
        customerOrder.setOrderNo("ORD-202603120001-1001");
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

    private SupportTicket buildTicket() {
        SupportTicket supportTicket = new SupportTicket();
        supportTicket.setId(4L);
        supportTicket.setOrder(buildOrder());
        supportTicket.setTicketNo("TKT-20260312150000000-1001");
        supportTicket.setRequestedByUserId(2L);
        supportTicket.setRequestedByUsername("demo");
        supportTicket.setTicketStatus(SupportTicketStatus.OPEN);
        supportTicket.setPriority(SupportTicketPriority.HIGH);
        supportTicket.setCategory("DELIVERY");
        supportTicket.setSubject("Parcel marked delivered but not received");
        supportTicket.setCustomerMessage("Nothing arrived at the building concierge.");
        supportTicket.setCreatedAt(LocalDateTime.of(2026, 3, 12, 15, 0));
        supportTicket.setUpdatedAt(LocalDateTime.of(2026, 3, 12, 15, 0));
        return supportTicket;
    }
}
