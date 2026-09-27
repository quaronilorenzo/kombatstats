package com.example.demo.user.service;

import com.example.demo.sport.entity.SportType;
import com.example.demo.user.dto.UserRequest;
import com.example.demo.user.dto.UserResponse;
import com.example.demo.user.dto.mapper.UserMapper;
import com.example.demo.user.entity.User;
import com.example.demo.user.exceptions.DuplicatedUserException;
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
    @InjectMocks
    private UserService userService;

    private static final String FIRST_NAME = "Claudia";
    private static final String LAST_NAME = "Rategni";
    private static final String EMAIL = "claudia.rategni@gmail.com";
    private static final LocalDate BIRTH_DATE = LocalDate.of(2007, 4, 13);

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
            return new UserRequest(FIRST_NAME, LAST_NAME, EMAIL, BIRTH_DATE,
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

            when(userMapper.userRequestToUser(request)).thenReturn(mapped);
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());
            when(userRepository.save(mapped)).thenReturn(saved);
            when(userSportService.createForUser(saved, request.sports())).thenReturn(savedSports);
            when(userSportMapper.userSportsToUserSportResponses(savedSports)).thenReturn(sportResponses);
            when(userMapper.userToUserResponse(saved, sportResponses)).thenReturn(expected);

            UserResponse result = userService.register(request);

            assertThat(result).isEqualTo(expected);

            verify(userSportService).createForUser(eq(saved), anyList());
        }

        @Test
        @DisplayName("never touches the sports when the email is already taken")
        void shouldNotCreateSports_whenEmailIsAlreadyUsed() {
            UserRequest request = requestWithSports();
            User mapped = newUser();
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
            List<User> users = List.of(persisted(newUser(), 1L), persisted(newUser(), 2L));
            when(userRepository.findAll()).thenReturn(users);

            assertThat(userService.findAll())
                    .hasSize(2)
                    .extracting(User::getId)
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
            when(userRepository.findById(1L)).thenReturn(Optional.of(saved));

            assertThat(userService.findUserById(1L)).contains(saved);
        }

        @Test
        @DisplayName("returns an empty Optional when the id is unknown")
        void shouldReturnEmptyOptional_whenIdDoesNotExist() {
            when(userRepository.findById(404L)).thenReturn(Optional.empty());

            assertThat(userService.findUserById(404L)).isEmpty();
        }
    }

    @Nested
    @DisplayName("findUserByFirstname(String)")
    class FindUserByFirstname {

        @Test
        @DisplayName("returns the user when the first name exists")
        void shouldReturnUser_whenFirstNameExists() {
            User saved = persisted(newUser(), 1L);
            when(userRepository.findByFirstName(FIRST_NAME)).thenReturn(Optional.of(saved));

            assertThat(userService.findUserByFirstname(FIRST_NAME)).contains(saved);
        }

        @Test
        @DisplayName("returns an empty Optional when the first name is unknown")
        void shouldReturnEmptyOptional_whenFirstNameDoesNotExist() {
            when(userRepository.findByFirstName("Nobody")).thenReturn(Optional.empty());

            assertThat(userService.findUserByFirstname("Nobody")).isEmpty();
        }
    }
}
