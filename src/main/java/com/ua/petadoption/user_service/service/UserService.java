package com.ua.petadoption.user_service.service;

import com.ua.petadoption.commons.exception.ServiceException;
import com.ua.petadoption.commons.user.Role;
import com.ua.petadoption.commons.user.UserDTO;
import com.ua.petadoption.user_service.exception.UserErrorCode;
import com.ua.petadoption.user_service.mapper.UserMapper;
import com.ua.petadoption.user_service.model.User;
import com.ua.petadoption.user_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Transactional
    public UserDTO createUser(String keycloakId, String email, String firstName, String lastName, Role role) {
        User user = new User();
        user.setKeycloakId(keycloakId);
        user.setEmail(email);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setRole(role);
        return userMapper.toDto(userRepository.save(user));
    }

    @Transactional(readOnly = true)
    public boolean existsByKeycloakId(String keycloakId) {
        return userRepository.findByKeycloakId(keycloakId).isPresent();
    }

    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }
}
