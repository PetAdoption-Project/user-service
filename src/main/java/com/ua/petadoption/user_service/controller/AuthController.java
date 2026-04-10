package com.ua.petadoption.user_service.controller;

import com.ua.petadoption.commons.security.UserHeaders;
import com.ua.petadoption.user_service.dto.*;
import com.ua.petadoption.user_service.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody @Valid RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(authService.register(request.email(), request.password(),
                        request.firstName(), request.lastName(), request.role()));
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@RequestBody @Valid LoginRequest request) {
        return ResponseEntity.ok(authService.login(request.email(), request.password()));
    }

    @PostMapping("/complete-registration")
    public ResponseEntity<AuthResponse> completeRegistration(
            @RequestHeader(UserHeaders.USER_ID) String keycloakId,
            @RequestBody @Valid CompleteRegistrationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(authService.completeRegistration(keycloakId, request.role()));
    }
}
