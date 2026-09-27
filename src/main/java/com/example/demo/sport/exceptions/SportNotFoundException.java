package com.example.demo.sport.exceptions;

import com.example.demo.sport.entity.SportType;

import java.util.Collection;
import java.util.stream.Collectors;

public class SportNotFoundException extends RuntimeException {

    private final String missingSports;

    public SportNotFoundException(Collection<SportType> missingSports) {
        super(missingSports.stream().map(Enum::name).collect(Collectors.joining(", ")));
        this.missingSports = getMessage();
    }

    public String getMissingSports() {
        return missingSports;
    }
}
