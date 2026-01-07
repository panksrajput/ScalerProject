package com.ecommerce.authservice.service;

import com.ecommerce.authservice.dto.request.CreateUserRequest;
import com.ecommerce.authservice.dto.request.UpdatePasswordRequest;
import com.ecommerce.authservice.dto.request.UpdateUserRequest;
import com.ecommerce.authservice.dto.response.UserProfileResponse;
import com.ecommerce.authservice.dto.response.UserResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserService {
    Page<UserResponse> getAllUsers(Pageable pageable);
    UserProfileResponse getUserProfile(Long userId);
    UserProfileResponse createUser(CreateUserRequest request);
    UserProfileResponse updateUser(Long userId, UpdateUserRequest updateUserRequest);
    void updatePassword(Long userId, UpdatePasswordRequest updatePasswordRequest);
    void deleteUser(Long userId);
}
