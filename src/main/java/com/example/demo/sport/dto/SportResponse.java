package com.example.demo.sport.dto;

import com.example.demo.sport.entity.SportType;

public record SportResponse(
        Long idSport,
        SportType sportType
) {}
