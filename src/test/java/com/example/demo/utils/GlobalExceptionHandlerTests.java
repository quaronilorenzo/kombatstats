package com.example.demo.utils;

import com.example.demo.user.costants.UserErrors;
import com.example.demo.user.exceptions.DuplicatedUserException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.WebRequest;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTests {

    private final GlobalExceptionHandler globalExceptionHandler = new GlobalExceptionHandler();
    private final WebRequest webRequest = mock(WebRequest.class);

    // handleDataIntegrityViolation

    @Test
    void handleDataIntegrityViolation_withInvalidEmailFormat_shouldReturnBadRequest() {
        DataIntegrityViolationException ex = new DataIntegrityViolationException(UserErrors.constraintEmailFormat);

        ResponseEntity<Object> response = globalExceptionHandler.handleDataIntegrityViolation(ex, webRequest);
        ProblemDetail problem = (ProblemDetail) response.getBody();

        assertAll(
                () -> assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode()),
                () -> assertEquals(UserErrors.emailFormatMessage, problem.getTitle()),
                () -> assertEquals("email must be a valid email address", problem.getDetail()),
                () -> assertEquals(URI.create(UserErrors.emailFormatUri), problem.getType())
        );
    }

    @Test
    void handleDataIntegrityViolation_withDuplicateEmail_shouldReturnConflict() {
        DataIntegrityViolationException ex = new DataIntegrityViolationException(UserErrors.constraintEmailUnique);

        ResponseEntity<Object> response = globalExceptionHandler.handleDataIntegrityViolation(ex, webRequest);
        ProblemDetail problem = (ProblemDetail) response.getBody();

        assertAll(
                () -> assertEquals(HttpStatus.CONFLICT, response.getStatusCode()),
                () -> assertEquals(UserErrors.userDuplicatedMessage, problem.getTitle()),
                () -> assertEquals("email is already used", problem.getDetail()),
                () -> assertEquals(URI.create(UserErrors.userDuplicatedUri), problem.getType())
        );
    }

    @Test
    void handleDataIntegrityViolation_withBirthDateNotInPast_shouldReturnBadRequest() {
        DataIntegrityViolationException ex = new DataIntegrityViolationException(UserErrors.constraintBirthDatePast);

        ResponseEntity<Object> response = globalExceptionHandler.handleDataIntegrityViolation(ex, webRequest);
        ProblemDetail problem = (ProblemDetail) response.getBody();

        assertAll(
                () -> assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode()),
                () -> assertEquals(UserErrors.birthDatePastMessage, problem.getTitle()),
                () -> assertEquals("birthDate must be a date in the past", problem.getDetail()),
                () -> assertEquals(URI.create(UserErrors.birthDatePastUri), problem.getType())
        );
    }

    @Test
    void handleDataIntegrityViolation_withUnknownConstraint_shouldReturnGenericConflict() {
        DataIntegrityViolationException ex = new DataIntegrityViolationException("some_other_constraint");

        ResponseEntity<Object> response = globalExceptionHandler.handleDataIntegrityViolation(ex, webRequest);
        ProblemDetail problem = (ProblemDetail) response.getBody();

        assertAll(
                () -> assertEquals(HttpStatus.CONFLICT, response.getStatusCode()),
                () -> assertEquals("Data integrity violation", problem.getTitle()),
                () -> assertEquals("Database constraint violated", problem.getDetail()),
                () -> assertEquals(URI.create("http://localhost:8080/data-integrity-violation"), problem.getType())
        );
    }

    // ---- handleUserDuplicatedException ----

    @Test
    void handleUserDuplicatedException_shouldReturnConflictWithEmailInDetail() {
        DuplicatedUserException ex = new DuplicatedUserException("john@example.com");

        ResponseEntity<Object> response = globalExceptionHandler.handleUserDuplicatedException(ex, webRequest);
        ProblemDetail problem = (ProblemDetail) response.getBody();

        assertAll(
                () -> assertEquals(HttpStatus.CONFLICT, response.getStatusCode()),
                () -> assertEquals(UserErrors.userDuplicatedMessage, problem.getTitle()),
                () -> assertEquals("john@example.com is already used", problem.getDetail()),
                () -> assertEquals(URI.create(UserErrors.userDuplicatedUri), problem.getType())
        );
    }

    // ---- handleMethodArgumentNotValid ----

    @Test
    void handleMethodArgumentNotValid_withMissingField_shouldReturnMandatoryFieldError() {
        FieldError fieldError = new FieldError("user", "email", null, false, new String[]{"NotNull"}, null, null);
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        when(ex.getFieldError()).thenReturn(fieldError);

        ResponseEntity<Object> response = globalExceptionHandler.handleMethodArgumentNotValid(
                ex, new HttpHeaders(), HttpStatus.BAD_REQUEST, webRequest);
        ProblemDetail problem = (ProblemDetail) response.getBody();

        assertAll(
                () -> assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode()),
                () -> assertEquals("email is mandatory", problem.getTitle()),
                () -> assertEquals("email must not be null", problem.getDetail()),
                () -> assertEquals(URI.create(UserErrors.fieldMandatoryUri), problem.getType())
        );
    }

    @Test
    void handleMethodArgumentNotValid_withInvalidEmailField_shouldReturnEmailFormatError() {
        FieldError fieldError = new FieldError("user", "email", null, false, new String[]{"Email"}, null, null);
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        when(ex.getFieldError()).thenReturn(fieldError);

        ResponseEntity<Object> response = globalExceptionHandler.handleMethodArgumentNotValid(
                ex, new HttpHeaders(), HttpStatus.BAD_REQUEST, webRequest);
        ProblemDetail problem = (ProblemDetail) response.getBody();

        assertAll(
                () -> assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode()),
                () -> assertEquals(UserErrors.emailFormatMessage, problem.getTitle()),
                () -> assertEquals("email must be a valid email address", problem.getDetail()),
                () -> assertEquals(URI.create(UserErrors.emailFormatUri), problem.getType())
        );
    }

    @Test
    void handleMethodArgumentNotValid_withBirthDateNotInPast_shouldReturnPastDateError() {
        FieldError fieldError = new FieldError("user", "birthDate", null, false, new String[]{"Past"}, null, null);
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        when(ex.getFieldError()).thenReturn(fieldError);

        ResponseEntity<Object> response = globalExceptionHandler.handleMethodArgumentNotValid(
                ex, new HttpHeaders(), HttpStatus.BAD_REQUEST, webRequest);
        ProblemDetail problem = (ProblemDetail) response.getBody();

        assertAll(
                () -> assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode()),
                () -> assertEquals(UserErrors.birthDatePastMessage, problem.getTitle()),
                () -> assertEquals("birthDate must be a date in the past", problem.getDetail()),
                () -> assertEquals(URI.create(UserErrors.birthDatePastUri), problem.getType())
        );
    }

    @Test
    void handleMethodArgumentNotValid_withUnrecognizedValidationCode_shouldReturnGenericFieldError() {
        FieldError fieldError = new FieldError("user", "username", null, false, new String[]{"Size"}, null, null);
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        when(ex.getFieldError()).thenReturn(fieldError);

        ResponseEntity<Object> response = globalExceptionHandler.handleMethodArgumentNotValid(
                ex, new HttpHeaders(), HttpStatus.BAD_REQUEST, webRequest);
        ProblemDetail problem = (ProblemDetail) response.getBody();

        assertAll(
                () -> assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode()),
                () -> assertEquals("username is invalid", problem.getTitle()),
                () -> assertEquals("username failed validation", problem.getDetail()),
                () -> assertEquals(URI.create(UserErrors.fieldInvalidUri), problem.getType())
        );
    }

    @Test
    void handleMethodArgumentNotValid_withNoFieldError_shouldReturnUnknownFieldError() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        when(ex.getFieldError()).thenReturn(null);

        ResponseEntity<Object> response = globalExceptionHandler.handleMethodArgumentNotValid(
                ex, new HttpHeaders(), HttpStatus.BAD_REQUEST, webRequest);
        ProblemDetail problem = (ProblemDetail) response.getBody();

        assertAll(
                () -> assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode()),
                () -> assertEquals("unknown is invalid", problem.getTitle()),
                () -> assertEquals("unknown failed validation", problem.getDetail()),
                () -> assertEquals(URI.create(UserErrors.fieldInvalidUri), problem.getType())
        );
    }
}
