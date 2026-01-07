package com.ecommerce.authservice.service;

import com.ecommerce.authservice.entity.RefreshToken;

import java.util.Optional;

public interface RefreshTokenService {
    void revokeByUserId(Long id);

    RefreshToken createRefreshToken(Long id);

    Optional<RefreshToken> findByToken(String requestRefreshToken);

    int deleteByToken(String refreshToken);

    RefreshToken verifyExpiration(RefreshToken refreshToken);
}
