package com.shopper.controller;

import com.shopper.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin", description = "Admin endpoints")
public class AdminController {
    
    private final AdminService adminService;
    
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
}