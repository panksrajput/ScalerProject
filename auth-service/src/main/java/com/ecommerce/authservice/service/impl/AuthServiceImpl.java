package com.ecommerce.authservice.service.impl;

import com.ecommerce.authservice.dto.event.NotificationEvent;
import com.ecommerce.authservice.dto.request.LoginRequest;
import com.ecommerce.authservice.dto.request.SignupRequest;
import com.ecommerce.authservice.dto.response.JwtResponse;
import com.ecommerce.authservice.entity.RefreshToken;
import com.ecommerce.authservice.entity.Role;
import com.ecommerce.authservice.entity.User;
import com.ecommerce.authservice.exception.EmailAlreadyExistsException;
import com.ecommerce.authservice.exception.TokenRefreshException;
import com.ecommerce.authservice.exception.UsernameAlreadyExistsException;
import com.ecommerce.authservice.repository.RoleRepository;
import com.ecommerce.authservice.repository.UserRepository;
import com.ecommerce.authservice.security.jwt.JwtUtils;
import com.ecommerce.authservice.security.services.UserDetailsImpl;
import com.ecommerce.authservice.service.AuthService;
import com.ecommerce.authservice.service.EventPublisherService;
import com.ecommerce.authservice.service.RefreshTokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class AuthServiceImpl implements AuthService {
    private static final Logger logger = LoggerFactory.getLogger(AuthServiceImpl.class);

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final RefreshTokenService refreshTokenService;
    private final EventPublisherService eventPublisherService;

    @Value("${app.base-url}")
    private String appBaseUrl;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Autowired
    public AuthServiceImpl(AuthenticationManager authenticationManager,
                      UserRepository userRepository,
                      RoleRepository roleRepository,
                      PasswordEncoder passwordEncoder,
                      JwtUtils jwtUtils,
                      RefreshTokenServiceImpl refreshTokenService,
                      EventPublisherService eventPublisherService) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtils = jwtUtils;
        this.refreshTokenService = refreshTokenService;
        this.eventPublisherService = eventPublisherService;
    }

    @Transactional
    public JwtResponse authenticateUser(LoginRequest loginRequest) {
        logger.debug("Starting authentication for: {}", loginRequest.getUsernameOrEmail());

        try {
            // Log authentication attempt
            if (loginRequest.getUsernameOrEmail().contains("@")) {
                logger.debug("Email-based authentication attempt");
            } else {
                logger.debug("Username-based authentication attempt");
            }

            Authentication authentication;
            try {
                authentication = authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(
                                loginRequest.getUsernameOrEmail().contains("@") ?
                                        userRepository.findByEmail(loginRequest.getUsernameOrEmail())
                                                .orElseThrow(() -> {
                                                    logger.warn("Authentication failed: User not found with email: {}", loginRequest.getUsernameOrEmail());
                                                    return new UsernameNotFoundException("Invalid credentials");
                                                })
                                                .getUsername() :
                                        loginRequest.getUsernameOrEmail(),
                                loginRequest.getPassword()
                        )
                );
            } catch (BadCredentialsException e) {
                logger.warn("Authentication failed: Invalid credentials for user: {}", loginRequest.getUsernameOrEmail());
                throw new BadCredentialsException("Invalid username/email or password");
            }

            SecurityContextHolder.getContext().setAuthentication(authentication);
            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

            // Check if account is enabled
            if (!userDetails.isEnabled()) {
                logger.warn("Authentication failed: Account is not enabled for user: {}", userDetails.getUsername());
                throw new DisabledException("Account is not enabled. Please verify your email first.");
            }

            logger.info("User authenticated successfully: {}", userDetails.getUsername());

            String jwt = jwtUtils.generateJwtToken(userDetails);
            logger.debug("JWT token generated for user: {}", userDetails.getUsername());

            // Revoke all existing refresh tokens for this user
            logger.debug("Revoking existing refresh tokens for user: {}", userDetails.getId());
            refreshTokenService.revokeByUserId(userDetails.getId());

            // Generate new refresh token
            RefreshToken refreshToken = refreshTokenService.createRefreshToken(userDetails.getId());
            logger.debug("New refresh token generated for user: {}", userDetails.getId());

            List<String> roles = userDetails.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toList());
            logger.debug("User roles retrieved: {}", roles);

            return new JwtResponse(
                    jwt,
                    refreshToken.getToken(),
                    userDetails.getId(),
                    userDetails.getUsername(),
                    userDetails.getEmail(),
                    userDetails.getFirstName(),
                    userDetails.getLastName(),
                    roles,
                    jwtUtils.getJwtExpirationMs(),
                    jwtUtils.getJwtRefreshExpirationMs()
            );
        } catch (BadCredentialsException | DisabledException e) {
            // Re-throw authentication-specific exceptions with their original messages
            logger.error("Authentication error: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            // Log unexpected errors
            logger.error("Unexpected error during authentication for user {}: {}",
                    loginRequest.getUsernameOrEmail(), e.getMessage(), e);
            throw new RuntimeException("Authentication failed due to an unexpected error", e);
        }
    }

    @Transactional
    public void registerUser(SignupRequest signUpRequest) {
        logger.info("Starting user registration for username: {}", signUpRequest.getUsername());

        // Validate username availability
        if (userRepository.existsByUsername(signUpRequest.getUsername())) {
            logger.warn("Registration failed: Username {} is already taken", signUpRequest.getUsername());
            throw new UsernameAlreadyExistsException("Username is already taken!");
        }

        // Validate email availability
        if (userRepository.existsByEmail(signUpRequest.getEmail())) {
            logger.warn("Registration failed: Email {} is already in use", signUpRequest.getEmail());
            throw new EmailAlreadyExistsException("Email is already in use!");
        }

        try {
            // Create new user's account
            logger.debug("Creating new user account for: {}", signUpRequest.getEmail());
            User user = new User();
            user.setUsername(signUpRequest.getUsername());
            user.setEmail(signUpRequest.getEmail());
            user.setPassword(passwordEncoder.encode(signUpRequest.getPassword()));
            user.setFirstName(signUpRequest.getFirstName());
            user.setLastName(signUpRequest.getLastName());
            user.setVerificationToken(UUID.randomUUID().toString());

            // Assign default role
            logger.debug("Assigning default USER role");
            Set<Role> roles = new HashSet<>();
            Role userRole = roleRepository.findByName(Role.ERole.ROLE_USER)
                    .orElseThrow(() -> {
                        logger.error("Default USER role not found in database");
                        return new RuntimeException("System configuration error: Default role not found");
                    });
            roles.add(userRole);
            user.setRoles(roles);

            // Save user
            user = userRepository.save(user);
            logger.info("User registered successfully with ID: {}", user.getId());

            // Publish notification event for email verification
            try {
                NotificationEvent event = NotificationEvent.builder()
                        .type("EMAIL_VERIFICATION")
                        .recipient(user.getEmail())
                        .data(Map.of(
                                "verificationToken", user.getVerificationToken(),
                                "appUrl", appBaseUrl
                        ))
                        .build();

                eventPublisherService.publishNotification(event);
                logger.debug("User registration notification sent for user: {}", user.getId());
            } catch (Exception e) {
                logger.error("Failed to send registration notification: {}", e.getMessage(), e);
                // Don't fail registration if notification fails
            }

        } catch (Exception e) {
            logger.error("Error during user registration: {}", e.getMessage(), e);
            throw e; // Re-throw to be handled by the controller
        }
    }

    @Transactional
    public void forgotPassword(String email) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("User not found with email: " + email));

        // Generate reset token
        String resetToken = UUID.randomUUID().toString();
        user.setResetPasswordToken(resetToken);
        userRepository.save(user);

        // Publish password reset email event
        NotificationEvent event = NotificationEvent.builder()
                .type("PASSWORD_RESET")
                .recipient(user.getEmail())
                .data(Map.of(
                        "verificationToken", resetToken,
                        "appUrl", frontendUrl
                ))
                .build();

        eventPublisherService.publishNotification(event);
        logger.info("Published password reset email event for user: {}", user.getEmail());
    }

    @Transactional
    public boolean verifyEmail(String verificationToken) {
        return userRepository.verify(verificationToken) > 0;
    }

    @Transactional
    public JwtResponse refreshToken(String requestRefreshToken) {
        return refreshTokenService.findByToken(requestRefreshToken)
                .map(refreshTokenService::verifyExpiration)
                .map(RefreshToken::getUser)
                .map(user -> {
                    UserDetailsImpl userDetails = UserDetailsImpl.build(user);
                    if (user == null) {
                        throw new TokenRefreshException(requestRefreshToken, "User not found for refresh token");
                    }

                    String token = jwtUtils.generateJwtToken(UserDetailsImpl.build(user));
                    RefreshToken newRefreshToken = refreshTokenService.createRefreshToken(user.getId());

                    // Get user roles as List
                    List<String> roles = user.getRoles().stream()
                            .map(role -> role.getName().name())
                            .collect(Collectors.toList());

                    return new JwtResponse(
                            token,
                            newRefreshToken.getToken(),
                            user.getId(),
                            user.getUsername(),
                            user.getEmail(),
                            user.getFirstName(),
                            user.getLastName(),
                            roles,
                            jwtUtils.getJwtExpirationMs(),
                            jwtUtils.getJwtRefreshExpirationMs()
                    );
                })
                .orElseThrow(() -> new TokenRefreshException(requestRefreshToken, "Refresh token is not in database!"));
    }

    @Transactional
    public boolean logoutUser(String refreshToken) {
        logger.debug("Attempting to logout with token: [HIDDEN]");

        // First, try to find the token
        logger.debug("Looking up refresh token in database");
        Optional<RefreshToken> tokenOpt = refreshTokenService.findByToken(refreshToken);

        if (tokenOpt.isEmpty()) {
            logger.warn("Refresh token not found in database");
            return false;
        }
        logger.debug("Refresh token found in database");

        RefreshToken token = tokenOpt.get();
        logger.debug("Found refresh token for user: {}",
            token.getUser() != null ? token.getUser().getUsername() : "[No User]");

        // Delete the refresh token using the provided token string
        logger.debug("Deleting refresh token");
        int deleted = refreshTokenService.deleteByToken(refreshToken);
        if (deleted == 0) {
            logger.error("Failed to delete refresh token from database");
            return false;
        }
        logger.debug("Successfully deleted refresh token");
        return true;
    }
}
