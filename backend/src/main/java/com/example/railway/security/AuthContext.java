package com.example.railway.security;

public final class AuthContext {

    private static final ThreadLocal<AuthPrincipal> CURRENT = new ThreadLocal<AuthPrincipal>();

    private AuthContext() {
    }

    public static void set(AuthPrincipal principal) {
        CURRENT.set(principal);
    }

    public static AuthPrincipal current() {
        AuthPrincipal principal = CURRENT.get();
        if (principal == null) {
            throw new AuthenticationException("请先登录");
        }
        return principal;
    }

    public static void clear() {
        CURRENT.remove();
    }
}
