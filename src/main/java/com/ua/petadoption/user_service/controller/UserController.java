package com.ua.petadoption.user_service.controller;

import com.ua.petadoption.commons.security.UserHeaders;
import com.ua.petadoption.commons.user.UserDTO;
import com.ua.petadoption.user_service.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<UserDTO> getMe(@RequestHeader(UserHeaders.AUTH_SUBJECT) String keycloakId) {
        return ResponseEntity.ok(userService.getByKeycloakId(keycloakId));
    }
}
