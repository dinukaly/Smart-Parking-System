package com.spms.userservice.service;

import com.spms.userservice.dto.request.UpdateProfileRequest;
import com.spms.userservice.dto.response.BookingLogResponse;
import com.spms.userservice.dto.response.UserProfileResponse;
import com.spms.userservice.exception.ResourceNotFoundException;
import com.spms.userservice.model.entity.BookingLog;
import com.spms.userservice.model.entity.User;
import com.spms.userservice.repository.BookingLogRepository;
import com.spms.userservice.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final BookingLogRepository bookingLogRepository;

    public UserServiceImpl(UserRepository userRepository, BookingLogRepository bookingLogRepository) {
        this.userRepository = userRepository;
        this.bookingLogRepository = bookingLogRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getUserProfile(UUID userId) {
        User user = findUserById(userId);
        return mapToUserProfileResponse(user);
    }

    @Override
    @Transactional
    public UserProfileResponse updateUserProfile(UUID userId, UpdateProfileRequest request) {
        User user = findUserById(userId);

        if (request.getFirstName() != null && !request.getFirstName().isBlank()) {
            user.setFirstName(request.getFirstName().trim());
        }
        if (request.getLastName() != null && !request.getLastName().isBlank()) {
            user.setLastName(request.getLastName().trim());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone().trim());
        }

        User updatedUser = userRepository.save(user);
        return mapToUserProfileResponse(updatedUser);
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getUserById(UUID userId) {
        User user = findUserById(userId);
        return mapToUserProfileResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserProfileResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::mapToUserProfileResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingLogResponse> getUserBookings(UUID userId) {
        // Ensure user exists
        findUserById(userId);

        List<BookingLog> logs = bookingLogRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return logs.stream()
                .map(this::mapToBookingLogResponse)
                .collect(Collectors.toList());
    }

    private User findUserById(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
    }

    private UserProfileResponse mapToUserProfileResponse(User user) {
        return UserProfileResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole())
                .phone(user.getPhone())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    private BookingLogResponse mapToBookingLogResponse(BookingLog log) {
        return BookingLogResponse.builder()
                .id(log.getId())
                .reservationId(log.getReservationId())
                .action(log.getAction())
                .createdAt(log.getCreatedAt())
                .build();
    }
}
