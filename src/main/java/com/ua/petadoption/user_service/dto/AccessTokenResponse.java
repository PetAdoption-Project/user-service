package com.ua.petadoption.user_service.dto;

public record AccessTokenResponse(
        String accessToken,
        long expiresIn
) {
}
