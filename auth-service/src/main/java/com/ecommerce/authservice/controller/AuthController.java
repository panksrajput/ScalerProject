package com.ecommerce.authservice.controller;

import com.ecommerce.authservice.dto.request.LoginRequest;
import com.ecommerce.authservice.dto.request.SignupRequest;
import com.ecommerce.authservice.dto.request.TokenRefreshRequest;
import com.ecommerce.authservice.dto.response.JwtResponse;
import com.ecommerce.authservice.dto.response.MessageResponse;
import com.ecommerce.authservice.service.AuthService;
import com.ecommerce.authservice.service.impl.RefreshTokenServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import com.ecommerce.authservice.entity.User;
import com.ecommerce.authservice.entity.RefreshToken;
import com.ecommerce.authservice.repository.UserRepository;
import com.ecommerce.authservice.repository.RoleRepository;

import java.util.Optional;

import javax.validation.Valid;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);
    
    private final AuthService authService;
    private final RefreshTokenServiceImpl refreshTokenService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public AuthController(AuthService authService,
                          RefreshTokenServiceImpl refreshTokenService,
                         UserRepository userRepository,
                         RoleRepository roleRepository,
                         PasswordEncoder passwordEncoder) {
        this.authService = authService;
        this.refreshTokenService = refreshTokenService;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/signup")
    public ResponseEntity<?> registerUser(@Valid @RequestBody SignupRequest signUpRequest) {
        logger.info("Received signup request for username: {}", signUpRequest.getUsername());
        try {
            authService.registerUser(signUpRequest);
            logger.info("User registration successful for username: {}", signUpRequest.getUsername());
            return ResponseEntity.ok(new MessageResponse("User registered successfully! Please check your email to verify your account."));
        } catch (Exception e) {
            logger.error("Error during user registration for username: {} - {}", signUpRequest.getUsername(), e.getMessage(), e);
            return ResponseEntity
                    .badRequest()
                    .body(new MessageResponse("Error: " + e.getMessage()));
        }
    }
    
    @PostMapping("/signin")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        logger.info("Authentication attempt for user: {}", loginRequest.getUsernameOrEmail());
        try {
            JwtResponse jwtResponse = authService.authenticateUser(loginRequest);
            logger.info("Authentication successful for user: {}", loginRequest.getUsernameOrEmail());
            return ResponseEntity.ok(jwtResponse);
        } catch (Exception e) {
            logger.error("Authentication failed for user: {} - {}", loginRequest.getUsernameOrEmail(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new MessageResponse("Error: " + e.getMessage()));
        }
    }

    @PostMapping("/refreshtoken")
    public ResponseEntity<?> refreshtoken(@Valid @RequestBody TokenRefreshRequest request) {
        logger.debug("Received token refresh request");
        String requestRefreshToken = request.getRefreshToken();
        try {
            JwtResponse response = authService.refreshToken(requestRefreshToken);
            logger.info("Token refresh successful");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Token refresh failed: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new MessageResponse("Error refreshing token: " + e.getMessage()));
        }
    }

    @PostMapping("/signout")
    public ResponseEntity<?> logoutUser(@RequestHeader("Authorization") String authHeader) {
        logger.info("Logout request received. Authorization header: {}",
            authHeader != null ? "Bearer [token]" : "null");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            logger.warn("Invalid Authorization header format. Must be 'Bearer [token]'");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new MessageResponse("Invalid Authorization header format. Must be 'Bearer [token]'"));
        }

        try {
            String token = authHeader.substring(7).trim();
            logger.debug("Processing logout for token: [HIDDEN]");

            boolean loggedOut = authService.logoutUser(token);

            if (loggedOut) {
                logger.info("Logout successful");
                return ResponseEntity.ok(new MessageResponse("Log out successful!"));
            } else {
                // Check if token exists but logout failed for another reason
                Optional<RefreshToken> tokenOpt = refreshTokenService.findByToken(token);
                if (tokenOpt.isPresent()) {
                    logger.warn("Logout failed for existing token");
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(new MessageResponse("Logout failed due to server error"));
                } else {
                    logger.warn("Invalid or expired refresh token");
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new MessageResponse("Invalid or expired refresh token"));
                }
            }
        } catch (Exception e) {
            logger.error("Error during logout: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new MessageResponse("An error occurred during logout: " + e.getMessage()));
        }
    }
    
    @GetMapping("/verify-email")
    public ResponseEntity<?> verifyEmail(@RequestParam String token) {
        try {
            // Find user by verification token
            User user = userRepository.findByVerificationToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid verification token"));

            // Mark user as verified
            user.setEmailVerified(true);
            user.setVerificationToken(null);
            userRepository.save(user);

            return ResponseEntity.ok(new MessageResponse("Email verified successfully!"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(new MessageResponse("Error verifying email: " + e.getMessage()));
        }
    }
    
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestParam String email) {
        try {
            authService.forgotPassword(email);
            return ResponseEntity.ok(new MessageResponse("Password reset link sent to your email"));
        } catch (Exception e) {
            logger.error("Error processing password reset request for email: {}", email, e);
            return ResponseEntity.badRequest()
                .body(new MessageResponse("Error processing password reset request: " + e.getMessage()));
        }
    }
    
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(
            @RequestParam String token,
            @RequestParam String newPassword) {
        try {
            // Find user by reset token
            User user = userRepository.findByResetPasswordToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid or expired reset token"));

            // Update password
            user.setPassword(passwordEncoder.encode(newPassword));
            user.setResetPasswordToken(null);
            userRepository.save(user);

            return ResponseEntity.ok(new MessageResponse("Password reset successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(new MessageResponse("Error resetting password: " + e.getMessage()));
        }
    }
}
