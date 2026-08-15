package com.spms.userservice.service;

import com.spms.userservice.dto.request.UpdateProfileRequest;
import com.spms.userservice.dto.response.BookingLogResponse;
import com.spms.userservice.dto.response.UserProfileResponse;

import java.util.List;
import java.util.UUID;

public interface UserService {

    UserProfileResponse getUserProfile(UUID userId);

    UserProfileResponse updateUserProfile(UUID userId, UpdateProfileRequest request);

    UserProfileResponse getUserById(UUID userId);

    List<UserProfileResponse> getAllUsers();

    List<BookingLogResponse> getUserBookings(UUID userId);
}
