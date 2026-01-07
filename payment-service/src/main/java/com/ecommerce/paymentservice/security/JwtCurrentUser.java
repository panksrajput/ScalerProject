package com.ecommerce.paymentservice.security;

import com.ecommerce.paymentservice.security.CurrentUser;
import org.springframework.security.oauth2.jwt.Jwt;

public class JwtCurrentUser implements CurrentUser {

    private final Jwt jwt;

    public JwtCurrentUser(Jwt jwt) {
        this.jwt = jwt;
    }

    @Override
    public Long getUserId() {
        Object userId = jwt.getClaim("userId");
        if (userId == null) {
            throw new IllegalStateException("userId claim missing");
        }
        return Long.valueOf(userId.toString());
    }
}
