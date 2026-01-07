package com.ecommerce.authservice.service.impl;

import com.ecommerce.authservice.dto.request.CreateUserRequest;
import com.ecommerce.authservice.dto.request.UpdatePasswordRequest;
import com.ecommerce.authservice.dto.request.UpdateUserRequest;
import com.ecommerce.authservice.dto.response.UserProfileResponse;
import com.ecommerce.authservice.dto.response.UserResponse;
import com.ecommerce.authservice.entity.Role;
import com.ecommerce.authservice.entity.User;
import com.ecommerce.authservice.exception.EmailAlreadyExistsException;
import com.ecommerce.authservice.exception.PasswordMismatchException;
import com.ecommerce.authservice.exception.ResourceNotFoundException;
import com.ecommerce.authservice.exception.UsernameAlreadyExistsException;
import com.ecommerce.authservice.repository.RoleRepository;
import com.ecommerce.authservice.repository.UserRepository;
import com.ecommerce.authservice.service.UserService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public UserServiceImpl(UserRepository userRepository, 
                          RoleRepository roleRepository,
                          PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserResponse> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable)
                .map(this::convertToUserResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getUserProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        
        return convertToUserProfileResponse(user);
    }

    @Override
    @Transactional
    public UserProfileResponse createUser(CreateUserRequest request) {
        // Check if username is already taken
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new UsernameAlreadyExistsException("Username is already taken!");
        }

        // Check if email is already in use
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException("Email is already in use!");
        }

        // Create new user
        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEmailVerified(false);
        user.setAccountNonLocked(true);

        // Set default role
        Set<Role> roles = new HashSet<>();
        Role userRole = roleRepository.findByName(Role.ERole.ROLE_USER)
                .orElseGet(() -> {
                    Role role = new Role();
                    role.setName(Role.ERole.ROLE_USER);
                    return roleRepository.save(role);
                });
        roles.add(userRole);
        user.setRoles(roles);

        User savedUser = userRepository.save(user);
        return convertToUserProfileResponse(savedUser);
    }

    @Override
    @Transactional
    public UserProfileResponse updateUser(Long userId, UpdateUserRequest updateUserRequest) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        
        // Check if the new email is already taken by another user
        if (!user.getEmail().equals(updateUserRequest.getEmail()) && 
            userRepository.existsByEmail(updateUserRequest.getEmail())) {
            throw new EmailAlreadyExistsException("Email is already in use!");
        }
        
        // Update user details
        user.setFirstName(updateUserRequest.getFirstName());
        user.setLastName(updateUserRequest.getLastName());
        user.setEmail(updateUserRequest.getEmail());
        
        User updatedUser = userRepository.save(user);
        return convertToUserProfileResponse(updatedUser);
    }

    @Override
    @Transactional
    public void updatePassword(Long userId, UpdatePasswordRequest updatePasswordRequest) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        
        // Check if current password is correct
        if (!passwordEncoder.matches(updatePasswordRequest.getCurrentPassword(), user.getPassword())) {
            throw new PasswordMismatchException("Current password is incorrect");
        }
        
        // Check if new passwords match
        if (!updatePasswordRequest.getNewPassword().equals(updatePasswordRequest.getConfirmNewPassword())) {
            throw new PasswordMismatchException("New passwords do not match");
        }
        
        // Update password
        user.setPassword(passwordEncoder.encode(updatePasswordRequest.getNewPassword()));
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void deleteUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        
        // Soft delete
        user.setDeleted(true);
        userRepository.save(user);
    }
    
    private UserProfileResponse convertToUserProfileResponse(User user) {
        UserProfileResponse response = new UserProfileResponse();
        BeanUtils.copyProperties(user, response);
        
        // Convert roles to role names
        Set<String> roleNames = user.getRoles().stream()
                .map(role -> role.getName().name())
                .collect(Collectors.toSet());
        
        response.setRoles(roleNames);
        response.setAccountNonLocked(user.getAccountNonLocked());
        response.setEmailVerified(user.getEmailVerified());
        
        return response;
    }
    
    private UserResponse convertToUserResponse(User user) {
        UserResponse response = new UserResponse();
        BeanUtils.copyProperties(user, response);
        
        // Convert roles to role names
        Set<String> roleNames = user.getRoles().stream()
                .map(role -> role.getName().name())
                .collect(Collectors.toSet());
        
        response.setRoles(roleNames);
        return response;
    }
}
