package com.example.demo.user.service;

import com.example.demo.user.dto.UserRequest;
import com.example.demo.user.dto.UserResponse;
import com.example.demo.user.dto.mapper.UserMapper;
import com.example.demo.user.entity.User;
import com.example.demo.user.exceptions.DuplicatedUserException;
import com.example.demo.user.repository.UserRepository;
import com.example.demo.usersport.dto.UserSportResponse;
import com.example.demo.usersport.dto.mapper.UserSportMapper;
import com.example.demo.usersport.entity.UserSport;
import com.example.demo.usersport.service.UserSportService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;

    private final UserSportService userSportService;

    private final UserMapper userMapper;

    private final UserSportMapper userSportMapper;

    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, UserSportService userSportService, UserMapper userMapper, UserSportMapper userSportMapper, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.userSportService = userSportService;
        this.userMapper = userMapper;
        this.userSportMapper = userSportMapper;
        this.passwordEncoder = passwordEncoder;
    }


    @Transactional
    public UserResponse register(UserRequest request) {
        String hashPassword = passwordEncoder.encode(request.password());
        User user = userMapper.userRequestToUser(request);
        user.setHashPassword(hashPassword);
        User savedUser = addUser(user);
        List<UserSport> savedSports = userSportService.createForUser(savedUser, request.sports());
        List<UserSportResponse> sportResponses =
                userSportMapper.userSportsToUserSportResponses(savedSports);

        return userMapper.userToUserResponse(savedUser, sportResponses);
    }

    public User addUser(User inputUser) {
        if (userRepository.findByEmail(inputUser.getEmail()).isPresent()) {
            throw new DuplicatedUserException(inputUser.getEmail());
        }
        return userRepository.save(inputUser);
    }

    public List<User> findAll() {
        return userRepository.findAll();
    }

    public Optional<UserResponse> findUserById(Long id) {
        Optional<UserResponse> userResponse = Optional.empty();
        if(userRepository.findById(id).isPresent()){
            UserResponse user = userMapper.userToUserResponse((userRepository.findById(id).get()));
            userResponse = Optional.of(user);
        }

        return userResponse;
    }

    public Optional<User> findUserByFirstname(String name) {
        return userRepository.findByFirstName(name);
    }
}
