package com.eason.ecom.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.eason.ecom.config.AppProperties;
import com.eason.ecom.dto.AuthResponse;
import com.eason.ecom.dto.LoginRequest;
import com.eason.ecom.dto.RegisterRequest;
import com.eason.ecom.entity.UserAccount;
import com.eason.ecom.entity.UserRole;
import com.eason.ecom.exception.BadRequestException;
import com.eason.ecom.repository.UserAccountRepository;
import com.eason.ecom.security.JwtService;
import com.eason.ecom.security.TokenStoreService;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private TokenStoreService tokenStoreService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        AppProperties appProperties = new AppProperties();
        appProperties.getJwt().setExpiration(3_600_000);
        authService = new AuthService(
                userAccountRepository,
                passwordEncoder,
                jwtService,
                tokenStoreService,
                appProperties);
    }

    @Test
    void registerNormalizesUsernameAndEmailBeforeSaving() {
        when(userAccountRepository.existsByUsername("alice")).thenReturn(false);
        when(userAccountRepository.existsByEmail("alice@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Secret123!")).thenReturn("encoded-password");
        when(userAccountRepository.save(any(UserAccount.class))).thenAnswer(invocation -> {
            UserAccount saved = invocation.getArgument(0);
            saved.setId(7L);
            saved.setCreatedAt(LocalDateTime.of(2026, 3, 11, 10, 0));
            return saved;
        });

        var response = authService.register(new RegisterRequest(
                "  alice  ",
                " Alice@Example.com ",
                "Secret123!"));

        assertEquals("alice", response.username());
        assertEquals("alice@example.com", response.email());
        verify(userAccountRepository).existsByUsername("alice");
        verify(userAccountRepository).existsByEmail("alice@example.com");
    }

    @Test
    void registerRejectsDuplicateEmailAfterNormalization() {
        when(userAccountRepository.existsByUsername("alice")).thenReturn(false);
        when(userAccountRepository.existsByEmail("alice@example.com")).thenReturn(true);

        BadRequestException exception = assertThrows(BadRequestException.class, () -> authService.register(
                new RegisterRequest(" alice ", " Alice@Example.com ", "Secret123!")));

        assertEquals("Email already exists", exception.getMessage());
        verify(userAccountRepository, never()).save(any(UserAccount.class));
    }

    @Test
    void loginTrimsAndLowercasesEmailLookup() {
        UserAccount userAccount = buildUser(2L, "jack", "jack@example.com", "encoded-password", UserRole.CUSTOMER);
        Instant expiresAt = Instant.parse("2026-03-12T00:00:00Z");

        when(userAccountRepository.findByUsername("Jack@example.com")).thenReturn(Optional.empty());
        when(userAccountRepository.findByEmail("jack@example.com")).thenReturn(Optional.of(userAccount));
        when(passwordEncoder.matches("123456", "encoded-password")).thenReturn(true);
        when(jwtService.generateToken(any())).thenReturn("jwt-token");
        when(jwtService.extractExpiration("jwt-token")).thenReturn(expiresAt);

        AuthResponse response = authService.login(new LoginRequest("  Jack@example.com  ", "123456"));

        assertEquals("jwt-token", response.token());
        assertEquals("jack", response.user().username());
        verify(tokenStoreService).store("jwt-token", Duration.ofMillis(3_600_000));
    }

    @Test
    void logoutRejectsMissingBearerToken() {
        BadRequestException exception = assertThrows(BadRequestException.class, () -> authService.logout(null));

        assertTrue(exception.getMessage().contains("Missing Bearer token"));
    }

    private UserAccount buildUser(Long id, String username, String email, String password, UserRole role) {
        UserAccount userAccount = new UserAccount();
        userAccount.setId(id);
        userAccount.setUsername(username);
        userAccount.setEmail(email);
        userAccount.setPassword(password);
        userAccount.setRole(role);
        userAccount.setCreatedAt(LocalDateTime.of(2026, 3, 11, 9, 0));
        return userAccount;
    }
}
