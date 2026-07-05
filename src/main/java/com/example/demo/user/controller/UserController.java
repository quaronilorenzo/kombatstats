package com.example.demo.user.controller;

import com.example.demo.user.dto.UserRequest;
import com.example.demo.user.entity.User;
import com.example.demo.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RequestMapping("users")
@RestController
public class UserController{
    @Autowired
    UserService userService;

    @PostMapping
    public User addUser(@RequestBody UserRequest request) {
        return userService.addUser(
                request.firstName(),
                request.lastName(),
                request.email(),
                request.birthDate(),
                request.sport());
    }

    @GetMapping("/allusers")
    public List<User> getAllUsers(){
        return userService.findAll();
    }

    @GetMapping("/userbyid")
    public Optional<User> getUserById(@RequestParam int id){
        return userService.findUserById(id);
    }
}
