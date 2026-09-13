package com.example.demo.testcontainers;

import com.example.demo.PostgreDbTestsConfiguration;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(PostgreDbTestsConfiguration.class)
@AutoConfigureMockMvc
public abstract class AbstractIntegrationTest {
    @BeforeEach
    void resetDatabase(){
        flyway.clean();
        flyway.migrate();
    }

    @Autowired
    Flyway flyway;
}
