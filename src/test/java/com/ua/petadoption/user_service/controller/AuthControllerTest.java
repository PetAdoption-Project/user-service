package com.ua.petadoption.user_service.controller;

import com.ua.petadoption.commons.exception.ServiceException;
import com.ua.petadoption.commons.user.Role;
import com.ua.petadoption.user_service.dto.KeycloakTokenResponse;
import com.ua.petadoption.user_service.dto.LoginRequest;
import com.ua.petadoption.user_service.dto.RegisterRequest;
import com.ua.petadoption.user_service.exception.UserErrorCode;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import java.util.stream.Stream;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AuthControllerTest extends BaseControllerTest {

    @Test
    void login_validRequest_shouldReturn200WithAccessTokenAndCookie() throws Exception {
        LoginRequest request = new LoginRequest("user@test.com", "password123");
        KeycloakTokenResponse tokens = new KeycloakTokenResponse("access-token", "refresh-token", 300L, 2592000L);
        when(authService.login("user@test.com", "password123")).thenReturn(tokens);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(jsonPath("$.expiresIn").value(300))
                .andExpect(jsonPath("$.refreshToken").doesNotExist())
                .andExpect(cookie().exists("refresh_token"))
                .andExpect(cookie().httpOnly("refresh_token", true));
    }

    @ParameterizedTest
    @MethodSource("invalidLoginRequests")
    void login_invalidRequest_shouldReturn400(LoginRequest request) throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    static Stream<LoginRequest> invalidLoginRequests() {
        return Stream.of(
                new LoginRequest("not-an-email", "password123"),
                new LoginRequest("user@test.com", ""),
                new LoginRequest("", "password123")
        );
    }

    @Test
    void register_validRequest_shouldReturn201() throws Exception {
        RegisterRequest request = new RegisterRequest("user@test.com", "password123", Role.ADOPTER, "John", "Doe");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        verify(authService).register("user@test.com", "password123", "John", "Doe", Role.ADOPTER);
    }

    @Test
    void register_existingEmail_shouldReturn409() throws Exception {
        RegisterRequest request = new RegisterRequest("user@test.com", "password123", Role.ADOPTER, "John", "Doe");
        doThrow(new ServiceException(HttpStatus.CONFLICT, UserErrorCode.USER_EMAIL_ALREADY_EXISTS))
                .when(authService).register("user@test.com", "password123", "John", "Doe", Role.ADOPTER);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("USER_EMAIL_ALREADY_EXISTS"));
    }

    @ParameterizedTest
    @MethodSource("invalidRegisterRequests")
    void register_invalidRequest_shouldReturn400(RegisterRequest request) throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    static Stream<RegisterRequest> invalidRegisterRequests() {
        return Stream.of(
                new RegisterRequest("not-an-email", "password123", Role.ADOPTER, "John", "Doe"),
                new RegisterRequest("user@test.com", "short", Role.ADOPTER, "John", "Doe"),
                new RegisterRequest("user@test.com", "password123", null, "John", "Doe"),
                new RegisterRequest("user@test.com", "password123", Role.ADOPTER, "", "Doe"),
                new RegisterRequest("user@test.com", "password123", Role.ADOPTER, "John", "")
        );
    }


    @Test
    void refresh_validCookie_shouldReturn200WithNewAccessTokenAndCookie() throws Exception {
        KeycloakTokenResponse tokens = new KeycloakTokenResponse("new-access-token", "new-refresh-token", 300L, 2592000L);
        when(authService.refresh("valid-refresh-token")).thenReturn(tokens);

        mockMvc.perform(post("/api/auth/refresh")
                        .cookie(new Cookie("refresh_token", "valid-refresh-token")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new-access-token"))
                .andExpect(jsonPath("$.expiresIn").value(300))
                .andExpect(jsonPath("$.refreshToken").doesNotExist())
                .andExpect(cookie().exists("refresh_token"));
    }

    @Test
    void refresh_missingCookie_shouldReturn401() throws Exception {
        when(authService.refresh(null))
                .thenThrow(new ServiceException(HttpStatus.UNAUTHORIZED, UserErrorCode.INVALID_CREDENTIALS));

        mockMvc.perform(post("/api/auth/refresh"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }
}
