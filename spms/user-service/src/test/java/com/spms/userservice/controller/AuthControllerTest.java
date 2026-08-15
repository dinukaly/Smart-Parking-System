package com.spms.userservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.spms.userservice.dto.request.LoginRequest;
import com.spms.userservice.dto.request.LogoutRequest;
import com.spms.userservice.dto.request.RefreshTokenRequest;
import com.spms.userservice.dto.request.RegisterRequest;
import com.spms.userservice.dto.response.AuthResponse;
import com.spms.userservice.dto.response.UserResponse;
import com.spms.userservice.exception.GlobalExceptionHandler;
import com.spms.userservice.exception.InvalidTokenException;
import com.spms.userservice.exception.UserAlreadyExistsException;
import com.spms.userservice.model.entity.enums.Role;
import com.spms.userservice.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(authController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void shouldRegisterUserSuccessfully() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .email("john@example.com")
                .password("SecurePass123!")
                .firstName("John")
                .lastName("Doe")
                .role(Role.DRIVER)
                .phone("+94771234567")
                .build();

        UserResponse response = UserResponse.builder()
                .id(UUID.randomUUID())
                .email("john@example.com")
                .firstName("John")
                .lastName("Doe")
                .role(Role.DRIVER)
                .createdAt(Instant.now())
                .build();

        when(authService.register(any(RegisterRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.email").value("john@example.com"))
                .andExpect(jsonPath("$.role").value("DRIVER"));
    }

    @Test
    void shouldFailRegisterWhenEmailIsDuplicate() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .email("duplicate@example.com")
                .password("SecurePass123!")
                .firstName("John")
                .lastName("Doe")
                .build();

        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new UserAlreadyExistsException("User with email duplicate@example.com already exists"));

        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("User with email duplicate@example.com already exists"));
    }

    @Test
    void shouldFailRegisterWhenValidationFails() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .email("invalid-email-format")
                .password("123") // too short
                .build();

        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void shouldLoginSuccessfully() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .email("john@example.com")
                .password("SecurePass123!")
                .build();

        AuthResponse response = AuthResponse.builder()
                .accessToken("mock-access-token")
                .refreshToken("mock-refresh-token")
                .expiresIn(900)
                .user(AuthResponse.UserSummary.builder()
                        .id(UUID.randomUUID())
                        .email("john@example.com")
                        .role(Role.DRIVER)
                        .build())
                .build();

        when(authService.login(any(LoginRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("mock-access-token"))
                .andExpect(jsonPath("$.refreshToken").value("mock-refresh-token"))
                .andExpect(jsonPath("$.user.email").value("john@example.com"));
    }

    @Test
    void shouldFailLoginWhenCredentialsAreWrong() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .email("john@example.com")
                .password("WrongPassword")
                .build();

        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        mockMvc.perform(post("/api/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    void shouldRefreshTokenSuccessfully() throws Exception {
        RefreshTokenRequest request = RefreshTokenRequest.builder()
                .refreshToken("valid-refresh-token")
                .build();

        AuthResponse response = AuthResponse.builder()
                .accessToken("new-mock-access-token")
                .refreshToken("rotated-mock-refresh-token")
                .expiresIn(900)
                .build();

        when(authService.refreshToken(any(RefreshTokenRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/users/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new-mock-access-token"))
                .andExpect(jsonPath("$.refreshToken").value("rotated-mock-refresh-token"));
    }

    @Test
    void shouldFailRefreshTokenWhenTokenIsExpiredOrInvalid() throws Exception {
        RefreshTokenRequest request = RefreshTokenRequest.builder()
                .refreshToken("expired-token")
                .build();

        when(authService.refreshToken(any(RefreshTokenRequest.class)))
                .thenThrow(new InvalidTokenException("Invalid or expired refresh token"));

        mockMvc.perform(post("/api/users/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Invalid or expired refresh token"));
    }

    @Test
    void shouldLogoutSuccessfully() throws Exception {
        LogoutRequest request = LogoutRequest.builder()
                .refreshToken("mock-refresh-token")
                .build();

        doNothing().when(authService).logout(any(LogoutRequest.class));

        mockMvc.perform(post("/api/users/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Logged out successfully"));
    }
}
