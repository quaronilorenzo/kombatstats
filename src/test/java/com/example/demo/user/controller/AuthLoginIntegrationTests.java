package com.example.demo.user.controller;

import com.example.demo.testcontainers.AbstractIntegrationTest;
import com.example.demo.testcontainers.JsonPathMock;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class AuthLoginIntegrationTests extends AbstractIntegrationTest {

    @Autowired
    protected MockMvc mockMvc;
    @Autowired
    protected PasswordEncoder passwordEncoder;

    private static final String LOGIN_URL = "/auth/login";
    private static final String EMAIL = "login.user@example.com";
    private static final String PASSWORD = "Str0ng!Passw0rd";

    private static String loginJson(String email, String password) {
        return """
                {"email": "%s", "password": "%s"}
                """.formatted(email, password);
    }

    private ResultActions login(String body) throws Exception {
        return mockMvc.perform(MockMvcRequestBuilders.post(LOGIN_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
                .accept(MediaType.APPLICATION_JSON));
    }

    private long insertUserWithPassword(String email, String rawPassword) {
        return insertUser(email, passwordEncoder.encode(rawPassword));
    }

    private static String accessToken(MvcResult result) throws Exception {
        return JsonPath.read(result.getResponse().getContentAsString(), "$.accessToken");
    }

    @Nested
    @DisplayName("200 - successful login")
    class SuccessfulLogin {

        @Test
        @DisplayName("correct credentials: only accessToken, carrying the user's id")
        void shouldReturnTokenForThatUser_whenCredentialsAreCorrect() throws Exception {
            long id = insertUserWithPassword(EMAIL, PASSWORD);

            MvcResult result = login(loginJson(EMAIL, PASSWORD))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andReturn();

            Map<String, Object> body = JsonPath.read(result.getResponse().getContentAsString(), "$");
            assertThat(body).as("response fields").containsOnlyKeys("accessToken");
            assertThat(jwtService.extractUserId(accessToken(result))).isEqualTo(id);
        }

        @Test
        @DisplayName("full flow: register -> login -> protected route with the token")
        void shouldAccessProtectedRoute_afterRegisterAndLogin() throws Exception {
            String registerJson = readJson(JsonPathMock.VALID_USER_REQUEST_JSON_PATH);
            String email = JsonPath.read(registerJson, "$.email");
            String password = JsonPath.read(registerJson, "$.password");

            mockMvc.perform(MockMvcRequestBuilders.post("/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(registerJson))
                    .andExpect(status().isCreated());

            String token = accessToken(login(loginJson(email, password)).andExpect(status().isOk()).andReturn());

            mockMvc.perform(MockMvcRequestBuilders.get("/users/allusers")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("registered with uppercase, logs in with the same uppercase string")
        void shouldLogin_whenEmailHasUppercase() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.post("/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"firstName": "Qa", "lastName": "User", "email": "QA.User@Example.com",
                                     "password": "%s", "birthDate": "1990-01-01"}
                                    """.formatted(PASSWORD)))
                    .andExpect(status().isCreated());

            login(loginJson("QA.User@Example.com", PASSWORD)).andExpect(status().isOk());
        }

        @Test
        @DisplayName("email with surrounding spaces: trimmed as the feature README promises")
        void shouldLogin_whenEmailHasSurroundingSpaces() throws Exception {
            insertUserWithPassword(EMAIL, PASSWORD);

            login(loginJson("  " + EMAIL + "  ", PASSWORD)).andExpect(status().isOk());
        }

        @Test
        @DisplayName("old password that no longer meets the registration regex still logs in")
        void shouldLogin_whenPasswordPredatesCurrentPolicy() throws Exception {
            insertUserWithPassword(EMAIL, "short");

            login(loginJson(EMAIL, "short")).andExpect(status().isOk());
        }

        @Test
        @DisplayName("an invalid Authorization header does not block the public login route")
        void shouldLogin_whenRequestCarriesAnInvalidToken() throws Exception {
            insertUserWithPassword(EMAIL, PASSWORD);

            mockMvc.perform(MockMvcRequestBuilders.post(LOGIN_URL)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer not.a.token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(loginJson(EMAIL, PASSWORD)))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("401 - wrong credentials")
    class WrongCredentials {

        @Test
        @DisplayName("wrong password")
        void shouldReturn401_whenPasswordIsWrong() throws Exception {
            insertUserWithPassword(EMAIL, PASSWORD);

            login(loginJson(EMAIL, "Wr0ng!Passw0rd")).andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("unknown email")
        void shouldReturn401_whenEmailDoesNotExist() throws Exception {
            login(loginJson("ghost@example.com", PASSWORD)).andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("right password with inverted case")
        void shouldReturn401_whenPasswordCaseDiffers() throws Exception {
            insertUserWithPassword(EMAIL, PASSWORD);

            login(loginJson(EMAIL, PASSWORD.toUpperCase())).andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("wrong password and unknown email are indistinguishable: same status, same body")
        void shouldAnswerTheSame_forWrongPasswordAndUnknownEmail() throws Exception {
            insertUserWithPassword(EMAIL, PASSWORD);

            MvcResult wrongPassword = login(loginJson(EMAIL, "Wr0ng!Passw0rd")).andReturn();
            MvcResult unknownEmail = login(loginJson("ghost@example.com", "Wr0ng!Passw0rd")).andReturn();

            assertThat(wrongPassword.getResponse().getStatus()).isEqualTo(401);
            assertThat(unknownEmail.getResponse().getStatus()).isEqualTo(wrongPassword.getResponse().getStatus());
            assertThat(unknownEmail.getResponse().getContentAsString())
                    .isEqualTo(wrongPassword.getResponse().getContentAsString());
        }
    }

    @Nested
    @DisplayName("4xx - malformed requests")
    class MalformedRequests {

        @ParameterizedTest(name = "{0}")
        @ValueSource(strings = {
                "{\"email\": \"login.user@example.com\", \"password\": \"\"}",
                "{\"email\": \"login.user@example.com\", \"password\": \"   \"}",
                "{\"email\": \"login.user@example.com\"}",
                "{\"password\": \"Str0ng!Passw0rd\"}",
                "{\"email\": \"\", \"password\": \"Str0ng!Passw0rd\"}",
                "{\"email\": \"not-an-email\", \"password\": \"Str0ng!Passw0rd\"}",
                "{}"
        })
        @DisplayName("400 - missing or invalid field")
        void shouldReturn400_whenFieldIsMissingOrInvalid(String body) throws Exception {
            login(body)
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                    .andExpect(jsonPath("$.status").value(400));
        }

        @Test
        @DisplayName("400 - syntactically invalid JSON")
        void shouldReturn400_whenBodyIsMalformedJson() throws Exception {
            login("{\"email\": ").andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("400 - no body")
        void shouldReturn400_whenBodyIsMissing() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.post(LOGIN_URL).contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("415 - Content-Type other than application/json")
        void shouldReturn415_whenContentTypeIsNotJson() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.post(LOGIN_URL)
                            .contentType(MediaType.TEXT_PLAIN)
                            .content(loginJson(EMAIL, PASSWORD)))
                    .andExpect(status().isUnsupportedMediaType());
        }

        @Test
        @DisplayName("405 - GET on /auth/login")
        void shouldReturn405_whenMethodIsGet() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.get(LOGIN_URL))
                    .andExpect(status().isMethodNotAllowed());
        }
    }
}
