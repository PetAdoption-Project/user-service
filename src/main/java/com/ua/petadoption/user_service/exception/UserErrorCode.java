package com.ua.petadoption.user_service.exception;

import com.ua.petadoption.commons.exception.ErrorCode;

public enum UserErrorCode implements ErrorCode {

    USER_NOT_FOUND("user.not.found"),
    USER_EMAIL_ALREADY_EXISTS("user.email.already.exists"),
    USER_REGISTRATION_ALREADY_COMPLETED("user.registration.already.completed"),
    KEYCLOAK_USER_CREATION_FAILED("keycloak.user.creation.failed"),
    INVALID_CREDENTIALS("auth.invalid.credentials");

    private final String messageKey;

    UserErrorCode(String messageKey) {
        this.messageKey = messageKey;
    }

    @Override
    public String getCode() {
        return this.name();
    }

    @Override
    public String getMessageKey() {
        return messageKey;
    }
}
