package com.example.demo.utils;

import com.example.demo.post.costants.PostErrors;
import com.example.demo.post.exceptions.DuplicatedPostException;
import com.example.demo.sport.exceptions.SportNotFoundException;
import com.example.demo.user.costants.UserErrors;
import com.example.demo.user.exceptions.DuplicatedUserException;
import com.example.demo.user.exceptions.UserNotFoundException;
import com.example.demo.usersport.costants.UserSportErrors;
import com.example.demo.usersport.exceptions.DuplicatedUserSportException;
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
        if (message.contains(UserErrors.constraintEmailFormat)) {
            problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "email must be a valid email address");
            problem.setTitle(UserErrors.emailFormatMessage);
            problem.setType(URI.create(UserErrors.emailFormatUri));
        } else if (message.contains(UserErrors.constraintEmailUnique)) {
            problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "email is already used");
            problem.setTitle(UserErrors.userDuplicatedMessage);
            problem.setType(URI.create(UserErrors.userDuplicatedUri));
        } else if (message.contains(UserErrors.constraintBirthDatePast)) {
            problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "birthDate must be a date in the past");
            problem.setTitle(UserErrors.birthDatePastMessage);
            problem.setType(URI.create(UserErrors.birthDatePastUri));
        } else if (message.contains(UserSportErrors.constraintUserSportUnique)) {
            problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "the user already practices this sport");
            problem.setTitle(UserSportErrors.userSportDuplicatedMessage);
            problem.setType(URI.create(UserSportErrors.userSportDuplicatedUri));
        } else if (message.contains(UserSportErrors.constraintYearsPracticedCheck)) {
            problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "yearsPracticed must not be negative");
            problem.setTitle(UserSportErrors.yearsPracticedMessage);
            problem.setType(URI.create(UserSportErrors.yearsPracticedUri));
        } else {
            problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "Database constraint violated");
            problem.setTitle("Data integrity violation");
            problem.setType(URI.create("http://localhost:8080/data-integrity-violation"));
        }
        return super.handleExceptionInternal(ex, problem, new HttpHeaders(), HttpStatusCode.valueOf(problem.getStatus()), request);
    }

    @ExceptionHandler(DuplicatedUserException.class)
    public ResponseEntity<Object> handleUserDuplicatedException(DuplicatedUserException duplicatedUserException, WebRequest request){
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                duplicatedUserException.getMessage() + " is already used");
        problem.setTitle(UserErrors.userDuplicatedMessage);
        problem.setType(URI.create(UserErrors.userDuplicatedUri));
        return super.handleExceptionInternal(duplicatedUserException, problem, new HttpHeaders(), HttpStatusCode.valueOf(problem.getStatus()), request);
    }
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<Object> handleUserNotFound(UserNotFoundException ex, WebRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle(UserErrors.userNotFoundMessage);
        problem.setType(URI.create(UserErrors.userNotFoundUri));
        return super.handleExceptionInternal(ex, problem, new HttpHeaders(),
                HttpStatusCode.valueOf(problem.getStatus()), request);
    }

    @ExceptionHandler(SportNotFoundException.class)
    public ResponseEntity<Object> handleSportNotFound(SportNotFoundException ex, WebRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND,
                "unknown sport: " + ex.getMissingSports());
        problem.setTitle(UserSportErrors.sportNotFoundMessage);
        problem.setType(URI.create(UserSportErrors.sportNotFoundUri));
        return super.handleExceptionInternal(ex, problem, new HttpHeaders(),
                HttpStatusCode.valueOf(problem.getStatus()), request);
    }

    @ExceptionHandler(DuplicatedUserSportException.class)
    public ResponseEntity<Object> handleDuplicatedUserSport(DuplicatedUserSportException ex, WebRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                ex.getMessage() + " is listed more than once");
        problem.setTitle(UserSportErrors.userSportDuplicatedMessage);
        problem.setType(URI.create(UserSportErrors.userSportDuplicatedUri));
        return super.handleExceptionInternal(ex, problem, new HttpHeaders(),
                HttpStatusCode.valueOf(problem.getStatus()), request);
    }

    @ExceptionHandler(DuplicatedPostException.class)
    public ResponseEntity<Object> handleDuplicatedPost(DuplicatedPostException ex, WebRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                "a post with title " + ex.getMessage() + " already exists");
        problem.setTitle(PostErrors.postDuplicatedMessage);
        problem.setType(URI.create(PostErrors.postDuplicatedUri));
        return super.handleExceptionInternal(ex, problem, new HttpHeaders(),
                HttpStatusCode.valueOf(problem.getStatus()), request);
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
                uri = UserErrors.fieldMandatoryUri;
            }
            case "Email" -> {
                title = UserErrors.emailFormatMessage;
                detail = field + " must be a valid email address";
                uri = UserErrors.emailFormatUri;
            }
            case "Past" -> {
                title = UserErrors.birthDatePastMessage;
                detail = field + " must be a date in the past";
                uri = UserErrors.birthDatePastUri;
            }
            default -> {
                title = field + " is invalid";
                detail = field + " failed validation";
                uri = UserErrors.fieldInvalidUri;
            }
        }

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
        problemDetail.setTitle(title);
        problemDetail.setType(URI.create(uri));
        return super.handleExceptionInternal(ex, problemDetail, headers, HttpStatus.BAD_REQUEST, request);
    }
}
