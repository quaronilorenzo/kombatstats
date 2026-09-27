package com.example.demo.usersport.costants;

public interface UserSportErrors {
    String constraintUserSportUnique = "user_sport_user_sport_unique";
    String constraintYearsPracticedCheck = "user_sport_years_practiced_check";

    String sportNotFoundUri = "http://localhost:8080/errors/sport-not-found";
    String sportNotFoundMessage = "Unknown sport";

    String userSportDuplicatedUri = "http://localhost:8080/errors/duplicate-user-sport";
    String userSportDuplicatedMessage = "Sport listed more than once";

    String yearsPracticedUri = "http://localhost:8080/errors/invalid-years-practiced";
    String yearsPracticedMessage = "Invalid years practiced";
}
