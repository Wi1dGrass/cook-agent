package com.fontal.cookagent.dto;

import com.fontal.cookagent.entity.User;

import java.time.LocalDateTime;

public record UserResponse(
        Long id,
        String username,
        String role,
        String phone,
        Integer enabled,
        LocalDateTime createdAt
) {
    public static UserResponse from(User u) {
        return new UserResponse(
                u.getId(), u.getUsername(), u.getRole(), u.getPhone(),
                u.getEnabled(), u.getCreatedAt());
    }
}