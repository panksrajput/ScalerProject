package com.ecommerce.authservice.controller;

import com.ecommerce.authservice.dto.request.CreateUserRequest;
import com.ecommerce.authservice.dto.request.UpdateUserRequest;
import com.ecommerce.authservice.dto.response.UserProfileResponse;
import com.ecommerce.authservice.dto.response.UserResponse;
import com.ecommerce.authservice.service.UserService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    private final UserService userService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<UserResponse>> getAllUsers(
            @PageableDefault(size = 10) Pageable pageable) {
        logger.info("Received request to get all users with pagination: {}", pageable);
        Page<UserResponse> users = userService.getAllUsers(pageable);
        logger.debug("Retrieved {} users out of {}", users.getNumberOfElements(), users.getTotalElements());
        return ResponseEntity.ok(users);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or #id == authentication.principal.id")
    public ResponseEntity<UserProfileResponse> getUserById(@PathVariable Long id) {
        logger.info("Received request to get user by id: {}", id);
        UserProfileResponse userProfile = userService.getUserProfile(id);
        logger.debug("Retrieved user profile for id {}: {}", id, userProfile);
        return ResponseEntity.ok(userProfile);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserProfileResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        logger.info("Received request to create new user with username: {}", request.getUsername());
        UserProfileResponse createdUser = userService.createUser(request);
        logger.info("Successfully created user with id: {}", createdUser.getId());
        return new ResponseEntity<>(createdUser, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or #id == authentication.principal.id")
    public ResponseEntity<UserProfileResponse> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest request) {
        logger.info("Received request to update user with id: {}", id);
        UserProfileResponse updatedUser = userService.updateUser(id, request);
        logger.info("Successfully updated user with id: {}", id);
        return ResponseEntity.ok(updatedUser);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN') or #id == authentication.principal.id")
    public void deleteUser(@PathVariable Long id) {
        logger.info("Received request to delete user with id: {}", id);
        userService.deleteUser(id);
        logger.info("Successfully deleted user with id: {}", id);
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserProfileResponse> getCurrentUserProfile(
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = ((com.ecommerce.authservice.security.services.UserDetailsImpl) userDetails).getId();
        logger.debug("Fetching profile for current authenticated user with id: {}", userId);
        UserProfileResponse userProfile = userService.getUserProfile(userId);
        logger.debug("Successfully retrieved profile for user id: {}", userId);
        return ResponseEntity.ok(userProfile);
    }
}
