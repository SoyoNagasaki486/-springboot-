package edu.lab.service;

import edu.lab.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, passwordEncoder);
    }

    @Test
    void shouldLoginAndUpgradeLegacyPlainPassword() {
        Map<String, Object> storedUser = new LinkedHashMap<>();
        storedUser.put("id", 1L);
        storedUser.put("username", "student");
        storedUser.put("password", "1234");
        storedUser.put("real_name", "张同学");
        storedUser.put("role", "STUDENT");
        storedUser.put("qualified", true);
        when(userRepository.findByUsername("student")).thenReturn(Optional.of(storedUser));
        when(passwordEncoder.encode("1234")).thenReturn("encoded-password");

        Map<String, Object> result = authService.login("student", "1234");

        assertEquals("张同学", result.get("real_name"));
        assertFalse(result.containsKey("password"));
        verify(userRepository).updatePassword(1L, "encoded-password");
    }

    @Test
    void shouldRejectIncorrectPassword() {
        Map<String, Object> storedUser = new LinkedHashMap<>();
        storedUser.put("id", 1L);
        storedUser.put("password", "$2a$encoded");
        when(userRepository.findByUsername("student")).thenReturn(Optional.of(storedUser));
        when(passwordEncoder.matches("wrong", "$2a$encoded")).thenReturn(false);

        assertThrows(ResponseStatusException.class, () -> authService.login("student", "wrong"));
    }

    @Test
    void shouldRejectDuplicateRegistration() {
        when(userRepository.exists("student")).thenReturn(true);

        assertThrows(ResponseStatusException.class,
                () -> authService.register("student", "12345678", "张同学", "STUDENT"));
    }

    @Test
    void shouldEncodePasswordWhenRegistering() {
        when(userRepository.exists("new_user")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded-password");

        authService.register("new_user", "12345678", "新用户", "TEACHER");

        verify(userRepository).save("new_user", "encoded-password", "新用户", "TEACHER");
    }
}
