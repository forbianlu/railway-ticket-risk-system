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

        HandlerMethod handlerMethod = (HandlerMethod) handler;
        RequiredRole requiredRole = handlerMethod.getMethodAnnotation(RequiredRole.class);
        if (requiredRole == null) {
            requiredRole = handlerMethod.getBeanType().getAnnotation(RequiredRole.class);
        }
        if (requiredRole == null) {
            return true;
        }

        AuthPrincipal principal = AuthContext.current();
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
}
