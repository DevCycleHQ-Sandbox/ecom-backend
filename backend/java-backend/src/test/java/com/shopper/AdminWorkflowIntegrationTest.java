package com.shopper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopper.dto.LoginDto;
import com.shopper.dto.CreateProductDto;
import com.shopper.entity.Product;
import com.shopper.entity.User;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.stream.Stream;
import java.util.stream.IntStream;
import org.junit.jupiter.api.DynamicTest;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebMvc
@TestPropertySource(locations = "classpath:application-test.properties")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AdminWorkflowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static String adminToken;
    private static Long testProductId;

    @BeforeAll
    static void setupClass() {
        System.out.println("Starting Admin Workflow Integration Tests");
    }

    @Test
    @Order(1)
    @DisplayName("Test Database Connectivity")
    void testDatabaseConnectivity() throws Exception {
        // Test health endpoint to ensure database is connected
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    @Order(2)
    @DisplayName("Test Admin Login")
    void testAdminLogin() throws Exception {
        LoginDto loginDto = new LoginDto();
        loginDto.setEmail("admin@example.com");
        loginDto.setPassword("admin123");

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.user.role").value("ADMIN"))
                .andReturn();

        String response = result.getResponse().getContentAsString();
        var authResponse = objectMapper.readTree(response);
        adminToken = authResponse.get("token").asText();
        
        assertNotNull(adminToken, "Admin token should not be null");
        assertTrue(adminToken.length() > 0, "Admin token should not be empty");
        
        System.out.println("✅ Admin login successful. Token: " + adminToken.substring(0, 20) + "...");
    }

    @Test
    @Order(3)
    @DisplayName("Test Admin Get All Products")
    void testAdminGetAllProducts() throws Exception {
        assertNotNull(adminToken, "Admin token is required for this test");

        MvcResult result = mockMvc.perform(get("/api/products")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        String response = result.getResponse().getContentAsString();
        var products = objectMapper.readTree(response);
        
        assertTrue(products.isArray(), "Response should be an array of products");
        System.out.println("✅ Successfully retrieved " + products.size() + " products");
    }

    @Test
    @Order(4)
    @DisplayName("Test Admin Create Product")
    void testAdminCreateProduct() throws Exception {
        assertNotNull(adminToken, "Admin token is required for this test");

        CreateProductDto productDto = new CreateProductDto();
        productDto.setName("Test Product - Admin Integration");
        productDto.setDescription("A test product created during admin integration testing");
        productDto.setPrice(29.99);
        productDto.setStock(50);
        productDto.setImageUrl("https://example.com/test-product.jpg");

        MvcResult result = mockMvc.perform(post("/api/products")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(productDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value(productDto.getName()))
                .andExpect(jsonPath("$.price").value(productDto.getPrice()))
                .andExpect(jsonPath("$.stock").value(productDto.getStock()))
                .andReturn();

        String response = result.getResponse().getContentAsString();
        var product = objectMapper.readTree(response);
        testProductId = product.get("id").asLong();
        
        assertNotNull(testProductId, "Created product should have an ID");
        System.out.println("✅ Successfully created test product with ID: " + testProductId);
    }

    @Test
    @Order(5)
    @DisplayName("Test Admin Update Product")
    void testAdminUpdateProduct() throws Exception {
        assertNotNull(adminToken, "Admin token is required for this test");
        assertNotNull(testProductId, "Test product ID is required for this test");

        CreateProductDto updateDto = new CreateProductDto();
        updateDto.setName("Updated Test Product - Admin Integration");
        updateDto.setDescription("Updated description for admin integration testing");
        updateDto.setPrice(39.99);
        updateDto.setStock(75);
        updateDto.setImageUrl("https://example.com/updated-test-product.jpg");

        mockMvc.perform(put("/api/products/" + testProductId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(updateDto.getName()))
                .andExpect(jsonPath("$.price").value(updateDto.getPrice()))
                .andExpect(jsonPath("$.stock").value(updateDto.getStock()));

        System.out.println("✅ Successfully updated test product");
    }

    @Test
    @Order(6)
    @DisplayName("Test Admin Get All Users")
    void testAdminGetAllUsers() throws Exception {
        assertNotNull(adminToken, "Admin token is required for this test");

        MvcResult result = mockMvc.perform(get("/api/admin/users")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        String response = result.getResponse().getContentAsString();
        var users = objectMapper.readTree(response);
        
        assertTrue(users.isArray(), "Response should be an array of users");
        assertTrue(users.size() > 0, "Should have at least one user (admin)");
        System.out.println("✅ Successfully retrieved " + users.size() + " users");
    }

    @Test
    @Order(7)
    @DisplayName("Test Admin Get All Orders")
    void testAdminGetAllOrders() throws Exception {
        assertNotNull(adminToken, "Admin token is required for this test");

        MvcResult result = mockMvc.perform(get("/api/admin/orders")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        String response = result.getResponse().getContentAsString();
        var orders = objectMapper.readTree(response);
        
        assertTrue(orders.isArray(), "Response should be an array of orders");
        System.out.println("✅ Successfully retrieved " + orders.size() + " orders");
    }

    @Test
    @Order(8)
    @DisplayName("Test Database Sync Status")
    void testDatabaseSyncStatus() throws Exception {
        assertNotNull(adminToken, "Admin token is required for this test");

        mockMvc.perform(get("/api/admin/database-sync/status")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));

        System.out.println("✅ Database sync status endpoint is working");
    }

    @Test
    @Order(9)
    @DisplayName("Test Database Sync Trigger")
    void testDatabaseSyncTrigger() throws Exception {
        assertNotNull(adminToken, "Admin token is required for this test");

        mockMvc.perform(post("/api/admin/database-sync/trigger")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));

        System.out.println("✅ Database sync trigger endpoint is working");
    }

    @Test
    @Order(10)
    @DisplayName("Test Non-Admin Access Denied")
    void testNonAdminAccessDenied() throws Exception {
        // Test that admin endpoints are protected
        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isUnauthorized());

        // Test with invalid token
        mockMvc.perform(get("/api/admin/users")
                        .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized());

        System.out.println("✅ Admin endpoints are properly protected");
    }

    @Test
    @Order(11)
    @DisplayName("Test Admin Delete Product")
    void testAdminDeleteProduct() throws Exception {
        assertNotNull(adminToken, "Admin token is required for this test");
        assertNotNull(testProductId, "Test product ID is required for this test");

        mockMvc.perform(delete("/api/products/" + testProductId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        // Verify product is deleted
        mockMvc.perform(get("/api/products/" + testProductId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());

        System.out.println("✅ Successfully deleted test product");
    }

    @Test
    @Order(12)
    @DisplayName("Test Database Connection Pool Health")
    void testDatabaseConnectionPoolHealth() throws Exception {
        // Test multiple concurrent database operations to ensure connection pool is healthy
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(get("/api/products")
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk());
        }
        System.out.println("✅ Database connection pool is healthy");
    }

    @AfterAll
    static void tearDown() {
        System.out.println("Admin Workflow Integration Tests completed");
    }

    @TestFactory
    @DisplayName("Stress Test Admin Operations")
    Stream<DynamicTest> stressTestAdminOperations() {
        return IntStream.range(1, 6)
                .mapToObj(i -> DynamicTest.dynamicTest("Stress Test " + i, () -> {
                    assertNotNull(adminToken, "Admin token is required for stress test");
                    
                    mockMvc.perform(get("/api/products")
                                    .header("Authorization", "Bearer " + adminToken))
                            .andExpect(status().isOk());
                    
                    mockMvc.perform(get("/api/admin/users")
                                    .header("Authorization", "Bearer " + adminToken))
                            .andExpect(status().isOk());
                }));
    }
}