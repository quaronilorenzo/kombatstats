package com.example.demo.user.exceptions;

public class UnauthenticatedUserException extends RuntimeException {

    public UnauthenticatedUserException() {
        super("no authenticated user for this request");
    }
}
