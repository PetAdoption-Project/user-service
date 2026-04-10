package com.ua.petadoption.user_service.dto;

import com.ua.petadoption.commons.user.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegisterRequest(

        @NotBlank @Email
        String email,

        @NotBlank @Size(min = 8, max = 100)
        String password,

        @NotNull
        Role role,

        @NotBlank
        String firstName,

        @NotBlank
        String lastName
) {}
