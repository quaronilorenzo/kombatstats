package com.example.demo.user.service;

import com.example.demo.security.JwtService;
import com.example.demo.user.costants.AuthCostants;
import com.example.demo.user.dto.LoginRequest;
import com.example.demo.user.dto.LoginResponse;
import com.example.demo.user.entity.User;
import com.example.demo.user.repository.UserRepository;
import org.apache.coyote.BadRequestException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.security.auth.login.CredentialException;
import java.util.Optional;

@Service
public class LoginService {
    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    private final JwtService jwtService;

    public LoginService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public LoginResponse login(LoginRequest loginRequest) throws CredentialException {
        Optional<User> user = userRepository.findByEmail(loginRequest.email());
        // Timing attack prevention
        String userPassword = user
                .isPresent() ? user.get().getHashPassword() : AuthCostants.FAKE_PASSWORD;
        if(!passwordEncoder.matches(loginRequest.password(), userPassword)) {
            throw new CredentialException();
        }
        return new LoginResponse(jwtService.generateToken(user.get()));
    }
}
