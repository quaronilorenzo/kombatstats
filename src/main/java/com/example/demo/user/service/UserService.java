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
    public UserService(UserRepository userRepository,
                       UserSportService userSportService,
                       UserMapper userMapper,
                       UserSportMapper userSportMapper) {
        this.userRepository = userRepository;
        this.userSportService = userSportService;
        this.userMapper = userMapper;
        this.userSportMapper = userSportMapper;
    }

    @Transactional
    public UserResponse register(UserRequest request) {
        User savedUser = addUser(userMapper.userRequestToUser(request));

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

    public Optional<User> findUserById(Long id) {
        return userRepository.findById(id);
    }

    public Optional<User> findUserByFirstname(String name) {
        return userRepository.findByFirstName(name);
    }
}
