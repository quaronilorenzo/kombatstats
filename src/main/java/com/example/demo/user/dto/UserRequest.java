package com.example.demo.user.dto;

import com.example.demo.usersport.dto.UserSportRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;

import java.time.LocalDate;
import java.util.List;

public record UserRequest(
        @NotBlank(message = "User must have a first name.")
        String firstName,
        @NotBlank(message = "User must have a last name.")
        String lastName,
        @Email(regexp = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$", message = "User must have a valid email")
        @NotBlank
        String email,
        @NotNull(message = "Birth date is required")
        @Past(message = "Birth date must be in the past")
        LocalDate birthDate,
        @Valid
        List<UserSportRequest> sports
) {}
