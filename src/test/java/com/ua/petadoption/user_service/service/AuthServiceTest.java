package com.ua.petadoption.user_service.service;

import com.ua.petadoption.commons.exception.ServiceException;
import com.ua.petadoption.commons.user.Role;
import com.ua.petadoption.commons.user.UserDTO;
import com.ua.petadoption.user_service.client.KeycloakTokenClient;
import com.ua.petadoption.user_service.dto.KeycloakTokenResponse;
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
        KeycloakTokenResponse expected = new KeycloakTokenResponse("access-token", "refresh-token", 300L, 2592000L);
        when(keycloakTokenClient.getToken("user@test.com", "password")).thenReturn(expected);

        KeycloakTokenResponse result = authService.login("user@test.com", "password");

        assertThat(result).isEqualTo(expected);
        verify(keycloakTokenClient).getToken("user@test.com", "password");
    }

    @Test
    void login_invalidCredentials_shouldPropagateException() {
        when(keycloakTokenClient.getToken(any(), any()))
                .thenThrow(new ServiceException(HttpStatus.UNAUTHORIZED, UserErrorCode.INVALID_CREDENTIALS));

        assertThatThrownBy(() -> authService.login("user@test.com", "wrong"))
                .isInstanceOfSatisfying(ServiceException.class, se -> {
                    assertThat(se.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
                    assertThat(se.getErrorCode()).isEqualTo(UserErrorCode.INVALID_CREDENTIALS);
                });
    }

    @Test
    void register_newUser_shouldCreateUserAndAssignRole() {
        UserDTO userDto = new UserDTO(UUID.randomUUID(), "keycloak-id", "user@test.com", "John", "Doe", Role.ADOPTER, null);

        when(userService.existsByEmail("user@test.com")).thenReturn(false);
        when(keycloakAdminService.createUser("user@test.com", "password", "John", "Doe")).thenReturn("keycloak-id");
        when(userService.createUser("keycloak-id", "user@test.com", "John", "Doe", Role.ADOPTER)).thenReturn(userDto);

        authService.register("user@test.com", "password", "John", "Doe", Role.ADOPTER);

        verify(keycloakAdminService).createUser("user@test.com", "password", "John", "Doe");
        verify(keycloakAdminService).assignRole("keycloak-id", Role.ADOPTER);
        verify(userService).createUser("keycloak-id", "user@test.com", "John", "Doe", Role.ADOPTER);
    }

    @Test
    void register_existingEmail_shouldThrowConflict() {
        when(userService.existsByEmail("user@test.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register("user@test.com", "password", "John", "Doe", Role.ADOPTER))
                .isInstanceOfSatisfying(ServiceException.class, se -> {
                    assertThat(se.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(se.getErrorCode()).isEqualTo(UserErrorCode.USER_EMAIL_ALREADY_EXISTS);
                });

        verifyNoInteractions(keycloakAdminService, keycloakTokenClient);
    }

    @Test
    void refresh_validToken_shouldReturnNewTokens() {
        KeycloakTokenResponse expected = new KeycloakTokenResponse("new-access-token", "new-refresh-token", 300L, 2592000L);
        when(keycloakTokenClient.refreshToken("valid-refresh-token")).thenReturn(expected);

        KeycloakTokenResponse result = authService.refresh("valid-refresh-token");

        assertThat(result).isEqualTo(expected);
    }

    @Test
    void refresh_missingToken_shouldThrowUnauthorized() {
        assertThatThrownBy(() -> authService.refresh(null))
                .isInstanceOfSatisfying(ServiceException.class, se -> {
                    assertThat(se.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
                    assertThat(se.getErrorCode()).isEqualTo(UserErrorCode.INVALID_CREDENTIALS);
                });

        assertThatThrownBy(() -> authService.refresh("  "))
                .isInstanceOfSatisfying(ServiceException.class, se -> {
                    assertThat(se.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
                    assertThat(se.getErrorCode()).isEqualTo(UserErrorCode.INVALID_CREDENTIALS);
                });
    }
}
