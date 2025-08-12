package com.shopper.integration;

import com.shopper.BaseIntegrationTest;
import com.shopper.dto.AddToCartDto;
import com.shopper.dto.AuthResponseDto;
import com.shopper.dto.CreateProductDto;
import com.shopper.dto.LoginDto;
import com.shopper.entity.Product;
import com.shopper.service.DualDatabaseStrategy;
import com.shopper.service.FeatureFlagService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for feature flag controlled database routing
 * Verifies that users are routed to the correct database based on feature flags
 */
@DisplayName("Feature Flag Integration Tests")
class FeatureFlagIntegrationTest extends BaseIntegrationTest {

    @MockBean
    private FeatureFlagService featureFlagService;

    @Autowired
    private DualDatabaseStrategy dualDatabaseStrategy;

    private String adminToken;
    private String userToken;
    private Product testProduct;

    private static final String USE_NEON_FLAG = "use-neon";

    @BeforeEach
    void setupTokensAndTestData() throws Exception {
        // Setup comprehensive mocks for FeatureFlagService
        // Mock all possible feature flag calls with default values
        when(featureFlagService.getBooleanValue(anyString(), eq("new-flow"), eq(false)))
            .thenReturn(false);
        when(featureFlagService.getBooleanValue(anyString(), eq("enhanced-product-details"), eq(false)))
            .thenReturn(false);
        when(featureFlagService.getBooleanValue(anyString(), eq("premium-features"), eq(false)))
            .thenReturn(false);
        when(featureFlagService.getBooleanValue(anyString(), eq("beta-features"), eq(false)))
            .thenReturn(false);
        
        // Default mock for USE_NEON_FLAG - will be overridden in specific tests
        when(featureFlagService.getBooleanValue(anyString(), eq(USE_NEON_FLAG), eq(false)))
            .thenReturn(false);

        // Get admin token
        LoginDto adminLogin = new LoginDto();
        adminLogin.setUsername("admin@test.com");
        adminLogin.setPassword("admin123");

        MvcResult adminResult = mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(adminLogin)))
                .andExpect(status().isOk())
                .andReturn();

        AuthResponseDto adminResponse = fromJson(
            adminResult.getResponse().getContentAsString(),
            AuthResponseDto.class
        );
        adminToken = adminResponse.getAccessToken();

        // Get user token
        LoginDto userLogin = new LoginDto();
        userLogin.setUsername("testuser@test.com");
        userLogin.setPassword("testpass123");

        MvcResult userResult = mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(userLogin)))
                .andExpect(status().isOk())
                .andReturn();

        AuthResponseDto userResponse = fromJson(
            userResult.getResponse().getContentAsString(),
            AuthResponseDto.class
        );
        userToken = userResponse.getAccessToken();

        // Create test product
        CreateProductDto productDto = new CreateProductDto();
        productDto.setName("Feature Flag Test Product");
        productDto.setDescription("Product for testing feature flag routing");
        productDto.setPrice(BigDecimal.valueOf(19.99));
        productDto.setImageUrl("https://example.com/ff-test-product.jpg");
        productDto.setCategory("Test");
        productDto.setStockQuantity(50);

        MvcResult productResult = mockMvc.perform(post("/api/products")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(productDto)))
                .andExpect(status().isOk())
                .andReturn();

        testProduct = fromJson(productResult.getResponse().getContentAsString(), Product.class);
    }

    @Test
    @DisplayName("Should route admin user to primary database when feature flag is false")
    void testAdminUserPrimaryRouting() throws Exception {
        String adminUserId = testAdminUser.getId().toString();
        
        // Mock feature flag to return false for admin
        when(featureFlagService.getBooleanValue(eq(adminUserId), eq(USE_NEON_FLAG), eq(false)))
            .thenReturn(false);
        when(featureFlagService.getBooleanValue(eq("admin"), eq(USE_NEON_FLAG), eq(false)))
            .thenReturn(false);

        // Admin performs operations
        mockMvc.perform(get("/api/products")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        // Verify feature flag strategy correctly identifies routing
        assertThat(dualDatabaseStrategy.shouldUseSecondaryForRead(adminUserId)).isFalse();
    }

    @Test
    @DisplayName("Should route admin user to secondary database when feature flag is true")
    void testAdminUserSecondaryRouting() throws Exception {
        String adminUserId = testAdminUser.getId().toString();

        // Mock feature flag to return true for admin
        when(featureFlagService.getBooleanValue(eq(adminUserId), eq(USE_NEON_FLAG), eq(false)))
            .thenReturn(true);
        when(featureFlagService.getBooleanValue(eq("admin"), eq(USE_NEON_FLAG), eq(false)))
            .thenReturn(true);

        // Admin performs operations
        mockMvc.perform(get("/api/products")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        // Verify feature flag strategy correctly identifies routing
        assertThat(dualDatabaseStrategy.shouldUseSecondaryForRead(adminUserId)).isTrue();
    }

    @Test
    @DisplayName("Should route regular user to primary database when feature flag is false")
    void testRegularUserPrimaryRouting() throws Exception {
        String regularUserId = testRegularUser.getId().toString();

        // Mock feature flag to return false for regular user
        when(featureFlagService.getBooleanValue(eq(regularUserId), eq(USE_NEON_FLAG), eq(false)))
            .thenReturn(false);

        // User adds item to cart
        AddToCartDto addToCartDto = new AddToCartDto();
        addToCartDto.setProductId(testProduct.getId());
        addToCartDto.setQuantity(1);

        mockMvc.perform(post("/api/cart/add")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(addToCartDto)))
                .andExpect(status().isOk());

        // User retrieves cart
        mockMvc.perform(get("/api/cart")
                .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        // Verify feature flag strategy correctly identifies routing
        assertThat(dualDatabaseStrategy.shouldUseSecondaryForRead(regularUserId)).isFalse();
    }

    @Test
    @DisplayName("Should route regular user to secondary database when feature flag is true")
    void testRegularUserSecondaryRouting() throws Exception {
        String regularUserId = testRegularUser.getId().toString();

        // Mock feature flag to return true for regular user
        when(featureFlagService.getBooleanValue(eq(regularUserId), eq(USE_NEON_FLAG), eq(false)))
            .thenReturn(true);

        // User adds item to cart
        AddToCartDto addToCartDto = new AddToCartDto();
        addToCartDto.setProductId(testProduct.getId());
        addToCartDto.setQuantity(2);

        mockMvc.perform(post("/api/cart/add")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(addToCartDto)))
                .andExpect(status().isOk());

        // User retrieves cart
        mockMvc.perform(get("/api/cart")
                .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        // Verify feature flag strategy correctly identifies routing
        assertThat(dualDatabaseStrategy.shouldUseSecondaryForRead(regularUserId)).isTrue();
    }

    @Test
    @DisplayName("Should handle different routing for different users simultaneously")
    void testMixedUserRouting() throws Exception {
        String adminUserId = testAdminUser.getId().toString();
        String regularUserId = testRegularUser.getId().toString();

        // Mock feature flags - admin uses secondary, regular user uses primary
        when(featureFlagService.getBooleanValue(eq(adminUserId), eq(USE_NEON_FLAG), eq(false)))
            .thenReturn(true);
        when(featureFlagService.getBooleanValue(eq(regularUserId), eq(USE_NEON_FLAG), eq(false)))
            .thenReturn(false);
        when(featureFlagService.getBooleanValue(eq("admin"), eq(USE_NEON_FLAG), eq(false)))
            .thenReturn(true);

        // Admin operation
        mockMvc.perform(get("/api/products")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        // Regular user operation
        AddToCartDto addToCartDto = new AddToCartDto();
        addToCartDto.setProductId(testProduct.getId());
        addToCartDto.setQuantity(1);

        mockMvc.perform(post("/api/cart/add")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(addToCartDto)))
                .andExpect(status().isOk());

        // Verify different routing for different users
        assertThat(dualDatabaseStrategy.shouldUseSecondaryForRead(adminUserId)).isTrue();
        assertThat(dualDatabaseStrategy.shouldUseSecondaryForRead(regularUserId)).isFalse();
    }

    @Test
    @DisplayName("Should fallback to primary when feature flag service fails")
    void testFeatureFlagServiceFailureFallback() throws Exception {
        String userId = testRegularUser.getId().toString();

        // Mock feature flag service to throw exception
        when(featureFlagService.getBooleanValue(eq(userId), eq(USE_NEON_FLAG), eq(false)))
            .thenThrow(new RuntimeException("Feature flag service unavailable"));

        // User operation should still work (fallback to primary)
        AddToCartDto addToCartDto = new AddToCartDto();
        addToCartDto.setProductId(testProduct.getId());
        addToCartDto.setQuantity(1);

        mockMvc.perform(post("/api/cart/add")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(addToCartDto)))
                .andExpect(status().isOk());

        // Should fallback to primary (false)
        assertThat(dualDatabaseStrategy.shouldUseSecondaryForRead(userId)).isFalse();
    }

    @Test
    @DisplayName("Should maintain consistent routing during user session")
    void testConsistentSessionRouting() throws Exception {
        String userId = testRegularUser.getId().toString();

        // Mock feature flag to return true consistently
        when(featureFlagService.getBooleanValue(eq(userId), eq(USE_NEON_FLAG), eq(false)))
            .thenReturn(true);

        // Perform multiple operations in the same session
        for (int i = 1; i <= 3; i++) {
            AddToCartDto addToCartDto = new AddToCartDto();
            addToCartDto.setProductId(testProduct.getId());
            addToCartDto.setQuantity(i);

            mockMvc.perform(post("/api/cart/add")
                    .header("Authorization", "Bearer " + userToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(toJson(addToCartDto)))
                    .andExpect(status().isOk());

            // Check cart after each addition
            mockMvc.perform(get("/api/cart")
                    .header("Authorization", "Bearer " + userToken))
                    .andExpect(status().isOk());

            // Verify consistent routing
            assertThat(dualDatabaseStrategy.shouldUseSecondaryForRead(userId)).isTrue();
        }
    }

    @Test
    @DisplayName("Should handle feature flag changes dynamically")
    void testDynamicFeatureFlagChanges() throws Exception {
        String userId = testRegularUser.getId().toString();

        // Initially feature flag is false
        when(featureFlagService.getBooleanValue(eq(userId), eq(USE_NEON_FLAG), eq(false)))
            .thenReturn(false);

        assertThat(dualDatabaseStrategy.shouldUseSecondaryForRead(userId)).isFalse();

        // Change feature flag to true
        when(featureFlagService.getBooleanValue(eq(userId), eq(USE_NEON_FLAG), eq(false)))
            .thenReturn(true);

        assertThat(dualDatabaseStrategy.shouldUseSecondaryForRead(userId)).isTrue();

        // Change feature flag back to false
        when(featureFlagService.getBooleanValue(eq(userId), eq(USE_NEON_FLAG), eq(false)))
            .thenReturn(false);

        assertThat(dualDatabaseStrategy.shouldUseSecondaryForRead(userId)).isFalse();
    }

    @Test
    @DisplayName("Should work correctly when secondary database is disabled")
    void testSecondaryDatabaseDisabled() throws Exception {
        String userId = testRegularUser.getId().toString();

        // Even if feature flag is true, if secondary database is disabled, should use primary
        when(featureFlagService.getBooleanValue(eq(userId), eq(USE_NEON_FLAG), eq(false)))
            .thenReturn(true);

        // Test operations still work
        AddToCartDto addToCartDto = new AddToCartDto();
        addToCartDto.setProductId(testProduct.getId());
        addToCartDto.setQuantity(1);

        mockMvc.perform(post("/api/cart/add")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(addToCartDto)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/cart")
                .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should verify admin access permissions across both databases")
    void testAdminPermissionsAcrossDatabases() throws Exception {
        String adminUserId = testAdminUser.getId().toString();

        // Test with feature flag false (primary)
        when(featureFlagService.getBooleanValue(eq(adminUserId), eq(USE_NEON_FLAG), eq(false)))
            .thenReturn(false);
        when(featureFlagService.getBooleanValue(eq("admin"), eq(USE_NEON_FLAG), eq(false)))
            .thenReturn(false);

        // Admin can create products
        CreateProductDto productDto = new CreateProductDto();
        productDto.setName("Admin Permission Test Product");
        productDto.setDescription("Testing admin permissions");
        productDto.setPrice(BigDecimal.valueOf(49.99));
        productDto.setImageUrl("https://example.com/admin-perm-test.jpg");
        productDto.setCategory("Admin");
        productDto.setStockQuantity(25);

        mockMvc.perform(post("/api/products")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(productDto)))
                .andExpect(status().isOk());

        // Test with feature flag true (secondary)
        when(featureFlagService.getBooleanValue(eq(adminUserId), eq(USE_NEON_FLAG), eq(false)))
            .thenReturn(true);
        when(featureFlagService.getBooleanValue(eq("admin"), eq(USE_NEON_FLAG), eq(false)))
            .thenReturn(true);

        // Admin can still access products
        mockMvc.perform(get("/api/products")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("Should verify regular user permissions across both databases")
    void testRegularUserPermissionsAcrossDatabases() throws Exception {
        String userId = testRegularUser.getId().toString();

        // Test with feature flag false (primary)
        when(featureFlagService.getBooleanValue(eq(userId), eq(USE_NEON_FLAG), eq(false)))
            .thenReturn(false);

        // Regular user can add to cart
        AddToCartDto addToCartDto = new AddToCartDto();
        addToCartDto.setProductId(testProduct.getId());
        addToCartDto.setQuantity(1);

        mockMvc.perform(post("/api/cart/add")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(addToCartDto)))
                .andExpect(status().isOk());

        // Regular user cannot create products (should get 403 or appropriate error)
        CreateProductDto productDto = new CreateProductDto();
        productDto.setName("Unauthorized Product");
        productDto.setDescription("This should fail");
        productDto.setPrice(BigDecimal.valueOf(99.99));
        productDto.setImageUrl("https://example.com/unauthorized.jpg");
        productDto.setCategory("Fail");
        productDto.setStockQuantity(10);

        mockMvc.perform(post("/api/products")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(productDto)))
                .andExpect(status().isForbidden());

        // Test with feature flag true (secondary)
        when(featureFlagService.getBooleanValue(eq(userId), eq(USE_NEON_FLAG), eq(false)))
            .thenReturn(true);

        // Regular user can still add to cart
        addToCartDto.setQuantity(2);
        mockMvc.perform(post("/api/cart/add")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(addToCartDto)))
                .andExpect(status().isOk());

        // Regular user still cannot create products
        mockMvc.perform(post("/api/products")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(productDto)))
                .andExpect(status().isForbidden());
    }
} 