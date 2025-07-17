package com.shopper.service;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Strategy interface for handling dual database operations.
 * Supports writing to both databases and reading from the appropriate database
 * based on feature flag configuration.
 */
public interface DualDatabaseStrategy {
    
    /**
     * Execute a read operation on the appropriate database based on feature flag
     * @param userId User ID for feature flag evaluation
     * @param primaryOperation Function to execute on primary database
     * @param secondaryOperation Function to execute on secondary database
     * @param <T> Return type
     * @return Result from the appropriate database
     */
    <T> T executeRead(String userId, Supplier<T> primaryOperation, Supplier<T> secondaryOperation);
    
    /**
     * Execute a write operation on both databases
     * @param primaryOperation Function to execute on primary database
     * @param secondaryOperation Function to execute on secondary database
     * @param <T> Return type
     * @return Result from the primary database (or feature flag selected database)
     */
    <T> T executeWrite(Supplier<T> primaryOperation, Supplier<T> secondaryOperation);
    
    /**
     * Execute a write operation on both databases with user context
     * @param userId User ID for feature flag evaluation
     * @param primaryOperation Function to execute on primary database
     * @param secondaryOperation Function to execute on secondary database
     * @param <T> Return type
     * @return Result from the appropriate database based on feature flag
     */
    <T> T executeWriteWithUser(String userId, Supplier<T> primaryOperation, Supplier<T> secondaryOperation);
    
    /**
     * Check if secondary database is enabled and available
     * @return true if secondary database is available
     */
    boolean isSecondaryDatabaseEnabled();
    
    /**
     * Check if we should use the secondary database for reads based on feature flag
     * @param userId User ID for feature flag evaluation
     * @return true if secondary database should be used for reads
     */
    boolean shouldUseSecondaryForRead(String userId);
} 