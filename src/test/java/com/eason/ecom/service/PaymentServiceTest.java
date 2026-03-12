package com.eason.ecom.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
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

import com.eason.ecom.dto.PaymentCallbackRequest;
import com.eason.ecom.dto.PaymentInitiationRequest;
import com.eason.ecom.dto.PaymentTransactionResponse;
import com.eason.ecom.entity.CustomerOrder;
import com.eason.ecom.entity.OrderStatus;
import com.eason.ecom.entity.PaymentMethod;
import com.eason.ecom.entity.PaymentStatus;
import com.eason.ecom.entity.PaymentTransaction;
import com.eason.ecom.entity.UserAccount;
import com.eason.ecom.entity.UserRole;
import com.eason.ecom.integration.stripe.StripePaymentGateway;
import com.eason.ecom.integration.stripe.StripePaymentIntentResult;
import com.eason.ecom.messaging.OrderLifecycleEventFactory;
import com.eason.ecom.messaging.OrderLifecycleEventPublisher;
import com.eason.ecom.repository.CustomerOrderRepository;
import com.eason.ecom.repository.PaymentTransactionRepository;
import com.eason.ecom.support.PaymentReferenceGenerator;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentTransactionRepository paymentTransactionRepository;

    @Mock
    private CustomerOrderRepository customerOrderRepository;

    @Mock
    private OrderService orderService;

    @Mock
    private RefundService refundService;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private CommerceMetricsService commerceMetricsService;

    @Mock
    private PaymentReferenceGenerator paymentReferenceGenerator;

    @Mock
    private OrderLifecycleEventFactory orderLifecycleEventFactory;

    @Mock
    private OrderLifecycleEventPublisher orderLifecycleEventPublisher;

    @Mock
    private StripePaymentGateway stripePaymentGateway;

    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentService(
                paymentTransactionRepository,
                customerOrderRepository,
                orderService,
                refundService,
                auditLogService,
                commerceMetricsService,
                paymentReferenceGenerator,
                orderLifecycleEventFactory,
                orderLifecycleEventPublisher,
                stripePaymentGateway);
    }

    @Test
    void initiatePaymentMovesCreatedOrderToPaymentPending() {
        CustomerOrder customerOrder = buildOrder(55L, OrderStatus.CREATED);
        when(customerOrderRepository.findWithDetailsById(55L)).thenReturn(Optional.of(customerOrder));
        when(paymentTransactionRepository.existsByOrderIdAndPaymentStatus(55L, PaymentStatus.SUCCEEDED)).thenReturn(false);
        when(paymentReferenceGenerator.next()).thenReturn("PAY-20260312150000000-1111");
        when(paymentTransactionRepository.save(any(PaymentTransaction.class))).thenAnswer(invocation -> {
            PaymentTransaction paymentTransaction = invocation.getArgument(0);
            paymentTransaction.setId(9L);
            paymentTransaction.setCreatedAt(LocalDateTime.of(2026, 3, 12, 15, 0));
            paymentTransaction.setUpdatedAt(LocalDateTime.of(2026, 3, 12, 15, 0));
            return paymentTransaction;
        });

        PaymentTransactionResponse response = paymentService.initiatePayment(
                55L,
                new PaymentInitiationRequest(
                        "CARD",
                        BigDecimal.valueOf(129.99),
                        "SIMULATED_GATEWAY",
                        null,
                        null,
                        null,
                        "Checkout payment"),
                1L,
                "admin");

        assertEquals("PENDING", response.paymentStatus());
        assertEquals("CARD", response.paymentMethod());
        assertEquals("PAY-20260312150000000-1111", response.transactionRef());
        verify(orderService).updateOrderStatusForAdmin(
                55L,
                OrderStatus.PAYMENT_PENDING.name(),
                "Payment initiated for order ORD-202603120001-1001",
                1L,
                "admin");
    }

    @Test
    void callbackSuccessMarksPaymentAsSucceededAndMovesOrderToPaid() {
        CustomerOrder customerOrder = buildOrder(55L, OrderStatus.PAYMENT_PENDING);
        PaymentTransaction paymentTransaction = buildPayment(customerOrder);
        when(paymentTransactionRepository.findByTransactionRef("PAY-20260312150000000-1111"))
                .thenReturn(Optional.of(paymentTransaction));
        when(paymentTransactionRepository.save(any(PaymentTransaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentTransactionResponse response = paymentService.handleCallback(
                new PaymentCallbackRequest(
                        "PAY-20260312150000000-1111",
                        "SUCCEEDED",
                        "evt_demo_001",
                        "SIMULATED_GATEWAY",
                        "Gateway settled payment"));

        assertEquals("SUCCEEDED", response.paymentStatus());
        assertEquals("evt_demo_001", response.providerEventId());
        verify(orderService).updateOrderStatusForAdmin(
                55L,
                OrderStatus.PAID.name(),
                "Payment callback confirmed settlement",
                null,
                "payment-callback");
    }

    @Test
    void initiateStripePaymentReturnsProviderReferenceAndClientSecret() {
        CustomerOrder customerOrder = buildOrder(55L, OrderStatus.CREATED);
        when(customerOrderRepository.findWithDetailsById(55L)).thenReturn(Optional.of(customerOrder));
        when(paymentTransactionRepository.existsByOrderIdAndPaymentStatus(55L, PaymentStatus.SUCCEEDED)).thenReturn(false);
        when(paymentReferenceGenerator.next()).thenReturn("PAY-20260312150000000-2222");
        when(stripePaymentGateway.createPaymentIntent(any(CustomerOrder.class), any(PaymentTransaction.class), any(PaymentInitiationRequest.class)))
                .thenReturn(new StripePaymentIntentResult(
                        "pi_test_123",
                        "pi_test_123_secret_456",
                        "PENDING",
                        "Stripe PaymentIntent status: requires_payment_method"));
        when(paymentTransactionRepository.save(any(PaymentTransaction.class))).thenAnswer(invocation -> {
            PaymentTransaction paymentTransaction = invocation.getArgument(0);
            paymentTransaction.setId(11L);
            paymentTransaction.setCreatedAt(LocalDateTime.of(2026, 3, 12, 15, 5));
            paymentTransaction.setUpdatedAt(LocalDateTime.of(2026, 3, 12, 15, 5));
            return paymentTransaction;
        });

        PaymentTransactionResponse response = paymentService.initiatePayment(
                55L,
                new PaymentInitiationRequest(
                        "CARD",
                        BigDecimal.valueOf(129.99),
                        "STRIPE",
                        "cad",
                        "pm_card_visa",
                        false,
                        "Stripe test mode"),
                1L,
                "admin");

        assertEquals("STRIPE", response.providerCode());
        assertEquals("pi_test_123", response.providerReference());
        assertEquals("pi_test_123_secret_456", response.clientSecret());
        assertEquals("PENDING", response.paymentStatus());
        verify(stripePaymentGateway).createPaymentIntent(any(CustomerOrder.class), any(PaymentTransaction.class), any(PaymentInitiationRequest.class));
    }

    @Test
    void duplicateSucceededCallbackIsHandledIdempotently() {
        CustomerOrder customerOrder = buildOrder(55L, OrderStatus.PAID);
        PaymentTransaction paymentTransaction = buildPayment(customerOrder);
        paymentTransaction.setPaymentStatus(PaymentStatus.SUCCEEDED);
        paymentTransaction.setProviderEventId("evt_demo_001");
        paymentTransaction.setPaidAt(LocalDateTime.of(2026, 3, 12, 15, 2));
        when(paymentTransactionRepository.findByTransactionRef("PAY-20260312150000000-1111"))
                .thenReturn(Optional.of(paymentTransaction));

        PaymentTransactionResponse response = paymentService.handleCallback(
                new PaymentCallbackRequest(
                        "PAY-20260312150000000-1111",
                        "SUCCEEDED",
                        "evt_demo_001",
                        "SIMULATED_GATEWAY",
                        "Duplicate callback"));

        assertEquals("SUCCEEDED", response.paymentStatus());
        verify(paymentTransactionRepository, never()).save(any(PaymentTransaction.class));
        verify(orderService, never()).updateOrderStatusForAdmin(
                eq(55L),
                eq(OrderStatus.PAID.name()),
                any(),
                any(),
                any());
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

    private PaymentTransaction buildPayment(CustomerOrder customerOrder) {
        PaymentTransaction paymentTransaction = new PaymentTransaction();
        paymentTransaction.setId(9L);
        paymentTransaction.setOrder(customerOrder);
        paymentTransaction.setPaymentMethod(PaymentMethod.CARD);
        paymentTransaction.setPaymentStatus(PaymentStatus.PENDING);
        paymentTransaction.setTransactionRef("PAY-20260312150000000-1111");
        paymentTransaction.setProviderCode("SIMULATED_GATEWAY");
        paymentTransaction.setAmount(BigDecimal.valueOf(129.99));
        paymentTransaction.setCreatedAt(LocalDateTime.of(2026, 3, 12, 15, 0));
        paymentTransaction.setUpdatedAt(LocalDateTime.of(2026, 3, 12, 15, 0));
        return paymentTransaction;
    }
}
