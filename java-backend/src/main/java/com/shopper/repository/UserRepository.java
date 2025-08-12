package com.shopper.repository;

import com.shopper.entity.User;
import com.shopper.repository.primary.PrimaryUserRepository;
import com.shopper.repository.secondary.SecondaryUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
@Slf4j
public class UserRepository extends DualDatabaseRepository<User, UUID> {
    
    private final PrimaryUserRepository primaryRepository;
    
    @Autowired(required = false)
    private SecondaryUserRepository secondaryRepository;
    
    @Override
    protected JpaRepository<User, UUID> getPrimaryRepository() {
        return primaryRepository;
    }
    
    @Override
    protected JpaRepository<User, UUID> getSecondaryRepository() {
        return secondaryRepository;
    }
    
    public Optional<User> findByUsername(String username) {
        return executeReadOperation(
            "system", // Using system context for user lookups
            () -> primaryRepository.findByUsername(username),
            () -> secondaryRepository != null ? secondaryRepository.findByUsername(username) : Optional.empty()
        );
    }
    
    public Optional<User> findByEmail(String email) {
        return executeReadOperation(
            "system",
            () -> primaryRepository.findByEmail(email),
            () -> secondaryRepository != null ? secondaryRepository.findByEmail(email) : Optional.empty()
        );
    }
    
    public boolean existsByUsername(String username) {
        return executeReadOperation(
            "system",
            () -> primaryRepository.existsByUsername(username),
            () -> secondaryRepository != null ? secondaryRepository.existsByUsername(username) : false
        );
    }
    
    public boolean existsByEmail(String email) {
        return executeReadOperation(
            "system",
            () -> primaryRepository.existsByEmail(email),
            () -> secondaryRepository != null ? secondaryRepository.existsByEmail(email) : false
        );
    }
    
    public long countAdmins() {
        return executeReadOperation(
            "system",
            () -> primaryRepository.countAdmins(),
            () -> secondaryRepository != null ? secondaryRepository.countAdmins() : 0L
        );
    }
    
    public long countUsers() {
        return executeReadOperation(
            "system",
            () -> primaryRepository.countUsers(),
            () -> secondaryRepository != null ? secondaryRepository.countUsers() : 0L
        );
    }
    
    // Standard JpaRepository methods using dual database strategy with ID synchronization
    public User save(User user) {
        return saveDualWithIdSync("system", user);
    }
    
    // Helper method to save with proper ID synchronization
    private User saveDualWithIdSync(String userId, User user) {
        // First save to primary database to get the generated ID
        User savedPrimary = primaryRepository.save(user);
        log.info("User {} saved to primary database with ID: {}", savedPrimary.getEmail(), savedPrimary.getId());
        
        // Then save to secondary database with the same ID if available
        if (isSecondaryAvailable()) {
            log.info("Secondary database is available, attempting to sync user {}", savedPrimary.getId());
            try {
                // Ensure timestamps are set
                LocalDateTime now = LocalDateTime.now();
                LocalDateTime createdAt = savedPrimary.getCreatedAt() != null ? savedPrimary.getCreatedAt() : now;
                LocalDateTime updatedAt = savedPrimary.getUpdatedAt() != null ? savedPrimary.getUpdatedAt() : now;
                
                // First delete any existing user with the same email or username to avoid conflicts
                // Delete related cart items first to avoid foreign key constraints
                secondaryRepository.deleteCartItemsByUserEmail(savedPrimary.getEmail());
                secondaryRepository.deleteCartItemsByUserUsername(savedPrimary.getUsername());
                
                // Then delete the user
                secondaryRepository.deleteByEmail(savedPrimary.getEmail());
                secondaryRepository.deleteByUsername(savedPrimary.getUsername());
                
                // Then insert with the specific ID
                secondaryRepository.saveWithSpecificId(
                    savedPrimary.getId().toString(),
                    savedPrimary.getUsername(),
                    savedPrimary.getEmail(),
                    savedPrimary.getPassword(),
                    savedPrimary.getRole().name(),
                    createdAt,
                    updatedAt
                );
                log.info("User {} saved to secondary database with ID: {}", savedPrimary.getEmail(), savedPrimary.getId());
            } catch (Exception e) {
                log.error("Failed to save user to secondary database for user {}: {}", userId, e.getMessage());
            }
        }
        
        return savedPrimary;
    }
    
    public Optional<User> findById(UUID id) {
        return findByIdDual("system", id);
    }
    
    public void deleteById(UUID id) {
        deleteByIdDual("system", id);
    }
    
    public void delete(User user) {
        deleteByIdDual("system", user.getId());
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
    
    // Helper method to execute read operations
    protected <T> T executeReadOperation(String userId, java.util.function.Supplier<T> primaryOperation, java.util.function.Supplier<T> secondaryOperation) {
        return dualDatabaseStrategy.executeRead(userId, primaryOperation, secondaryOperation);
    }
    
    // Helper method to check if secondary database is available
    protected boolean isSecondaryAvailable() {
        return secondaryRepository != null && dualDatabaseStrategy.isSecondaryDatabaseEnabled();
    }
}