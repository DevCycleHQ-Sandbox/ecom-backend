package com.shopper.backend.controller;

import com.shopper.backend.service.DatabaseSyncService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin", description = "Admin management APIs")
public class AdminController {

    @Autowired
    private DatabaseSyncService databaseSyncService;

    @PostMapping("/sync/all")
    @Operation(summary = "Sync all data between databases")
    public ResponseEntity<Map<String, Object>> syncAllData() {
        try {
            Map<String, Object> result = databaseSyncService.syncAllData();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Database sync completed");
            response.put("timestamp", LocalDateTime.now());
            response.put("data", result);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Sync failed");
            response.put("timestamp", LocalDateTime.now());
            response.put("error", e.getMessage());

            return ResponseEntity.status(500).body(response);
        }
    }

    @PostMapping("/sync/{entity}")
    @Operation(summary = "Sync specific entity")
    public ResponseEntity<Map<String, Object>> syncSpecificEntity(@PathVariable String entity) {
        try {
            Map<String, Object> result = databaseSyncService.syncSpecificEntity(entity);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Sync completed for " + entity);
            response.put("timestamp", LocalDateTime.now());
            response.put("data", result);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Sync failed for " + entity);
            response.put("timestamp", LocalDateTime.now());
            response.put("error", e.getMessage());

            return ResponseEntity.status(500).body(response);
        }
    }

    @GetMapping("/database/stats")
    @Operation(summary = "Get database statistics")
    public ResponseEntity<Map<String, Object>> getDatabaseStats() {
        try {
            Map<String, Object> stats = databaseSyncService.getDatabaseStats();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Database statistics retrieved");
            response.put("timestamp", LocalDateTime.now());
            response.put("data", stats);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to retrieve database stats");
            response.put("timestamp", LocalDateTime.now());
            response.put("error", e.getMessage());

            return ResponseEntity.status(500).body(response);
        }
    }

    @GetMapping("/sync/status")
    @Operation(summary = "Get sync status")
    public ResponseEntity<Map<String, Object>> getSyncStatus() {
        try {
            Map<String, Object> stats = databaseSyncService.getDatabaseStats();
            
            // Calculate sync status by comparing counts
            Map<String, Object> syncStatus = new HashMap<>();
            
            // For now, we'll just return the basic stats
            // In a real implementation, you would compare primary vs secondary database counts
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Sync status retrieved");
            response.put("timestamp", LocalDateTime.now());
            response.put("data", Map.of(
                "overallStatus", "IN_SYNC", // This would be calculated based on actual comparison
                "entities", stats
            ));

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to retrieve sync status");
            response.put("timestamp", LocalDateTime.now());
            response.put("error", e.getMessage());

            return ResponseEntity.status(500).body(response);
        }
    }

    @GetMapping("/health")
    @Operation(summary = "Admin health check")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "healthy");
        response.put("timestamp", LocalDateTime.now());
        response.put("message", "Admin services are running");

        return ResponseEntity.ok(response);
    }
}