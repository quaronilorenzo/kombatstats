package com.example.demo.user.service;

import com.example.demo.user.entity.User;
import com.example.demo.user.exceptions.DuplicatedUserException;
import com.example.demo.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {
    @Mock
    private UserRepository userRepository;
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
    @DisplayName("addUser(firstName, lastName, email, birthDate)")
    class AddUserFromFields {

        @Test
        @DisplayName("builds the user from the arguments and returns what was persisted")
        void shouldBuildAndReturnUser_fromTheGivenFields() {
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
                User toSave = invocation.getArgument(0);
                return persisted(toSave, 1L);
            });

            User result = userService.addUser(FIRST_NAME, LAST_NAME, EMAIL, BIRTH_DATE);

            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getFirstName()).isEqualTo(FIRST_NAME);
            assertThat(result.getLastName()).isEqualTo(LAST_NAME);
            assertThat(result.getEmail()).isEqualTo(EMAIL);
            assertThat(result.getBirthDate()).isEqualTo(BIRTH_DATE);
        }

        @Test
        @DisplayName("does NOT reject a duplicated email - unlike addUser(User), it never calls findByEmail")
        void shouldPersistWithoutDuplicateCheck_whenEmailIsAlreadyUsed() {
            // Characterisation test: this overload skips the guard that addUser(User) applies,
            // so a duplicate only fails later, on the database unique constraint.
            // It documents the current behaviour, it does not endorse it.
            when(userRepository.save(any(User.class))).thenAnswer(invocation ->
                    persisted(invocation.getArgument(0), 2L));

            User result = userService.addUser(FIRST_NAME, LAST_NAME, EMAIL, BIRTH_DATE);

            assertThat(result.getEmail()).isEqualTo(EMAIL);
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
