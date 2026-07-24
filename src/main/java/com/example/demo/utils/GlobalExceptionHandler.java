package com.example.demo.utils;

import com.example.demo.user.exceptions.DuplicatedUserException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {
    @ExceptionHandler(DuplicatedUserException.class)
    public ResponseEntity<Object> handleUserDuplicatedException(DuplicatedUserException duplicatedUserException, WebRequest request){
        StringBuilder body = new StringBuilder();
        body = body.append("Email already in use: ").append(duplicatedUserException.getMessage());
        return super.handleExceptionInternal(duplicatedUserException, body, new HttpHeaders(), HttpStatus.CONFLICT, request);
    }

}
