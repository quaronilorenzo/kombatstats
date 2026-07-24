package com.example.demo.user.exceptions;

public class DuplicatedUserException extends RuntimeException {
    public DuplicatedUserException(String email) {
        super(email);
    }
}
