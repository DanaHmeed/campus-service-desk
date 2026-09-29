package com.campus.servicedesk.service;

import com.campus.servicedesk.dto.AuthResponse;
import com.campus.servicedesk.dto.LoginRequest;
import com.campus.servicedesk.dto.RegisterRequest;
import com.campus.servicedesk.entity.Role;
import com.campus.servicedesk.entity.User;
import com.campus.servicedesk.exception.BadRequestException;
import com.campus.servicedesk.repository.UserRepository;
import com.campus.servicedesk.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtTokenProvider tokenProvider;

    @InjectMocks private AuthService authService;

    @Nested
    @DisplayName("register()")
    class Register {

        @Test
        @DisplayName("should register a new student and return JWT")
        void registerSuccess() {
            RegisterRequest request = RegisterRequest.builder()
                    .email("alice@university.edu")
                    .fullName("Alice Student")
                    .password("password123")
                    .build();

            when(userRepository.existsByEmail("alice@university.edu")).thenReturn(false);
            when(passwordEncoder.encode("password123")).thenReturn("$2a$hashed");
            when(userRepository.save(any(User.class))).thenAnswer(inv -> {
                User u = inv.getArgument(0);
                u.setId(1L);
                u.setCreatedAt(LocalDateTime.now());
                return u;
            });
            when(tokenProvider.generateToken(eq(1L), anyString(), eq("STUDENT"))).thenReturn("jwt-token");

            AuthResponse response = authService.register(request);

            assertThat(response.getToken()).isEqualTo("jwt-token");
            assertThat(response.getRole()).isEqualTo("STUDENT");
            assertThat(response.getEmail()).isEqualTo("alice@university.edu");
            verify(passwordEncoder).encode("password123");
        }

        @Test
        @DisplayName("should reject duplicate email")
        void rejectDuplicateEmail() {
            RegisterRequest request = RegisterRequest.builder()
                    .email("alice@university.edu")
                    .fullName("Alice Student")
                    .password("password123")
                    .build();

            when(userRepository.existsByEmail("alice@university.edu")).thenReturn(true);

            assertThatThrownBy(() -> authService.register(request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("already registered");
        }

        @Test
        @DisplayName("should reject invalid role")
        void rejectInvalidRole() {
            RegisterRequest request = RegisterRequest.builder()
                    .email("alice@university.edu")
                    .fullName("Alice Student")
                    .password("password123")
                    .role("SUPERUSER")
                    .build();

            when(userRepository.existsByEmail("alice@university.edu")).thenReturn(false);

            assertThatThrownBy(() -> authService.register(request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Invalid role");
        }
    }

    @Nested
    @DisplayName("login()")
    class Login {

        private User existingUser;

        @BeforeEach
        void setUp() {
            existingUser = User.builder()
                    .id(1L)
                    .email("alice@university.edu")
                    .fullName("Alice Student")
                    .passwordHash("$2a$hashed")
                    .role(Role.STUDENT)
                    .build();
        }

        @Test
        @DisplayName("should authenticate with correct credentials")
        void loginSuccess() {
            LoginRequest request = LoginRequest.builder()
                    .email("alice@university.edu")
                    .password("password123")
                    .build();

            when(userRepository.findByEmail("alice@university.edu")).thenReturn(Optional.of(existingUser));
            when(passwordEncoder.matches("password123", "$2a$hashed")).thenReturn(true);
            when(tokenProvider.generateToken(1L, "alice@university.edu", "STUDENT")).thenReturn("jwt-token");

            AuthResponse response = authService.login(request);

            assertThat(response.getToken()).isEqualTo("jwt-token");
            assertThat(response.getUserId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("should reject wrong password")
        void rejectWrongPassword() {
            LoginRequest request = LoginRequest.builder()
                    .email("alice@university.edu")
                    .password("wrong-password")
                    .build();

            when(userRepository.findByEmail("alice@university.edu")).thenReturn(Optional.of(existingUser));
            when(passwordEncoder.matches("wrong-password", "$2a$hashed")).thenReturn(false);

            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Invalid email or password");
        }

        @Test
        @DisplayName("should reject non-existent email")
        void rejectNonExistentEmail() {
            LoginRequest request = LoginRequest.builder()
                    .email("nobody@university.edu")
                    .password("password123")
                    .build();

            when(userRepository.findByEmail("nobody@university.edu")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Invalid email or password");
        }
    }
}
