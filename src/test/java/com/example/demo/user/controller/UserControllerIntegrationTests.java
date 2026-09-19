package com.example.demo.user.controller;


import com.example.demo.testcontainers.AbstractIntegrationTest;
import com.example.demo.testcontainers.JsonPathMock;
import org.junit.experimental.results.ResultMatchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.util.StreamUtils;

import java.nio.charset.Charset;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
public class UserControllerIntegrationTests extends AbstractIntegrationTest {
    @Autowired
    protected MockMvc mockMvc;
    @Test
    void shouldReturn201_whenaddUserCorrectly() throws Exception{
        String json = readJson(JsonPathMock.VALID_USER_JSON_PATH);
        RequestBuilder request = MockMvcRequestBuilders
                .post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json)
                .accept(MediaType.APPLICATION_JSON);
        ResultMatcher expectedResponse = MockMvcResultMatchers.status().isCreated();
        mockMvc.perform(request).andExpect(expectedResponse);
    }

}
