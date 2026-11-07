package com.example.demo.post.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// No authorId: the author is the authenticated user (see PostController.addPost).
// A client still sending authorId is not rejected, Jackson just ignores the unknown field.
public record PostRequest(

        @NotBlank(message = "Post must have a title")
        @Size(max = 255, message = "Post title must be at most 255 characters")
        String title,

        @NotBlank(message = "Post must have a content")
        String content
) {}
