package com.shopper.repository;

import com.shopper.entity.Product;
import com.shopper.repository.primary.PrimaryProductRepository;
import com.shopper.repository.secondary.SecondaryProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
@Slf4j
public class ProductRepository extends DualDatabaseRepository<Product, UUID> {
    
    private final PrimaryProductRepository primaryRepository;
    
    @Autowired(required = false)
    private SecondaryProductRepository secondaryRepository;
    
    @Override
    protected JpaRepository<Product, UUID> getPrimaryRepository() {
        return primaryRepository;
    }
    
    @Override
    protected JpaRepository<Product, UUID> getSecondaryRepository() {
        return secondaryRepository;
    }
    
    public List<Product> findByCategory(String category) {
        return executeReadOperation(
            "system",
            () -> primaryRepository.findByCategory(category),
            () -> secondaryRepository != null ? secondaryRepository.findByCategory(category) : List.of()
        );
    }
    
    public List<Product> findByNameContainingIgnoreCase(String name) {
        return executeReadOperation(
            "system",
            () -> primaryRepository.findByNameContainingIgnoreCase(name),
            () -> secondaryRepository != null ? secondaryRepository.findByNameContainingIgnoreCase(name) : List.of()
        );
    }
    
    public List<Product> findByDescriptionContainingIgnoreCase(String description) {
        return executeReadOperation(
            "system",
            () -> primaryRepository.findByDescriptionContainingIgnoreCase(description),
            () -> secondaryRepository != null ? List.of() : List.of() // Secondary doesn't have this method
        );
    }
    
    public List<Product> findInStockProducts() {
        return executeReadOperation(
            "system",
            () -> primaryRepository.findInStockProducts(),
            () -> secondaryRepository != null ? secondaryRepository.findInStock() : List.of()
        );
    }
    
    public List<Product> findInStockProductsByCategory(String category) {
        return executeReadOperation(
            "system",
            () -> primaryRepository.findInStockProductsByCategory(category),
            () -> secondaryRepository != null ? secondaryRepository.findByCategoryAndInStock(category) : List.of()
        );
    }
    
    public List<String> findAllCategories() {
        return executeReadOperation(
            "system",
            () -> primaryRepository.findAllCategories(),
            () -> secondaryRepository != null ? List.of() : List.of() // Secondary doesn't have this method
        );
    }
    
    public long countInStockProducts() {
        return executeReadOperation(
            "system",
            () -> primaryRepository.countInStockProducts(),
            () -> secondaryRepository != null ? 0L : 0L // Secondary doesn't have this method
        );
    }
    
    public long countOutOfStockProducts() {
        return executeReadOperation(
            "system",
            () -> primaryRepository.count() - primaryRepository.countInStockProducts(), // Calculate from difference
            () -> secondaryRepository != null ? 0L : 0L // Secondary doesn't have this method
        );
    }
    
    // User-context-aware methods for better feature flag evaluation
    public List<Product> findAll(String userId) {
        return findAllDual(userId);
    }
    
    public Optional<Product> findById(UUID id, String userId) {
        return findByIdDual(userId, id);
    }
    
    public Product save(Product product, String userId) {
        return saveDualWithIdSync(userId, product);
    }
    
    // Standard JpaRepository methods using dual database strategy (with system context)
    public Product save(Product product) {
        return saveDualWithIdSync("system", product);
    }
    
    // Helper method to save with proper ID synchronization
    private Product saveDualWithIdSync(String userId, Product product) {
        // First save to primary database to get the generated ID
        Product savedPrimary = primaryRepository.save(product);
        log.info("Product {} saved to primary database with ID: {}", savedPrimary.getName(), savedPrimary.getId());
        
        // Then save to secondary database with the same ID if available
        if (isSecondaryAvailable()) {
            log.info("Secondary database is available, attempting to sync product {}", savedPrimary.getId());
            try {
                // Ensure timestamps are set
                LocalDateTime now = LocalDateTime.now();
                LocalDateTime createdAt = savedPrimary.getCreatedAt() != null ? savedPrimary.getCreatedAt() : now;
                LocalDateTime updatedAt = savedPrimary.getUpdatedAt() != null ? savedPrimary.getUpdatedAt() : now;
                
                // Use native SQL to insert with the exact same ID
                secondaryRepository.saveWithSpecificId(
                    savedPrimary.getId().toString(),
                    savedPrimary.getName(),
                    savedPrimary.getDescription(),
                    savedPrimary.getPrice(),
                    savedPrimary.getImageUrl(),
                    savedPrimary.getCategory(),
                    savedPrimary.getStockQuantity(),
                    createdAt,
                    updatedAt
                );
                log.info("Product {} saved to secondary database with ID: {}", savedPrimary.getName(), savedPrimary.getId());
            } catch (Exception e) {
                log.error("Failed to save product to secondary database for user {}: {}", userId, e.getMessage());
                // Don't fail the operation, just log the error
            }
        } else {
            log.info("Secondary database is not available, skipping sync for product {}", savedPrimary.getId());
        }
        
        return savedPrimary;
    }
    
    public Optional<Product> findById(UUID id) {
        return findByIdDual("system", id);
    }
    
    public List<Product> findAll() {
        return findAllDual("system");
    }
    
    public void deleteById(UUID id) {
        deleteByIdDual("system", id);
    }
    
    public void delete(Product product) {
        deleteByIdDual("system", product.getId());
    }

    public long count() {
        return executeReadOperation(
            "system",
            () -> primaryRepository.count(),
            () -> secondaryRepository != null ? secondaryRepository.count() : 0L
        );
    }
    
    public boolean existsById(UUID id) {
        return executeReadOperation(
            "system",
            () -> primaryRepository.existsById(id),
            () -> secondaryRepository != null ? secondaryRepository.existsById(id) : false
        );
    }
    
    public void deleteAll() {
        deleteAllDual();
    }
}