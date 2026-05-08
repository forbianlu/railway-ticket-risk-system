package com.example.railway.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.railway.domain.AppUser;
import com.example.railway.dto.AuthResponse;
import com.example.railway.dto.LoginRequest;
import com.example.railway.repository.AppUserRepository;
import com.example.railway.security.AuthContext;
import com.example.railway.security.AuthPrincipal;
import com.example.railway.security.AuthenticationException;

@Service
public class AuthService {

    private final AppUserRepository appUserRepository;
    private final PasswordService passwordService;
    private final AuthTokenService authTokenService;
    private final OperationLogService operationLogService;

    public AuthService(AppUserRepository appUserRepository,
                       PasswordService passwordService,
                       AuthTokenService authTokenService,
                       OperationLogService operationLogService) {
        this.appUserRepository = appUserRepository;
        this.passwordService = passwordService;
        this.authTokenService = authTokenService;
        this.operationLogService = operationLogService;
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        AppUser user = appUserRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new AuthenticationException("用户名或密码错误"));
        if (!Boolean.TRUE.equals(user.getEnabled()) || !passwordService.matches(request.getPassword(), user.getPasswordHash())) {
            throw new AuthenticationException("用户名或密码错误");
        }

        String token = authTokenService.generate(user);
        operationLogService.record(
                user.getUsername(),
                "LOGIN",
                "APP_USER",
                String.valueOf(user.getId()),
                "用户登录系统"
        );
        return new AuthResponse(token, user.getUsername(), user.getDisplayName(), user.getRole().name(), authTokenService.expiresAt(token));
    }

    @Transactional(readOnly = true)
    public AuthResponse me() {
        AuthPrincipal principal = AuthContext.current();
        return new AuthResponse(null, principal.getUsername(), principal.getDisplayName(), principal.getRole().name(), principal.getExpiresAt());
    }
}
