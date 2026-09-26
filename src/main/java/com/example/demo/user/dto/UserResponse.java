package com.example.demo.user.dto;

import java.time.LocalDate;

public record UserResponse(
        String firstName,
        String lastName,
        LocalDate birthDate
) {
}
