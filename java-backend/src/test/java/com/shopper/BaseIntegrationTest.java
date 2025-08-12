package com.shopper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopper.entity.User;
import com.shopper.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Base class for integration tests providing common configuration and utilities
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.properties")
@Transactional
public abstract class BaseIntegrationTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected UserService userService;

    protected User testAdminUser;
    protected User testRegularUser;
    protected String adminJwtToken;
    protected String userJwtToken;

    @BeforeEach
    void baseSetUp() {
        createTestUsers();
    }

    protected void createTestUsers() {
        try {
            // Create admin user
            testAdminUser = userService.createUser(
                "admin@test.com",
                "admin@test.com", 
                "admin123",
                User.Role.ADMIN
            );

            // Create regular user
            testRegularUser = userService.createUser(
                "testuser@test.com",
                "testuser@test.com",
                "testpass123", 
                User.Role.USER
            );
        } catch (Exception e) {
            // Users might already exist, ignore
        }
    }

    protected String toJson(Object obj) throws Exception {
        return objectMapper.writeValueAsString(obj);
    }

    protected <T> T fromJson(String json, Class<T> clazz) throws Exception {
        return objectMapper.readValue(json, clazz);
    }
} 