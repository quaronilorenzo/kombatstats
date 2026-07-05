package com.example.demo.user.dto;

import com.example.demo.user.entity.Sport;

import java.util.Date;
import java.util.List;

public record UserRequest(
        String firstName,
        String lastName,
        String email,
        Date birthDate,
        List<Sport> sport
) {}
