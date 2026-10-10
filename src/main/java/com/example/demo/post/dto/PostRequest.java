package com.example.demo.post.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record PostRequest(

        @NotNull(message = "Post must have an author")
        @Positive(message = "Author id must be positive")
        Long authorId,

        @NotBlank(message = "Post must have a title")
        @Size(max = 255, message = "Post title must be at most 255 characters")
        String title,

        @NotBlank(message = "Post must have a content")
        String content
) {}
