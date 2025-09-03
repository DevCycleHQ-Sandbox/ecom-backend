package com.shopper.service;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.function.Supplier;

/**
 * Implementation of DualDatabaseStrategy that manages dual database operations
 * with feature flag controlled routing.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DualDatabaseStrategyImpl implements DualDatabaseStrategy {
    
    private final FeatureFlagService featureFlagService;
    

    
    @Value("${secondary.datasource.enabled:true}")
    private boolean secondaryDatabaseEnabled;
    
    private static final String USE_NEON_FLAG = "use-neon";
    
    @Override
    public <T> T executeRead(String userId, Supplier<T> primaryOperation, Supplier<T> secondaryOperation) {
        if (!isSecondaryDatabaseEnabled()) {
            log.debug("Secondary database not enabled, using primary for read operation");
            return primaryOperation.get();
        }
        
        boolean useNeon = shouldUseSecondaryForRead(userId);
        
        if (useNeon) {
            try {
                log.debug("Using secondary (Neon) database for read operation for user: {}", userId);
                return secondaryOperation.get();
            } catch (Exception e) {
                log.warn("Error reading from secondary database for user {}, falling back to primary: {}", userId, e.getMessage());
                return primaryOperation.get();
            }
        } else {
            log.debug("Using primary database for read operation for user: {}", userId);
            return primaryOperation.get();
        }
    }
    
    @Override
    public <T> T executeWrite(Supplier<T> primaryOperation, Supplier<T> secondaryOperation) {
        return executeWriteWithUser("system", primaryOperation, secondaryOperation);
    }
    
    @Override
    public <T> T executeWriteWithUser(String userId, Supplier<T> primaryOperation, Supplier<T> secondaryOperation) {
        T primaryResult = null;
        T secondaryResult = null;
        Exception primaryException = null;
        Exception secondaryException = null;
        
        // Always execute primary operation first to get the generated ID
        try {
            primaryResult = primaryOperation.get();
            log.debug("Primary database write completed successfully for user: {}", userId);
        } catch (Exception e) {
            primaryException = e;
            log.error("Primary database write failed for user {}: {}", userId, e.getMessage());
        }
        
        // Execute secondary operation if enabled and primary succeeded
        if (isSecondaryDatabaseEnabled() && primaryResult != null) {
            try {
                secondaryResult = secondaryOperation.get();
                log.debug("Secondary database write completed successfully for user: {}", userId);
            } catch (Exception e) {
                secondaryException = e;
                log.error("Secondary database write failed for user {}: {}", userId, e.getMessage());
                // Don't fail the entire operation if secondary fails, just log it
            }
        }
        
        // Always return the primary result since it's the source of truth for IDs
        if (primaryResult != null) {
            return primaryResult;
        } else if (primaryException != null) {
            throw new RuntimeException("Primary database operation failed", primaryException);
        }
        
        // Fallback - this shouldn't happen
        throw new RuntimeException("Primary database operation failed");
    }
    
    @Override
    public boolean isSecondaryDatabaseEnabled() {
        return secondaryDatabaseEnabled;
    }
    
    @Override
    public boolean shouldUseSecondaryForRead(String userId) {
        if (!isSecondaryDatabaseEnabled()) {
            return false;
        }
        
        try {
            boolean useNeon = featureFlagService.getBooleanValue(userId, USE_NEON_FLAG, false);
            log.debug("Feature flag '{}' for user '{}': {}", USE_NEON_FLAG, userId, useNeon);
            return useNeon;
        } catch (Exception e) {
            log.warn("Error evaluating feature flag '{}' for user '{}', defaulting to false: {}", 
                    USE_NEON_FLAG, userId, e.getMessage());
            return false;
        }
    }
    

} 