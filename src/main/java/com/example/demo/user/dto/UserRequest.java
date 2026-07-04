package com.example.demo.user.dto;

import com.example.demo.user.entity.Sport;

import java.util.Date;
import java.util.List;

public record UserRequest() {
     public static String firstName;
     public static String lastName;
     public static String email;
     public static Date birthDate;
     public static List<Sport> sport;
}
