package com.example.demo.user.controller;


import com.example.demo.testcontainers.AbstractIntegrationTest;
import org.junit.experimental.results.ResultMatchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import com.fasterxml.jackson.databind.ObjectMapper;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
public class UserControllerIntegrationTests extends AbstractIntegrationTest {
    @Test
    void shouldReturn201_whenaddUserCorrectly() throws Exception{
        String json = """
        {
          "firstName": "Lorenzo",
          "lastName": "Quaroni",
          "email": "lorenzo.quaroni@example.com",
          "birthDate": "1999-04-23",
          "sport": ["BJJ", "MMA"]
        }
        """;
        RequestBuilder request = MockMvcRequestBuilders
                .post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json)
                .accept(MediaType.APPLICATION_JSON);
        ResultMatcher expectedResponse = MockMvcResultMatchers.status().isCreated();
        mockMvc.perform(request).andExpect(expectedResponse);
    }
    @Autowired
    protected MockMvc mockMvc;
    @Autowired
    protected ObjectMapper objectMapper;
}
