package com.example.demo.user.service;

import com.example.demo.security.JwtService;
import com.example.demo.user.dto.LoginRequest;
import com.example.demo.user.dto.LoginResponse;
import com.example.demo.user.entity.User;
import com.example.demo.user.exceptions.InvalidCredentialsException;
import com.example.demo.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class LoginServiceTest {
    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;
    @InjectMocks
    private LoginService loginService;

    private static final String EMAIL = "claudia.rategni@gmail.com";
    private static final String PASSWORD = "Str0ngP4ssw0rd!";
    private static final String HASHED_PASSWORD = "$2a$10$hashedPasswordForTests";
    private static final String TOKEN = "header.payload.signature";
    // $2a/$2b/$2y, two-digit cost, 22-char salt + 31-char hash.
    private static final String BCRYPT_FORMAT = "^\\$2[aby]\\$\\d{2}\\$[./A-Za-z0-9]{53}$";

    private static User persistedUser() {
        User user = new User(LocalDate.of(2007, 4, 13), EMAIL, "Rategni", "Claudia");
        user.setId(5L);
        user.setHashPassword(HASHED_PASSWORD);
        return user;
    }

    @Test
    @DisplayName("correct credentials: token generated for that user")
    void shouldReturnToken_whenCredentialsAreCorrect() {
        User user = persistedUser();
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(PASSWORD, HASHED_PASSWORD)).thenReturn(true);
        when(jwtService.generateToken(user)).thenReturn(TOKEN);

        LoginResponse response = loginService.login(new LoginRequest(EMAIL, PASSWORD));

        assertThat(response.accessToken()).isEqualTo(TOKEN);
    }

    @Test
    @DisplayName("email with uppercase and spaces: looked up lowercase and trimmed")
    void shouldNormalizeEmail_beforeLookup() {
        User user = persistedUser();
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(PASSWORD, HASHED_PASSWORD)).thenReturn(true);
        when(jwtService.generateToken(user)).thenReturn(TOKEN);

        loginService.login(new LoginRequest("  Claudia.Rategni@GMAIL.com  ", PASSWORD));

        verify(userRepository).findByEmail(EMAIL);
    }

    @Test
    @DisplayName("wrong password: rejected, no token generated")
    void shouldReject_whenPasswordIsWrong() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(persistedUser()));
        when(passwordEncoder.matches("WrongPassword1!", HASHED_PASSWORD)).thenReturn(false);

        assertThatThrownBy(() -> loginService.login(new LoginRequest(EMAIL, "WrongPassword1!")))
                .isInstanceOf(InvalidCredentialsException.class);

        verify(jwtService, never()).generateToken(any());
    }

    @Test
    @DisplayName("unknown email: rejected with the credentials error, not NoSuchElementException")
    void shouldReject_whenEmailDoesNotExist() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> loginService.login(new LoginRequest(EMAIL, PASSWORD)))
                .isInstanceOf(InvalidCredentialsException.class);

        verify(jwtService, never()).generateToken(any());
    }

    @Test
    @DisplayName("unknown email: rejected even if the password matches the fake hash")
    void shouldReject_whenEmailDoesNotExistAndFakeHashMatches() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());
        when(passwordEncoder.matches(eq(PASSWORD), anyString())).thenReturn(true);

        assertThatThrownBy(() -> loginService.login(new LoginRequest(EMAIL, PASSWORD)))
                .isInstanceOf(InvalidCredentialsException.class);

        verify(jwtService, never()).generateToken(any());
    }

    @Test
    @DisplayName("unknown email: BCrypt still runs against a real hash, so timing matches an existing email")
    void shouldCompareAgainstARealBcryptHash_whenEmailDoesNotExist() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> loginService.login(new LoginRequest(EMAIL, PASSWORD)))
                .isInstanceOf(InvalidCredentialsException.class);

        ArgumentCaptor<String> hash = ArgumentCaptor.forClass(String.class);
        verify(passwordEncoder).matches(eq(PASSWORD), hash.capture());
        // A non-BCrypt value makes BCryptPasswordEncoder return false immediately, skipping the cost.
        assertThat(hash.getValue()).as("hash compared for an unknown email").matches(BCRYPT_FORMAT);
    }
}
