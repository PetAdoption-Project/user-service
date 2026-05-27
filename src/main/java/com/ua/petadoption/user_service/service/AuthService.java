package com.ua.petadoption.user_service.service;

import com.ua.petadoption.commons.exception.ServiceException;
import com.ua.petadoption.commons.user.Role;
import com.ua.petadoption.user_service.client.KeycloakTokenClient;
import com.ua.petadoption.user_service.dto.KeycloakTokenResponse;
import com.ua.petadoption.user_service.exception.UserErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final KeycloakAdminService keycloakAdminService;
    private final KeycloakTokenClient keycloakTokenClient;
    private final UserService userService;

    public void register(String email, String password, String firstName, String lastName, Role role) {
        log.info("Registering new user {} {} with role {}", firstName, lastName, role);

        if (userService.existsByEmail(email)) {
            log.warn("Registration failed - user already exists");
            throw new ServiceException(HttpStatus.CONFLICT, UserErrorCode.USER_EMAIL_ALREADY_EXISTS);
        }

        String keycloakId = keycloakAdminService.createUser(email, password, firstName, lastName);
        keycloakAdminService.assignRole(keycloakId, role);
        userService.createUser(keycloakId, email, firstName, lastName, role);

        log.info("User registered successfully with role {}", role);
    }

    public KeycloakTokenResponse login(String email, String password) {
        log.info("Login attempt for user {}", email);
        return keycloakTokenClient.getToken(email, password);
    }

    public void completeRegistration(String keycloakId, Role role) {
        log.info("Completing OAuth2 registration for keycloakId {} with role {}", keycloakId, role);

        if (userService.existsByKeycloakId(keycloakId)) {
            log.warn("Complete registration failed - already completed for keycloakId {}", keycloakId);
            throw new ServiceException(HttpStatus.CONFLICT, UserErrorCode.USER_REGISTRATION_ALREADY_COMPLETED);
        }

        UserRepresentation keycloakUser = keycloakAdminService.getUserById(keycloakId);
        keycloakAdminService.assignRole(keycloakId, role);
        userService.createUser(keycloakId, keycloakUser.getEmail(),
                keycloakUser.getFirstName(), keycloakUser.getLastName(), role);

        log.info("OAuth2 registration completed for keycloakId {} with role {}", keycloakId, role);
    }

    public KeycloakTokenResponse refresh(String refreshToken) {
        if (!StringUtils.hasText(refreshToken)) {
            log.debug("Refresh token not present");
            throw new ServiceException(HttpStatus.UNAUTHORIZED, UserErrorCode.INVALID_CREDENTIALS);
        }
        log.debug("Refreshing token");
        return keycloakTokenClient.refreshToken(refreshToken);
    }
}
