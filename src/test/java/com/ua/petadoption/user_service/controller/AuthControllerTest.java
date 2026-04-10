package com.ua.petadoption.user_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ua.petadoption.commons.user.Role;
import com.ua.petadoption.commons.user.UserDTO;
import com.ua.petadoption.user_service.dto.AuthResponse;
import com.ua.petadoption.user_service.dto.LoginRequest;
import com.ua.petadoption.user_service.dto.RegisterRequest;
import com.ua.petadoption.user_service.dto.TokenResponse;
import com.ua.petadoption.user_service.service.AuthService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;
import java.util.stream.Stream;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    @Test
    void login_validRequest_shouldReturn200WithTokens() throws Exception {
        LoginRequest request = new LoginRequest("user@test.com", "password123");
        TokenResponse tokens = new TokenResponse("access-token", "refresh-token", 300L);
        when(authService.login("user@test.com", "password123")).thenReturn(tokens);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token"))
                .andExpect(jsonPath("$.expiresIn").value(300));
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
    void register_validRequest_shouldReturn201WithUserAndTokens() throws Exception {
        RegisterRequest request = new RegisterRequest("user@test.com", "password123", Role.ADOPTER, "John", "Doe");
        UserDTO userDto = new UserDTO(UUID.randomUUID(), "keycloak-id", "user@test.com", "John", "Doe", Role.ADOPTER, null);
        AuthResponse response = new AuthResponse(userDto, "access-token", "refresh-token", 300L);
        when(authService.register("user@test.com", "password123", "John", "Doe", Role.ADOPTER)).thenReturn(response);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.user.email").value("user@test.com"))
                .andExpect(jsonPath("$.accessToken").value("access-token"));
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
}
