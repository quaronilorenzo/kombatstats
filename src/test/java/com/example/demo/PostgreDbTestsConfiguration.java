package com.example.demo;

import com.example.demo.testcontainers.IntegrationTestContainers;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;

@TestConfiguration(proxyBeanMethods = false)
@ImportTestcontainers(IntegrationTestContainers.class)
public class PostgreDbTestsConfiguration {}
