package com.ua.petadoption.user_service.controller;

import com.ua.petadoption.commons.security.UserHeaders;
import com.ua.petadoption.user_service.dto.*;
import com.ua.petadoption.user_service.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.server.Cookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final String REFRESH_TOKEN_COOKIE = "refresh_token";

    private final AuthService authService;

    @Value("${app.cookie.secure:true}")
    private boolean secureCookie;

    @PostMapping("/register")
    public ResponseEntity<Void> register(@RequestBody @Valid RegisterRequest request) {
        authService.register(request.email(), request.password(), request.firstName(), request.lastName(), request.role());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/login")
    public ResponseEntity<AccessTokenResponse> login(@RequestBody @Valid LoginRequest request) {
        KeycloakTokenResponse tokens = authService.login(request.email(), request.password());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, buildRefreshCookie(tokens.refreshToken(), tokens.refreshExpiresIn()))
                .body(new AccessTokenResponse(tokens.accessToken(), tokens.expiresIn()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AccessTokenResponse> refresh(
            @CookieValue(value = REFRESH_TOKEN_COOKIE, required = false) String refreshToken) {
        KeycloakTokenResponse tokens = authService.refresh(refreshToken);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, buildRefreshCookie(tokens.refreshToken(), tokens.refreshExpiresIn()))
                .body(new AccessTokenResponse(tokens.accessToken(), tokens.expiresIn()));
    }

    @PostMapping("/complete-registration")
    public ResponseEntity<Void> completeRegistration(
            @RequestHeader(UserHeaders.AUTH_SUBJECT) String keycloakId,
            @RequestBody @Valid CompleteRegistrationRequest request) {
        authService.completeRegistration(keycloakId, request.role());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    private String buildRefreshCookie(String refreshToken, long refreshExpiresIn) {
        return ResponseCookie.from(REFRESH_TOKEN_COOKIE, refreshToken)
                .httpOnly(true)
                .secure(secureCookie)
                .path("/api/auth/refresh")
                .maxAge(Duration.ofSeconds(refreshExpiresIn))
                .sameSite(Cookie.SameSite.LAX.attributeValue())
                .build()
                .toString();
    }
}
