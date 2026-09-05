package com.example.demo.testcontainers;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;

public interface IntegrationTestContainers {
    @Container
    @ServiceConnection
    PostgreSQLContainer<?> postgresTest = new PostgreSQLContainer<>("postgres:16:9");
}
