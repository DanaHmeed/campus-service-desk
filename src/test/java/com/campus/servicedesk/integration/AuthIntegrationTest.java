package com.campus.servicedesk.integration;

import com.campus.servicedesk.dto.AuthResponse;
import com.campus.servicedesk.dto.LoginRequest;
import com.campus.servicedesk.dto.RegisterRequest;
import com.campus.servicedesk.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;

    @BeforeEach
    void cleanUp() {
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("POST /api/auth/register — registers new user and returns JWT")
    void registerSuccess() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .email("alice@university.edu")
                .fullName("Alice Student")
                .password("password123")
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.email").value("alice@university.edu"))
                .andExpect(jsonPath("$.role").value("STUDENT"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    @DisplayName("POST /api/auth/register — rejects invalid email")
    void registerInvalidEmail() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .email("not-an-email")
                .fullName("Alice")
                .password("password123")
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.email").exists());
    }

    @Test
    @DisplayName("POST /api/auth/register — rejects short password")
    void registerShortPassword() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .email("alice@university.edu")
                .fullName("Alice")
                .password("short")
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.password").exists());
    }

    @Test
    @DisplayName("POST /api/auth/register — rejects duplicate email")
    void registerDuplicateEmail() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .email("alice@university.edu")
                .fullName("Alice Student")
                .password("password123")
                .build();

        // First registration
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Duplicate
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("already registered")));
    }

    @Test
    @DisplayName("POST /api/auth/login — authenticates with correct credentials")
    void loginSuccess() throws Exception {
        // Register first
        RegisterRequest reg = RegisterRequest.builder()
                .email("alice@university.edu")
                .fullName("Alice Student")
                .password("password123")
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reg)))
                .andExpect(status().isCreated());

        // Login
        LoginRequest login = LoginRequest.builder()
                .email("alice@university.edu")
                .password("password123")
                .build();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.email").value("alice@university.edu"));
    }

    @Test
    @DisplayName("POST /api/auth/login — rejects wrong password")
    void loginWrongPassword() throws Exception {
        RegisterRequest reg = RegisterRequest.builder()
                .email("alice@university.edu")
                .fullName("Alice Student")
                .password("password123")
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reg)))
                .andExpect(status().isCreated());

        LoginRequest login = LoginRequest.builder()
                .email("alice@university.edu")
                .password("wrongpassword")
                .build();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("Invalid email or password")));
    }

    @Test
    @DisplayName("Protected endpoint returns 401 without token")
    void protectedEndpointRequiresAuth() throws Exception {
        mockMvc.perform(get("/api/tickets"))
                .andExpect(status().isUnauthorized());
    }

    // Helper to register and return token
    String registerAndGetToken(String email, String name, String password, String role) throws Exception {
        RegisterRequest req = RegisterRequest.builder()
                .email(email).fullName(name).password(password).role(role).build();

        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();

        AuthResponse auth = objectMapper.readValue(
                result.getResponse().getContentAsString(), AuthResponse.class);
        return auth.getToken();
    }
}
