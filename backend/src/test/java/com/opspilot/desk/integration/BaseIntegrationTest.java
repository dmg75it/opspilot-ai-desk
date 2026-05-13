package com.opspilot.desk.integration;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Base class for integration tests.
 * Uses the 'test' Spring profile which connects to a local PostgreSQL instance.
 * Testcontainers is not used here due to Docker API version incompatibility
 * with Docker Engine 29.x (minimum API 1.40, but docker-java shaded library requests 1.32).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles({"fake-ai", "test"})
public abstract class BaseIntegrationTest {
}
