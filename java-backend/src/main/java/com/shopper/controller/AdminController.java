package com.shopper.controller;

import com.shopper.service.AdminService;
import com.shopper.service.DatabaseSyncService;
import com.shopper.service.FeatureFlagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin", description = "Admin endpoints")
public class AdminController {
    
    private final AdminService adminService;
    private final FeatureFlagService featureFlagService;
    
    @Autowired(required = false)
    private DatabaseSyncService databaseSyncService;
    
    @PostMapping("/sync/all")
    @Operation(summary = "Sync all data between databases")
    public ResponseEntity<Map<String, Object>> syncAllData() {
        try {
            Map<String, Object> result = adminService.syncAllData();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Sync failed: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "message", "Sync failed",
                "error", e.getMessage()
            ));
        }
    }
    
    @PostMapping("/sync/{entity}")
    @Operation(summary = "Sync specific entity")
    public ResponseEntity<Map<String, Object>> syncSpecificEntity(@PathVariable String entity) {
        try {
            Map<String, Object> result = adminService.syncSpecificEntity(entity);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Sync failed for entity {}: {}", entity, e.getMessage());
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "message", "Sync failed for " + entity,
                "error", e.getMessage()
            ));
        }
    }
    
    @GetMapping("/database/stats")
    @Operation(summary = "Get database statistics")
    public ResponseEntity<Map<String, Object>> getDatabaseStats() {
        try {
            Map<String, Object> stats = adminService.getDatabaseStats();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("Failed to get database stats: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "message", "Failed to retrieve database stats",
                "error", e.getMessage()
            ));
        }
    }
    
    @GetMapping("/sync/status")
    @Operation(summary = "Get sync status")
    public ResponseEntity<Map<String, Object>> getSyncStatus() {
        try {
            Map<String, Object> status = adminService.getSyncStatus();
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            log.error("Failed to get sync status: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "message", "Failed to retrieve sync status",
                "error", e.getMessage()
            ));
        }
    }
    
    // Database sync endpoints
    @PostMapping("/database/sync/products")
    @Operation(summary = "Sync products from primary to secondary database")
    public ResponseEntity<Map<String, Object>> syncProductsToSecondary() {
        if (databaseSyncService == null) {
            return ResponseEntity.status(503).body(Map.of(
                "success", false,
                "message", "Database sync service not available (secondary database not enabled)"
            ));
        }
        
        try {
            int syncedCount = databaseSyncService.syncProductsToSecondary();
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Products synced to secondary database",
                "syncedCount", syncedCount
            ));
        } catch (Exception e) {
            log.error("Failed to sync products to secondary: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "message", "Sync failed",
                "error", e.getMessage()
            ));
        }
    }

    @PostMapping("/database/sync/cart-items")
    @Operation(summary = "Sync cart items from primary to secondary database")
    public ResponseEntity<Map<String, Object>> syncCartItemsToSecondary() {
        if (databaseSyncService == null) {
            return ResponseEntity.status(503).body(Map.of(
                "success", false,
                "message", "Database sync service not available (secondary database not enabled)"
            ));
        }
        
        try {
            int syncedCount = databaseSyncService.syncCartItemsToSecondary();
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Cart items synced to secondary database",
                "syncedCount", syncedCount
            ));
        } catch (Exception e) {
            log.error("Failed to sync cart items to secondary: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "message", "Sync failed",
                "error", e.getMessage()
            ));
        }
    }
    
    @PostMapping("/database/sync/bidirectional")
    @Operation(summary = "Perform bidirectional database synchronization")
    public ResponseEntity<Map<String, Object>> performBidirectionalSync() {
        if (databaseSyncService == null) {
            return ResponseEntity.status(503).body(Map.of(
                "success", false,
                "message", "Database sync service not available (secondary database not enabled)"
            ));
        }
        
        try {
            DatabaseSyncService.SyncResult result = databaseSyncService.performBidirectionalSync();
            return ResponseEntity.ok(Map.of(
                "success", result.success,
                "message", result.message,
                "primaryToSecondaryCount", result.primaryToSecondaryCount,
                "secondaryToPrimaryCount", result.secondaryToPrimaryCount,
                "timestamp", result.timestamp
            ));
        } catch (Exception e) {
            log.error("Failed to perform bidirectional sync: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "message", "Bidirectional sync failed",
                "error", e.getMessage()
            ));
        }
    }

    @PostMapping("/database/fix-postgresql-schema")
    @Operation(summary = "Fix PostgreSQL schema to use proper UUID columns")
    public ResponseEntity<Map<String, Object>> fixPostgreSQLSchema() {
        if (databaseSyncService == null) {
            return ResponseEntity.status(503).body(Map.of(
                "success", false,
                "message", "Database sync service not available (secondary database not enabled)"
            ));
        }
        
        try {
            String result = databaseSyncService.fixPostgreSQLSchema();
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "PostgreSQL schema successfully fixed",
                "details", result
            ));
        } catch (Exception e) {
            log.error("Failed to fix PostgreSQL schema: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "message", "Failed to fix PostgreSQL schema",
                "error", e.getMessage()
            ));
        }
    }
    
    @GetMapping("/database/consistency")
    @Operation(summary = "Verify data consistency between databases")
    public ResponseEntity<Map<String, Object>> verifyDataConsistency() {
        if (databaseSyncService == null) {
            return ResponseEntity.status(503).body(Map.of(
                "success", false,
                "message", "Database sync service not available (secondary database not enabled)"
            ));
        }
        
        try {
            DatabaseSyncService.ConsistencyReport report = databaseSyncService.verifyDataConsistency();
            return ResponseEntity.ok(Map.of(
                "success", true,
                "isConsistent", report.isConsistent,
                "message", report.message,
                "primaryCount", report.primaryCount,
                "secondaryCount", report.secondaryCount,
                "inconsistencies", report.inconsistencies,
                "timestamp", report.timestamp
            ));
        } catch (Exception e) {
            log.error("Failed to verify data consistency: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "message", "Consistency check failed",
                "error", e.getMessage()
            ));
        }
    }
    
    // Feature flag endpoints
    @GetMapping("/feature-flags")
    @Operation(summary = "Get all feature flags for admin user")
    public ResponseEntity<Map<String, Object>> getAllFeatureFlags() {
        try {
            Map<String, Object> flags = featureFlagService.getAllFeatures("admin");
            return ResponseEntity.ok(Map.of(
                "success", true,
                "flags", flags
            ));
        } catch (Exception e) {
            log.error("Failed to get feature flags: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "message", "Failed to retrieve feature flags",
                "error", e.getMessage()
            ));
        }
    }
        
    @GetMapping("/feature-flags/devcycle/status")
    @Operation(summary = "Check DevCycle integration status")
    public ResponseEntity<Map<String, Object>> getDevCycleStatus() {
        try {
            boolean isInitialized = featureFlagService.isInitialized();
            Map<String, Object> status = new HashMap<>();
            status.put("isDevCycleConnected", isInitialized);
            status.put("source", isInitialized ? "DevCycle/OpenFeature" : "Fallback");
            
            if (isInitialized) {
                // Test the use-neon flag specifically with different user contexts
                Map<String, Object> useNeonTests = new HashMap<>();
                String[] testUsers = {"admin", "user1", "test-user"};
                
                for (String userId : testUsers) {
                    boolean flagValue = featureFlagService.getBooleanValue(userId, "use-neon", false);
                    useNeonTests.put(userId, flagValue);
                }
                
                status.put("useNeonFlagTests", useNeonTests);
            }
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "status", status
            ));
        } catch (Exception e) {
            log.error("Failed to check DevCycle status: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "message", "Failed to check DevCycle status",
                "error", e.getMessage()
            ));
        }
    }
}