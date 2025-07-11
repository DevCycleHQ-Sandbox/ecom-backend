package com.shopper.backend.service;

import com.shopper.backend.dto.CreateProductDto;
import com.shopper.backend.dto.UpdateProductDto;
import com.shopper.backend.entity.Product;
import com.shopper.backend.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class ProductService {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private FeatureFlagService featureFlagService;

    public List<Product> findAll(String username) {
        // Check feature flags for enhanced product display
        boolean showEnhancedView = featureFlagService.getBooleanFeatureFlag(
            username != null ? username : "anonymous",
            "enhanced-product-view",
            false
        );

        List<Product> products = productRepository.findAll();
        
        // Apply feature flag logic
        if (showEnhancedView) {
            // Enhanced view logic could be implemented here
            System.out.println("✅ Enhanced product view enabled for user: " + username);
        }

        return products;
    }

    public Optional<Product> findById(UUID id, String username) {
        // Feature flag for detailed product information
        boolean showDetailedInfo = featureFlagService.getBooleanFeatureFlag(
            username != null ? username : "anonymous",
            "detailed-product-info",
            false
        );

        Optional<Product> product = productRepository.findById(id);
        
        if (showDetailedInfo && product.isPresent()) {
            System.out.println("✅ Detailed product info enabled for user: " + username);
        }

        return product;
    }

    public Product create(CreateProductDto createProductDto) {
        Product product = new Product();
        product.setName(createProductDto.getName());
        product.setDescription(createProductDto.getDescription());
        product.setPrice(createProductDto.getPrice());
        product.setImageUrl(createProductDto.getImageUrl());
        product.setCategory(createProductDto.getCategory());
        product.setStockQuantity(createProductDto.getStockQuantity());

        return productRepository.save(product);
    }

    public Product update(UUID id, UpdateProductDto updateProductDto, String username) {
        Optional<Product> optionalProduct = productRepository.findById(id);
        if (optionalProduct.isEmpty()) {
            throw new RuntimeException("Product not found");
        }

        Product product = optionalProduct.get();
        
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

    public void delete(UUID id) {
        if (!productRepository.existsById(id)) {
            throw new RuntimeException("Product not found");
        }
        productRepository.deleteById(id);
    }

    public List<Product> findByCategory(String category) {
        return productRepository.findByCategory(category);
    }

    public List<Product> findAvailableProducts() {
        return productRepository.findAvailableProducts();
    }

    public List<String> getCategories() {
        return productRepository.findDistinctCategories();
    }
}