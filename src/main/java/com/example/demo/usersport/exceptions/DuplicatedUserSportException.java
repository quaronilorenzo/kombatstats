package com.example.demo.usersport.exceptions;

import com.example.demo.sport.entity.SportType;

public class DuplicatedUserSportException extends RuntimeException {

    public DuplicatedUserSportException(SportType sportType) {
        super(sportType.name());
    }
}
