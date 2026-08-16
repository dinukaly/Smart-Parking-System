package com.spms.userservice.service;

import com.spms.userservice.dto.request.LoginRequest;
import com.spms.userservice.dto.request.LogoutRequest;
import com.spms.userservice.dto.request.RefreshTokenRequest;
import com.spms.userservice.dto.request.RegisterRequest;
import com.spms.userservice.dto.response.AuthResponse;
import com.spms.userservice.dto.response.UserResponse;

public interface AuthService {

    UserResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    AuthResponse refreshToken(RefreshTokenRequest request);

    void logout(LogoutRequest request);
}
