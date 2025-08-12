package com.shopper.controller;

import com.shopper.dto.CreateProductDto;
import com.shopper.dto.UpdateProductDto;
import com.shopper.entity.Product;
import com.shopper.entity.User;
import com.shopper.repository.primary.PrimaryCartItemRepository;
import com.shopper.repository.secondary.SecondaryCartItemRepository;
import com.shopper.service.FeatureFlagService;
import com.shopper.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import com.shopper.entity.CartItem;

@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Products", description = "Product management endpoints")
public class ProductController {
    
    private final ProductService productService;
    private final FeatureFlagService featureFlagService;
    
    // Add this for debugging the sync issue
    @Autowired
    private PrimaryCartItemRepository primaryCartItemRepository;
    
    @Autowired(required = false)
    private SecondaryCartItemRepository secondaryCartItemRepository;
    
    @GetMapping
    @Operation(summary = "Get all products")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Returns all products successfully"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<List<Product>> getAllProducts() {
        String username = getCurrentUsername();
        List<Product> products = productService.findAll(username);
        return ResponseEntity.ok(products);
    }
    
    @GetMapping("/with-feature-flag")
    @Operation(summary = "Get products with feature flag evaluation")
    @ApiResponse(responseCode = "200", description = "Returns products with feature flag evaluation")
    public ResponseEntity<Map<String, Object>> getProductsWithFeatureFlag() {
        String username = getCurrentUsername();
        
        // Get products
        List<Product> products = productService.findAll(username);
        
        // Evaluate feature flag
        boolean newFlowFeature = featureFlagService.getBooleanValue(username, "new-flow", false);
        
        log.info("🎛️ Feature flag 'new-flow' evaluated: {} for user {}", newFlowFeature, username);
        
        if (newFlowFeature) {
            log.info("✅ New flow enabled");
        }
        
        return ResponseEntity.ok(Map.of(
            "products", products,
            "featureFlags", Map.of(
                "newFlow", newFlowFeature,
                "reason", "TARGETING_MATCH",
                "variant", newFlowFeature ? "on" : "off"
            )
        ));
    }
    
    @GetMapping("/premium-only")
    @Operation(summary = "Premium feature - only available when feature flag is enabled")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Premium feature - only available when feature flag is enabled"),
        @ApiResponse(responseCode = "403", description = "Feature not enabled")
    })
    public ResponseEntity<Map<String, Object>> getPremiumProducts() {
        String username = getCurrentUsername();
        
        try {
            List<Product> products = productService.getPremiumProducts(username);
            return ResponseEntity.ok(Map.of(
                "products", products,
                "isPremium", true
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.status(403).body(Map.of(
                "error", "Feature not enabled",
                "message", e.getMessage()
            ));
        }
    }
    
    @GetMapping("/{id}")
    @Operation(summary = "Get product by ID")
    public ResponseEntity<Product> getProductById(@Parameter(description = "Product ID") @PathVariable UUID id) {
        String username = getCurrentUsername();
        Optional<Product> product = productService.findById(id, username);
        return product.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }
    
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a new product (Admin only)")
    public ResponseEntity<Product> createProduct(@Valid @RequestBody CreateProductDto createProductDto) {
        Product product = productService.create(createProductDto);
        return ResponseEntity.ok(product);
    }
    
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update product (Admin only)")
    public ResponseEntity<Product> updateProduct(
            @Parameter(description = "Product ID") @PathVariable UUID id,
            @Valid @RequestBody UpdateProductDto updateProductDto) {
        String username = getCurrentUsername();
        
        try {
            Product product = productService.update(id, updateProductDto, username);
            return ResponseEntity.ok(product);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete product (Admin only)")
    public ResponseEntity<Void> deleteProduct(@Parameter(description = "Product ID") @PathVariable UUID id) {
        try {
            productService.delete(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    @GetMapping("/categories")
    @Operation(summary = "Get all product categories")
    public ResponseEntity<List<String>> getCategories() {
        List<String> categories = productService.findAllCategories();
        return ResponseEntity.ok(categories);
    }
    
    @GetMapping("/search")
    @Operation(summary = "Search products by name")
    public ResponseEntity<List<Product>> searchProducts(@RequestParam String name) {
        List<Product> products = productService.searchByName(name);
        return ResponseEntity.ok(products);
    }
    
    @PostMapping("/admin/bulk-import")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Bulk import products from JSON file (Admin only)")
    public ResponseEntity<Map<String, Object>> bulkImportProducts(
            @RequestParam(defaultValue = "false") boolean clearExisting,
            @RequestParam(defaultValue = "../data/products.json") String filePath) {
        try {
            List<Product> importedProducts = productService.bulkImportProducts(filePath, clearExisting);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Products imported successfully");
            response.put("importedCount", importedProducts.size());
            response.put("totalProducts", productService.getProductCount());
            response.put("clearedExisting", clearExisting);
            response.put("timestamp", LocalDateTime.now());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Failed to bulk import products: {}", e.getMessage());
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Failed to import products");
            errorResponse.put("message", e.getMessage());
            errorResponse.put("timestamp", LocalDateTime.now());
            
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
    
    @PostMapping("/admin/force-update")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Force update all products from JSON file (Admin only)")
    public ResponseEntity<Map<String, Object>> forceUpdateProducts(
            @RequestParam(defaultValue = "../data/products.json") String filePath) {
        try {
            List<Product> updatedProducts = productService.forceUpdateProductsFromJson(filePath);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Products force updated successfully");
            response.put("updatedCount", updatedProducts.size());
            response.put("totalProducts", productService.getProductCount());
            response.put("timestamp", LocalDateTime.now());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Failed to force update products: {}", e.getMessage());
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Failed to force update products");
            errorResponse.put("message", e.getMessage());
            errorResponse.put("timestamp", LocalDateTime.now());
            
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
    
    @GetMapping("/admin/stats")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get product statistics (Admin only)")
    public ResponseEntity<Map<String, Object>> getProductStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalProducts", productService.getProductCount());
        stats.put("inStockProducts", productService.countInStockProducts());
        stats.put("outOfStockProducts", productService.countOutOfStockProducts());
        stats.put("categories", productService.findAllCategories());
        stats.put("timestamp", LocalDateTime.now());
        
        return ResponseEntity.ok(stats);
    }
    
    @GetMapping("/debug/cart-items")
    public Map<String, Object> debugCartItems() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // Test primary repository
            List<CartItem> primaryItems = primaryCartItemRepository.findAll();
            result.put("primary_count", primaryItems.size());
            result.put("primary_items", primaryItems);
            log.info("Primary repository found {} cart items", primaryItems.size());
            
            // Test secondary repository
            if (secondaryCartItemRepository != null) {
                List<CartItem> secondaryItems = secondaryCartItemRepository.findAll();
                result.put("secondary_count", secondaryItems.size());
                result.put("secondary_items", secondaryItems);
                log.info("Secondary repository found {} cart items", secondaryItems.size());
            } else {
                result.put("secondary_count", "Repository not available");
                log.info("Secondary repository not available");
            }
            
            result.put("success", true);
        } catch (Exception e) {
            log.error("Error debugging cart items: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("error", e.getMessage());
        }
        
        return result;
    }
    
    private String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            Object principal = authentication.getPrincipal();
            
            // Handle both UserDetails and User entity cases
            if (principal instanceof User) {
                return ((User) principal).getUsername();
            } else if (principal instanceof org.springframework.security.core.userdetails.UserDetails) {
                return ((org.springframework.security.core.userdetails.UserDetails) principal).getUsername();
            }
        }
        return "anonymous";
    }
}