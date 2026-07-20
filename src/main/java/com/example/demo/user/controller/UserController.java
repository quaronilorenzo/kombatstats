package com.example.demo.user.controller;

import com.example.demo.user.dto.UserRequest;
import com.example.demo.user.dto.UserResponse;
import com.example.demo.user.dto.mapper.UserMapper;
import com.example.demo.user.entity.User;
import com.example.demo.user.service.UserService;
import jakarta.validation.Valid;
import org.apache.coyote.Response;
import org.hibernate.query.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RequestMapping("users")
@RestController
public class UserController{

    @PostMapping
    public ResponseEntity<UserResponse> addUser(@RequestBody @Valid UserRequest request) {
        User savedUser = _userService.addUser(userMapper.userRequestToUser(request));
        UserResponse userResponse = userMapper.userToUserResponse(savedUser);
        return new ResponseEntity<UserResponse>(userResponse, HttpStatus.CREATED);
    }

    @GetMapping("/allusers")
    public List<UserResponse> getAllUsers(){
        List<UserResponse> allUsersResponse = new ArrayList<>();
        _userService.findAll().forEach(user -> { allUsersResponse.add(userMapper.userToUserResponse(user));});
        return allUsersResponse;
    }

    @GetMapping("/userbyid")
    public ResponseEntity<User> getUserById(@RequestParam int id){
            return _userService.findUserById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/userbyname")
    public ResponseEntity<User> getUserByName(@RequestParam String name){
        return _userService.findUserByFirstname(name).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @Autowired
    private UserService _userService;
    @Autowired
    UserMapper userMapper;
}
