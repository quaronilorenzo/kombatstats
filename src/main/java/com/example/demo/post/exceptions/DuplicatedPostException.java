package com.example.demo.post.exceptions;

public class DuplicatedPostException extends RuntimeException {
    public DuplicatedPostException(String title) {
        super(title);
    }
}
