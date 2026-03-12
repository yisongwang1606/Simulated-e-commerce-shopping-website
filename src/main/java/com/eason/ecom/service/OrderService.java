package com.eason.ecom.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.eason.ecom.dto.OrderItemResponse;
import com.eason.ecom.dto.OrderResponse;
import com.eason.ecom.entity.CustomerOrder;
import com.eason.ecom.entity.OrderItem;
import com.eason.ecom.entity.OrderStatus;
import com.eason.ecom.entity.Product;
import com.eason.ecom.entity.UserAccount;
import com.eason.ecom.exception.BadRequestException;
import com.eason.ecom.exception.ResourceNotFoundException;
import com.eason.ecom.repository.CustomerOrderRepository;
import com.eason.ecom.repository.ProductRepository;
import com.eason.ecom.repository.UserAccountRepository;

@Service
public class OrderService {

    private final CustomerOrderRepository customerOrderRepository;
    private final ProductRepository productRepository;
    private final UserAccountRepository userAccountRepository;
    private final CartService cartService;
    private final ProductService productService;

    public OrderService(
            CustomerOrderRepository customerOrderRepository,
            ProductRepository productRepository,
            UserAccountRepository userAccountRepository,
            CartService cartService,
            ProductService productService) {
        this.customerOrderRepository = customerOrderRepository;
        this.productRepository = productRepository;
        this.userAccountRepository = userAccountRepository;
        this.cartService = cartService;
        this.productService = productService;
    }

    @Transactional
    public OrderResponse createOrder(Long userId) {
        Map<Long, Integer> cartQuantities = cartService.getCartQuantities(userId);
        if (cartQuantities.isEmpty()) {
            throw new BadRequestException("Shopping cart is empty");
        }

        UserAccount userAccount = userAccountRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Map<Long, Product> products = productRepository.findAllById(cartQuantities.keySet()).stream()
                .collect(java.util.stream.Collectors.toMap(Product::getId, product -> product));

        CustomerOrder customerOrder = new CustomerOrder();
        customerOrder.setUser(userAccount);
        customerOrder.setStatus(OrderStatus.CREATED);

        BigDecimal totalPrice = BigDecimal.ZERO;
        List<Long> touchedProductIds = new ArrayList<>();

        for (Map.Entry<Long, Integer> entry : cartQuantities.entrySet()) {
            Product product = products.get(entry.getKey());
            if (product == null) {
                throw new BadRequestException("Product " + entry.getKey() + " no longer exists");
            }
            int quantity = entry.getValue();
            if (quantity > product.getStock()) {
                throw new BadRequestException("Insufficient stock for product " + product.getName());
            }

            product.setStock(product.getStock() - quantity);

            OrderItem orderItem = new OrderItem();
            orderItem.setProduct(product);
            orderItem.setQuantity(quantity);
            orderItem.setPrice(product.getPrice());
            customerOrder.addItem(orderItem);

            totalPrice = totalPrice.add(product.getPrice().multiply(BigDecimal.valueOf(quantity)));
            touchedProductIds.add(product.getId());
        }

        customerOrder.setTotalPrice(totalPrice);

        CustomerOrder savedOrder = customerOrderRepository.save(customerOrder);
        cartService.clearCart(userId);
        productService.evictProductCaches(touchedProductIds);

        return toOrderResponse(savedOrder);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersForUser(Long userId) {
        return customerOrderRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toOrderResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderForUser(Long userId, Long orderId) {
        CustomerOrder customerOrder = customerOrderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        return toOrderResponse(customerOrder);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getAllOrders() {
        return customerOrderRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toOrderResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderForAdmin(Long orderId) {
        CustomerOrder customerOrder = customerOrderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        return toOrderResponse(customerOrder);
    }

    private OrderResponse toOrderResponse(CustomerOrder customerOrder) {
        List<OrderItemResponse> items = customerOrder.getItems().stream()
                .map(item -> new OrderItemResponse(
                        item.getProduct().getId(),
                        item.getProduct().getName(),
                        item.getQuantity(),
                        item.getPrice(),
                        item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()))))
                .toList();

        return new OrderResponse(
                customerOrder.getId(),
                customerOrder.getUser().getId(),
                customerOrder.getUser().getUsername(),
                customerOrder.getTotalPrice(),
                customerOrder.getStatus().name(),
                customerOrder.getCreatedAt(),
                items);
    }
}
