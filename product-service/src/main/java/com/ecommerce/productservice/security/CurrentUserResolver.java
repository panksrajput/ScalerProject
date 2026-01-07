package com.ecommerce.productservice.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
public class CurrentUserResolver {

    public CurrentUser resolve() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("Unauthenticated access");
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof Jwt) {
            return new JwtCurrentUser((Jwt) principal);
        }

        throw new IllegalStateException("Unsupported authentication type: " + principal.getClass());
    }
}
