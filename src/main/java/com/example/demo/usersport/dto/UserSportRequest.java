package com.example.demo.usersport.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record UserSportRequest(
        @NotNull(message = "User sport must reference a sport")
        Long idSport,
        @PositiveOrZero(message = "Years practiced must not be negative")
        BigDecimal yearsPracticed,
        boolean isCompeting
) {}
