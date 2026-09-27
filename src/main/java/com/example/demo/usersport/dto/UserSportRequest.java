package com.example.demo.usersport.dto;

import com.example.demo.sport.entity.SportType;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record UserSportRequest(

        @NotNull(message = "User sport must reference a sport")
        SportType sportType,

        @PositiveOrZero(message = "Years practiced must not be negative")
        @Digits(integer = 3, fraction = 1, message = "Years practiced allows at most one decimal digit")
        BigDecimal yearsPracticed,

        boolean isCompeting
) {}
