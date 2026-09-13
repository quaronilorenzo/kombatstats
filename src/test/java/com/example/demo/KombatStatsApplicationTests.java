package com.example.demo;

import com.example.demo.testcontainers.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(PostgreDbTestsConfiguration.class)
@ImportTestcontainers
class KombatStatsApplicationTests extends AbstractIntegrationTest {

	@Test
	void contextLoads() {
	}

}
