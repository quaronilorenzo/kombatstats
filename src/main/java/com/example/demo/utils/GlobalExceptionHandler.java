package com.example.demo.utils;

import com.example.demo.user.costants.UserErrors;
import com.example.demo.user.exceptions.DuplicatedUserException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.net.URI;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {
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
}
