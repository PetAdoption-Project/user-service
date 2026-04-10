package com.ua.petadoption.user_service.dto;

public record KeycloakTokenResponse(
        String accessToken,
        String refreshToken,
        long expiresIn,
        long refreshExpiresIn
) {
}
