package com.shopper.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class FeatureFlagService {
    
    @Value("${app.devcycle.server-sdk-key}")
    private String devCycleServerSdkKey;
    
    private final Map<String, Object> featureFlags = new ConcurrentHashMap<>();
    private boolean initialized = false;
    
    private void ensureInitialized() {
        if (!initialized) {
            // Initialize with default feature flags for demo purposes
            featureFlags.put("new-flow", false);
            featureFlags.put("premium-features", true);
            featureFlags.put("enhanced-product-details", true);
            featureFlags.put("beta-features", false);
            
            initialized = true;
            log.info("Feature flags initialized with default values");
        }
    }
    
    public boolean getBooleanValue(String userId, String key, boolean defaultValue) {
        ensureInitialized();
        
        try {
            Object value = featureFlags.get(key);
            if (value instanceof Boolean) {
                boolean flagValue = (Boolean) value;
                log.debug("Feature flag '{}' evaluated to: {} for user: {}", key, flagValue, userId);
                return flagValue;
            }
            
            log.debug("Feature flag '{}' not found, returning default value: {} for user: {}", key, defaultValue, userId);
            return defaultValue;
            
        } catch (Exception e) {
            log.warn("Error getting feature flag '{}' for user '{}': {}", key, userId, e.getMessage());
            return defaultValue;
        }
    }
    
    public String getStringValue(String userId, String key, String defaultValue) {
        ensureInitialized();
        
        try {
            Object value = featureFlags.get(key);
            if (value instanceof String) {
                String flagValue = (String) value;
                log.debug("Feature flag '{}' evaluated to: {} for user: {}", key, flagValue, userId);
                return flagValue;
            }
            
            log.debug("Feature flag '{}' not found, returning default value: {} for user: {}", key, defaultValue, userId);
            return defaultValue;
            
        } catch (Exception e) {
            log.warn("Error getting feature flag '{}' for user '{}': {}", key, userId, e.getMessage());
            return defaultValue;
        }
    }
    
    public Number getNumberValue(String userId, String key, Number defaultValue) {
        ensureInitialized();
        
        try {
            Object value = featureFlags.get(key);
            if (value instanceof Number) {
                Number flagValue = (Number) value;
                log.debug("Feature flag '{}' evaluated to: {} for user: {}", key, flagValue, userId);
                return flagValue;
            }
            
            log.debug("Feature flag '{}' not found, returning default value: {} for user: {}", key, defaultValue, userId);
            return defaultValue;
            
        } catch (Exception e) {
            log.warn("Error getting feature flag '{}' for user '{}': {}", key, userId, e.getMessage());
            return defaultValue;
        }
    }
    
    public Object getObjectValue(String userId, String key, Object defaultValue) {
        ensureInitialized();
        
        try {
            Object value = featureFlags.get(key);
            if (value != null) {
                log.debug("Feature flag '{}' evaluated to: {} for user: {}", key, value, userId);
                return value;
            }
            
            log.debug("Feature flag '{}' not found, returning default value: {} for user: {}", key, defaultValue, userId);
            return defaultValue;
            
        } catch (Exception e) {
            log.warn("Error getting feature flag '{}' for user '{}': {}", key, userId, e.getMessage());
            return defaultValue;
        }
    }
    
    public Map<String, Object> getAllFeatures(String userId) {
        ensureInitialized();
        
        try {
            Map<String, Object> userFeatures = new HashMap<>(featureFlags);
            log.debug("All features for user '{}': {}", userId, userFeatures);
            return userFeatures;
            
        } catch (Exception e) {
            log.warn("Error getting all features for user '{}': {}", userId, e.getMessage());
            return new HashMap<>();
        }
    }
    
    public boolean isInitialized() {
        return initialized;
    }
    
    // Admin methods to update feature flags at runtime
    public void updateFeatureFlag(String key, Object value) {
        ensureInitialized();
        featureFlags.put(key, value);
        log.info("Feature flag '{}' updated to: {}", key, value);
    }
    
    public void removeFeatureFlag(String key) {
        ensureInitialized();
        featureFlags.remove(key);
        log.info("Feature flag '{}' removed", key);
    }
    
    public Map<String, Object> getAllFeatureFlags() {
        ensureInitialized();
        return new HashMap<>(featureFlags);
    }
}