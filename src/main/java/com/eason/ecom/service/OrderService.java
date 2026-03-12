package com.eason.ecom.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.eason.ecom.config.AppProperties;
import com.eason.ecom.dto.OrderItemResponse;
import com.eason.ecom.dto.OrderAddressSnapshotResponse;
import com.eason.ecom.dto.PagedResponse;
import com.eason.ecom.dto.OrderResponse;
import com.eason.ecom.entity.CustomerOrder;
import com.eason.ecom.entity.CustomerAddress;
import com.eason.ecom.entity.OrderItem;
import com.eason.ecom.entity.OrderStatus;
import com.eason.ecom.entity.Product;
import com.eason.ecom.entity.UserAccount;
import com.eason.ecom.exception.BadRequestException;
import com.eason.ecom.exception.ResourceNotFoundException;
import com.eason.ecom.repository.CustomerOrderRepository;
import com.eason.ecom.repository.ProductRepository;
import com.eason.ecom.repository.UserAccountRepository;
import com.eason.ecom.support.OrderNumberGenerator;
import jakarta.persistence.criteria.Predicate;

@Service
public class OrderService {

    private final CustomerOrderRepository customerOrderRepository;
    private final ProductRepository productRepository;
    private final UserAccountRepository userAccountRepository;
    private final CartService cartService;
    private final ProductService productService;
    private final AddressService addressService;
    private final InventoryService inventoryService;
    private final AuditLogService auditLogService;
    private final AppProperties appProperties;
    private final OrderNumberGenerator orderNumberGenerator;

    private static final Map<OrderStatus, Set<OrderStatus>> ALLOWED_TRANSITIONS = buildAllowedTransitions();

    public OrderService(
            CustomerOrderRepository customerOrderRepository,
            ProductRepository productRepository,
            UserAccountRepository userAccountRepository,
            CartService cartService,
            ProductService productService,
            AddressService addressService,
            InventoryService inventoryService,
            AuditLogService auditLogService,
            AppProperties appProperties,
            OrderNumberGenerator orderNumberGenerator) {
        this.customerOrderRepository = customerOrderRepository;
        this.productRepository = productRepository;
        this.userAccountRepository = userAccountRepository;
        this.cartService = cartService;
        this.productService = productService;
        this.addressService = addressService;
        this.inventoryService = inventoryService;
        this.auditLogService = auditLogService;
        this.appProperties = appProperties;
        this.orderNumberGenerator = orderNumberGenerator;
    }

    @Transactional
    public OrderResponse createOrder(Long userId) {
        return createOrder(userId, null);
    }

    @Transactional
    public OrderResponse createOrder(Long userId, Long addressId) {
        Map<Long, Integer> cartQuantities = cartService.getCartQuantities(userId);
        if (cartQuantities.isEmpty()) {
            throw new BadRequestException("Shopping cart is empty");
        }

        UserAccount userAccount = userAccountRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Map<Long, Product> products = productRepository.findAllById(cartQuantities.keySet()).stream()
                .collect(java.util.stream.Collectors.toMap(Product::getId, product -> product));

        CustomerOrder customerOrder = new CustomerOrder();
        customerOrder.setOrderNo(orderNumberGenerator.next());
        customerOrder.setUser(userAccount);
        customerOrder.setStatus(OrderStatus.CREATED);
        customerOrder.setStatusNote("Order submitted by customer");
        applyShippingAddress(customerOrder, userId, addressId);

        BigDecimal subtotalAmount = BigDecimal.ZERO;
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
            BigDecimal lineTotal = product.getPrice().multiply(BigDecimal.valueOf(quantity));

            OrderItem orderItem = new OrderItem();
            orderItem.setProduct(product);
            orderItem.setSkuSnapshot(product.getSku());
            orderItem.setProductNameSnapshot(product.getName());
            orderItem.setQuantity(quantity);
            orderItem.setUnitPrice(product.getPrice());
            orderItem.setLineTotal(lineTotal);
            customerOrder.addItem(orderItem);

            subtotalAmount = subtotalAmount.add(lineTotal);
            touchedProductIds.add(product.getId());
        }

        BigDecimal taxAmount = subtotalAmount
                .multiply(appProperties.getOrder().getDefaultTaxRate())
                .setScale(2, java.math.RoundingMode.HALF_UP);
        BigDecimal shippingAmount = appProperties.getOrder().getDefaultShippingFee();
        BigDecimal discountAmount = BigDecimal.ZERO.setScale(2, java.math.RoundingMode.HALF_UP);
        BigDecimal totalAmount = subtotalAmount.add(taxAmount).add(shippingAmount).subtract(discountAmount);

