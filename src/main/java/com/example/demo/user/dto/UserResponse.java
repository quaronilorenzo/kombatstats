package com.example.demo.user.dto;

import com.example.demo.user.entity.Sport;

import java.util.Date;
import java.util.List;

public record UserResponse(
        String firstName,
        String lastName,
        Date birthDate,
        List<Sport> sport
) {
}
