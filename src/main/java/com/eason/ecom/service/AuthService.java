package com.eason.ecom.service;

import java.time.Duration;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.eason.ecom.config.AppProperties;
import com.eason.ecom.dto.AuthResponse;
import com.eason.ecom.dto.LoginRequest;
import com.eason.ecom.dto.RegisterRequest;
import com.eason.ecom.dto.UserProfileResponse;
import com.eason.ecom.entity.UserAccount;
import com.eason.ecom.entity.UserRole;
import com.eason.ecom.exception.BadRequestException;
import com.eason.ecom.exception.ResourceNotFoundException;
import com.eason.ecom.repository.UserAccountRepository;
import com.eason.ecom.security.AuthenticatedUser;
import com.eason.ecom.security.JwtService;
import com.eason.ecom.security.TokenStoreService;

@Service
public class AuthService {

    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final TokenStoreService tokenStoreService;
    private final AppProperties appProperties;

    public AuthService(
            UserAccountRepository userAccountRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            TokenStoreService tokenStoreService,
            AppProperties appProperties) {
        this.userAccountRepository = userAccountRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.tokenStoreService = tokenStoreService;
        this.appProperties = appProperties;
    }

    @Transactional
    public UserProfileResponse register(RegisterRequest request) {
        if (userAccountRepository.existsByUsername(request.username())) {
            throw new BadRequestException("Username already exists");
        }
        if (userAccountRepository.existsByEmail(request.email())) {
            throw new BadRequestException("Email already exists");
        }

        UserAccount userAccount = new UserAccount();
        userAccount.setUsername(request.username().trim());
        userAccount.setEmail(request.email().trim().toLowerCase());
        userAccount.setPassword(passwordEncoder.encode(request.password()));
        userAccount.setRole(UserRole.CUSTOMER);

        return toUserProfile(userAccountRepository.save(userAccount));
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        UserAccount userAccount = userAccountRepository.findByUsername(request.username())
                .or(() -> userAccountRepository.findByEmail(request.username().toLowerCase()))
                .orElseThrow(() -> new BadRequestException("Invalid credentials"));

        if (!passwordEncoder.matches(request.password(), userAccount.getPassword())) {
            throw new BadRequestException("Invalid credentials");
        }

        AuthenticatedUser authenticatedUser = new AuthenticatedUser(userAccount);
        String token = jwtService.generateToken(authenticatedUser);
        tokenStoreService.store(token, Duration.ofMillis(appProperties.getJwt().getExpiration()));

        return new AuthResponse(token, jwtService.extractExpiration(token), toUserProfile(userAccount));
    }

    public void logout(String authorizationHeader) {
        String token = extractBearerToken(authorizationHeader);
        tokenStoreService.revoke(token);
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getUserProfile(Long userId) {
        UserAccount userAccount = userAccountRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return toUserProfile(userAccount);
    }

    public UserProfileResponse toUserProfile(UserAccount userAccount) {
        return new UserProfileResponse(
                userAccount.getId(),
                userAccount.getUsername(),
                userAccount.getEmail(),
                userAccount.getRole().name(),
                userAccount.getCreatedAt());
    }

    private String extractBearerToken(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new BadRequestException("Missing Bearer token");
        }
        return authorizationHeader.substring(7).trim();
    }
}
