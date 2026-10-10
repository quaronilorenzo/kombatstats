package com.example.demo.post.dto;

import java.time.Instant;

public record PostResponse(
        Long id,
        Long authorId,
        String title,
        String content,
        Instant createdAt
) {}
