package com.eason.ecom.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import com.eason.ecom.dto.CustomerAddressResponse;
import com.eason.ecom.dto.OrderResponse;
import com.eason.ecom.dto.PagedResponse;
import com.eason.ecom.config.AppProperties;
import com.eason.ecom.entity.CustomerAddress;
import com.eason.ecom.entity.CustomerOrder;
import com.eason.ecom.entity.OrderStatus;
import com.eason.ecom.entity.Product;
import com.eason.ecom.entity.UserAccount;
import com.eason.ecom.entity.UserRole;
import com.eason.ecom.exception.BadRequestException;
import com.eason.ecom.repository.CustomerOrderRepository;
import com.eason.ecom.repository.ProductRepository;
import com.eason.ecom.repository.UserAccountRepository;
import com.eason.ecom.support.OrderNumberGenerator;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private CustomerOrderRepository customerOrderRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private CartService cartService;

    @Mock
    private ProductService productService;

    @Mock
    private AddressService addressService;

    @Mock
    private InventoryService inventoryService;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private OrderNumberGenerator orderNumberGenerator;

    private OrderService orderService;

    @BeforeEach
    void setUp() {
        AppProperties appProperties = new AppProperties();
        orderService = new OrderService(
                customerOrderRepository,
                productRepository,
                userAccountRepository,
                cartService,
                productService,
                addressService,
                inventoryService,
                auditLogService,
                appProperties,
                orderNumberGenerator);
    }

    @Test
    void createOrderRejectsEmptyCart() {
        when(cartService.getCartQuantities(7L)).thenReturn(Map.of());

        BadRequestException exception = assertThrows(BadRequestException.class, () -> orderService.createOrder(7L));

        assertEquals("Shopping cart is empty", exception.getMessage());
        verify(customerOrderRepository, never()).save(any(CustomerOrder.class));
    }

    @Test
    void createOrderRejectsMissingProducts() {
        when(cartService.getCartQuantities(7L)).thenReturn(Map.of(99L, 1));
        when(userAccountRepository.findById(7L)).thenReturn(Optional.of(buildUser(7L, "jack")));
        when(productRepository.findAllById(any())).thenReturn(List.of());

        BadRequestException exception = assertThrows(BadRequestException.class, () -> orderService.createOrder(7L));

        assertEquals("Product 99 no longer exists", exception.getMessage());
        verify(customerOrderRepository, never()).save(any(CustomerOrder.class));
        verify(cartService, never()).clearCart(7L);
    }

    @Test
    void createOrderPersistsOrderClearsCartAndEvictsCaches() {
        Map<Long, Integer> cart = new LinkedHashMap<>();
        cart.put(1L, 2);
        cart.put(2L, 1);

        Product first = buildProduct(1L, "Laptop Stand", BigDecimal.valueOf(25.00), 5);
        Product second = buildProduct(2L, "Mouse", BigDecimal.valueOf(15.50), 3);

        when(cartService.getCartQuantities(7L)).thenReturn(cart);
        when(userAccountRepository.findById(7L)).thenReturn(Optional.of(buildUser(7L, "jack")));
        when(productRepository.findAllById(cart.keySet())).thenReturn(List.of(first, second));
        when(orderNumberGenerator.next()).thenReturn("ORD-202603120001-1001");
        when(customerOrderRepository.save(any(CustomerOrder.class))).thenAnswer(invocation -> {
            CustomerOrder order = invocation.getArgument(0);
            order.setId(55L);
            order.setCreatedAt(LocalDateTime.of(2026, 3, 11, 11, 30));
            return order;
        });

        OrderResponse response = orderService.createOrder(7L);

        assertEquals(55L, response.id());
        assertEquals("ORD-202603120001-1001", response.orderNo());
        assertEquals(BigDecimal.valueOf(65.50).setScale(2), response.totalPrice().setScale(2));
        assertEquals("CREATED", response.status());
        assertEquals(2, response.items().size());
        assertEquals(3, first.getStock());
        assertEquals(2, second.getStock());
        verify(cartService).clearCart(7L);
        verify(inventoryService).recordOrderReservation(any(CustomerOrder.class));

        ArgumentCaptor<Iterable<Long>> productIdsCaptor = ArgumentCaptor.forClass(Iterable.class);
        verify(productService).evictProductCaches(productIdsCaptor.capture());
        assertEquals(List.of(1L, 2L), toList(productIdsCaptor.getValue()));
    }

    @Test
    void createOrderUsesExplicitAddressSnapshotWhenProvided() {
        Map<Long, Integer> cart = new LinkedHashMap<>();
        cart.put(1L, 1);
        Product product = buildProduct(1L, "Laptop Stand", BigDecimal.valueOf(25.00), 5);
        CustomerAddress address = buildAddress(9L);

        when(cartService.getCartQuantities(7L)).thenReturn(cart);
        when(userAccountRepository.findById(7L)).thenReturn(Optional.of(buildUser(7L, "jack")));
        when(productRepository.findAllById(cart.keySet())).thenReturn(List.of(product));
        when(addressService.getAddressEntityForUser(7L, 9L)).thenReturn(address);
        when(orderNumberGenerator.next()).thenReturn("ORD-202603120001-1001");
        when(customerOrderRepository.save(any(CustomerOrder.class))).thenAnswer(invocation -> {
            CustomerOrder order = invocation.getArgument(0);
            order.setId(55L);
            order.setCreatedAt(LocalDateTime.of(2026, 3, 11, 11, 30));
            return order;
        });

        OrderResponse response = orderService.createOrder(7L, 9L);

        assertEquals("Alex Morgan", response.shippingAddress().receiverName());
        assertEquals("T2P 2S6", response.shippingAddress().postalCode());
    }

    @Test
    void updateOrderStatusRejectsInvalidTransition() {
        CustomerOrder customerOrder = buildOrder(55L, OrderStatus.CREATED);
        when(customerOrderRepository.findWithDetailsById(55L)).thenReturn(Optional.of(customerOrder));

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> orderService.updateOrderStatusForAdmin(55L, "SHIPPED", "Skip warehouse", 1L, "admin"));

        assertEquals("Invalid order status transition from CREATED to SHIPPED", exception.getMessage());
    }

    @Test
    void cancelOrderReleasesInventoryAndStoresStatusNote() {
        CustomerOrder customerOrder = buildOrder(55L, OrderStatus.PAYMENT_PENDING);
        when(customerOrderRepository.findWithDetailsById(55L)).thenReturn(Optional.of(customerOrder));
        when(customerOrderRepository.save(any(CustomerOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderResponse response = orderService.updateOrderStatusForAdmin(55L, "CANCELLED", "Customer requested cancellation", 1L, "admin");

        assertEquals("CANCELLED", response.status());
        assertEquals("Customer requested cancellation", response.statusNote());
        verify(inventoryService).releaseOrderReservation(eq(customerOrder), eq(1L), eq("admin"), eq("Customer requested cancellation"));
        verify(auditLogService).recordUserAction(eq(1L), eq("admin"), eq("ORDER_STATUS_UPDATED"), eq("ORDER"), eq("ORD-202603120001-1001"), any(), any());
    }

    @Test
    void searchOrdersForAdminReturnsPagedResults() {
        CustomerOrder customerOrder = buildOrder(55L, OrderStatus.CREATED);
        when(customerOrderRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(customerOrder), PageRequest.of(0, 20), 1));

        PagedResponse<OrderResponse> response = orderService.searchOrdersForAdmin(
                "CREATED",
                "jack",
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 31),
                0,
                20);

        assertEquals(1, response.totalElements());
        assertEquals("ORD-202603120001-1001", response.items().getFirst().orderNo());
    }

    private UserAccount buildUser(Long id, String username) {
        UserAccount userAccount = new UserAccount();
        userAccount.setId(id);
        userAccount.setUsername(username);
        userAccount.setEmail(username + "@example.com");
        userAccount.setPassword("encoded");
        userAccount.setRole(UserRole.CUSTOMER);
        userAccount.setCreatedAt(LocalDateTime.of(2026, 3, 11, 8, 0));
        return userAccount;
    }

    private Product buildProduct(Long id, String name, BigDecimal price, int stock) {
        Product product = new Product();
        product.setId(id);
        product.setSku("SKU-" + id);
        product.setName(name);
        product.setBrand("NorthPeak Tech");
        product.setCategory("Electronics");
        product.setDescription(name + " description");
        product.setPrice(price);
        product.setCostPrice(price.multiply(BigDecimal.valueOf(0.6)).setScale(2));
        product.setStock(stock);
        product.setSafetyStock(2);
        product.setTaxClass(com.eason.ecom.entity.TaxClass.STANDARD);
        product.setStatus(com.eason.ecom.entity.ProductStatus.ACTIVE);
        product.setWeightKg(BigDecimal.valueOf(0.5));
        product.setLeadTimeDays(3);
        product.setFeatured(false);
        return product;
    }

    private CustomerOrder buildOrder(Long id, OrderStatus status) {
        CustomerOrder customerOrder = new CustomerOrder();
        customerOrder.setId(id);
        customerOrder.setOrderNo("ORD-202603120001-1001");
        customerOrder.setUser(buildUser(7L, "jack"));
        customerOrder.setStatus(status);
        customerOrder.setStatusUpdatedAt(LocalDateTime.of(2026, 3, 11, 11, 30));
        customerOrder.setCreatedAt(LocalDateTime.of(2026, 3, 11, 11, 0));
        customerOrder.setUpdatedAt(LocalDateTime.of(2026, 3, 11, 11, 0));

        Product product = buildProduct(1L, "Laptop Stand", BigDecimal.valueOf(25.00), 5);
        com.eason.ecom.entity.OrderItem item = new com.eason.ecom.entity.OrderItem();
        item.setProduct(product);
        item.setSkuSnapshot(product.getSku());
        item.setProductNameSnapshot(product.getName());
        item.setQuantity(2);
        item.setUnitPrice(product.getPrice());
        item.setLineTotal(product.getPrice().multiply(BigDecimal.valueOf(2)));
        customerOrder.addItem(item);

        customerOrder.setSubtotalAmount(BigDecimal.valueOf(50.00));
        customerOrder.setTaxAmount(BigDecimal.ZERO);
        customerOrder.setShippingAmount(BigDecimal.ZERO);
        customerOrder.setDiscountAmount(BigDecimal.ZERO);
        customerOrder.setTotalAmount(BigDecimal.valueOf(50.00));
        return customerOrder;
    }

    private CustomerAddress buildAddress(Long id) {
        CustomerAddress address = new CustomerAddress();
        address.setId(id);
        address.setUser(buildUser(7L, "jack"));
        address.setAddressLabel("Home");
        address.setReceiverName("Alex Morgan");
        address.setPhone("+1-403-555-0199");
        address.setLine1("1200 4 Ave SW");
        address.setLine2("Unit 1806");
        address.setCity("Calgary");
        address.setProvince("Alberta");
        address.setPostalCode("T2P 2S6");
        address.setIsDefault(true);
        return address;
    }

    private List<Long> toList(Iterable<Long> productIds) {
        java.util.ArrayList<Long> ids = new java.util.ArrayList<>();
        productIds.forEach(ids::add);
        return ids;
    }
}
