package com.example.demo.user.service;

import com.example.demo.user.entity.Sport;
import com.example.demo.user.entity.User;
import com.example.demo.user.exceptions.DuplicatedUserException;
import com.example.demo.user.repository.UserRepository;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {
    @Mock
    private UserRepository userRepository;
    @InjectMocks
    private UserService userService;
    // addUser(User user)
    @ParameterizedTest
    @MethodSource("_passingValuesToaddUserWithDuplicatedEmail_shouldReturnConflictRequest")
    void addUserWithDuplicatedEmail_shouldReturnConflictRequest(String firstName, String lastName, String email, LocalDate birthDate, List<Sport> sports){
        User user = new User(sports, birthDate, email, lastName, firstName);
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        assertThrows(DuplicatedUserException.class, () -> userService.addUser(user));
    }
    private static Stream<Arguments> _passingValuesToaddUserWithDuplicatedEmail_shouldReturnConflictRequest(){
        return Stream.of(
                Arguments.of("Claudia", "Rategni", "claudia.rategni@gmail.com", LocalDate.of(2007,4,13), List.of(Sport.BJJ, Sport.MMA))
        );
    }
}
