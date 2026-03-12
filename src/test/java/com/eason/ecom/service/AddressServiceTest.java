package com.eason.ecom.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
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

import com.eason.ecom.dto.CustomerAddressRequest;
import com.eason.ecom.dto.CustomerAddressResponse;
import com.eason.ecom.entity.CustomerAddress;
import com.eason.ecom.entity.UserAccount;
import com.eason.ecom.entity.UserRole;
import com.eason.ecom.repository.CustomerAddressRepository;
import com.eason.ecom.repository.UserAccountRepository;

@ExtendWith(MockitoExtension.class)
class AddressServiceTest {

    @Mock
    private CustomerAddressRepository customerAddressRepository;

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private AuditLogService auditLogService;

    private AddressService addressService;

    @BeforeEach
    void setUp() {
        addressService = new AddressService(customerAddressRepository, userAccountRepository, auditLogService);
    }

    @Test
    void firstAddressBecomesDefault() {
        UserAccount user = buildUser();
        when(userAccountRepository.findById(2L)).thenReturn(Optional.of(user));
        when(customerAddressRepository.countByUserId(2L)).thenReturn(0L);
        when(customerAddressRepository.save(any(CustomerAddress.class))).thenAnswer(invocation -> {
            CustomerAddress address = invocation.getArgument(0);
            address.setId(8L);
            address.setCreatedAt(LocalDateTime.of(2026, 3, 12, 16, 0));
            address.setUpdatedAt(LocalDateTime.of(2026, 3, 12, 16, 0));
            return address;
        });

        CustomerAddressResponse response = addressService.createAddress(
                2L,
                new CustomerAddressRequest(
                        "Home",
                        "Alex Morgan",
                        "+1-403-555-0199",
                        "1200 4 Ave SW",
                        "Unit 1806",
                        "Calgary",
                        "Alberta",
                        "T2P 2S6",
                        false));

        assertEquals(8L, response.id());
        assertEquals(true, response.isDefault());
        assertEquals("T2P 2S6", response.postalCode());
    }

    @Test
    void setDefaultAddressClearsPreviousDefault() {
        CustomerAddress existingDefault = buildAddress(7L, true);
        CustomerAddress nextAddress = buildAddress(8L, false);
        when(customerAddressRepository.findByIdAndUserId(8L, 2L)).thenReturn(Optional.of(nextAddress));
        when(customerAddressRepository.findByUserIdAndIsDefaultTrue(2L)).thenReturn(Optional.of(existingDefault));
        when(customerAddressRepository.save(any(CustomerAddress.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CustomerAddressResponse response = addressService.setDefaultAddress(2L, 8L);

        assertEquals(true, response.isDefault());
        assertEquals(false, existingDefault.getIsDefault());
        verify(customerAddressRepository).save(existingDefault);
        verify(customerAddressRepository).save(nextAddress);
    }

    private UserAccount buildUser() {
        UserAccount user = new UserAccount();
        user.setId(2L);
        user.setUsername("demo");
        user.setEmail("demo@ecom.local");
        user.setRole(UserRole.CUSTOMER);
        user.setPassword("encoded");
        user.setCreatedAt(LocalDateTime.of(2026, 3, 12, 12, 0));
        return user;
    }

    private CustomerAddress buildAddress(Long id, boolean isDefault) {
        CustomerAddress address = new CustomerAddress();
        address.setId(id);
        address.setUser(buildUser());
        address.setAddressLabel("Home");
        address.setReceiverName("Alex Morgan");
        address.setPhone("+1-403-555-0199");
        address.setLine1("1200 4 Ave SW");
        address.setLine2("Unit 1806");
        address.setCity("Calgary");
        address.setProvince("Alberta");
        address.setPostalCode("T2P 2S6");
        address.setIsDefault(isDefault);
        address.setCreatedAt(LocalDateTime.of(2026, 3, 12, 12, 30));
        address.setUpdatedAt(LocalDateTime.of(2026, 3, 12, 12, 30));
        return address;
    }
}
