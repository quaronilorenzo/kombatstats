package com.example.demo.post.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PostUpdateRequest(

        @NotNull(message = "Post id is required")
        Long id,

        @Size(max = 255, message = "Post title must be at most 255 characters")
        @Pattern(regexp = ".*\\S.*", message = "Post title must not be blank")
        String title,

        @Pattern(regexp = "(?s).*\\S.*", message = "Post content must not be blank")
        String content
) {}
