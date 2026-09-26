package com.example.demo.sport.dto;

import com.example.demo.sport.entity.SportType;
import jakarta.validation.constraints.NotNull;

public record SportRequest(
        @NotNull(message = "Sport must have a type")
        SportType sportType
) {}
