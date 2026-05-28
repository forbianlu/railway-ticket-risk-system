package com.example.railway.security;

import java.util.Arrays;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import com.example.railway.domain.UserRole;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        AuthPrincipal principal = AuthContext.currentOrNull();
        if (principal != null && UserRole.USER.equals(principal.getRole()) && isManagementPath(request.getRequestURI())) {
            throw new AuthorizationException("Passenger users are not allowed to access management APIs");
        }

        HandlerMethod handlerMethod = (HandlerMethod) handler;
        RequiredRole requiredRole = handlerMethod.getMethodAnnotation(RequiredRole.class);
        if (requiredRole == null) {
            requiredRole = handlerMethod.getBeanType().getAnnotation(RequiredRole.class);
        }
        if (requiredRole == null) {
            return true;
        }

        principal = AuthContext.current();
        checkRole(requiredRole.value(), principal.getRole());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        AuthContext.clear();
    }

    private void checkRole(UserRole[] allowedRoles, UserRole currentRole) {
        if (allowedRoles.length == 0) {
            return;
        }
        List<UserRole> roles = Arrays.asList(allowedRoles);
        if (!roles.contains(currentRole)) {
            throw new AuthorizationException("Current role is not allowed to access this API");
        }
    }

    private boolean isManagementPath(String uri) {
        return uri.startsWith("/api/orders")
                || uri.startsWith("/api/payments")
                || uri.startsWith("/api/refunds")
                || uri.startsWith("/api/risks")
                || uri.startsWith("/api/logs")
                || uri.startsWith("/api/outbox-events")
                || uri.startsWith("/api/cache")
                || uri.startsWith("/api/rate-limit")
                || uri.startsWith("/api/dashboard");
    }
}
