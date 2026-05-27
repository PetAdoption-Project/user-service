package com.ua.petadoption.user_service.dto;

import com.ua.petadoption.commons.user.Role;
import jakarta.validation.constraints.NotNull;

public record CompleteRegistrationRequest(

        @NotNull
        Role role
) {}
