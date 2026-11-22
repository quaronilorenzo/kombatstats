package com.example.demo.user.service;

import com.example.demo.sport.entity.Sport;
import com.example.demo.config.AdminBootstrapProperties;
import com.example.demo.user.dto.UserRequest;
import com.example.demo.user.dto.UserResponse;
import com.example.demo.user.dto.mapper.UserMapper;
import com.example.demo.user.entity.Role;
import com.example.demo.user.entity.User;
import com.example.demo.user.exceptions.DuplicatedUserException;
import com.example.demo.user.exceptions.UnauthenticatedUserException;
import com.example.demo.user.exceptions.UserNotFoundException;
import com.example.demo.user.repository.UserRepository;
import com.example.demo.usersport.dto.UserSportResponse;
import com.example.demo.usersport.dto.mapper.UserSportMapper;
import com.example.demo.usersport.entity.UserSport;
import com.example.demo.usersport.service.UserSportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private static final Pattern PASSWORD_POLICY =
            Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9])\\S{12,72}$");

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
        user.setEmail(request.email().trim().toLowerCase());
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

    public List<UserResponse> findAll() {
        List<User> allUsers = userRepository.findAll();
        List<UserResponse> allUsersResponse = new ArrayList<>();
        allUsers.forEach(user -> {
            UserResponse userResponse = userMapper.userToUserResponse(user, getSportsForUser(user));
            allUsersResponse.add(userResponse);
        });
        return allUsersResponse;
    }

    public UserResponse findUserById(Long id) {
        User user = getUserById(id);
        return userMapper.userToUserResponse(user, getSportsForUser(user));
    }

    public UserResponse findCurrentUser(Long userId) {
        if (userId == null) {
            throw new UnauthenticatedUserException();
        }
        return findUserById(userId);
    }

    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> UserNotFoundException.byId(id));
    }

    /**
     * Finds every user with the given first name (case-insensitive).
     *
     * @throws UserNotFoundException when no user matches. An empty result is deliberately treated as an error
     *                               instead of returning an empty list, because the caller asks for a user by
     *                               name and a name nobody has is a missing resource (mapped to 404).
     */
    public List<UserResponse> findUserByFirstname(String name) {
        List<User> users = userRepository.findByFirstNameIgnoreCase(name);
        if (users.isEmpty()) {
            throw UserNotFoundException.byFirstName(name);
        }
        List<UserResponse> response = new ArrayList<>();
        for (User user : users) {
            response.add(userMapper.userToUserResponse(user, getSportsForUser(user)));
        }
        return response;
    }

    @Transactional
    public void createInitialAdmin(AdminBootstrapProperties properties) {
        if (userRepository.existsByRole(Role.ADMIN)) {
            log.info("Admin already present, skipping bootstrap");
            return;
        }
        if (isBlank(properties.email()) || isBlank(properties.password())
                || isBlank(properties.firstName()) || isBlank(properties.lastName())
                || properties.birthDate() == null) {
            log.warn("No admin exists and app.bootstrap.admin.* is incomplete, skipping bootstrap");
            return;
        }
        if (!PASSWORD_POLICY.matcher(properties.password()).matches()) {
            log.warn("app.bootstrap.admin.password does not meet the password policy, skipping bootstrap");
            return;
        }
        String email = properties.email().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            log.error("A user with email {} already exists, it will not be promoted to admin", email);
            return;
        }
        User admin = new User(properties.birthDate(), email, properties.lastName().trim(), properties.firstName().trim());
        admin.setHashPassword(passwordEncoder.encode(properties.password()));
        admin.setRole(Role.ADMIN);
        userRepository.save(admin);
        log.info("Initial admin {} created", email);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private List<UserSportResponse> getSportsForUser(User user){
        List<UserSport> sports = userSportService.findByUserId(user.getId());
        return userSportMapper.userSportsToUserSportResponses(sports);

    }
}
