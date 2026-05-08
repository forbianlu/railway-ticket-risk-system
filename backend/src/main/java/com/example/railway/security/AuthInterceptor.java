package com.example.railway.security;

import java.util.Arrays;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import com.example.railway.domain.UserRole;
import com.example.railway.service.AuthTokenService;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    private final AuthTokenService authTokenService;

    public AuthInterceptor(AuthTokenService authTokenService) {
        this.authTokenService = authTokenService;
    }

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

        AuthPrincipal principal = authTokenService.parse(extractBearerToken(request));
        AuthContext.set(principal);
        checkRole(requiredRole.value(), principal.getRole());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        AuthContext.clear();
    }

    private String extractBearerToken(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new AuthenticationException("请先登录");
        }
        return authorization.substring("Bearer ".length()).trim();
    }

    private void checkRole(UserRole[] allowedRoles, UserRole currentRole) {
        if (allowedRoles.length == 0) {
            return;
        }
        List<UserRole> roles = Arrays.asList(allowedRoles);
        if (!roles.contains(currentRole)) {
            throw new AuthorizationException("当前角色无权访问该接口");
        }
    }
}
