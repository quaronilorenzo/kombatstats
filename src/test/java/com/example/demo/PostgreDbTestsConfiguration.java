package com.example.demo;

import org.testcontainers.containers.PostgreSQLContainer;

public abstract class PostgreDbTestsConfiguration {
    static PostgreSQLContainer<?> postgreSQLContainer = new PostgreSQLContainer<>("postgres:16.9");
        

}
