package com.example.demo.user.service;

import com.example.demo.sport.entity.SportType;
import com.example.demo.user.dto.UserRequest;
import com.example.demo.user.dto.UserResponse;
import com.example.demo.user.dto.mapper.UserMapper;
import com.example.demo.user.entity.User;
import com.example.demo.user.exceptions.DuplicatedUserException;
import com.example.demo.user.exceptions.UserNotFoundException;
import com.example.demo.user.repository.UserRepository;
import com.example.demo.usersport.dto.UserSportRequest;
import com.example.demo.usersport.dto.UserSportResponse;
import com.example.demo.usersport.dto.mapper.UserSportMapper;
import com.example.demo.usersport.entity.UserSport;
import com.example.demo.usersport.service.UserSportService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {
    @Mock
    private UserRepository userRepository;
    @Mock
    private UserSportService userSportService;
    @Mock
    private UserMapper userMapper;
    @Mock
    private UserSportMapper userSportMapper;
    @Mock
    private PasswordEncoder passwordEncoder;
    @InjectMocks
    private UserService userService;

    private static final String FIRST_NAME = "Claudia";
    private static final String LAST_NAME = "Rategni";
    private static final String EMAIL = "claudia.rategni@gmail.com";
    private static final LocalDate BIRTH_DATE = LocalDate.of(2007, 4, 13);
    private static final String PASSWORD = "Str0ngP4ssw0rd!";
    private static final String HASHED_PASSWORD = "$2a$10$hashedPasswordForTests";

    private static User newUser() {
        return new User(BIRTH_DATE, EMAIL, LAST_NAME, FIRST_NAME);
    }

    private static User persisted(User user, long id) {
        User saved = new User(user.getBirthDate(), user.getEmail(),
                user.getLastName(), user.getFirstName());
        saved.setId(id);
        return saved;
    }

    @Nested
    @DisplayName("addUser(User)")
    class AddUser {

        @Test
        @DisplayName("returns the persisted user when the email is free")
        void shouldReturnPersistedUser_whenEmailIsNotAlreadyUsed() {
            User input = newUser();
            User saved = persisted(input, 1L);
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());
            when(userRepository.save(input)).thenReturn(saved);

            User result = userService.addUser(input);

            assertThat(result).isSameAs(saved);
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getEmail()).isEqualTo(EMAIL);
            assertThat(result.getFirstName()).isEqualTo(FIRST_NAME);
            assertThat(result.getLastName()).isEqualTo(LAST_NAME);
            assertThat(result.getBirthDate()).isEqualTo(BIRTH_DATE);
        }

        @Test
        @DisplayName("throws DuplicatedUserException carrying the email when it is taken")
        void shouldThrowDuplicatedUserException_whenEmailIsAlreadyUsed() {
            User input = newUser();
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(persisted(input, 1L)));

            assertThatThrownBy(() -> userService.addUser(input))
                    .isInstanceOf(DuplicatedUserException.class)
                    .hasMessage(EMAIL);
        }
    }

    @Nested
    @DisplayName("register(UserRequest)")
    class Register {

        private static UserRequest requestWithSports() {
            return new UserRequest(FIRST_NAME, LAST_NAME, EMAIL, BIRTH_DATE, PASSWORD,
                    List.of(new UserSportRequest(SportType.BJJ, new BigDecimal("4.5"), true)));
        }

        @Test
        @DisplayName("saves the user first, then delegates the sports to UserSportService")
        void shouldPersistUserThenSports_andReturnTheAssembledResponse() {
            UserRequest request = requestWithSports();
            User mapped = newUser();
            User saved = persisted(mapped, 1L);
            List<UserSport> savedSports = List.of(new UserSport());
            List<UserSportResponse> sportResponses =
                    List.of(new UserSportResponse(10L, SportType.BJJ, new BigDecimal("4.5"), true));
            UserResponse expected = new UserResponse(1L, FIRST_NAME, LAST_NAME, BIRTH_DATE, sportResponses);

            when(passwordEncoder.encode(PASSWORD)).thenReturn(HASHED_PASSWORD);
            when(userMapper.userRequestToUser(request)).thenReturn(mapped);
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());
            when(userRepository.save(mapped)).thenReturn(saved);
            when(userSportService.createForUser(saved, request.sports())).thenReturn(savedSports);
            when(userSportMapper.userSportsToUserSportResponses(savedSports)).thenReturn(sportResponses);
            when(userMapper.userToUserResponse(saved, sportResponses)).thenReturn(expected);

            UserResponse result = userService.register(request);

            assertThat(result).isEqualTo(expected);
            assertThat(mapped.getHashPassword()).isEqualTo(HASHED_PASSWORD);

            verify(userSportService).createForUser(eq(saved), anyList());
        }

        @Test
        @DisplayName("never touches the sports when the email is already taken")
        void shouldNotCreateSports_whenEmailIsAlreadyUsed() {
            UserRequest request = requestWithSports();
            User mapped = newUser();
            when(passwordEncoder.encode(PASSWORD)).thenReturn(HASHED_PASSWORD);
            when(userMapper.userRequestToUser(request)).thenReturn(mapped);
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(persisted(mapped, 1L)));

            assertThatThrownBy(() -> userService.register(request))
                    .isInstanceOf(DuplicatedUserException.class)
                    .hasMessage(EMAIL);

            verify(userSportService, never()).createForUser(any(), anyList());
        }
    }

    @Nested
    @DisplayName("findAll()")
    class FindAll {

        @Test
        @DisplayName("returns every user the repository holds")
        void shouldReturnAllUsers_whenUsersExist() {
            User first = persisted(newUser(), 1L);
            User second = persisted(newUser(), 2L);
            UserResponse firstResponse = new UserResponse(1L, FIRST_NAME, LAST_NAME, BIRTH_DATE, List.of());
            UserResponse secondResponse = new UserResponse(2L, FIRST_NAME, LAST_NAME, BIRTH_DATE, List.of());
            when(userRepository.findAll()).thenReturn(List.of(first, second));
            when(userSportService.findByUserId(1L)).thenReturn(List.of());
            when(userSportService.findByUserId(2L)).thenReturn(List.of());
            when(userSportMapper.userSportsToUserSportResponses(List.of())).thenReturn(List.of());
            when(userMapper.userToUserResponse(first, List.of())).thenReturn(firstResponse);
            when(userMapper.userToUserResponse(second, List.of())).thenReturn(secondResponse);

            assertThat(userService.findAll())
                    .extracting(UserResponse::id)
                    .containsExactly(1L, 2L);
        }

        @Test
        @DisplayName("returns an empty list when there is no user")
        void shouldReturnEmptyList_whenNoUserExists() {
            when(userRepository.findAll()).thenReturn(List.of());

            assertThat(userService.findAll()).isEmpty();
        }
    }

    @Nested
    @DisplayName("findUserById(Long)")
    class FindUserById {

        @Test
        @DisplayName("returns the user when the id exists")
        void shouldReturnUser_whenIdExists() {
            User saved = persisted(newUser(), 1L);
            List<UserSport> sports = List.of(new UserSport());
            List<UserSportResponse> sportResponses =
                    List.of(new UserSportResponse(10L, SportType.BJJ, new BigDecimal("4.5"), true));
            UserResponse expected = new UserResponse(1L, FIRST_NAME, LAST_NAME, BIRTH_DATE, sportResponses);
            when(userRepository.findById(1L)).thenReturn(Optional.of(saved));
            when(userSportService.findByUserId(1L)).thenReturn(sports);
            when(userSportMapper.userSportsToUserSportResponses(sports)).thenReturn(sportResponses);
            when(userMapper.userToUserResponse(saved, sportResponses)).thenReturn(expected);

            assertThat(userService.findUserById(1L)).isEqualTo(expected);
        }

        @Test
        @DisplayName("throws UserNotFoundException carrying the id when it is unknown")
        void shouldThrowUserNotFoundException_whenIdDoesNotExist() {
            when(userRepository.findById(404L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.findUserById(404L))
                    .isInstanceOf(UserNotFoundException.class)
                    .hasMessage("user with id 404 not found");

            verify(userSportService, never()).findByUserId(any());
        }
    }

    @Nested
    @DisplayName("findUserByFirstname(String)")
    class FindUserByFirstname {

        @Test
        @DisplayName("returns every user matching the first name")
        void shouldReturnUsers_whenFirstNameExists() {
            User saved = persisted(newUser(), 1L);
            UserResponse expected = new UserResponse(1L, FIRST_NAME, LAST_NAME, BIRTH_DATE, List.of());
            when(userRepository.findByFirstNameIgnoreCase(FIRST_NAME)).thenReturn(List.of(saved));
            when(userSportService.findByUserId(1L)).thenReturn(List.of());
            when(userSportMapper.userSportsToUserSportResponses(List.of())).thenReturn(List.of());
            when(userMapper.userToUserResponse(saved, List.of())).thenReturn(expected);

            assertThat(userService.findUserByFirstname(FIRST_NAME)).containsExactly(expected);
        }

        @Test
        @DisplayName("throws UserNotFoundException carrying the name when it is unknown")
        void shouldThrowUserNotFoundException_whenFirstNameDoesNotExist() {
            when(userRepository.findByFirstNameIgnoreCase("Nobody")).thenReturn(List.of());

            assertThatThrownBy(() -> userService.findUserByFirstname("Nobody"))
                    .isInstanceOf(UserNotFoundException.class)
                    .hasMessage("user with first name Nobody not found");

            verify(userSportService, never()).findByUserId(any());
        }
    }
}
