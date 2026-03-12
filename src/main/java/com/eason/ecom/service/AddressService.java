package com.eason.ecom.service;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.eason.ecom.dto.CustomerAddressRequest;
import com.eason.ecom.dto.CustomerAddressResponse;
import com.eason.ecom.entity.CustomerAddress;
import com.eason.ecom.entity.UserAccount;
import com.eason.ecom.exception.ResourceNotFoundException;
import com.eason.ecom.repository.CustomerAddressRepository;
import com.eason.ecom.repository.UserAccountRepository;

@Service
public class AddressService {

    private final CustomerAddressRepository customerAddressRepository;
    private final UserAccountRepository userAccountRepository;
    private final AuditLogService auditLogService;

    public AddressService(
            CustomerAddressRepository customerAddressRepository,
            UserAccountRepository userAccountRepository,
            AuditLogService auditLogService) {
        this.customerAddressRepository = customerAddressRepository;
        this.userAccountRepository = userAccountRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public List<CustomerAddressResponse> getAddressesForUser(Long userId) {
        return customerAddressRepository.findByUserIdOrderByIsDefaultDescCreatedAtDesc(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public CustomerAddressResponse createAddress(Long userId, CustomerAddressRequest request) {
        UserAccount userAccount = loadUser(userId);
        CustomerAddress address = new CustomerAddress();
        address.setUser(userAccount);
        applyRequest(address, request);

        boolean shouldBeDefault = Boolean.TRUE.equals(request.isDefault())
                || customerAddressRepository.countByUserId(userId) == 0;
        if (shouldBeDefault) {
            clearDefaultAddress(userId);
        }
        address.setIsDefault(shouldBeDefault);

        CustomerAddress savedAddress = customerAddressRepository.save(address);
        auditLogService.recordUserAction(
                userId,
                userAccount.getUsername(),
                "ADDRESS_CREATED",
                "USER",
                String.valueOf(userId),
                "Customer address created",
                Map.of(
                        "addressId", savedAddress.getId(),
                        "label", savedAddress.getAddressLabel(),
                        "isDefault", savedAddress.getIsDefault()));
        return toResponse(savedAddress);
    }

    @Transactional
    public CustomerAddressResponse updateAddress(Long userId, Long addressId, CustomerAddressRequest request) {
        CustomerAddress address = customerAddressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found"));
        applyRequest(address, request);

        if (Boolean.TRUE.equals(request.isDefault())) {
            clearDefaultAddress(userId);
            address.setIsDefault(true);
        }

        CustomerAddress savedAddress = customerAddressRepository.save(address);
        auditLogService.recordUserAction(
                userId,
                address.getUser().getUsername(),
                "ADDRESS_UPDATED",
                "USER",
                String.valueOf(userId),
                "Customer address updated",
                Map.of(
                        "addressId", savedAddress.getId(),
                        "label", savedAddress.getAddressLabel(),
                        "isDefault", savedAddress.getIsDefault()));
        return toResponse(savedAddress);
    }

    @Transactional
    public CustomerAddressResponse setDefaultAddress(Long userId, Long addressId) {
        CustomerAddress address = customerAddressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found"));
        clearDefaultAddress(userId);
        address.setIsDefault(true);
        CustomerAddress savedAddress = customerAddressRepository.save(address);
        auditLogService.recordUserAction(
                userId,
                address.getUser().getUsername(),
                "ADDRESS_DEFAULT_SET",
                "USER",
                String.valueOf(userId),
                "Default shipping address updated",
                Map.of(
                        "addressId", savedAddress.getId(),
                        "label", savedAddress.getAddressLabel()));
        return toResponse(savedAddress);
    }

    @Transactional
    public void deleteAddress(Long userId, Long addressId) {
        CustomerAddress address = customerAddressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found"));
        boolean wasDefault = Boolean.TRUE.equals(address.getIsDefault());
        customerAddressRepository.delete(address);
        if (wasDefault) {
            customerAddressRepository.findByUserIdOrderByIsDefaultDescCreatedAtDesc(userId).stream()
                    .findFirst()
                    .ifPresent(nextAddress -> {
                        nextAddress.setIsDefault(true);
                        customerAddressRepository.save(nextAddress);
                    });
        }
        auditLogService.recordUserAction(
                userId,
                address.getUser().getUsername(),
                "ADDRESS_DELETED",
                "USER",
                String.valueOf(userId),
                "Customer address deleted",
                Map.of(
                        "addressId", addressId,
                        "label", address.getAddressLabel()));
    }

    @Transactional(readOnly = true)
    public CustomerAddress getDefaultAddressEntity(Long userId) {
        return customerAddressRepository.findByUserIdAndIsDefaultTrue(userId).orElse(null);
    }

    @Transactional(readOnly = true)
    public CustomerAddress getAddressEntityForUser(Long userId, Long addressId) {
        return customerAddressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found"));
    }

    private UserAccount loadUser(Long userId) {
        return userAccountRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private void clearDefaultAddress(Long userId) {
        customerAddressRepository.findByUserIdAndIsDefaultTrue(userId)
                .ifPresent(existingDefault -> {
                    existingDefault.setIsDefault(false);
                    customerAddressRepository.save(existingDefault);
                });
    }

    private void applyRequest(CustomerAddress address, CustomerAddressRequest request) {
        address.setAddressLabel(request.addressLabel().trim());
        address.setReceiverName(request.receiverName().trim());
        address.setPhone(request.phone().trim());
        address.setLine1(request.line1().trim());
        address.setLine2(normalizeOptional(request.line2()));
        address.setCity(request.city().trim());
        address.setProvince(request.province().trim());
        address.setPostalCode(request.postalCode().trim().toUpperCase());
    }

    private CustomerAddressResponse toResponse(CustomerAddress address) {
        return new CustomerAddressResponse(
                address.getId(),
                address.getAddressLabel(),
                address.getReceiverName(),
                address.getPhone(),
                address.getLine1(),
                address.getLine2(),
                address.getCity(),
                address.getProvince(),
                address.getPostalCode(),
                Boolean.TRUE.equals(address.getIsDefault()),
                address.getCreatedAt(),
                address.getUpdatedAt());
    }

    private String normalizeOptional(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
