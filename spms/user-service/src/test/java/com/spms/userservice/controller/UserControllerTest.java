package com.spms.userservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.spms.userservice.dto.request.UpdateProfileRequest;
import com.spms.userservice.dto.response.BookingLogResponse;
import com.spms.userservice.dto.response.UserProfileResponse;
import com.spms.userservice.exception.GlobalExceptionHandler;
import com.spms.userservice.exception.ResourceNotFoundException;
import com.spms.userservice.model.entity.enums.BookingAction;
import com.spms.userservice.model.entity.enums.Role;
import com.spms.userservice.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.security.Principal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    private UUID userId;
    private Principal principal;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(userController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();

        userId = UUID.randomUUID();
        principal = new UsernamePasswordAuthenticationToken(
                userId.toString(),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_DRIVER"))
        );
    }

    @Test
    void shouldGetCurrentUserProfile() throws Exception {
        UserProfileResponse response = UserProfileResponse.builder()
                .id(userId)
                .email("john@example.com")
                .firstName("John")
                .lastName("Doe")
                .role(Role.DRIVER)
                .phone("+94771234567")
                .createdAt(Instant.now())
                .build();

        when(userService.getUserProfile(userId)).thenReturn(response);

        mockMvc.perform(get("/api/users/profile")
                        .principal(principal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId.toString()))
                .andExpect(jsonPath("$.email").value("john@example.com"))
                .andExpect(jsonPath("$.role").value("DRIVER"));
    }

    @Test
    void shouldUpdateCurrentUserProfile() throws Exception {
        UpdateProfileRequest request = UpdateProfileRequest.builder()
                .firstName("Johnny")
                .lastName("Doe")
                .phone("+94779999999")
                .build();

        UserProfileResponse response = UserProfileResponse.builder()
                .id(userId)
                .email("john@example.com")
                .firstName("Johnny")
                .lastName("Doe")
                .role(Role.DRIVER)
                .phone("+94779999999")
                .updatedAt(Instant.now())
                .build();

        when(userService.updateUserProfile(eq(userId), any(UpdateProfileRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/users/profile")
                        .principal(principal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Johnny"))
                .andExpect(jsonPath("$.phone").value("+94779999999"));
    }

    @Test
    void shouldGetUserById() throws Exception {
        UserProfileResponse response = UserProfileResponse.builder()
                .id(userId)
                .email("john@example.com")
                .firstName("John")
                .lastName("Doe")
                .role(Role.DRIVER)
                .build();

        when(userService.getUserById(userId)).thenReturn(response);

        mockMvc.perform(get("/api/users/" + userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId.toString()))
                .andExpect(jsonPath("$.email").value("john@example.com"));
    }

    @Test
    void shouldReturn404WhenUserNotFound() throws Exception {
        UUID nonExistentId = UUID.randomUUID();

        when(userService.getUserById(nonExistentId))
                .thenThrow(new ResourceNotFoundException("User not found with id: " + nonExistentId));

        mockMvc.perform(get("/api/users/" + nonExistentId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("User not found with id: " + nonExistentId));
    }

    @Test
    void shouldGetAllUsers() throws Exception {
        UserProfileResponse user1 = UserProfileResponse.builder()
                .id(UUID.randomUUID())
                .email("user1@example.com")
                .firstName("User")
                .lastName("One")
                .role(Role.DRIVER)
                .build();

        UserProfileResponse user2 = UserProfileResponse.builder()
                .id(UUID.randomUUID())
                .email("admin@example.com")
                .firstName("Admin")
                .lastName("User")
                .role(Role.ADMIN)
                .build();

        when(userService.getAllUsers()).thenReturn(List.of(user1, user2));

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].email").value("user1@example.com"))
                .andExpect(jsonPath("$[1].role").value("ADMIN"));
    }

    @Test
    void shouldGetUserBookings() throws Exception {
        BookingLogResponse log1 = BookingLogResponse.builder()
                .id(UUID.randomUUID())
                .reservationId("res-101")
                .action(BookingAction.RESERVED)
                .createdAt(Instant.now())
                .build();

        BookingLogResponse log2 = BookingLogResponse.builder()
                .id(UUID.randomUUID())
                .reservationId("res-101")
                .action(BookingAction.COMPLETED)
                .createdAt(Instant.now())
                .build();

        when(userService.getUserBookings(userId)).thenReturn(List.of(log1, log2));

        mockMvc.perform(get("/api/users/bookings")
                        .principal(principal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].reservationId").value("res-101"))
                .andExpect(jsonPath("$[0].action").value("RESERVED"))
                .andExpect(jsonPath("$[1].action").value("COMPLETED"));
    }
}
