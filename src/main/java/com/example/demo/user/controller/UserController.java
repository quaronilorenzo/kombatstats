package com.example.demo.user.controller;

import com.example.demo.user.dto.UserRequest;
import com.example.demo.user.dto.UserResponse;
import com.example.demo.user.dto.mapper.UserMapper;
import com.example.demo.user.entity.User;
import com.example.demo.user.service.UserService;
import org.hibernate.query.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RequestMapping("users")
@RestController
public class UserController{

    @PostMapping
    public UserResponse addUser(@RequestBody UserRequest request) {
        User user = _userService.addUser(userMapper.userRequestToUser(request));
        return new UserResponse(user.getFirstName(), user.getLastName(), user.getBirthDate(), user.getSport());
    }

    @GetMapping("/allusers")
    public List<User> getAllUsers(){
        return _userService.findAll();
    }

    @GetMapping("/userbyid")
    public ResponseEntity<User> getUserById(@RequestParam int id){
            return _userService.findUserById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/userbyname")
    public Optional<User> getUserByName(@RequestParam String name){
        return _userService.findUserByFirstname(name);
    }

    @Autowired
    private UserService _userService;
    @Autowired
    UserMapper userMapper;
}
