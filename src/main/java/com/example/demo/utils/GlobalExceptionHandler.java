package com.example.demo.utils;

import com.example.demo.user.costants.UserErrors;
import com.example.demo.user.exceptions.DuplicatedUserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.net.URI;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(DuplicatedUserException.class)
    public ResponseEntity<Object> handleUserDuplicatedException(DuplicatedUserException duplicatedUserException, WebRequest request){
        // RFC 9457 Problem Details for HTTP APIs
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                duplicatedUserException.getMessage() + " is already used");
        problem.setTitle(UserErrors.userDuplicatedMessage);
        problem.setType(URI.create(UserErrors.userDuplicatedUri));
        return super.handleExceptionInternal(duplicatedUserException, problem, new HttpHeaders(), HttpStatus.CONFLICT, request);
    }
    @Override
    public ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, String.valueOf(ex.getFieldError()));
        problemDetail.setTitle(String.valueOf(ex.getFieldError().getField() + " is mandatory"));
        problemDetail.setType(URI.create("http://localhost:8080/argument-is-mandatory"));
        return super.handleExceptionInternal(ex, problemDetail, headers, HttpStatus.BAD_REQUEST, request);
    }
}
