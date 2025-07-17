package com.shopper.service;

import com.shopper.dto.CreateProductDto;
import com.shopper.dto.UpdateProductDto;
import com.shopper.entity.Product;
import com.shopper.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {
    
    private final ProductRepository productRepository;
    private final FeatureFlagService featureFlagService;
    
    public List<Product> findAll(String username) {
        // Check feature flag for new flow
        boolean newFlow = featureFlagService.getBooleanValue(username, "new-flow", false);
        
        if (newFlow) {
            log.info("✅ New flow enabled for user: {}", username);
            return productRepository.findInStockProducts();
        }
        
        return productRepository.findAll();
    }
    
    public Optional<Product> findById(UUID id, String username) {
        // Check feature flag for enhanced product details
        boolean enhancedDetails = featureFlagService.getBooleanValue(username, "enhanced-product-details", false);
        
        if (enhancedDetails) {
            log.info("Enhanced product details enabled for user: {}", username);
        }
        
        return productRepository.findById(id);
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
}