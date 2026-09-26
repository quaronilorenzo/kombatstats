package com.example.demo.usersport.dto;

import com.example.demo.sport.entity.SportType;

import java.math.BigDecimal;

public record UserSportResponse(
        Long idUserSport,
        SportType sportType,
        BigDecimal yearsPracticed,
        boolean isCompeting
) {}
