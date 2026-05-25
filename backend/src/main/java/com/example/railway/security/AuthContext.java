package com.example.railway.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class AuthContext {

    private static final ThreadLocal<AuthPrincipal> CURRENT = new ThreadLocal<AuthPrincipal>();

    private AuthContext() {
    }

    public static void set(AuthPrincipal principal) {
        CURRENT.set(principal);
    }

    public static AuthPrincipal current() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AuthPrincipal) {
            return (AuthPrincipal) authentication.getPrincipal();
        }
        AuthPrincipal principal = CURRENT.get();
        if (principal == null) {
            throw new AuthenticationException("Please login first");
        }
        return principal;
    }

    public static AuthPrincipal currentOrNull() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AuthPrincipal) {
            return (AuthPrincipal) authentication.getPrincipal();
        }
        return CURRENT.get();
    }

    public static void clear() {
        CURRENT.remove();
    }
}
