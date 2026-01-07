package com.ecommerce.authservice.service;

import com.ecommerce.authservice.dto.request.LoginRequest;
import com.ecommerce.authservice.dto.request.SignupRequest;
import com.ecommerce.authservice.dto.response.JwtResponse;

import javax.validation.Valid;

public interface AuthService {
    void registerUser(@Valid SignupRequest signUpRequest);
    JwtResponse authenticateUser(@Valid LoginRequest loginRequest);
    JwtResponse refreshToken(String requestRefreshToken);
    boolean logoutUser(String token);
    void forgotPassword(String email);
}
