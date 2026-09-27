package com.example.demo.user.dto;

import com.example.demo.usersport.dto.UserSportResponse;

import java.time.LocalDate;
import java.util.List;

public record UserResponse(
        Long id,
        String firstName,
        String lastName,
        LocalDate birthDate,
        List<UserSportResponse> sports
) {
}
