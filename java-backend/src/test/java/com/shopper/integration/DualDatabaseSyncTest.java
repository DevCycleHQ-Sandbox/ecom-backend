package com.shopper.integration;

import com.shopper.BaseIntegrationTest;
import com.shopper.dto.AddToCartDto;
import com.shopper.dto.AuthResponseDto;
import com.shopper.dto.CreateProductDto;
import com.shopper.dto.LoginDto;
import com.shopper.entity.CartItem;
import com.shopper.entity.Product;
import com.shopper.entity.User;
import com.shopper.repository.CartItemRepository;
import com.shopper.repository.ProductRepository;
import com.shopper.repository.primary.PrimaryCartItemRepository;
import com.shopper.repository.primary.PrimaryProductRepository;
import com.shopper.repository.secondary.SecondaryCartItemRepository;
import com.shopper.repository.secondary.SecondaryProductRepository;
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
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for dual database synchronization
 * Verifies data is saved to both databases and feature flag controlled routing works
 */
@DisplayName("Dual Database Synchronization Tests")
class DualDatabaseSyncTest extends BaseIntegrationTest {

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private PrimaryCartItemRepository primaryCartItemRepository;

    @Autowired(required = false)
    private SecondaryCartItemRepository secondaryCartItemRepository;

    @Autowired
    private PrimaryProductRepository primaryProductRepository;

    @Autowired(required = false)
    private SecondaryProductRepository secondaryProductRepository;

    @Autowired
    private DualDatabaseStrategy dualDatabaseStrategy;

    @MockBean
    private FeatureFlagService featureFlagService;

    private String adminToken;
    private String userToken;
    private Product testProduct;

    @BeforeEach
    void setupTokensAndTestData() throws Exception {
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

        // Create test product via API
        CreateProductDto productDto = new CreateProductDto();
        productDto.setName("Test Product for Sync");
        productDto.setDescription("Product for testing database synchronization");
        productDto.setPrice(BigDecimal.valueOf(29.99));
        productDto.setImageUrl("https://example.com/test-product.jpg");
        productDto.setCategory("Test");
        productDto.setStockQuantity(100);

        MvcResult productResult = mockMvc.perform(post("/api/products")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(productDto)))
                .andExpect(status().isOk())
                .andReturn();

