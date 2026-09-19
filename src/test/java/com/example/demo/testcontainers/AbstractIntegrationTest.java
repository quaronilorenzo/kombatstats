package com.example.demo.testcontainers;

import com.example.demo.PostgreDbTestsConfiguration;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@SpringBootTest
@Import(PostgreDbTestsConfiguration.class)
@AutoConfigureMockMvc
public abstract class AbstractIntegrationTest {
    @Autowired
    Flyway flyway;
    @BeforeEach
    void resetDatabase(){
        flyway.clean();
        flyway.migrate();
    }
    protected String readJson(String path) throws IOException {
        ClassPathResource classPathResource = new ClassPathResource(path);
        return classPathResource.getContentAsString(StandardCharsets.UTF_8);
    }
}
