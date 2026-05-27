package com.ua.petadoption.user_service.controller;

import com.ua.petadoption.commons.exception.ServiceException;
import com.ua.petadoption.commons.security.UserHeaders;
import com.ua.petadoption.commons.user.Role;
import com.ua.petadoption.commons.user.UserDTO;
import com.ua.petadoption.user_service.exception.UserErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserControllerTest extends BaseControllerTest {

    @Test
    void getMe_validKeycloakId_shouldReturnUserDto() throws Exception {
        UUID userId = UUID.randomUUID();
        UserDTO user = new UserDTO(userId, "keycloak-id", "user@test.com", "John", "Doe", Role.ADOPTER, null);
        when(userService.getByKeycloakId("keycloak-id")).thenReturn(user);

        mockMvc.perform(get("/api/users/me")
                        .header(UserHeaders.AUTH_SUBJECT, "keycloak-id"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId.toString()))
                .andExpect(jsonPath("$.email").value("user@test.com"))
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.lastName").value("Doe"))
                .andExpect(jsonPath("$.role").value("ADOPTER"));
    }

    @Test
    void getMe_unknownKeycloakId_shouldReturn404() throws Exception {
        when(userService.getByKeycloakId("unknown-id"))
                .thenThrow(new ServiceException(HttpStatus.NOT_FOUND, UserErrorCode.USER_NOT_FOUND));

        mockMvc.perform(get("/api/users/me")
                        .header(UserHeaders.AUTH_SUBJECT, "unknown-id"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"));
    }
}
