package com.shopper.service;

import com.shopper.dto.CreateProductDto;
import com.shopper.dto.UpdateProductDto;
import com.shopper.entity.Product;
import com.shopper.repository.ProductRepository;
import com.shopper.repository.CartItemRepository;
import com.shopper.repository.OrderItemRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {
    
    private final ProductRepository productRepository;
    private final CartItemRepository cartItemRepository;
    private final OrderItemRepository orderItemRepository;
    private final FeatureFlagService featureFlagService;
    
    public List<Product> findAll(String username) {
        // Check feature flag for new flow
        boolean newFlow = featureFlagService.getBooleanValue(username, "new-flow", false);
        
        if (newFlow) {
            log.info("✅ New flow enabled for user: {}", username);
            boolean shouldError = Math.random() < 0.2;
            if (shouldError) {
                throw new RuntimeException("Error in findAll");
            }
            return productRepository.findInStockProducts();
        }
        
        // Use user-context-aware method for feature flag evaluation
        return productRepository.findAll(username);
    }
    
    public Optional<Product> findById(UUID id, String username) {
        // Check feature flag for enhanced product details
        boolean enhancedDetails = featureFlagService.getBooleanValue(username, "enhanced-product-details", false);
        
        if (enhancedDetails) {
            log.info("Enhanced product details enabled for user: {}", username);
        }
        
        // Use user-context-aware method for feature flag evaluation
        return productRepository.findById(id, username);
    }
    
    @Transactional
    public Product create(CreateProductDto createProductDto) {
        Product product = Product.builder()
                .name(createProductDto.getName())
                .description(createProductDto.getDescription())
                .price(createProductDto.getPrice())
                .imageUrl(createProductDto.getImageUrl())
                .category(createProductDto.getCategory())
                .stockQuantity(createProductDto.getStockQuantity())
                .build();
        
        log.info("Creating product: {} (will be saved to both databases)", product.getName());
        return productRepository.save(product);
    }
    
    @Transactional
    public Product update(UUID id, UpdateProductDto updateProductDto, String username) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        
        // Update only non-null fields
        if (updateProductDto.getName() != null) {
            product.setName(updateProductDto.getName());
        }
        if (updateProductDto.getDescription() != null) {
            product.setDescription(updateProductDto.getDescription());
        }
        if (updateProductDto.getPrice() != null) {
            product.setPrice(updateProductDto.getPrice());
        }
        if (updateProductDto.getImageUrl() != null) {
            product.setImageUrl(updateProductDto.getImageUrl());
        }
        if (updateProductDto.getCategory() != null) {
            product.setCategory(updateProductDto.getCategory());
        }
        if (updateProductDto.getStockQuantity() != null) {
            product.setStockQuantity(updateProductDto.getStockQuantity());
        }
        
        return productRepository.save(product);
    }
    
    @Transactional
    public void delete(UUID id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        
        productRepository.delete(product);
    }
    
    public List<Product> findByCategory(String category) {
        return productRepository.findByCategory(category);
    }
    
    public List<Product> findInStockProducts() {
        return productRepository.findInStockProducts();
    }
    
    public List<Product> findInStockProductsByCategory(String category) {
        return productRepository.findInStockProductsByCategory(category);
    }
    
    public List<String> findAllCategories() {
        return productRepository.findAllCategories();
    }
    
    public List<Product> searchByName(String name) {
        return productRepository.findByNameContainingIgnoreCase(name);
    }
    
    public List<Product> searchByDescription(String description) {
        return productRepository.findByDescriptionContainingIgnoreCase(description);
    }
    
    public long countInStockProducts() {
        return productRepository.countInStockProducts();
    }
    
    public long countOutOfStockProducts() {
        return productRepository.countOutOfStockProducts();
    }
    
    public List<Product> getPremiumProducts(String username) {
        // Premium feature - only available when feature flag is enabled
        boolean premiumEnabled = featureFlagService.getBooleanValue(username, "premium-features", false);
        
        if (!premiumEnabled) {
            throw new RuntimeException("Premium features not enabled");
        }
        
        List<Product> products = findInStockProducts();
        return products.stream().limit(5).toList(); // Premium users get first 5 products
    }
    
    @Transactional
    public List<Product> bulkImportProducts(String jsonFilePath, boolean clearExisting) {
        log.info("Starting bulk import of products from file: {}", jsonFilePath);
        
        try {
            File jsonFile = new File(jsonFilePath);
            log.info("File path resolved to: {}", jsonFile.getAbsolutePath());
            log.info("File exists: {}, Can read: {}", jsonFile.exists(), jsonFile.canRead());
            
            if (!jsonFile.exists()) {
                throw new RuntimeException("JSON file not found: " + jsonFile.getAbsolutePath());
            }
            
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(jsonFile);
            log.info("JSON parsed successfully, root node: {}", rootNode != null);
            
            JsonNode productsNode = rootNode.get("products");
            log.info("Products node found: {}, Is array: {}", productsNode != null, 
                    productsNode != null && productsNode.isArray());
            
            if (productsNode == null || !productsNode.isArray()) {
                throw new RuntimeException("Invalid JSON structure: 'products' array not found");
            }
            
            List<Product> importedProducts = new ArrayList<>();
            
            // Clear existing products if requested (with constraint handling)
            if (clearExisting) {
                log.info("Clearing existing products before import");
                List<Product> existingProducts = productRepository.findAll();
                int successfulDeletes = 0;
                int constraintViolations = 0;
                
                for (Product product : existingProducts) {
                    try {
                        productRepository.delete(product);
                        successfulDeletes++;
                    } catch (Exception e) {
                        // Handle foreign key constraint violations gracefully
                        log.warn("Could not delete product '{}' (ID: {}) due to foreign key constraints: {}", 
                                product.getName(), product.getId(), e.getMessage());
                        constraintViolations++;
                    }
                }
                log.info("Cleared {} existing products, {} products retained due to constraints", 
                        successfulDeletes, constraintViolations);
            }
            
            // Import new products (with upsert logic)
            for (JsonNode productNode : productsNode) {
                try {
                    String productName = productNode.get("name").asText();
                    String description = productNode.get("description").asText();
                    BigDecimal price = BigDecimal.valueOf(productNode.get("price").asDouble());
                    String imageUrl = productNode.has("image_url") && !productNode.get("image_url").isNull() 
                        ? productNode.get("image_url").asText() : null;
                    String category = productNode.get("category").asText();
                    int stockQuantity = productNode.get("stock_quantity").asInt();
                    
                    log.info("Parsing product: {} with imageUrl: {}", productName, imageUrl);
                    
                    // Check if product with this name already exists
                    List<Product> existingProducts = productRepository.findByNameContainingIgnoreCase(productName);
                    Product product;
                    
                    if (!existingProducts.isEmpty()) {
                        // Update existing product
                        product = existingProducts.get(0);
                        product.setDescription(description);
                        product.setPrice(price);
                        product.setImageUrl(imageUrl);
                        product.setCategory(category);
                        product.setStockQuantity(stockQuantity);
                        log.info("Updating existing product: {}", productName);
                    } else {
                        // Create new product
                        product = Product.builder()
                                .name(productName)
                                .description(description)
                                .price(price)
                                .imageUrl(imageUrl)
                                .category(category)
                                .stockQuantity(stockQuantity)
                                .build();
                        log.info("Creating new product: {}", productName);
                    }
                    
                    Product savedProduct = productRepository.save(product);
                    importedProducts.add(savedProduct);
                    log.info("Imported product: {} (ID: {})", savedProduct.getName(), savedProduct.getId());
                    
                } catch (Exception e) {
                    log.error("Failed to import product: {}", productNode, e);
                    // Continue with other products instead of failing the entire import
                }
            }
            
            log.info("Bulk import completed successfully. Imported {} products", importedProducts.size());
            return importedProducts;
            
        } catch (IOException e) {
            log.error("Failed to read JSON file: {}", jsonFilePath, e);
            throw new RuntimeException("Failed to read products JSON file", e);
        } catch (Exception e) {
            log.error("Failed to import products from file: {}", jsonFilePath, e);
            throw new RuntimeException("Failed to import products", e);
        }
    }
    
    @Transactional
    public List<Product> forceUpdateProductsFromJson(String jsonFilePath) {
        log.info("Force updating products from JSON file: {}", jsonFilePath);
        return completeProductReplacement(jsonFilePath);
    }
    
    @Transactional
    public List<Product> completeProductReplacement(String jsonFilePath) {
        log.info("Starting complete product replacement from JSON file: {}", jsonFilePath);
        
        try {
            // Step 1: Delete all cart items using individual deletion (safer approach)
            log.info("Deleting all cart items to remove foreign key constraints...");
            List<com.shopper.entity.CartItem> cartItems = cartItemRepository.findAll();
            log.info("Found {} cart items to delete", cartItems.size());
            for (com.shopper.entity.CartItem cartItem : cartItems) {
                try {
                    cartItemRepository.delete(cartItem);
                } catch (Exception e) {
                    log.warn("Failed to delete cart item {}: {}", cartItem.getId(), e.getMessage());
                }
            }
            log.info("Deleted cart items");
            
            // Step 2: Delete all order items using individual deletion
            log.info("Deleting all order items to remove foreign key constraints...");
            List<com.shopper.entity.OrderItem> orderItems = orderItemRepository.findAll();
            log.info("Found {} order items to delete", orderItems.size());
            for (com.shopper.entity.OrderItem orderItem : orderItems) {
                try {
                    orderItemRepository.delete(orderItem);
                } catch (Exception e) {
                    log.warn("Failed to delete order item {}: {}", orderItem.getId(), e.getMessage());
                }
            }
            log.info("Deleted order items");
            
            // Step 3: Delete all products using individual deletion
            log.info("Deleting all existing products...");
            List<Product> products = productRepository.findAll();
            log.info("Found {} products to delete", products.size());
            for (Product product : products) {
                try {
                    productRepository.delete(product);
                } catch (Exception e) {
                    log.warn("Failed to delete product {}: {}", product.getId(), e.getMessage());
                }
            }
            log.info("Deleted all products");
            
            // Step 4: Import new products (with clearExisting=false since we already cleared everything)
            log.info("Importing new products from JSON...");
            List<Product> importedProducts = bulkImportProducts(jsonFilePath, false);
            
            log.info("Complete product replacement finished successfully. Imported {} new products", importedProducts.size());
            return importedProducts;
            
        } catch (Exception e) {
            log.error("Failed to perform complete product replacement: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to perform complete product replacement", e);
        }
    }
    
    public long getProductCount() {
        return productRepository.count();
    }
}