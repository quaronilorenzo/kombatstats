package com.example.demo.user.costants;

public interface UserErrors {
    String userDuplicatedUri = "https://localhost:8080/errors/duplicate-user";
    String userDuplicatedMessage = "Email already in use";

    String userNotFoundUri = "http://localhost:8080/errors/user-not-found";
    String userNotFoundMessage = "User not found";

    String constraintEmailFormat = "users_email_format";
    String constraintEmailUnique = "users_email_unique";
    String constraintBirthDatePast = "users_birth_date_past";

    String emailFormatUri = "http://localhost:8080/invalid-email-format";
    String emailFormatMessage = "Invalid email format";

    String birthDatePastUri = "http://localhost:8080/date-must-be-past";
    String birthDatePastMessage = "Invalid date";

    String fieldMandatoryUri = "http://localhost:8080/field-is-mandatory";
    String fieldInvalidUri = "http://localhost:8080/field-invalid";
}
