package com.example.demo.security;

import com.example.demo.testcontainers.AbstractIntegrationTest;
import com.example.demo.testcontainers.JsonPathMock;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.io.Encoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class JwtAuthenticationIntegrationTests extends AbstractIntegrationTest {

    @Autowired
    protected MockMvc mockMvc;
    @Value("${app.jwt.secret}")
    private String secret;

    private static final String PROTECTED_URL = "/users/allusers";

    private long userId;

    @BeforeEach
    void seedCaller() {
        userId = insertUser("caller@example.com");
    }

    private SecretKey key() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
    }

    private static Date inMinutes(long minutes) {
        return Date.from(Instant.now().plusSeconds(minutes * 60));
    }

    private ResultActions getProtected(String authorization) throws Exception {
        return mockMvc.perform(MockMvcRequestBuilders.get(PROTECTED_URL)
                .header(HttpHeaders.AUTHORIZATION, authorization));
    }

    /** Every route that must require a token; Supplier because each test needs a fresh builder. */
    static Stream<Arguments> protectedRoutes() {
        String postBody = "{\"authorId\": 1, \"title\": \"t\", \"content\": \"c\"}";
        return Stream.of(
                Arguments.of("GET /users/allusers", (Supplier<MockHttpServletRequestBuilder>) () ->
                        MockMvcRequestBuilders.get("/users/allusers")),
                Arguments.of("GET /users/userbyid", (Supplier<MockHttpServletRequestBuilder>) () ->
                        MockMvcRequestBuilders.get("/users/userbyid").param("id", "1")),
                Arguments.of("GET /users/userbyname", (Supplier<MockHttpServletRequestBuilder>) () ->
                        MockMvcRequestBuilders.get("/users/userbyname").param("name", "Test")),
                Arguments.of("GET /posts/postsbyauthor", (Supplier<MockHttpServletRequestBuilder>) () ->
                        MockMvcRequestBuilders.get("/posts/postsbyauthor").param("authorId", "1")),
                Arguments.of("GET /posts/recentposts", (Supplier<MockHttpServletRequestBuilder>) () ->
                        MockMvcRequestBuilders.get("/posts/recentposts")),
                Arguments.of("GET /posts/recentpostscursor", (Supplier<MockHttpServletRequestBuilder>) () ->
                        MockMvcRequestBuilders.get("/posts/recentpostscursor")),
                Arguments.of("POST /posts", (Supplier<MockHttpServletRequestBuilder>) () ->
                        MockMvcRequestBuilders.post("/posts").contentType(MediaType.APPLICATION_JSON).content(postBody))
        );
    }

    @Nested
    @DisplayName("route rules")
    class RouteRules {

        @ParameterizedTest(name = "{0}")
        @MethodSource("com.example.demo.security.JwtAuthenticationIntegrationTests#protectedRoutes")
        @DisplayName("401 - protected route without token")
        void shouldReturn401_whenProtectedRouteHasNoToken(String route, Supplier<MockHttpServletRequestBuilder> request) throws Exception {
            mockMvc.perform(request.get()).andExpect(status().isUnauthorized());
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("com.example.demo.security.JwtAuthenticationIntegrationTests#protectedRoutes")
        @DisplayName("not 401 - protected route with a valid token")
        void shouldLetThrough_whenProtectedRouteHasValidToken(String route, Supplier<MockHttpServletRequestBuilder> request) throws Exception {
            int status = mockMvc.perform(request.get().with(asUser(userId))).andReturn().getResponse().getStatus();

            assertThat(status).as("status for %s with a valid token", route).isNotEqualTo(401);
        }

        @Test
        @DisplayName("public - /auth/register without token")
        void shouldReachRegister_withoutToken() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.post("/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(readJson(JsonPathMock.VALID_USER_REQUEST_JSON_PATH)))
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("public - /auth/login without token is reached (400 from validation, not 401)")
        void shouldReachLogin_withoutToken() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("public - an invalid token does not block a public route")
        void shouldReachRegister_whenTokenIsInvalid() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.post("/auth/register")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer not.a.token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(readJson(JsonPathMock.VALID_USER_REQUEST_JSON_PATH)))
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("unknown path: 401 without token, 404 with token")
        void shouldHideUnknownPaths_fromAnonymousCallers() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.get("/does/not/exist"))
                    .andExpect(status().isUnauthorized());
            mockMvc.perform(MockMvcRequestBuilders.get("/does/not/exist").with(asUser(userId)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("400 - validation errors still reach an authenticated client (/error is open)")
        void shouldReturn400_whenAuthenticatedRequestIsMalformed() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.post("/posts")
                            .with(asUser(userId))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("Authorization header")
    class AuthorizationHeader {

        @Test
        @DisplayName("200 - valid token")
        void shouldReturn200_whenTokenIsValid() throws Exception {
            getProtected(bearerFor(userId)).andExpect(status().isOk());
        }

        @ParameterizedTest(name = "\"{0}\"")
        @ValueSource(strings = {"Bearer ", "Bearer    ", "Bearer"})
        @DisplayName("401, not 500 - Bearer with nothing after it")
        void shouldReturn401_whenTokenIsEmpty(String authorization) throws Exception {
            getProtected(authorization).andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("401 - lowercase 'bearer'")
        void shouldReturn401_whenSchemeIsLowercase() throws Exception {
            getProtected(bearerFor(userId).replace("Bearer", "bearer")).andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("401 - token without the 'Bearer ' prefix")
        void shouldReturn401_whenPrefixIsMissing() throws Exception {
            getProtected(bearerFor(userId).substring("Bearer ".length())).andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("401 - another scheme carrying a valid token")
        void shouldReturn401_whenSchemeIsNotBearer() throws Exception {
            getProtected(bearerFor(userId).replace("Bearer", "Token")).andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("401 - Basic credentials")
        void shouldReturn401_whenBasicAuthIsUsed() throws Exception {
            String basic = Encoders.BASE64.encode("caller@example.com:password".getBytes(StandardCharsets.UTF_8));
            getProtected("Basic " + basic).andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("401 - two spaces after Bearer")
        void shouldReturn401_whenThereAreTwoSpaces() throws Exception {
            getProtected(bearerFor(userId).replace("Bearer ", "Bearer  ")).andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("token content")
    class TokenContent {

        @ParameterizedTest(name = "\"{0}\"")
        @ValueSource(strings = {"not-a-jwt", "abc.def.ghi", "a.b"})
        @DisplayName("401 - not a JWT")
        void shouldReturn401_whenTokenIsMalformed(String token) throws Exception {
            getProtected("Bearer " + token).andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("401 - signature changed")
        void shouldReturn401_whenSignatureIsTampered() throws Exception {
            String token = bearerFor(userId);
            int signatureStart = token.lastIndexOf('.') + 1;
            char replacement = token.charAt(signatureStart) == 'A' ? 'B' : 'A';

            getProtected(token.substring(0, signatureStart) + replacement + token.substring(signatureStart + 1))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("401 - signed with another key")
        void shouldReturn401_whenSignedWithAnotherKey() throws Exception {
            SecretKey otherKey = Keys.hmacShaKeyFor("fedcba9876543210fedcba9876543210".getBytes(StandardCharsets.UTF_8));
            String token = Jwts.builder().subject(String.valueOf(userId)).expiration(inMinutes(10)).signWith(otherKey).compact();

            getProtected("Bearer " + token).andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("401 - expired")
        void shouldReturn401_whenExpired() throws Exception {
            String token = Jwts.builder().subject(String.valueOf(userId))
                    .issuedAt(inMinutes(-30))
                    .expiration(inMinutes(-1))
                    .signWith(key())
                    .compact();

            getProtected("Bearer " + token).andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("401 - unsigned (alg=none)")
        void shouldReturn401_whenUnsigned() throws Exception {
            String token = Jwts.builder().subject(String.valueOf(userId)).expiration(inMinutes(10)).compact();

            getProtected("Bearer " + token).andExpect(status().isUnauthorized());
        }

        @ParameterizedTest(name = "sub=\"{0}\"")
        @ValueSource(strings = {"abc", "1.5", "99999999999999999999"})
        @DisplayName("401, not 500 - validly signed but sub is not a long")
        void shouldReturn401_whenSubjectIsNotALong(String subject) throws Exception {
            String token = Jwts.builder().subject(subject).expiration(inMinutes(10)).signWith(key()).compact();

            getProtected("Bearer " + token).andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("401, not 500 - validly signed but no sub")
        void shouldReturn401_whenSubjectIsMissing() throws Exception {
            String token = Jwts.builder().expiration(inMinutes(10)).signWith(key()).compact();

            getProtected("Bearer " + token).andExpect(status().isUnauthorized());
        }

        @Test
        @Disabled("""
                Not decided yet: today the filter trusts sub and never checks the user still \
                exists, so a deleted user keeps access until the token expires (15 minutes). \
                Consistent with "no revocation" in the feature README; enable if the filter \
                starts checking the user.""")
        @DisplayName("401 - token of a user that no longer exists")
        void shouldReturn401_whenUserNoLongerExists() throws Exception {
            String token = bearerFor(userId);
            jdbcClient.sql("DELETE FROM users WHERE id = :id").param("id", userId).update();

            getProtected(token).andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("stateless")
    class Stateless {

        @Test
        @DisplayName("no session cookie, and authentication does not survive to the next request")
        void shouldNotKeepAuthentication_betweenRequests() throws Exception {
            getProtected(bearerFor(userId))
                    .andExpect(status().isOk())
                    .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE));

            mockMvc.perform(MockMvcRequestBuilders.get(PROTECTED_URL))
                    .andExpect(status().isUnauthorized());
        }
    }
}
