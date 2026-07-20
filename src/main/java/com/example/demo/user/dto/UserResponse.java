package com.example.demo.user.dto;

import com.example.demo.user.entity.Sport;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;

public record UserResponse(
        String firstName,
        String lastName,
        LocalDate birthDate,
        List<Sport> sport
) {
}
