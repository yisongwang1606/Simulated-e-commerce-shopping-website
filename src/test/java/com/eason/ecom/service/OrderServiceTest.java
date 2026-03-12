package com.eason.ecom.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
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

import com.eason.ecom.dto.OrderResponse;
import com.eason.ecom.entity.CustomerOrder;
import com.eason.ecom.entity.Product;
import com.eason.ecom.entity.UserAccount;
import com.eason.ecom.entity.UserRole;
import com.eason.ecom.exception.BadRequestException;
import com.eason.ecom.repository.CustomerOrderRepository;
import com.eason.ecom.repository.ProductRepository;
import com.eason.ecom.repository.UserAccountRepository;

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

    private OrderService orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderService(
                customerOrderRepository,
                productRepository,
                userAccountRepository,
                cartService,
                productService);
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
        when(customerOrderRepository.save(any(CustomerOrder.class))).thenAnswer(invocation -> {
            CustomerOrder order = invocation.getArgument(0);
            order.setId(55L);
            order.setCreatedAt(LocalDateTime.of(2026, 3, 11, 11, 30));
            return order;
        });

        OrderResponse response = orderService.createOrder(7L);

        assertEquals(55L, response.id());
        assertEquals(BigDecimal.valueOf(65.50).setScale(2), response.totalPrice().setScale(2));
        assertEquals("CREATED", response.status());
        assertEquals(2, response.items().size());
        assertEquals(3, first.getStock());
        assertEquals(2, second.getStock());
        verify(cartService).clearCart(7L);

        ArgumentCaptor<Iterable<Long>> productIdsCaptor = ArgumentCaptor.forClass(Iterable.class);
        verify(productService).evictProductCaches(productIdsCaptor.capture());
        assertEquals(List.of(1L, 2L), toList(productIdsCaptor.getValue()));
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
        product.setName(name);
        product.setCategory("Electronics");
        product.setDescription(name + " description");
        product.setPrice(price);
        product.setStock(stock);
        return product;
    }

    private List<Long> toList(Iterable<Long> productIds) {
        java.util.ArrayList<Long> ids = new java.util.ArrayList<>();
        productIds.forEach(ids::add);
        return ids;
    }
}
