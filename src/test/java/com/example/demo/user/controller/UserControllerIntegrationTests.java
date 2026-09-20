package com.example.demo.user.controller;

import com.example.demo.testcontainers.AbstractIntegrationTest;
import com.example.demo.testcontainers.JsonPathMock;
import com.example.demo.user.entity.User;
import com.example.demo.user.repository.UserRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

// entry point completo di AssertJ: contiene assertThat per Optional, per List/Iterable
// e per i tipi primitivi. AssertionsForClassTypes non ha quello per le collezioni.
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class UserControllerIntegrationTests extends AbstractIntegrationTest {

    @Autowired
    protected MockMvc mockMvc;
    @Autowired
    protected UserRepository userRepository;
    // JdbcClient e' auto-configurato da Spring Boot appena esiste un DataSource.
    // Serve per leggere user_sport con SQL diretto: quella collezione e' @ElementCollection
    // LAZY, quindi toccarla dall'entity fuori transazione darebbe LazyInitializationException.
    @Autowired
    protected JdbcClient jdbcClient;

    @Test
    void shouldReturn201_whenaddUserCorrectly() throws Exception {
        String json = readJson(JsonPathMock.VALID_USER_REQUEST_JSON_PATH);
        String expectedEmail = JsonPath.read(json, "$.email");
        String expectedFirstName = JsonPath.read(json, "$.firstName");
        String expectedLastName = JsonPath.read(json, "$.lastName");
        String expectedBirthDate = JsonPath.read(json, "$.birthDate");
        List<String> expectedSports = JsonPath.read(json, "$.sport");

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
        assertThat(user).as("utente con email %s", expectedEmail).isPresent();
        User saved = user.orElseThrow();
        assertThat(saved.getFirstName()).as("first_name salvato").isEqualTo(expectedFirstName);
        assertThat(saved.getLastName()).as("last_name salvato").isEqualTo(expectedLastName);
        assertThat(saved.getBirthDate()).as("birth_date salvata").isEqualTo(LocalDate.parse(expectedBirthDate));
        assertThat(saved.getId()).as("id generato").isPositive();

        // need a better way to do this  ?
        List<String> savedSports = jdbcClient
                .sql("SELECT sport FROM user_sport WHERE user_id = :userId")
                .param("userId", saved.getId())
                .query(String.class)
                .list();
        assertThat(savedSports)
                .as("righe in user_sport per l'utente %d", saved.getId())
                .containsExactlyInAnyOrderElementsOf(expectedSports);
    }

}
