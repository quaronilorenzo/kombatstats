package com.example.demo.post.dto;

import java.time.Instant;

public record PostCursor(
        Instant createdAt,
        Long id
) {}
