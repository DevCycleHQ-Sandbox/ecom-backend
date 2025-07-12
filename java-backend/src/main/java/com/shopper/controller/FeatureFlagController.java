package com.shopper.controller;

import com.shopper.entity.User;
import com.shopper.service.FeatureFlagService;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/feature-flags")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Feature Flags", description = "Feature flag management and testing endpoints")
public class FeatureFlagController {

    private final FeatureFlagService featureFlagService;
    private final Tracer tracer;

    @GetMapping("/test")
    @Operation(summary = "Test feature flag evaluation")
    public ResponseEntity<Map<String, Object>> testFeatureFlags() {
        Span span = tracer.spanBuilder("feature_flag_test").startSpan();
        
        try {
            String username = getCurrentUsername();
            
            span.setAttribute("user.name", username);
            
            Map<String, Object> flags = Map.of(
                "new-flow", featureFlagService.getBooleanValue(username, "new-flow", false),
                "premium-features", featureFlagService.getBooleanValue(username, "premium-features", true),
                "enhanced-product-details", featureFlagService.getBooleanValue(username, "enhanced-product-details", true),
                "beta-features", featureFlagService.getBooleanValue(username, "beta-features", false)
            );
            
            span.setAttribute("flags.count", flags.size());
            
            log.info("🎛️ Feature flags evaluated for user '{}': {}", username, flags);
            
            return ResponseEntity.ok(Map.of(
                "user", username,
                "flags", flags,
                "timestamp", System.currentTimeMillis()
            ));
            
        } finally {
            span.end();
        }
    }

    @GetMapping("/all")
    @Operation(summary = "Get all available feature flags")
    public ResponseEntity<Map<String, Object>> getAllFeatureFlags() {
        String username = getCurrentUsername();
        Map<String, Object> allFlags = featureFlagService.getAllFeatures(username);
        
        return ResponseEntity.ok(Map.of(
            "user", username,
            "flags", allFlags,
            "count", allFlags.size(),
            "timestamp", System.currentTimeMillis()
        ));
    }

    @GetMapping("/flag/{flagKey}")
    @Operation(summary = "Get specific feature flag value")
    public ResponseEntity<Map<String, Object>> getFeatureFlag(@PathVariable String flagKey) {
        String username = getCurrentUsername();
        
        // Try different types
        Object value;
        try {
            value = featureFlagService.getBooleanValue(username, flagKey, false);
        } catch (Exception e) {
            try {
                value = featureFlagService.getStringValue(username, flagKey, "");
            } catch (Exception e2) {
                try {
                    value = featureFlagService.getNumberValue(username, flagKey, 0);
                } catch (Exception e3) {
                    value = featureFlagService.getObjectValue(username, flagKey, null);
                }
            }
        }
        
        return ResponseEntity.ok(Map.of(
            "user", username,
            "flagKey", flagKey,
            "value", value,
            "type", value != null ? value.getClass().getSimpleName() : "null",
            "timestamp", System.currentTimeMillis()
        ));
    }

    @PostMapping("/admin/update/{flagKey}")
    @Operation(summary = "Update fallback feature flag value (Admin only)")
    public ResponseEntity<Map<String, Object>> updateFeatureFlag(
            @PathVariable String flagKey,
            @RequestBody Map<String, Object> request) {
        
        Object value = request.get("value");
        featureFlagService.updateFeatureFlag(flagKey, value);
        
        return ResponseEntity.ok(Map.of(
            "message", "Feature flag updated",
            "flagKey", flagKey,
            "value", value,
            "timestamp", System.currentTimeMillis()
        ));
    }

    @DeleteMapping("/admin/remove/{flagKey}")
    @Operation(summary = "Remove fallback feature flag (Admin only)")
    public ResponseEntity<Map<String, Object>> removeFeatureFlag(@PathVariable String flagKey) {
        featureFlagService.removeFeatureFlag(flagKey);
        
        return ResponseEntity.ok(Map.of(
            "message", "Feature flag removed",
            "flagKey", flagKey,
            "timestamp", System.currentTimeMillis()
        ));
    }

    @GetMapping("/status")
    @Operation(summary = "Get feature flag service status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        return ResponseEntity.ok(Map.of(
            "initialized", featureFlagService.isInitialized(),
            "service", "FeatureFlagService",
            "provider", "OpenFeature with DevCycle",
            "timestamp", System.currentTimeMillis()
        ));
    }

    private String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && authentication.getPrincipal() instanceof User) {
            return ((User) authentication.getPrincipal()).getUsername();
        }
        return "anonymous";
    }
}