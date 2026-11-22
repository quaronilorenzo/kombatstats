package com.example.demo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@ConfigurationProperties(prefix = "app.bootstrap.admin")
public record AdminBootstrapProperties(
        String email,
        String password,
        String firstName,
        String lastName,
        @DateTimeFormat(pattern = "MM-dd-yyyy")
        LocalDate birthDate
) {}
