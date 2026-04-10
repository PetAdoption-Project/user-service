package com.ua.petadoption.user_service.service;

import com.ua.petadoption.commons.exception.ServiceException;
import com.ua.petadoption.commons.user.Role;
import com.ua.petadoption.commons.user.UserDTO;
import com.ua.petadoption.user_service.client.KeycloakTokenClient;
import com.ua.petadoption.user_service.dto.AuthResponse;
import com.ua.petadoption.user_service.dto.TokenResponse;
import com.ua.petadoption.user_service.exception.UserErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final KeycloakAdminService keycloakAdminService;
    private final KeycloakTokenClient keycloakTokenClient;
    private final UserService userService;

    public AuthResponse register(String email, String password, String firstName, String lastName, Role role) {
        log.info("Registering new user {} {} with role {}", firstName, lastName, role);

        if (userService.existsByEmail(email)) {
            log.warn("Registration failed - user already exists");
            throw new ServiceException(HttpStatus.CONFLICT, UserErrorCode.USER_EMAIL_ALREADY_EXISTS);
        }

        String keycloakId = keycloakAdminService.createUser(email, password, firstName, lastName);
        keycloakAdminService.assignRole(keycloakId, role);
        UserDTO user = userService.createUser(keycloakId, email, firstName, lastName, role);
        TokenResponse tokens = keycloakTokenClient.getToken(email, password);

        log.info("User registered successfully with role {}", role);
        return new AuthResponse(user, tokens.accessToken(), tokens.refreshToken(), tokens.expiresIn());
    }

    public TokenResponse login(String email, String password) {
        log.info("Login attempt for user {}", email);
        return keycloakTokenClient.getToken(email, password);
    }

    public AuthResponse completeRegistration(String keycloakId, Role role) {
        log.info("Completing OAuth2 registration for keycloakId {} with role {}", keycloakId, role);

        if (userService.existsByKeycloakId(keycloakId)) {
            log.warn("Complete registration failed - already completed for keycloakId {}", keycloakId);
            throw new ServiceException(HttpStatus.CONFLICT, UserErrorCode.USER_REGISTRATION_ALREADY_COMPLETED);
        }

        UserRepresentation keycloakUser = keycloakAdminService.getUserById(keycloakId);
        keycloakAdminService.assignRole(keycloakId, role);
        UserDTO user = userService.createUser(keycloakId, keycloakUser.getEmail(),
                keycloakUser.getFirstName(), keycloakUser.getLastName(), role);

        log.info("OAuth2 registration completed for keycloakId {} with role {}", keycloakId, role);
        return new AuthResponse(user, null, null, 0);
    }
}
