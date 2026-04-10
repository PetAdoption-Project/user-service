package com.ua.petadoption.user_service.service;

import com.ua.petadoption.commons.exception.ServiceException;
import com.ua.petadoption.commons.user.Role;
import com.ua.petadoption.commons.user.UserDTO;
import com.ua.petadoption.user_service.client.KeycloakTokenClient;
import com.ua.petadoption.user_service.dto.AuthResponse;
import com.ua.petadoption.user_service.dto.TokenResponse;
import com.ua.petadoption.user_service.exception.UserErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private KeycloakAdminService keycloakAdminService;

    @Mock
    private KeycloakTokenClient keycloakTokenClient;

    @Mock
    private UserService userService;

    @InjectMocks
    private AuthService authService;

    @Test
    void login_validCredentials_shouldReturnTokens() {
        TokenResponse expected = new TokenResponse("access-token", "refresh-token", 300L);
        when(keycloakTokenClient.getToken("user@test.com", "password")).thenReturn(expected);

        TokenResponse result = authService.login("user@test.com", "password");

        assertThat(result).isEqualTo(expected);
        verify(keycloakTokenClient).getToken("user@test.com", "password");
    }

    @Test
    void login_invalidCredentials_shouldPropagateException() {
        when(keycloakTokenClient.getToken(any(), any()))
                .thenThrow(new ServiceException(HttpStatus.UNAUTHORIZED, UserErrorCode.INVALID_CREDENTIALS));

        assertThatThrownBy(() -> authService.login("user@test.com", "wrong"))
                .isInstanceOf(ServiceException.class)
                .extracting(e -> ((ServiceException) e).getStatus())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void register_newUser_shouldCreateKeycloakUserAndReturnAuthResponse() {
        UserDTO userDto = new UserDTO(UUID.randomUUID(), "keycloak-id", "user@test.com", "John", "Doe", Role.ADOPTER, null);
        TokenResponse tokens = new TokenResponse("access-token", "refresh-token", 300L);

        when(userService.existsByEmail("user@test.com")).thenReturn(false);
        when(keycloakAdminService.createUser("user@test.com", "password", "John", "Doe")).thenReturn("keycloak-id");
        when(userService.createUser("keycloak-id", "user@test.com", "John", "Doe", Role.ADOPTER)).thenReturn(userDto);
        when(keycloakTokenClient.getToken("user@test.com", "password")).thenReturn(tokens);

        AuthResponse result = authService.register("user@test.com", "password", "John", "Doe", Role.ADOPTER);

        assertThat(result.user()).isEqualTo(userDto);
        assertThat(result.accessToken()).isEqualTo("access-token");
        assertThat(result.refreshToken()).isEqualTo("refresh-token");
        assertThat(result.expiresIn()).isEqualTo(300L);
        verify(keycloakAdminService).assignRole("keycloak-id", Role.ADOPTER);
    }

    @Test
    void register_existingEmail_shouldThrowConflict() {
        when(userService.existsByEmail("user@test.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register("user@test.com", "password", "John", "Doe", Role.ADOPTER))
                .isInstanceOf(ServiceException.class)
                .extracting(e -> ((ServiceException) e).getStatus())
                .isEqualTo(HttpStatus.CONFLICT);

        verifyNoInteractions(keycloakAdminService, keycloakTokenClient);
    }
}
