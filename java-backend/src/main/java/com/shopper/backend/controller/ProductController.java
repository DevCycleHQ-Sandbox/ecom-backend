package com.shopper.backend.controller;

import com.shopper.backend.dto.CreateProductDto;
import com.shopper.backend.dto.UpdateProductDto;
import com.shopper.backend.entity.Product;
import com.shopper.backend.service.FeatureFlagService;
import com.shopper.backend.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/products")
@Tag(name = "Products", description = "Product management APIs")
public class ProductController {

    @Autowired
    private ProductService productService;

    @Autowired
    private FeatureFlagService featureFlagService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a new product")
    public ResponseEntity<Product> createProduct(@Valid @RequestBody CreateProductDto createProductDto) {
        try {
            Product product = productService.create(createProductDto);
            return ResponseEntity.ok(product);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @GetMapping
    @Operation(summary = "Get all products")
    public ResponseEntity<List<Product>> getAllProducts() {
        String username = getCurrentUsername();
        List<Product> products = productService.findAll(username);
        return ResponseEntity.ok(products);
    }

    @GetMapping("/with-feature-flag")
    @Operation(summary = "Get products with feature flag evaluation")
    public ResponseEntity<Map<String, Object>> getProductsWithFeatureFlag() {
        String username = getCurrentUsername();
        
        boolean newFlowFeature = featureFlagService.getBooleanFeatureFlag(
            username != null ? username : "anonymous", 
            "new-flow", 
            false
        );

        System.out.println("🎛️ Feature flag 'new-flow' evaluated: " + newFlowFeature + " for user " + username);

        List<Product> products = productService.findAll(username);
        
        Map<String, Object> response = new HashMap<>();
        response.put("products", products);
        response.put("featureFlags", Map.of(
            "newFlow", newFlowFeature,
            "reason", "TARGETING_MATCH",
            "variant", newFlowFeature ? "enabled" : "disabled"
        ));

        if (newFlowFeature) {
            System.out.println("✅ New flow enabled");
        }

        return ResponseEntity.ok(response);
    }

    @GetMapping("/premium-only")
    @Operation(summary = "Get premium products (feature flag controlled)")
    public ResponseEntity<Map<String, Object>> getPremiumProducts() {
        String username = getCurrentUsername();
        
        boolean premiumFeatures = featureFlagService.getBooleanFeatureFlag(
            username != null ? username : "anonymous", 
            "premium-features", 
            false
        );

        if (!premiumFeatures) {
            return ResponseEntity.status(403).body(Map.of(
                "error", "Feature not enabled",
                "message", "Premium features are not enabled for this user"
            ));
        }

        List<Product> products = productService.findAll(username);
        
        Map<String, Object> response = new HashMap<>();
        response.put("products", products.stream().limit(5).toList()); // Premium users get first 5 products
        response.put("isPremium", true);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get product by ID")
    public ResponseEntity<Product> getProductById(@PathVariable UUID id) {
        String username = getCurrentUsername();
        Optional<Product> product = productService.findById(id, username);
        
        if (product.isPresent()) {
            return ResponseEntity.ok(product.get());
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update product")
    public ResponseEntity<Product> updateProduct(@PathVariable UUID id, 
                                                @Valid @RequestBody UpdateProductDto updateProductDto) {
        try {
            String username = getCurrentUsername();
            Product product = productService.update(id, updateProductDto, username);
            return ResponseEntity.ok(product);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete product")
    public ResponseEntity<Map<String, String>> deleteProduct(@PathVariable UUID id) {
        try {
            productService.delete(id);
            return ResponseEntity.ok(Map.of("message", "Product deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/categories")
    @Operation(summary = "Get all product categories")
    public ResponseEntity<List<String>> getCategories() {
        List<String> categories = productService.getCategories();
        return ResponseEntity.ok(categories);
    }

    @GetMapping("/category/{category}")
    @Operation(summary = "Get products by category")
    public ResponseEntity<List<Product>> getProductsByCategory(@PathVariable String category) {
        List<Product> products = productService.findByCategory(category);
        return ResponseEntity.ok(products);
    }

    @GetMapping("/available")
    @Operation(summary = "Get available products (in stock)")
    public ResponseEntity<List<Product>> getAvailableProducts() {
        List<Product> products = productService.findAvailableProducts();
        return ResponseEntity.ok(products);
    }

    private String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            return authentication.getName();
        }
        return null;
    }
}