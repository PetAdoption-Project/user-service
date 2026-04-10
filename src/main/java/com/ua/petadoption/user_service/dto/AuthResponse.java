package com.ua.petadoption.user_service.dto;

import com.ua.petadoption.commons.user.UserDTO;

public record AuthResponse(
        UserDTO user,
        String accessToken,
        String refreshToken,
        long expiresIn
) {
}