        customerOrder.setSubtotalAmount(subtotalAmount);
        customerOrder.setTaxAmount(taxAmount);
        customerOrder.setShippingAmount(shippingAmount);
        customerOrder.setDiscountAmount(discountAmount);
        customerOrder.setTotalAmount(totalAmount);
        customerOrder.setStatusUpdatedAt(LocalDateTime.now());

        CustomerOrder savedOrder = customerOrderRepository.save(customerOrder);
        inventoryService.recordOrderReservation(savedOrder);
        auditLogService.recordUserAction(
                userAccount.getId(),
                userAccount.getUsername(),
                "ORDER_CREATED",
                "ORDER",
                savedOrder.getOrderNo(),
                "Customer created order " + savedOrder.getOrderNo(),
                Map.of(
                        "orderId", savedOrder.getId(),
                        "status", savedOrder.getStatus().name(),
                        "itemCount", savedOrder.getItems().size(),
                        "totalAmount", savedOrder.getTotalAmount()));
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
    public PagedResponse<OrderResponse> searchOrdersForAdmin(
            String status,
            String customer,
            LocalDate dateFrom,
            LocalDate dateTo,
            int page,
            int size) {
        Page<CustomerOrder> orderPage = customerOrderRepository.findAll(
                buildAdminSearchSpecification(status, customer, dateFrom, dateTo),
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));

        return new PagedResponse<>(
                orderPage.getContent().stream().map(this::toOrderResponse).toList(),
                orderPage.getTotalElements(),
                orderPage.getTotalPages(),
                orderPage.getNumber(),
                orderPage.getSize());
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderForAdmin(Long orderId) {
        CustomerOrder customerOrder = customerOrderRepository.findWithDetailsById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        return toOrderResponse(customerOrder);
    }

    @Transactional
    public OrderResponse updateOrderStatusForAdmin(
            Long orderId,
            String requestedStatus,
            String note,
            Long actorUserId,
            String actorUsername) {
        CustomerOrder customerOrder = customerOrderRepository.findWithDetailsById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        OrderStatus targetStatus = resolveOrderStatus(requestedStatus);
        OrderStatus currentStatus = customerOrder.getStatus();

        if (currentStatus == targetStatus) {
            throw new BadRequestException("Order is already in status " + targetStatus.name());
        }
        if (!ALLOWED_TRANSITIONS.getOrDefault(currentStatus, Set.of()).contains(targetStatus)) {
            throw new BadRequestException(
                    "Invalid order status transition from " + currentStatus.name() + " to " + targetStatus.name());
        }

        String normalizedNote = normalizeNote(note);
        if (shouldReleaseInventory(currentStatus, targetStatus)) {
            inventoryService.releaseOrderReservation(customerOrder, actorUserId, actorUsername, normalizedNote);
            productService.evictProductCaches(customerOrder.getItems().stream()
                    .map(item -> item.getProduct().getId())
                    .toList());
        }

        customerOrder.setStatus(targetStatus);
        customerOrder.setStatusNote(normalizedNote);
        customerOrder.setStatusUpdatedAt(LocalDateTime.now());
        CustomerOrder savedOrder = customerOrderRepository.save(customerOrder);

        auditLogService.recordUserAction(
                actorUserId,
                actorUsername,
                "ORDER_STATUS_UPDATED",
                "ORDER",
                savedOrder.getOrderNo(),
                "Order " + savedOrder.getOrderNo() + " moved to " + targetStatus.name(),
                Map.of(
                        "orderId", savedOrder.getId(),
                        "fromStatus", currentStatus.name(),
                        "toStatus", targetStatus.name(),
                        "note", normalizedNote == null ? "" : normalizedNote));

        return toOrderResponse(savedOrder);
    }

    private OrderResponse toOrderResponse(CustomerOrder customerOrder) {
        List<OrderItemResponse> items = customerOrder.getItems().stream()
                .map(item -> new OrderItemResponse(
                        item.getProduct().getId(),
                        item.getSkuSnapshot(),
                        item.getProductNameSnapshot(),
                        item.getQuantity(),
                        item.getUnitPrice(),
                        item.getLineTotal()))
                .toList();

        return new OrderResponse(
                customerOrder.getId(),
                customerOrder.getOrderNo(),
                customerOrder.getUser().getId(),
                customerOrder.getUser().getUsername(),
                customerOrder.getSubtotalAmount(),
                customerOrder.getTaxAmount(),
                customerOrder.getShippingAmount(),
                customerOrder.getDiscountAmount(),
                customerOrder.getTotalAmount(),
                toAddressSnapshot(customerOrder),
                customerOrder.getStatus().name(),
                customerOrder.getStatusNote(),
                customerOrder.getCreatedAt(),
                customerOrder.getStatusUpdatedAt(),
                customerOrder.getUpdatedAt(),
                items);
    }

