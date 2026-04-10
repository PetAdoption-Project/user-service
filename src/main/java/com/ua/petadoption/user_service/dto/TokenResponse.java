package com.ua.petadoption.user_service.dto;

public record TokenResponse(
        String accessToken,
        String refreshToken,
        long expiresIn
) {
}
