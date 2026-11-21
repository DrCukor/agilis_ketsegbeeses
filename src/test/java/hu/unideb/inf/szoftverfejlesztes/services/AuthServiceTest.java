package hu.unideb.inf.szoftverfejlesztes.services;

import hu.unideb.inf.szoftverfejlesztes.model.User;
import hu.unideb.inf.szoftverfejlesztes.repository.UserRepository;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {
    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @InjectMocks
    private AuthService authService;

    @Test
    void register_ShouldRejectInvalidEmail(){
        assertThrows(IllegalArgumentException.class,() -> authService.register("valami.test","Józska","asdasd"));
        verify(userRepository,never()).save(any());

    }
    @Test
    void register_shouldRejectEmptyPassword() {
        assertThrows(IllegalArgumentException.class,
                () -> authService.register(
                        "adam@gmail.com",
                        "Ádám",
                        ""
                ));

        verify(userRepository, never()).save(any());
    }

    @Test
    void register_shouldRejectDuplicateEmail() {
        when(userRepository.existsByEmail("adam@gmail.com"))
                .thenReturn(true);

        assertThrows(IllegalArgumentException.class,
                () -> authService.register(
                        "adam@gmail.com",
                        "Ádám",
                        "password123"
                ));

        verify(userRepository, never()).save(any());
    }

    @Test
    void login_shouldReturnUserForValidCredentials() {
        User user = new User(
                "adam@gmail.com",
                "Ádám",
                "hashedPassword"
        );

        when(userRepository.findByEmail("adam@gmail.com"))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches(
                "password123", "hashedPassword"
        )).thenReturn(true);

        Optional<User> result =
                authService.login("adam@gmail.com", "password123");

        assertTrue(result.isPresent());
        assertEquals("Ádám", result.get().getName());
    }

    @Test
    void login_shouldFailForWrongPassword() {
        User user = new User(
                "adam@gmail.com",
                "Ádám",
                "hashedPassword"
        );

        when(userRepository.findByEmail("adam@gmail.com"))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches(
                "wrongPassword", "hashedPassword"
        )).thenReturn(false);

        Optional<User> result =
                authService.login("adam@gmail.com", "wrongPassword");

        assertTrue(result.isEmpty());
    }

}
