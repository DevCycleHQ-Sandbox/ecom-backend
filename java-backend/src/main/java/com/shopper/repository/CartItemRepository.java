package com.shopper.repository;

import com.shopper.entity.CartItem;
import com.shopper.repository.primary.PrimaryCartItemRepository;
import com.shopper.repository.secondary.SecondaryCartItemRepository;
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
public class CartItemRepository extends DualDatabaseRepository<CartItem, UUID> {
    
    private final PrimaryCartItemRepository primaryRepository;
    
    @Autowired(required = false)
    private SecondaryCartItemRepository secondaryRepository;
    
    @Override
    protected JpaRepository<CartItem, UUID> getPrimaryRepository() {
        return primaryRepository;
    }
    
    @Override
    protected JpaRepository<CartItem, UUID> getSecondaryRepository() {
        return secondaryRepository;
    }
    
    public List<CartItem> findByUserIdWithProduct(UUID userId) {
        return executeReadOperation(
            userId.toString(),
            () -> primaryRepository.findByUserIdWithProduct(userId),
            () -> secondaryRepository != null ? secondaryRepository.findByUserIdWithProduct(userId) : List.of()
        );
    }
    
    public List<CartItem> findByUserId(UUID userId) {
        return executeReadOperation(
            userId.toString(),
            () -> primaryRepository.findByUserId(userId),
            () -> secondaryRepository != null ? secondaryRepository.findByUserId(userId) : List.of()
        );
    }
    
    public Optional<CartItem> findByUserIdAndProductId(UUID userId, UUID productId) {
        return executeReadOperation(
            userId.toString(),
            () -> primaryRepository.findByUserIdAndProductId(userId, productId),
            () -> secondaryRepository != null ? secondaryRepository.findByUserIdAndProductId(userId, productId) : Optional.empty()
        );
    }
    
    public void deleteByUserId(UUID userId) {
        executeWriteOperation(
            userId.toString(),
            () -> {
                primaryRepository.deleteByUserId(userId);
                return null;
            },
            () -> {
                if (secondaryRepository != null) {
                    secondaryRepository.deleteByUserId(userId);
                }
                return null;
            }
        );
    }
    
    public void deleteByUserIdAndProductId(UUID userId, UUID productId) {
        executeWriteOperation(
            userId.toString(),
            () -> {
                primaryRepository.deleteByUserIdAndProductId(userId, productId);
                return null;
            },
            () -> {
                if (secondaryRepository != null) {
                    secondaryRepository.deleteByUserIdAndProductId(userId, productId);
                }
                return null;
            }
        );
    }
    
    public long countByUserId(UUID userId) {
        return executeReadOperation(
            userId.toString(),
            () -> primaryRepository.countByUserId(userId),
            () -> secondaryRepository != null ? secondaryRepository.countByUserId(userId) : 0L
        );
    }
    
    public Integer sumQuantityByUserId(UUID userId) {
        return executeReadOperation(
            userId.toString(),
            () -> primaryRepository.sumQuantityByUserId(userId),
            () -> secondaryRepository != null ? secondaryRepository.sumQuantityByUserId(userId) : 0
        );
    }
    
    // Standard JpaRepository methods using dual database strategy with ID synchronization
    public CartItem save(CartItem cartItem) {
        return saveDualWithIdSync(cartItem.getUserId().toString(), cartItem);
    }
    
    // Helper method to save with proper ID synchronization
    private CartItem saveDualWithIdSync(String userId, CartItem cartItem) {
        // First save to primary database to get the generated ID
        CartItem savedPrimary = primaryRepository.save(cartItem);
        log.debug("CartItem saved to primary database with ID: {}", savedPrimary.getId());
        
        // Then save to secondary database with the same ID if available
        if (isSecondaryAvailable()) {
            try {
                // Ensure timestamps are set
                LocalDateTime now = LocalDateTime.now();
                LocalDateTime createdAt = savedPrimary.getCreatedAt() != null ? savedPrimary.getCreatedAt() : now;
                LocalDateTime updatedAt = savedPrimary.getUpdatedAt() != null ? savedPrimary.getUpdatedAt() : now;
                
                // Use native SQL to insert with the exact same ID
                secondaryRepository.saveWithSpecificId(
                    savedPrimary.getId().toString(),
                    savedPrimary.getUserId().toString(),
                    savedPrimary.getProductId().toString(),
                    savedPrimary.getQuantity(),
                    createdAt,
                    updatedAt
                );
                log.debug("CartItem saved to secondary database with ID: {}", savedPrimary.getId());
            } catch (Exception e) {
                log.error("Failed to save cart item to secondary database for user {}: {}", userId, e.getMessage());
                // Don't fail the operation, just log the error
            }
        }
        
        return savedPrimary;
    }
    
    public Optional<CartItem> findById(UUID id) {
        // For findById without user context, we use system as userId
        return findByIdDual("system", id);
    }
    
    public List<CartItem> findAll() {
        // For findAll without user context, we use system as userId
        return findAllDual("system");
    }
    
    public void deleteById(UUID id) {
        deleteByIdDual("system", id);
    }
    
    public void delete(CartItem cartItem) {
        deleteByIdDual(cartItem.getUserId().toString(), cartItem.getId());
    }

    public long count() {
        return executeReadOperation(
            "system",
            () -> primaryRepository.count(),
            () -> secondaryRepository != null ? secondaryRepository.count() : 0L
        );
    }
    
    public void deleteAll() {
        deleteAllDual();
    }
}