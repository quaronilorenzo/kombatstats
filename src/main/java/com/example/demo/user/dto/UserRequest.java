package com.example.demo.user.dto;

import com.example.demo.user.entity.Sport;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.List;

public record UserRequest(
        @NotBlank(message = "User must have a first name.")
        String firstName,
        @NotBlank(message = "User must have a last name.")
        String lastName,
        @Email(message = "User must have a valid email")
        String email,
        @NotNull(message = "Birth date is required")
        @Past(message = "Birth date must be in the past")
        LocalDate birthDate,
        List<Sport> sport
) {}
