package com.example.demo.user.dto;

import com.example.demo.user.entity.Role;
import com.example.demo.usersport.dto.UserSportResponse;

import java.time.LocalDate;
import java.util.List;

public record UserResponse(
        Long id,
        String firstName,
        String lastName,
        LocalDate birthDate,
        Role role,
        List<UserSportResponse> sports
) {
}
