package com.shopper.service;

import com.dynatrace.oneagent.sdk.OneAgentSDK;
import com.dynatrace.oneagent.sdk.api.DatabaseInfo;
import com.dynatrace.oneagent.sdk.api.DatabaseRequestTracer;
import com.dynatrace.oneagent.sdk.api.enums.ChannelType;
import com.dynatrace.oneagent.sdk.api.enums.DatabaseVendor;
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
    
    @Autowired
    private OneAgentSDK oneAgentSDK;
    
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
                return traceDatabaseOperation("SELECT", "secondary", secondaryOperation);
            } catch (Exception e) {
                log.warn("Error reading from secondary database for user {}, falling back to primary: {}", userId, e.getMessage());
                return traceDatabaseOperation("SELECT (fallback)", "primary", primaryOperation);
            }
        } else {
            log.debug("Using primary database for read operation for user: {}", userId);
            return traceDatabaseOperation("SELECT", "primary", primaryOperation);
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
            primaryResult = traceDatabaseOperation("INSERT/UPDATE", "primary", primaryOperation);
            log.debug("Primary database write completed successfully for user: {}", userId);
        } catch (Exception e) {
            primaryException = e;
            log.error("Primary database write failed for user {}: {}", userId, e.getMessage());
        }
        
        // Execute secondary operation if enabled and primary succeeded
        if (isSecondaryDatabaseEnabled() && primaryResult != null) {
            try {
                secondaryResult = traceDatabaseOperation("INSERT/UPDATE", "secondary", secondaryOperation);
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
    
    /**
     * Execute database operation with OneAgent SDK tracing
     */
    private <T> T traceDatabaseOperation(String operationType, String databaseType, Supplier<T> operation) {
        DatabaseInfo databaseInfo = createDatabaseInfo(databaseType);
        DatabaseRequestTracer tracer = oneAgentSDK.traceSqlDatabaseRequest(databaseInfo, operationType);
        tracer.start();
        
        try {
            T result = operation.get();
            // For successful operations, we don't set error status
            return result;
        } catch (Exception e) {
            tracer.error(e.getMessage());
            throw e;
        } finally {
            tracer.end();
        }
    }
    
    /**
     * Create DatabaseInfo for OneAgent SDK tracing
     */
    private DatabaseInfo createDatabaseInfo(String databaseType) {
        if ("secondary".equals(databaseType)) {
            // Neon (PostgreSQL) database
            return oneAgentSDK.createDatabaseInfo("Neon PostgreSQL", 
                    DatabaseVendor.POSTGRESQL.getVendorName(), 
                    ChannelType.TCP_IP, 
                    "neon-db:5432");
        } else {
            // Primary SQLite database
            return oneAgentSDK.createDatabaseInfo("Primary SQLite", 
                    DatabaseVendor.SQLITE.getVendorName(), 
                    ChannelType.OTHER, 
                    "local-sqlite");
        }
    }
} 