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

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.validation.FieldError;

import java.net.URI;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Object> handleDataIntegrityViolation(DataIntegrityViolationException ex, WebRequest request) {
        String message = ex.getMessage() != null ? ex.getMessage() : "";
        ProblemDetail problem;
        if (message.contains("users_email_format")) {
            problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "email must be a valid email address");
            problem.setTitle("Invalid email format");
            problem.setType(URI.create("http://localhost:8080/invalid-email-format"));
        } else if (message.contains("users_email_unique")) {
            problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "email is already used");
            problem.setTitle(UserErrors.userDuplicatedMessage);
            problem.setType(URI.create(UserErrors.userDuplicatedUri));
        } else if (message.contains("users_birth_date_past")) {
            problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "birthDate must be a date in the past");
            problem.setTitle("Invalid date");
            problem.setType(URI.create("http://localhost:8080/date-must-be-past"));
        } else {
            problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "Database constraint violated");
            problem.setTitle("Data integrity violation");
            problem.setType(URI.create("http://localhost:8080/data-integrity-violation"));
        }
        return super.handleExceptionInternal(ex, problem, new HttpHeaders(), problem.getStatus(), request);
    }

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
        FieldError fieldError = ex.getFieldError();
        String field = fieldError != null ? fieldError.getField() : "unknown";
        String code = (fieldError != null && fieldError.getCodes() != null)
                ? fieldError.getCodes()[fieldError.getCodes().length - 1]
                : "Unknown";

        String title;
        String detail;
        String uri;

        switch (code) {
            case "NotNull" -> {
                title = field + " is mandatory";
                detail = field + " must not be null";
                uri = "http://localhost:8080/field-is-mandatory";
            }
            case "Email" -> {
                title = "Invalid email format";
                detail = field + " must be a valid email address";
                uri = "http://localhost:8080/invalid-email-format";
            }
            case "Past" -> {
                title = "Invalid date";
                detail = field + " must be a date in the past";
                uri = "http://localhost:8080/date-must-be-past";
            }
            default -> {
                title = field + " is invalid";
                detail = field + " failed validation";
                uri = "http://localhost:8080/field-invalid";
            }
        }

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
        problemDetail.setTitle(title);
        problemDetail.setType(URI.create(uri));
        return super.handleExceptionInternal(ex, problemDetail, headers, HttpStatus.BAD_REQUEST, request);
    }
}
