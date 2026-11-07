package com.example.demo.user.controller;

import com.example.demo.user.dto.LoginRequest;
import com.example.demo.user.dto.LoginResponse;
import com.example.demo.user.dto.UserRequest;
import com.example.demo.user.dto.UserResponse;
import com.example.demo.user.service.LoginService;
import com.example.demo.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
@RequestMapping("auth")
@RestController
public class AuthController {
    private final UserService _userService;
    private final LoginService _loginService;

    public AuthController(UserService _userService, LoginService _loginService) {
        this._userService = _userService;
        this._loginService = _loginService;
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> addUser(@RequestBody @Valid UserRequest request) {
        UserResponse userResponse = _userService.register(request);

        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/users/userbyid")
                .queryParam("id", userResponse.id())
                .build()
                .toUri();
        return ResponseEntity.created(location).body(userResponse);
    }
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody @Valid LoginRequest loginRequest) {
        return ResponseEntity.ok().body(_loginService.login(loginRequest));
    }

}
