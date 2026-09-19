package com.example.demo.user.controller;


import com.example.demo.testcontainers.AbstractIntegrationTest;
import com.example.demo.testcontainers.JsonPathMock;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class UserControllerIntegrationTests extends AbstractIntegrationTest {
    @Autowired
    protected MockMvc mockMvc;
    @Test
    void shouldReturn201_whenaddUserCorrectly() throws Exception{
        String json = readJson(JsonPathMock.VALID_USER_REQUEST_JSON_PATH);
        RequestBuilder request = MockMvcRequestBuilders
                .post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json)
                .accept(MediaType.APPLICATION_JSON);
        ResultMatcher expectedResponse = status().isCreated();
        mockMvc.perform(request)
                .andExpectAll(expectedResponse,
                        content().json(readJson(JsonPathMock.VALID_USER_RESPONSE_JSON_PATH)),
                        content().contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(print());
    }

}
