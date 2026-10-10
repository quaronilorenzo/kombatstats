package com.example.demo.user.controller;

import com.example.demo.user.dto.UserRequest;
import com.example.demo.user.dto.UserResponse;
import com.example.demo.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RequestMapping("users")
@RestController
public class UserController {

    private final UserService _userService;

    public UserController(UserService userService) {
        this._userService = userService;
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
        return _userService.findAll();
    }

    @GetMapping("/userbyid")
    public ResponseEntity<UserResponse> getUserById(@RequestParam Long id) {
        return ResponseEntity.ok(_userService.findUserById(id));
    }

    /**
     * Looks up users by first name.
     * <p>
     * An unknown first name answers 404, not {@code 200 []}: this endpoint identifies users by name, so a name
     * that matches nobody is a missing resource. This differs from {@link #getAllUsers()}, where an empty list
     * is a valid result. Clients must therefore treat 404 here as a normal "no match" case, not as a failure.
     * <p>
     * The 404 is raised by {@code UserService} ({@code UserNotFoundException}) and mapped to a
     * {@code ProblemDetail} by {@code GlobalExceptionHandler}; this method always returns 200 on its own.
     */
    @GetMapping("/userbyname")
    public ResponseEntity<List<UserResponse>> getUserByName(@RequestParam String name) {
        return ResponseEntity.ok(_userService.findUserByFirstname(name));
    }
}
