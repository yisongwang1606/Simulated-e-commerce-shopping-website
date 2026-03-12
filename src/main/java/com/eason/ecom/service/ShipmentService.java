package com.eason.ecom.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.eason.ecom.dto.ShipmentCreateRequest;
import com.eason.ecom.dto.ShipmentResponse;
import com.eason.ecom.entity.CustomerOrder;
import com.eason.ecom.entity.OrderStatus;
import com.eason.ecom.entity.Shipment;
import com.eason.ecom.entity.ShipmentStatus;
import com.eason.ecom.exception.BadRequestException;
import com.eason.ecom.exception.ResourceNotFoundException;
import com.eason.ecom.repository.CustomerOrderRepository;
import com.eason.ecom.repository.ShipmentRepository;
import com.eason.ecom.support.ShipmentNumberGenerator;

@Service
public class ShipmentService {

    private final ShipmentRepository shipmentRepository;
    private final CustomerOrderRepository customerOrderRepository;
    private final OrderService orderService;
    private final AuditLogService auditLogService;
    private final ShipmentNumberGenerator shipmentNumberGenerator;

    public ShipmentService(
            ShipmentRepository shipmentRepository,
            CustomerOrderRepository customerOrderRepository,
            OrderService orderService,
            AuditLogService auditLogService,
            ShipmentNumberGenerator shipmentNumberGenerator) {
        this.shipmentRepository = shipmentRepository;
        this.customerOrderRepository = customerOrderRepository;
        this.orderService = orderService;
        this.auditLogService = auditLogService;
        this.shipmentNumberGenerator = shipmentNumberGenerator;
    }

    @Transactional
    public ShipmentResponse createShipment(
            Long orderId,
            ShipmentCreateRequest request,
            Long actorUserId,
            String actorUsername) {
        CustomerOrder customerOrder = customerOrderRepository.findWithDetailsById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        validateShipmentCreation(customerOrder);
        OrderStatus initialStatus = customerOrder.getStatus();

        if (initialStatus == OrderStatus.PAID) {
            orderService.updateOrderStatusForAdmin(
                    orderId,
                    OrderStatus.ALLOCATED.name(),
                    "Order allocated to warehouse fulfillment",
                    actorUserId,
                    actorUsername);
        }
        if (initialStatus == OrderStatus.PAID || initialStatus == OrderStatus.ALLOCATED) {
            orderService.updateOrderStatusForAdmin(
                    orderId,
                    OrderStatus.SHIPPED.name(),
                    "Shipment dispatched to carrier " + request.carrierCode().trim().toUpperCase(),
                    actorUserId,
                    actorUsername);
        }

        Shipment shipment = new Shipment();
        shipment.setOrder(customerOrder);
        shipment.setShipmentNo(shipmentNumberGenerator.next());
        shipment.setCarrierCode(request.carrierCode().trim().toUpperCase());
        shipment.setTrackingNo(request.trackingNo().trim().toUpperCase());
        shipment.setShipmentStatus(ShipmentStatus.IN_TRANSIT);
        shipment.setStatusNote(normalizeOptional(request.note()));
        shipment.setShippedAt(LocalDateTime.now());

        Shipment savedShipment = shipmentRepository.save(shipment);
        auditLogService.recordUserAction(
                actorUserId,
                actorUsername,
                "SHIPMENT_CREATED",
                "ORDER",
                customerOrder.getOrderNo(),
                "Shipment created for order " + customerOrder.getOrderNo(),
                Map.of(
                        "shipmentId", savedShipment.getId(),
                        "shipmentNo", savedShipment.getShipmentNo(),
                        "carrierCode", savedShipment.getCarrierCode(),
                        "trackingNo", savedShipment.getTrackingNo()));

        return toResponse(savedShipment);
    }

    @Transactional
    public ShipmentResponse markDelivered(Long shipmentId, String note, Long actorUserId, String actorUsername) {
        Shipment shipment = shipmentRepository.findWithDetailsById(shipmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Shipment not found"));

        if (shipment.getShipmentStatus() == ShipmentStatus.DELIVERED) {
            throw new BadRequestException("Shipment is already delivered");
        }
        if (shipment.getShipmentStatus() == ShipmentStatus.CANCELLED) {
            throw new BadRequestException("Cancelled shipment cannot be delivered");
        }

        shipment.setShipmentStatus(ShipmentStatus.DELIVERED);
        shipment.setDeliveredAt(LocalDateTime.now());
        shipment.setStatusNote(normalizeOptional(note));
        Shipment savedShipment = shipmentRepository.save(shipment);

        if (shipment.getOrder().getStatus() == OrderStatus.SHIPPED) {
            orderService.updateOrderStatusForAdmin(
                    shipment.getOrder().getId(),
                    OrderStatus.COMPLETED.name(),
                    "Shipment delivered to customer",
                    actorUserId,
                    actorUsername);
        }

        auditLogService.recordUserAction(
                actorUserId,
                actorUsername,
                "SHIPMENT_DELIVERED",
                "ORDER",
                shipment.getOrder().getOrderNo(),
                "Shipment delivered for order " + shipment.getOrder().getOrderNo(),
                Map.of(
                        "shipmentId", savedShipment.getId(),
                        "shipmentNo", savedShipment.getShipmentNo(),
                        "trackingNo", savedShipment.getTrackingNo()));

        return toResponse(savedShipment);
    }

    @Transactional(readOnly = true)
    public List<ShipmentResponse> getShipmentsForOrder(Long orderId) {
        return shipmentRepository.findByOrderIdOrderByCreatedAtDesc(orderId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ShipmentResponse> getShipmentsForCustomer(Long userId, Long orderId) {
        return shipmentRepository.findByOrderIdAndOrderUserIdOrderByCreatedAtDesc(orderId, userId).stream()
                .map(this::toResponse)
                .toList();
    }

    private void validateShipmentCreation(CustomerOrder customerOrder) {
        if (customerOrder.getStatus() != OrderStatus.PAID
                && customerOrder.getStatus() != OrderStatus.ALLOCATED) {
            throw new BadRequestException("Shipment cannot be created for the current order status");
        }
    }

    private String normalizeOptional(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private ShipmentResponse toResponse(Shipment shipment) {
        return new ShipmentResponse(
                shipment.getId(),
                shipment.getOrder().getId(),
                shipment.getOrder().getOrderNo(),
                shipment.getShipmentNo(),
                shipment.getCarrierCode(),
                shipment.getTrackingNo(),
                shipment.getShipmentStatus().name(),
                shipment.getStatusNote(),
                shipment.getCreatedAt(),
                shipment.getShippedAt(),
                shipment.getDeliveredAt(),
                shipment.getUpdatedAt());
    }
}