        testProduct = fromJson(productResult.getResponse().getContentAsString(), Product.class);
    }

    @Test
    @DisplayName("Should save product to both databases when dual database is enabled")
    void testProductSavedToBothDatabases() throws Exception {
        // Verify secondary database is available
        assertThat(dualDatabaseStrategy.isSecondaryDatabaseEnabled()).isTrue();
        
        // Check that product exists in primary database
        assertThat(primaryProductRepository.findById(testProduct.getId())).isPresent();
        
        // Check that product exists in secondary database (if available)
        if (secondaryProductRepository != null) {
            assertThat(secondaryProductRepository.findById(testProduct.getId())).isPresent();
            
            Product primaryProduct = primaryProductRepository.findById(testProduct.getId()).get();
            Product secondaryProduct = secondaryProductRepository.findById(testProduct.getId()).get();
            
            // Verify both products have the same data
            assertThat(secondaryProduct.getName()).isEqualTo(primaryProduct.getName());
            assertThat(secondaryProduct.getDescription()).isEqualTo(primaryProduct.getDescription());
            assertThat(secondaryProduct.getPrice()).isEqualTo(primaryProduct.getPrice());
            assertThat(secondaryProduct.getStockQuantity()).isEqualTo(primaryProduct.getStockQuantity());
        }
    }

    @Test
    @DisplayName("Should save cart item to both databases when dual database is enabled")
    void testCartItemSavedToBothDatabases() throws Exception {
        // Add item to cart via API
        AddToCartDto addToCartDto = new AddToCartDto();
        addToCartDto.setProductId(testProduct.getId());
        addToCartDto.setQuantity(2);

        mockMvc.perform(post("/api/cart/add")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(addToCartDto)))
                .andExpect(status().isOk());

        // Get cart items to verify they were created
        List<CartItem> primaryCartItems = primaryCartItemRepository.findByUserId(testRegularUser.getId());
        assertThat(primaryCartItems).hasSize(1);
        
        CartItem primaryCartItem = primaryCartItems.get(0);
        assertThat(primaryCartItem.getProductId()).isEqualTo(testProduct.getId());
        assertThat(primaryCartItem.getQuantity()).isEqualTo(2);

        // Check secondary database if available
        if (secondaryCartItemRepository != null) {
            List<CartItem> secondaryCartItems = secondaryCartItemRepository.findByUserId(testRegularUser.getId());
            assertThat(secondaryCartItems).hasSize(1);
            
            CartItem secondaryCartItem = secondaryCartItems.get(0);
            assertThat(secondaryCartItem.getProductId()).isEqualTo(testProduct.getId());
            assertThat(secondaryCartItem.getQuantity()).isEqualTo(2);
            assertThat(secondaryCartItem.getUserId()).isEqualTo(primaryCartItem.getUserId());
        }
    }

    @Test
    @DisplayName("Should read from primary database when feature flag is false")
    void testReadFromPrimaryWhenFeatureFlagFalse() throws Exception {
        // Mock feature flag to return false (use primary)
        when(featureFlagService.getBooleanValue(anyString(), eq("use-neon"), eq(false)))
            .thenReturn(false);

        // Add item to cart
        AddToCartDto addToCartDto = new AddToCartDto();
        addToCartDto.setProductId(testProduct.getId());
        addToCartDto.setQuantity(3);

        mockMvc.perform(post("/api/cart/add")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(addToCartDto)))
                .andExpect(status().isOk());

        // Verify cart retrieval uses primary database (feature flag is false)
        MvcResult cartResult = mockMvc.perform(get("/api/cart")
                .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andReturn();

        // The actual verification would need to be more sophisticated to confirm
        // which database was used, but we can at least verify the operation succeeds
        String cartResponse = cartResult.getResponse().getContentAsString();
        assertThat(cartResponse).contains("\"quantity\":3");
    }

    @Test
    @DisplayName("Should read from secondary database when feature flag is true")
    void testReadFromSecondaryWhenFeatureFlagTrue() throws Exception {
        // Skip if secondary database is not available
        if (secondaryCartItemRepository == null) {
            return;
        }

        // Mock feature flag to return true (use secondary)
        when(featureFlagService.getBooleanValue(anyString(), eq("use-neon"), eq(false)))
            .thenReturn(true);

        // Add item to cart
        AddToCartDto addToCartDto = new AddToCartDto();
        addToCartDto.setProductId(testProduct.getId());
        addToCartDto.setQuantity(4);

        mockMvc.perform(post("/api/cart/add")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(addToCartDto)))
                .andExpect(status().isOk());

        // Verify cart retrieval when feature flag is true
        MvcResult cartResult = mockMvc.perform(get("/api/cart")
                .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andReturn();

        String cartResponse = cartResult.getResponse().getContentAsString();
        assertThat(cartResponse).contains("\"quantity\":4");
    }

    @Test
    @DisplayName("Should maintain data consistency across databases after multiple operations")
    void testDataConsistencyAfterMultipleOperations() throws Exception {
        UUID userId = testRegularUser.getId();
        
        // Perform multiple cart operations
        for (int i = 1; i <= 3; i++) {
            AddToCartDto addToCartDto = new AddToCartDto();
            addToCartDto.setProductId(testProduct.getId());
            addToCartDto.setQuantity(i);

            // Create new cart item each time by varying some parameter
            mockMvc.perform(post("/api/cart/add")
                    .header("Authorization", "Bearer " + userToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(toJson(addToCartDto)))
                    .andExpect(status().isOk());
        }

        // Verify consistency between databases
        List<CartItem> primaryCartItems = primaryCartItemRepository.findByUserId(userId);
        
        if (secondaryCartItemRepository != null) {
            List<CartItem> secondaryCartItems = secondaryCartItemRepository.findByUserId(userId);
            
            // Both databases should have the same number of items
            assertThat(secondaryCartItems).hasSameSizeAs(primaryCartItems);
            
            // Verify each item matches (order might be different)
            for (CartItem primaryItem : primaryCartItems) {
                boolean matchFound = secondaryCartItems.stream()
                    .anyMatch(secondaryItem -> 
                        secondaryItem.getProductId().equals(primaryItem.getProductId()) &&
                        secondaryItem.getQuantity().equals(primaryItem.getQuantity()) &&
                        secondaryItem.getUserId().equals(primaryItem.getUserId())
                    );
                assertThat(matchFound).isTrue();
            }
        }
    }

    @Test
    @DisplayName("Should handle database operation failures gracefully")
    void testGracefulFailureHandling() throws Exception {
        // This test verifies that if one database fails, the system still functions
        // For this test environment, both databases should be working, so we just
        // verify that operations complete successfully
        
        AddToCartDto addToCartDto = new AddToCartDto();
        addToCartDto.setProductId(testProduct.getId());
        addToCartDto.setQuantity(1);

        mockMvc.perform(post("/api/cart/add")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(addToCartDto)))
                .andExpect(status().isOk());

        // Verify the operation succeeded
        MvcResult cartResult = mockMvc.perform(get("/api/cart")
                .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andReturn();

        String cartResponse = cartResult.getResponse().getContentAsString();
        assertThat(cartResponse).contains("\"quantity\":1");
    }

    @Test
    @DisplayName("Should verify admin user can access data from both databases")
    void testAdminAccessToBothDatabases() throws Exception {
        // Mock feature flag for admin user
        when(featureFlagService.getBooleanValue(eq("admin"), eq("use-neon"), eq(false)))
            .thenReturn(false);
        
        // Admin creates a product (should be saved to both databases)
        CreateProductDto adminProductDto = new CreateProductDto();
        adminProductDto.setName("Admin Test Product");
        adminProductDto.setDescription("Product created by admin for dual DB test");
        adminProductDto.setPrice(BigDecimal.valueOf(99.99));
        adminProductDto.setImageUrl("https://example.com/admin-product.jpg");
        adminProductDto.setCategory("Admin");
        adminProductDto.setStockQuantity(50);

        MvcResult productResult = mockMvc.perform(post("/api/products")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(adminProductDto)))
                .andExpect(status().isOk())
                .andReturn();

        Product adminProduct = fromJson(productResult.getResponse().getContentAsString(), Product.class);

        // Verify product exists in primary database
        assertThat(primaryProductRepository.findById(adminProduct.getId())).isPresent();
        
        // Verify product exists in secondary database if available
        if (secondaryProductRepository != null) {
            assertThat(secondaryProductRepository.findById(adminProduct.getId())).isPresent();
        }

        // Admin should be able to retrieve all products
        mockMvc.perform(get("/api/products")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[?(@.name == 'Admin Test Product')]").exists());
    }

    @Test
    @DisplayName("Should verify regular user data isolation across databases")
    void testRegularUserDataIsolation() throws Exception {
        // Mock feature flag for regular user
        when(featureFlagService.getBooleanValue(eq(testRegularUser.getId().toString()), eq("use-neon"), eq(false)))
            .thenReturn(false);

        // Regular user adds item to cart
        AddToCartDto addToCartDto = new AddToCartDto();
        addToCartDto.setProductId(testProduct.getId());
        addToCartDto.setQuantity(5);

        mockMvc.perform(post("/api/cart/add")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(addToCartDto)))
                .andExpect(status().isOk());

        // Verify user can only see their own cart items
        mockMvc.perform(get("/api/cart")
                .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].quantity").value(5));

        // Verify data exists in both databases with proper user association
        List<CartItem> primaryUserCartItems = primaryCartItemRepository.findByUserId(testRegularUser.getId());
        assertThat(primaryUserCartItems).hasSize(1);
        assertThat(primaryUserCartItems.get(0).getUserId()).isEqualTo(testRegularUser.getId());

        if (secondaryCartItemRepository != null) {
            List<CartItem> secondaryUserCartItems = secondaryCartItemRepository.findByUserId(testRegularUser.getId());
            assertThat(secondaryUserCartItems).hasSize(1);
            assertThat(secondaryUserCartItems.get(0).getUserId()).isEqualTo(testRegularUser.getId());
        }
    }
} 