package com.ua.petadoption.user_service.service;

import com.ua.petadoption.commons.exception.ServiceException;
import com.ua.petadoption.commons.user.Role;
import com.ua.petadoption.user_service.exception.UserErrorCode;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class KeycloakAdminService {

    private final Keycloak keycloak;

    @Value("${keycloak.realm}")
    private String realm;

    public String createUser(String email, String password, String firstName, String lastName) {
        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(password);
        credential.setTemporary(false);

        UserRepresentation user = new UserRepresentation();
        user.setEmail(email);
        user.setUsername(email);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEnabled(true);
        user.setEmailVerified(true);
        user.setRequiredActions(List.of());
        user.setCredentials(List.of(credential));

        try (Response response = keycloak.realm(realm).users().create(user)) {
            if (response.getStatus() == 409) {
                log.warn("User already exists in Keycloak — initiating crash recovery");
                return findUserByEmail(email);
            }
            if (response.getStatus() != 201) {
                log.error("Failed to create user in Keycloak, status: {}", response.getStatus());
                throw new ServiceException(HttpStatus.INTERNAL_SERVER_ERROR, UserErrorCode.KEYCLOAK_USER_CREATION_FAILED);
            }
            String keycloakId = response.getLocation().getPath().replaceAll(".*/", "");
            log.debug("User created in Keycloak with id {}", keycloakId);
            return keycloakId;
        }
    }

    public UserRepresentation getUserById(String keycloakId) {
        return keycloak.realm(realm).users().get(keycloakId).toRepresentation();
    }

    public String findUserByEmail(String email) {
        List<UserRepresentation> users = keycloak.realm(realm).users().searchByEmail(email, true);
        if (users.isEmpty()) {
            log.error("User with email '{}' not found in Keycloak", email);
            throw new ServiceException(HttpStatus.NOT_FOUND, UserErrorCode.USER_NOT_FOUND);
        }
        return users.getFirst().getId();
    }

    public void assignRole(String keycloakId, Role role) {
        log.debug("Assigning role {} to keycloakId {}", role, keycloakId);
        RoleRepresentation roleRepresentation = keycloak.realm(realm)
                .roles()
                .get(role.name())
                .toRepresentation();

        keycloak.realm(realm)
                .users()
                .get(keycloakId)
                .roles()
                .realmLevel()
                .add(List.of(roleRepresentation));
    }
}
