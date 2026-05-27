package com.ua.petadoption.user_service.repository;

import com.ua.petadoption.commons.user.Role;
import com.ua.petadoption.user_service.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import com.ua.petadoption.user_service.config.JpaConfig;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Import(JpaConfig.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    private User createUser() {
        User user = new User();
        user.setKeycloakId("keycloakId");
        user.setEmail("test@test.com");
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setRole(Role.ADOPTER);
        return userRepository.save(user);
    }

    @Test
    void shouldSaveUser() {
        User saved = createUser();

        assertNotNull(saved.getId());
        assertNotNull(saved.getCreatedAt());
    }

    @Test
    void shouldFindByEmail() {
        createUser();

        Optional<User> found = userRepository.findByEmail("test@test.com");

        assertTrue(found.isPresent());
        assertEquals("test@test.com", found.get().getEmail());
    }

    @Test
    void shouldReturnEmptyWhenEmailNotFound() {
        Optional<User> found = userRepository.findByEmail("notexist@test.com");

        assertFalse(found.isPresent());
    }

    @Test
    void shouldNotSaveDuplicateEmail() {
        createUser();

        assertThrows(DataIntegrityViolationException.class, () -> {
            createUser();
            userRepository.flush();
        });
    }
}