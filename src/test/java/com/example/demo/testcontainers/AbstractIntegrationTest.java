package com.example.demo.testcontainers;

import com.example.demo.PostgreDbTestsConfiguration;
import com.example.demo.security.JwtService;
import com.example.demo.user.entity.User;
import org.flywaydb.core.Flyway;
import org.json.JSONArray;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(PostgreDbTestsConfiguration.class)
@AutoConfigureMockMvc
public abstract class AbstractIntegrationTest {
    @Autowired
    Flyway flyway;
    @Autowired
    protected JdbcClient jdbcClient;
    @Autowired
    protected JwtService jwtService;
    @BeforeEach
    void resetDatabase(){
        flyway.migrate();
        jdbcClient.sql("TRUNCATE TABLE user_sport, users CASCADE").update();
    }
    protected String readJson(String path) throws IOException {
        ClassPathResource classPathResource = new ClassPathResource(path);
        return classPathResource.getContentAsString(StandardCharsets.UTF_8);
    }

    /** Inserts a user straight into the table and returns its id; the hash is not a real BCrypt one. */
    protected long insertUser(String email) {
        return insertUser(email, "not-a-real-hash");
    }

    protected long insertUser(String email, String hashPassword) {
        return jdbcClient.sql("""
                        INSERT INTO users (id, first_name, last_name, email, birth_date, hash_password)
                        VALUES (nextval('users_seq'), 'Test', 'User', :email, DATE '1999-04-23', :hash)
                        RETURNING id
                        """)
                .param("email", email)
                .param("hash", hashPassword)
                .query(Long.class)
                .single();
    }

    /** Real token from JwtService, so the request goes through JwtAuthenticationFilter like a client's would. */
    protected String bearerFor(long userId) {
        User user = new User();
        user.setId(userId);
        return "Bearer " + jwtService.generateToken(user);
    }

    protected RequestPostProcessor asUser(long userId) {
        String header = bearerFor(userId);
        return request -> {
            request.addHeader(HttpHeaders.AUTHORIZATION, header);
            return request;
        };
    }
}
