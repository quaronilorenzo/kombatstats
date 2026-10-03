package com.example.demo.user.controller;

import com.example.demo.user.dto.UserRequest;
import com.example.demo.user.dto.UserResponse;
import com.example.demo.user.dto.mapper.UserMapper;
import com.example.demo.user.entity.User;
import com.example.demo.user.service.UserService;
import jakarta.validation.Valid;
import org.apache.coyote.Response;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RequestMapping("users")
@RestController
public class UserController {

    private final UserService _userService;
    private final UserMapper userMapper;

    public UserController(UserService userService, UserMapper userMapper) {
        this._userService = userService;
        this.userMapper = userMapper;
    }

    @PostMapping
    public ResponseEntity<UserResponse> addUser(@RequestBody @Valid UserRequest request) {
        UserResponse userResponse = _userService.register(request);

        URI location = ServletUriComponentsBuilder.fromCurrentRequestUri()
                .path("/userbyid")
                .queryParam("id", userResponse.id())
                .build()
                .toUri();
        return ResponseEntity.created(location).body(userResponse);
    }

    @GetMapping("/allusers")
    public List<UserResponse> getAllUsers() {
        List<UserResponse> allUsersResponse = new ArrayList<>();
        _userService.findAll().forEach(user -> allUsersResponse.add(userMapper.userToUserResponse(user)));
        return allUsersResponse;
    }

    @GetMapping("/userbyid")
    public ResponseEntity<User> getUserById(@RequestParam Long id) {
        return _userService.findUserById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/userbyname")
    public ResponseEntity<User> getUserByName(@RequestParam String name) {
        return _userService.findUserByFirstname(name).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }
}
