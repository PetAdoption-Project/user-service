package com.ua.petadoption.user_service.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = SecurityConfig.class)
class PasswordEncoderTest {

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void shouldEncodePassword() {
        String raw = "mySecret123";
        String encoded = passwordEncoder.encode(raw);

        assertNotNull(encoded);
        assertNotEquals(raw, encoded);
    }

    @Test
    void shouldMatchEncodedPassword() {
        String raw = "mySecret123";
        String encoded = passwordEncoder.encode(raw);

        assertTrue(passwordEncoder.matches(raw, encoded));
    }

    @Test
    void shouldNotMatchWrongPassword() {
        String encoded = passwordEncoder.encode("mySecret123");

        assertFalse(passwordEncoder.matches("wrongPassword", encoded));
    }

    @Test
    void shouldProduceDifferentHashesForSamePassword() {
        String raw = "mySecret123";

        String hash1 = passwordEncoder.encode(raw);
        String hash2 = passwordEncoder.encode(raw);

        assertNotEquals(hash1, hash2); // різний salt кожного разу
    }
}
