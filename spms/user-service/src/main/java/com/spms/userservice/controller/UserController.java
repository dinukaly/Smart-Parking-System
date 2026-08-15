package com.spms.userservice.controller;

import com.spms.userservice.dto.request.UpdateProfileRequest;
import com.spms.userservice.dto.response.BookingLogResponse;
import com.spms.userservice.dto.response.UserProfileResponse;
import com.spms.userservice.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@Tag(name = "User Management", description = "Endpoints for managing user profiles and booking history")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/profile")
    @Operation(summary = "Get current user profile", description = "Retrieves profile details of the authenticated user")
    public ResponseEntity<UserProfileResponse> getCurrentProfile(Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        UserProfileResponse response = userService.getUserProfile(userId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/profile")
    @Operation(summary = "Update current user profile", description = "Updates firstName, lastName, or phone for the authenticated user")
    public ResponseEntity<UserProfileResponse> updateCurrentProfile(
            Authentication authentication,
            @Valid @RequestBody UpdateProfileRequest request
    ) {
        UUID userId = UUID.fromString(authentication.getName());
        UserProfileResponse response = userService.updateUserProfile(userId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or authentication.name == #id.toString()")
    @Operation(summary = "Get user by ID", description = "Retrieves user details by ID (Admin or own user)")
    public ResponseEntity<UserProfileResponse> getUserById(@PathVariable UUID id) {
        UserProfileResponse response = userService.getUserById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List all users", description = "Retrieves all users in the system (Admin only)")
    public ResponseEntity<List<UserProfileResponse>> getAllUsers() {
        List<UserProfileResponse> response = userService.getAllUsers();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/bookings")
    @Operation(summary = "Get user booking history", description = "Retrieves booking logs and reservation history for the authenticated user")
    public ResponseEntity<List<BookingLogResponse>> getUserBookings(Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        List<BookingLogResponse> response = userService.getUserBookings(userId);
        return ResponseEntity.ok(response);
    }
}
