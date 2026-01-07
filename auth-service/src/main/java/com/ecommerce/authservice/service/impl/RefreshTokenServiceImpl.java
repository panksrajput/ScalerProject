package com.ecommerce.authservice.service.impl;

import com.ecommerce.authservice.entity.RefreshToken;
import com.ecommerce.authservice.exception.TokenRefreshException;
import com.ecommerce.authservice.repository.RefreshTokenRepository;
import com.ecommerce.authservice.repository.UserRepository;
import com.ecommerce.authservice.service.RefreshTokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class RefreshTokenServiceImpl implements RefreshTokenService {
    private static final Logger logger = LoggerFactory.getLogger(RefreshTokenServiceImpl.class);
    
    @Value("${jwt.refresh.expiration.ms}")
    private Long refreshTokenDurationMs;
    
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;

    @Autowired
    public RefreshTokenServiceImpl(RefreshTokenRepository refreshTokenRepository, UserRepository userRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.userRepository = userRepository;
    }

    public Optional<RefreshToken> findByToken(String token) {
        logger.debug("Looking up token in database");
        if (token == null || token.trim().isEmpty()) {
            logger.warn("Attempted to look up null or empty token");
            return Optional.empty();
        }
        
        try {
            Optional<RefreshToken> tokenOpt = refreshTokenRepository.findByToken(token);
            logger.debug("Token lookup result: {}", tokenOpt.isPresent() ? "FOUND" : "NOT FOUND");
            
            if (tokenOpt.isEmpty()) {
                // Additional check with trimmed token
                String trimmedToken = token.trim();
                if (!trimmedToken.equals(token)) {
                    logger.debug("Trying with trimmed token");
                    Optional<RefreshToken> trimmedTokenOpt = refreshTokenRepository.findByToken(trimmedToken);
                    if (trimmedTokenOpt.isPresent()) {
                        logger.warn("Token found after trimming whitespace");
                        return trimmedTokenOpt;
                    }
                }
                
                // Log the first few characters of the token for debugging (but not the whole token for security)
                String tokenPreview = token.length() > 10 ? 
                    token.substring(0, 5) + "..." + token.substring(token.length() - 5) : 
                    "[token too short]";
                logger.debug("Token not found in database. Token preview: {}", tokenPreview);
            } else {
                logger.debug("Successfully found token in database");
                if (tokenOpt.get().getUser() == null) {
                    logger.warn("Found token but it has no associated user!");
                }
            }
            
            return tokenOpt;
        } catch (Exception e) {
            logger.error("Error looking up token: {}", e.getMessage(), e);
            return Optional.empty();
        }
    }

    public RefreshToken createRefreshToken(Long userId) {
        RefreshToken refreshToken = new RefreshToken();
        
        refreshToken.setUser(userRepository.findById(userId).get());
        refreshToken.setExpiryDate(Instant.now().plusMillis(refreshTokenDurationMs));
        refreshToken.setToken(UUID.randomUUID().toString());
        
        refreshToken = refreshTokenRepository.save(refreshToken);
        return refreshToken;
    }

    public RefreshToken verifyExpiration(RefreshToken token) {
        if (token.getExpiryDate().compareTo(Instant.now()) < 0) {
            refreshTokenRepository.delete(token);
            throw new TokenRefreshException(token.getToken(), "Refresh token was expired. Please make a new signin request");
        }
        return token;
    }

    @Transactional
    public int deleteByUserId(Long userId) {
        return refreshTokenRepository.deleteByUser(userRepository.findById(userId).get());
    }
    
    @Transactional
    public int deleteByToken(String token) {
        logger.debug("Attempting to delete refresh token: [HIDDEN]");
        try {
            int result = refreshTokenRepository.deleteByToken(token);
            logger.debug("Delete operation affected {} rows", result);
            
            // Verify the token was actually deleted
            if (result > 0) {
                boolean stillExists = refreshTokenRepository.findByToken(token).isPresent();
                if (stillExists) {
                    logger.error("Token still exists in database after deletion attempt!");
                } else {
                    logger.debug("Successfully verified token was deleted from database");
                }
            }
            
            return result;
        } catch (Exception e) {
            logger.error("Error deleting refresh token: {}", e.getMessage(), e);
            throw e;
        }
    }
    
    @Transactional
    public void revokeByUserId(Long userId) {
        refreshTokenRepository.revokeByUserId(userId);
    }
}
