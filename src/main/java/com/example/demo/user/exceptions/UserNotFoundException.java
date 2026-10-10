package com.example.demo.user.exceptions;

public class UserNotFoundException extends RuntimeException {

    private UserNotFoundException(String message) {
        super(message);
    }

    public static UserNotFoundException byId(Long id) {
        return new UserNotFoundException("user with id " + id + " not found");
    }

    public static UserNotFoundException byFirstName(String firstName) {
        return new UserNotFoundException("user with first name " + firstName + " not found");
    }
}
