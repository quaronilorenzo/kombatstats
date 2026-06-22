package com.example.demo.controller;

import com.example.demo.entity.Sport;
import com.example.demo.entity.User;
import com.example.demo.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;

@RequestMapping("users")
@RestController
public class UserController{
    @Autowired
    UserService userService;

    @PostMapping
    public User addUser(
            @RequestParam("firstName") String firstName,
            @RequestParam("lastName") String lastName,
            @RequestParam("email") String email,
            @RequestParam("birthDate")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            Date birthDate,
            @RequestParam("sport") List<Sport> sport) {
        return userService.addUser(firstName, lastName, email, birthDate, sport);
    }
    @GetMapping("/hello-world")
    public void helloWorld(){
        System.out.println("Hello, World!");
    }
}
