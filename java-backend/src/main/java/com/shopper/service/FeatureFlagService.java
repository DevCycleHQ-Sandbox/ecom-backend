package com.shopper.service;

import com.devcycle.sdk.server.common.model.DevCycleUser;
import com.devcycle.sdk.server.local.api.DevCycleLocalClient;

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
    
    private final DevCycleLocalClient devCycleLocalClient;
    private final Map<String, Object> fallbackFlags = new ConcurrentHashMap<>();
    private boolean initialized = false;
    
    private void ensureInitialized() {
        if (!initialized) {
            // Initialize with default feature flags for demo purposes (fallback)
            fallbackFlags.put("new-flow", false);
            fallbackFlags.put("premium-features", true);
            fallbackFlags.put("enhanced-product-details", true);
            fallbackFlags.put("beta-features", false);
            fallbackFlags.put("use-neon", false);
            
            initialized = true;
            log.info("Feature flags initialized with default values");
            
            if (devCycleLocalClient == null) {
                log.info("🔄 DevCycle client not available, using fallback values for all feature flags");
            }
        }
    }

    public boolean getBooleanValue(String userId, String key, boolean defaultValue) {
        ensureInitialized();
        
        if (devCycleLocalClient == null) {
            log.debug("🔄 DevCycle client not available, using fallback for '{}': {}", key, defaultValue);
            return defaultValue;
        }
        
        try {
            DevCycleUser user = DevCycleUser.builder().userId(userId).build();
            Boolean details = devCycleLocalClient.variableValue(user, key, defaultValue);
            
            boolean flagValue = details;
            log.debug("🎛️ Feature flag '{}' evaluated to: {} for user: {}", 
                     key, flagValue, userId);
            
            return flagValue;
            
        } catch (Exception e) {
            log.warn("Error getting feature flag '{}' for user '{}': {}, using default: {}", 
                    key, userId, e.getMessage(), defaultValue);
            return defaultValue;
        }
    }
    
    public String getStringValue(String userId, String key, String defaultValue) {
        ensureInitialized();
        
        if (devCycleLocalClient == null) {
            log.debug("🔄 DevCycle client not available, using fallback for '{}': {}", key, defaultValue);
            return defaultValue;
        }
        
        try {
            DevCycleUser user = DevCycleUser.builder().userId(userId).build();
            String flagValue = devCycleLocalClient.variableValue(user, key, defaultValue);
            
            log.debug("🎛️ Feature flag '{}' evaluated to: {} for user: {}", 
                        key, flagValue, userId);
            
            return flagValue;
            
        } catch (Exception e) {
            log.warn("Error getting feature flag '{}' for user '{}': {}, using default: {}", 
                    key, userId, e.getMessage(), defaultValue);
            return defaultValue;
        }
    }
    
    public Number getNumberValue(String userId, String key, Number defaultValue) {
        ensureInitialized();

        if (devCycleLocalClient == null) {
            log.debug("🔄 DevCycle client not available, using fallback for '{}': {}", key, defaultValue);
            return defaultValue;
        }
        
        try {
            DevCycleUser user = DevCycleUser.builder().userId(userId).build();
            Double flagValue = devCycleLocalClient.variableValue(user, key, defaultValue.doubleValue());
            
            log.debug("🎛️ Feature flag '{}' evaluated to: {} for user: {}", 
                     key, flagValue, userId);
            
            return flagValue;
            
        } catch (Exception e) {
            log.warn("Error getting feature flag '{}' for user '{}': {}, using default: {}", 
                    key, userId, e.getMessage(), defaultValue);
            return defaultValue;
        }
    }
    
    public Object getObjectValue(String userId, String key, Object defaultValue) {
        ensureInitialized();
        
        if (devCycleLocalClient == null) {
            log.debug("🔄 DevCycle client not available, using fallback for '{}': {}", key, defaultValue);
            return defaultValue;
        }
        
        try {
            DevCycleUser user = DevCycleUser.builder().userId(userId).build();
            Object flagValue = devCycleLocalClient.variableValue(user, key, defaultValue);
            
            log.debug("🎛️ Feature flag '{}' evaluated to: {} for user: {}", 
                     key, flagValue, userId);
            
            return flagValue;
            
        } catch (Exception e) {
            log.warn("Error getting feature flag '{}' for user '{}': {}, using default: {}", 
                    key, userId, e.getMessage(), defaultValue);
            return defaultValue;
        }
    }
    
    public Map<String, Object> getAllFeatures(String userId) {
        ensureInitialized();
        
        if (devCycleLocalClient == null) {
            log.debug("🔄 DevCycle client not available, returning fallback flags for user: {}", userId);
            return new HashMap<>(fallbackFlags);
        }
        
        try {
            Map<String, Object> features = new HashMap<>();
            
            DevCycleUser user = DevCycleUser.builder().userId(userId).build();
            
            features.put("new-flow", devCycleLocalClient.variableValue(user, "new-flow", false));
            features.put("premium-features", devCycleLocalClient.variableValue(user, "premium-features", true));
            features.put("enhanced-product-details", devCycleLocalClient.variableValue(user, "enhanced-product-details", true));
            features.put("beta-features", devCycleLocalClient.variableValue(user, "beta-features", false));
            features.put("use-neon", devCycleLocalClient.variableValue(user, "use-neon", false));
            
            log.debug("All features for user '{}': {}", userId, features);
            return features;
            
        } catch (Exception e) {
            log.warn("Error getting all features for user '{}': {}, using fallbacks", userId, e.getMessage());
            return new HashMap<>(fallbackFlags);
        }
    }
    
    public boolean isInitialized() {
        return initialized && (devCycleLocalClient == null || devCycleLocalClient.isInitialized());
    }
    
}