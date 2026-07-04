package com.example.demo.controller;

import com.example.demo.dto.UserRequest;
import com.example.demo.entity.User;
import com.example.demo.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RequestMapping("users")
@RestController
public class UserController{
    @Autowired
    UserService userService;

    @PostMapping
    public User addUser(@RequestBody UserRequest request) {
        return userService.addUser(
                request.firstName,
                request.lastName,
                request.email,
                request.birthDate,
                request.sport);
    }

    @GetMapping("/hello-world")
    public void helloWorld(){
        System.out.println("Hello, World!");
    }
}
