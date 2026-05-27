package com.ua.petadoption.user_service.service;

import com.ua.petadoption.commons.exception.ServiceException;
import com.ua.petadoption.commons.user.Role;
import com.ua.petadoption.commons.user.UserDTO;
import com.ua.petadoption.user_service.exception.UserErrorCode;
import com.ua.petadoption.user_service.mapper.UserMapper;
import com.ua.petadoption.user_service.model.User;
import com.ua.petadoption.user_service.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserService userService;

    @Test
    void getByKeycloakId_existingUser_shouldReturnUserDto() {
        User user = new User();
        user.setKeycloakId("keycloak-id");
        UserDTO expected = new UserDTO(UUID.randomUUID(), "keycloak-id", "user@test.com", "John", "Doe", Role.ADOPTER, null);

        when(userRepository.findByKeycloakId("keycloak-id")).thenReturn(Optional.of(user));
        when(userMapper.toDto(user)).thenReturn(expected);

        UserDTO result = userService.getByKeycloakId("keycloak-id");

        assertThat(result).isEqualTo(expected);
    }

    @Test
    void getByKeycloakId_unknownUser_shouldThrowNotFound() {
        when(userRepository.findByKeycloakId("unknown-id")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getByKeycloakId("unknown-id"))
                .isInstanceOfSatisfying(ServiceException.class, se -> {
                    assertThat(se.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(se.getErrorCode()).isEqualTo(UserErrorCode.USER_NOT_FOUND);
                });
    }
}
