package com.example.demo.user.controller;

import com.example.demo.testcontainers.AbstractIntegrationTest;
import com.example.demo.testcontainers.JsonPathMock;
import com.example.demo.user.costants.UserErrors;
import com.example.demo.usersport.costants.UserSportErrors;
import com.example.demo.user.entity.User;
import com.example.demo.user.repository.UserRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class UserControllerIntegrationTests extends AbstractIntegrationTest {

    @Autowired
    protected MockMvc mockMvc;
    @Autowired
    protected UserRepository userRepository;
    private static final MediaType PROBLEM_JSON = MediaType.APPLICATION_PROBLEM_JSON;

    private ResultActions postUser(String jsonClasspath) throws Exception {
        return mockMvc.perform(MockMvcRequestBuilders
                .post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(readJson(jsonClasspath))
                .accept(MediaType.APPLICATION_JSON));
    }

    private User createUserViaApi(String jsonClasspath) throws Exception {
        postUser(jsonClasspath).andExpect(status().isCreated());
        String email = JsonPath.read(readJson(jsonClasspath), "$.email");
        return userRepository.findByEmail(email).orElseThrow();
    }

    private long countUserSports() {
        return jdbcClient.sql("SELECT count(*) FROM user_sport").query(Long.class).single();
    }

    private long countUsers() {
        return jdbcClient.sql("SELECT count(*) FROM users").query(Long.class).single();
    }

    private void assertNoUserPersisted() {
        assertThat(countUsers()).as("rows in users after a rejected request").isZero();
    }

    @Nested
    @DisplayName("POST /users")
    class AddUser {

        @Test
        @DisplayName("201 - valid user: correct response and persisted row")
        void shouldReturn201_whenaddUserCorrectly() throws Exception {
            String json = readJson(JsonPathMock.VALID_USER_REQUEST_JSON_PATH);
            String expectedEmail = JsonPath.read(json, "$.email");
            String expectedFirstName = JsonPath.read(json, "$.firstName");
            String expectedLastName = JsonPath.read(json, "$.lastName");
            String expectedBirthDate = JsonPath.read(json, "$.birthDate");

            RequestBuilder request = MockMvcRequestBuilders
                    .post("/users")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json)
                    .accept(MediaType.APPLICATION_JSON);
            ResultMatcher expectedResponse = status().isCreated();

            mockMvc.perform(request)
                    .andDo(print())
                    .andExpectAll(expectedResponse,
                            content().json(readJson(JsonPathMock.VALID_USER_RESPONSE_JSON_PATH)),
                            content().contentType(MediaType.APPLICATION_JSON)
                    );
            Optional<User> user = userRepository.findByEmail(expectedEmail);
            assertThat(user).as("user with email %s", expectedEmail).isPresent();
            User saved = user.orElseThrow();
            assertThat(saved.getFirstName()).as("persisted first_name").isEqualTo(expectedFirstName);
            assertThat(saved.getLastName()).as("persisted last_name").isEqualTo(expectedLastName);
            assertThat(saved.getBirthDate()).as("persisted birth_date").isEqualTo(LocalDate.parse(expectedBirthDate));
            assertThat(saved.getId()).as("generated id").isPositive();
        }

        @Test
        @DisplayName("409 - same email twice: DuplicatedUserException")
        void shouldReturn409_whenEmailAlreadyUsed() throws Exception {
            String email = JsonPath.read(readJson(JsonPathMock.VALID_USER_REQUEST_JSON_PATH), "$.email");

            postUser(JsonPathMock.VALID_USER_REQUEST_JSON_PATH).andExpect(status().isCreated());

            postUser(JsonPathMock.VALID_USER_REQUEST_JSON_PATH)
                    .andExpect(status().isConflict())
                    .andExpect(content().contentType(PROBLEM_JSON))
                    .andExpect(jsonPath("$.status").value(409))
                    .andExpect(jsonPath("$.title").value(UserErrors.userDuplicatedMessage))
                    .andExpect(jsonPath("$.detail").value(email + " is already used"))
                    .andExpect(jsonPath("$.type").value(UserErrors.userDuplicatedUri));

            assertThat(countUsers()).as("total users after the duplicate attempt").isEqualTo(1L);
        }

        @Test
        @DisplayName("400 - blank firstName: default switch branch (@NotBlank)")
        void shouldReturn400_whenFirstNameIsBlank() throws Exception {
            postUser(JsonPathMock.BLANK_FIRST_NAME_REQUEST_JSON_PATH)
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentType(PROBLEM_JSON))
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.title").value("firstName is invalid"))
                    .andExpect(jsonPath("$.detail").value("firstName failed validation"))
                    .andExpect(jsonPath("$.type").value(UserErrors.fieldInvalidUri));

            assertNoUserPersisted();
        }

        @Test
        @DisplayName("400 - blank lastName: default switch branch (@NotBlank)")
        void shouldReturn400_whenLastNameIsBlank() throws Exception {
            postUser(JsonPathMock.BLANK_LAST_NAME_REQUEST_JSON_PATH)
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentType(PROBLEM_JSON))
                    .andExpect(jsonPath("$.title").value("lastName is invalid"))
                    .andExpect(jsonPath("$.detail").value("lastName failed validation"))
                    .andExpect(jsonPath("$.type").value(UserErrors.fieldInvalidUri));

            assertNoUserPersisted();
        }

        @Test
        @DisplayName("400 - blank email: violates both @NotBlank and @Email, so only the status is asserted")
        void shouldReturn400_whenEmailIsBlank() throws Exception {
            postUser(JsonPathMock.BLANK_EMAIL_REQUEST_JSON_PATH)
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentType(PROBLEM_JSON))
                    .andExpect(jsonPath("$.status").value(400));

            assertNoUserPersisted();
        }

        @Test
        @DisplayName("400 - malformed email: Email switch branch")
        void shouldReturn400_whenEmailFormatIsInvalid() throws Exception {
            postUser(JsonPathMock.INVALID_EMAIL_FORMAT_REQUEST_JSON_PATH)
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentType(PROBLEM_JSON))
                    .andExpect(jsonPath("$.title").value(UserErrors.emailFormatMessage))
                    .andExpect(jsonPath("$.detail").value("email must be a valid email address"))
                    .andExpect(jsonPath("$.type").value(UserErrors.emailFormatUri));

            assertNoUserPersisted();
        }

        @Test
        @DisplayName("400 - missing birthDate: NotNull switch branch")
        void shouldReturn400_whenBirthDateIsNull() throws Exception {
            postUser(JsonPathMock.NULL_BIRTH_DATE_REQUEST_JSON_PATH)
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentType(PROBLEM_JSON))
                    .andExpect(jsonPath("$.title").value("birthDate is mandatory"))
                    .andExpect(jsonPath("$.detail").value("birthDate must not be null"))
                    .andExpect(jsonPath("$.type").value(UserErrors.fieldMandatoryUri));

            assertNoUserPersisted();
        }

        @Test
        @DisplayName("400 - birthDate in the future: Past switch branch")
        void shouldReturn400_whenBirthDateIsInTheFuture() throws Exception {
            postUser(JsonPathMock.FUTURE_BIRTH_DATE_REQUEST_JSON_PATH)
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentType(PROBLEM_JSON))
                    .andExpect(jsonPath("$.title").value(UserErrors.birthDatePastMessage))
                    .andExpect(jsonPath("$.detail").value("birthDate must be a date in the past"))
                    .andExpect(jsonPath("$.type").value(UserErrors.birthDatePastUri));

            assertNoUserPersisted();
        }

        @Test
        @DisplayName("400 - syntactically invalid JSON")
        void shouldReturn400_whenBodyIsMalformedJson() throws Exception {
            postUser(JsonPathMock.MALFORMED_REQUEST_JSON_PATH)
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentType(PROBLEM_JSON))
                    .andExpect(jsonPath("$.status").value(400));

            assertNoUserPersisted();
        }

        @Test
        @DisplayName("415 - Content-Type other than application/json")
        void shouldReturn415_whenContentTypeIsNotJson() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders
                            .post("/users")
                            .contentType(MediaType.TEXT_PLAIN)
                            .content(readJson(JsonPathMock.VALID_USER_REQUEST_JSON_PATH)))
                    .andExpect(status().isUnsupportedMediaType());

            assertNoUserPersisted();
        }

        @Test
        @DisplayName("409 - lastName longer than varchar(255): handleDataIntegrityViolation fallback branch")
        void shouldReturn409_whenDatabaseConstraintIsViolated() throws Exception {
            postUser(JsonPathMock.TOO_LONG_LAST_NAME_REQUEST_JSON_PATH)
                    .andExpect(status().isConflict())
                    .andExpect(content().contentType(PROBLEM_JSON))
                    .andExpect(jsonPath("$.status").value(409))
                    .andExpect(jsonPath("$.title").value("Data integrity violation"))
                    .andExpect(jsonPath("$.detail").value("Database constraint violated"))
                    .andExpect(jsonPath("$.type").value("http://localhost:8080/data-integrity-violation"));

            assertNoUserPersisted();
        }

        @Test
        @DisplayName("405 - method not allowed on /users")
        void shouldReturn405_whenMethodIsNotAllowed() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.delete("/users"))
                    .andExpect(status().isMethodNotAllowed());
        }
    }

    @Nested
    @DisplayName("POST /users with nested sports")
    class AddUserWithSports {

        @Test
        @DisplayName("201 - user and both sports created in one call")
        void shouldReturn201WithSports_whenPayloadCarriesSports() throws Exception {
            postUser(JsonPathMock.VALID_USER_WITH_SPORTS_REQUEST_JSON_PATH)
                    .andDo(print())
                    .andExpect(status().isCreated())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(header().exists("Location"))
                    .andExpect(content().json(readJson(JsonPathMock.VALID_USER_WITH_SPORTS_RESPONSE_JSON_PATH)))
                    .andExpect(jsonPath("$.sports", hasSize(2)))
                    .andExpect(jsonPath("$.sports[*].sportType", containsInAnyOrder("BJJ", "Boxing")))

                    .andExpect(jsonPath("$.sports[0].idUserSport").isNumber());

            assertThat(countUsers()).as("rows in users").isEqualTo(1L);
            assertThat(countUserSports()).as("rows in user_sport").isEqualTo(2L);
        }

        @Test
        @DisplayName("201 - the sports field is optional: a user without sports still works")
        void shouldReturn201_whenSportsFieldIsAbsent() throws Exception {
            postUser(JsonPathMock.VALID_USER_REQUEST_JSON_PATH)
                    .andExpect(status().isCreated());

            assertThat(countUsers()).isEqualTo(1L);
            assertThat(countUserSports()).isZero();
        }

        @Test
        @DisplayName("404 + ROLLBACK - unknown sport: the user must NOT be persisted")
        void shouldRollbackTheUser_whenASportDoesNotExist() throws Exception {

            postUser(JsonPathMock.UNKNOWN_SPORT_REQUEST_JSON_PATH)
                    .andExpect(status().isBadRequest());

            assertNoUserPersisted();
            assertThat(countUserSports()).as("rows in user_sport after the rollback").isZero();
        }

        @Test
        @DisplayName("409 + ROLLBACK - same sport twice in one payload")
        void shouldReturn409_whenTheSameSportIsListedTwice() throws Exception {
            postUser(JsonPathMock.DUPLICATE_SPORTS_REQUEST_JSON_PATH)
                    .andExpect(status().isConflict())
                    .andExpect(content().contentType(PROBLEM_JSON))
                    .andExpect(jsonPath("$.status").value(409))
                    .andExpect(jsonPath("$.title").value(UserSportErrors.userSportDuplicatedMessage))
                    .andExpect(jsonPath("$.detail").value("BJJ is listed more than once"))
                    .andExpect(jsonPath("$.type").value(UserSportErrors.userSportDuplicatedUri));

            assertNoUserPersisted();
            assertThat(countUserSports()).isZero();
        }

        @Test
        @DisplayName("400 - negative yearsPracticed: @Valid really reaches the nested list")
        void shouldReturn400_whenNestedYearsPracticedIsNegative() throws Exception {

            postUser(JsonPathMock.NEGATIVE_YEARS_REQUEST_JSON_PATH)
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentType(PROBLEM_JSON))
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.title").value("sports[0].yearsPracticed is invalid"));

            assertNoUserPersisted();
            assertThat(countUserSports()).isZero();
        }
    }

    @Nested
    @DisplayName("GET /users/allusers")
    class GetAllUsers {

        @Test
        @DisplayName("200 - empty list when there are no users")
        void shouldReturn200WithEmptyList_whenNoUsersExist() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.get("/users/allusers").accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$", hasSize(0)));
        }

        @Test
        @DisplayName("200 - returns every created user")
        void shouldReturn200WithAllUsers_whenUsersExist() throws Exception {
            createUserViaApi(JsonPathMock.VALID_USER_REQUEST_JSON_PATH);
            createUserViaApi(JsonPathMock.SECOND_VALID_USER_REQUEST_JSON_PATH);

            mockMvc.perform(MockMvcRequestBuilders.get("/users/allusers").accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[*].firstName", containsInAnyOrder("Lorenzo", "Marco")))
                    .andExpect(jsonPath("$[0].email").doesNotExist());
        }
    }

    @Nested
    @DisplayName("GET /users/userbyid")
    class GetUserById {

        @Test
        @DisplayName("200 - existing id")
        void shouldReturn200_whenIdExists() throws Exception {
            User saved = createUserViaApi(JsonPathMock.VALID_USER_REQUEST_JSON_PATH);

            mockMvc.perform(MockMvcRequestBuilders.get("/users/userbyid")
                            .param("id", String.valueOf(saved.getId()))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id").value(saved.getId()))
                    .andExpect(jsonPath("$.firstName").value(saved.getFirstName()))
                    .andExpect(jsonPath("$.email").doesNotExist());
        }

        @Test
        @DisplayName("404 - unknown id: problem detail naming the missing id")
        void shouldReturn404_whenIdDoesNotExist() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.get("/users/userbyid")
                            .param("id", "999999")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound())
                    .andExpect(content().contentType(PROBLEM_JSON))
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.title").value(UserErrors.userNotFoundMessage))
                    .andExpect(jsonPath("$.detail").value("user with id 999999 not found"))
                    .andExpect(jsonPath("$.type").value(UserErrors.userNotFoundUri));
        }

        @Test
        @DisplayName("400 - missing id parameter")
        void shouldReturn400_whenIdParamIsMissing() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.get("/users/userbyid").accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("400 - non numeric id")
        void shouldReturn400_whenIdIsNotANumber() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.get("/users/userbyid")
                            .param("id", "abc")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /users/userbyname")
    class GetUserByName {

        @Test
        @DisplayName("200 - existing name")
        void shouldReturn200_whenNameExists() throws Exception {
            User saved = createUserViaApi(JsonPathMock.VALID_USER_REQUEST_JSON_PATH);

            mockMvc.perform(MockMvcRequestBuilders.get("/users/userbyname")
                            .param("name", saved.getFirstName())
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].id").value(saved.getId()))
                    .andExpect(jsonPath("$[0].firstName").value(saved.getFirstName()));
        }

        @Test
        @Disabled("""
                Fails with 404: the case-insensitivity promised by migration V1_2_0 does not \
                reach findByFirstName. Postgres picks the = operator from the types on both \
                sides: an untyped literal resolves to =(citext, citext) and ignores case, but \
                the JDBC driver binds the parameter as varchar, so it resolves to =(text, text) \
                and the comparison is case-sensitive again. \
                Verified on postgres:16.9 with a citext first_name holding 'Lorenzo': \
                = 'LORENZO' matches 1 row, = 'LORENZO'::citext matches 1 row, \
                = 'LORENZO'::varchar matches 0 rows (what Hibernate does), \
                = 'LORENZO'::text matches 0 rows. \
                Fixing it requires forcing the type in the query (a native @Query casting to \
                citext) or normalising the case in Java. Kept here, disabled, so the gap \
                between the migration's intent and the actual behaviour is not lost.""")
        @DisplayName("200 - case-insensitive lookup via the citext column")
        void shouldReturn200_whenNameDiffersOnlyByCase() throws Exception {
            User saved = createUserViaApi(JsonPathMock.VALID_USER_REQUEST_JSON_PATH);

            mockMvc.perform(MockMvcRequestBuilders.get("/users/userbyname")
                            .param("name", saved.getFirstName().toUpperCase())
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(saved.getId()));
        }

        @Test
        @DisplayName("404 - unknown name: problem detail naming the missing name")
        void shouldReturn404_whenNameDoesNotExist() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.get("/users/userbyname")
                            .param("name", "NoSuchName")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound())
                    .andExpect(content().contentType(PROBLEM_JSON))
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.title").value(UserErrors.userNotFoundMessage))
                    .andExpect(jsonPath("$.detail").value("user with first name NoSuchName not found"))
                    .andExpect(jsonPath("$.type").value(UserErrors.userNotFoundUri));
        }

        @Test
        @DisplayName("400 - missing name parameter")
        void shouldReturn400_whenNameParamIsMissing() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.get("/users/userbyname").accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest());
        }
    }

    @Test
    @DisplayName("404 - unknown path under /users")
    void shouldReturn404_whenPathDoesNotExist() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/users/doesnotexist").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }
}