    private void applyShippingAddress(CustomerOrder customerOrder, Long userId, Long addressId) {
        CustomerAddress selectedAddress;
        if (addressId != null) {
            selectedAddress = addressService.getAddressEntityForUser(userId, addressId);
        } else {
            selectedAddress = addressService.getDefaultAddressEntity(userId);
        }
        if (selectedAddress == null) {
            return;
        }
        customerOrder.setShippingReceiverName(selectedAddress.getReceiverName());
        customerOrder.setShippingPhone(selectedAddress.getPhone());
        customerOrder.setShippingLine1(selectedAddress.getLine1());
        customerOrder.setShippingLine2(selectedAddress.getLine2());
        customerOrder.setShippingCity(selectedAddress.getCity());
        customerOrder.setShippingProvince(selectedAddress.getProvince());
        customerOrder.setShippingPostalCode(selectedAddress.getPostalCode());
    }

    private OrderAddressSnapshotResponse toAddressSnapshot(CustomerOrder customerOrder) {
        if (customerOrder.getShippingReceiverName() == null && customerOrder.getShippingLine1() == null) {
            return null;
        }
        return new OrderAddressSnapshotResponse(
                customerOrder.getShippingReceiverName(),
                customerOrder.getShippingPhone(),
                customerOrder.getShippingLine1(),
                customerOrder.getShippingLine2(),
                customerOrder.getShippingCity(),
                customerOrder.getShippingProvince(),
                customerOrder.getShippingPostalCode());
    }

    private OrderStatus resolveOrderStatus(String requestedStatus) {
        try {
            return OrderStatus.valueOf(requestedStatus.trim().toUpperCase());
        } catch (Exception exception) {
            throw new BadRequestException("Unsupported order status: " + requestedStatus);
        }
    }

    private String normalizeNote(String note) {
        if (note == null) {
            return null;
        }
        String trimmed = note.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private boolean shouldReleaseInventory(OrderStatus currentStatus, OrderStatus targetStatus) {
        if (targetStatus != OrderStatus.CANCELLED) {
            return false;
        }
        return currentStatus != OrderStatus.SHIPPED
                && currentStatus != OrderStatus.COMPLETED
                && currentStatus != OrderStatus.REFUND_PENDING
                && currentStatus != OrderStatus.REFUNDED;
    }

    private static Map<OrderStatus, Set<OrderStatus>> buildAllowedTransitions() {
        Map<OrderStatus, Set<OrderStatus>> transitions = new EnumMap<>(OrderStatus.class);
        transitions.put(OrderStatus.CREATED, Set.of(OrderStatus.PAYMENT_PENDING, OrderStatus.CANCELLED));
        transitions.put(OrderStatus.PAYMENT_PENDING, Set.of(OrderStatus.PAID, OrderStatus.CANCELLED));
        transitions.put(OrderStatus.PAID, Set.of(OrderStatus.ALLOCATED, OrderStatus.REFUND_PENDING, OrderStatus.CANCELLED));
        transitions.put(OrderStatus.ALLOCATED, Set.of(OrderStatus.SHIPPED, OrderStatus.CANCELLED));
        transitions.put(OrderStatus.SHIPPED, Set.of(OrderStatus.COMPLETED, OrderStatus.REFUND_PENDING));
        transitions.put(OrderStatus.COMPLETED, Set.of(OrderStatus.REFUND_PENDING));
        transitions.put(OrderStatus.REFUND_PENDING, Set.of(OrderStatus.REFUNDED));
        transitions.put(OrderStatus.CANCELLED, Set.of());
        transitions.put(OrderStatus.REFUNDED, Set.of());
        return transitions;
    }

    private Specification<CustomerOrder> buildAdminSearchSpecification(
            String status,
            String customer,
            LocalDate dateFrom,
            LocalDate dateTo) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (StringUtils.hasText(status)) {
                OrderStatus orderStatus = resolveOrderStatus(status);
                predicates.add(criteriaBuilder.equal(root.get("status"), orderStatus));
            }
            if (StringUtils.hasText(customer)) {
                String keyword = "%" + customer.trim().toLowerCase() + "%";
                var userJoin = root.join("user");
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("orderNo")), keyword),
                        criteriaBuilder.like(criteriaBuilder.lower(userJoin.get("username")), keyword),
                        criteriaBuilder.like(criteriaBuilder.lower(userJoin.get("email")), keyword)));
            }
            if (dateFrom != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                        root.get("createdAt"),
                        dateFrom.atStartOfDay()));
            }
            if (dateTo != null) {
                predicates.add(criteriaBuilder.lessThan(
                        root.get("createdAt"),
                        dateTo.plusDays(1).atStartOfDay()));
            }
            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }
}
