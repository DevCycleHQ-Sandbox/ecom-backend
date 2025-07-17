package com.shopper.service;

import dev.openfeature.sdk.Client;
import dev.openfeature.sdk.EvaluationContext;
import dev.openfeature.sdk.FlagEvaluationDetails;
import dev.openfeature.sdk.MutableContext;
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
    
    private final Client openFeatureClient;
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
        }
    }

    private EvaluationContext createEvaluationContext(String userId) {
        return new MutableContext(userId).add("user_id", userId);
    }

    public boolean getBooleanValue(String userId, String key, boolean defaultValue) {
        ensureInitialized();
        
        if (!isOpenFeatureAvailable()) {
            return getFallbackBooleanValue(key, defaultValue);
        }
        
        try {
            EvaluationContext context = createEvaluationContext(userId);
            FlagEvaluationDetails<Boolean> details = openFeatureClient.getBooleanDetails(key, defaultValue, context);
            
            boolean flagValue = details.getValue();
            log.debug("🎛️ Feature flag '{}' evaluated to: {} for user: {} (reason: {})", 
                     key, flagValue, userId, details.getReason());
            
            return flagValue;
            
        } catch (Exception e) {
            log.warn("Error getting feature flag '{}' for user '{}': {}", key, userId, e.getMessage());
            return getFallbackBooleanValue(key, defaultValue);
        }
    }
    
    public String getStringValue(String userId, String key, String defaultValue) {
        ensureInitialized();
        
        if (!isOpenFeatureAvailable()) {
            return getFallbackStringValue(key, defaultValue);
        }
        
        try {
            EvaluationContext context = createEvaluationContext(userId);
            FlagEvaluationDetails<String> details = openFeatureClient.getStringDetails(key, defaultValue, context);
            
            String flagValue = details.getValue();
            log.debug("🎛️ Feature flag '{}' evaluated to: {} for user: {} (reason: {})", 
                     key, flagValue, userId, details.getReason());
            
            return flagValue;
            
        } catch (Exception e) {
            log.warn("Error getting feature flag '{}' for user '{}': {}", key, userId, e.getMessage());
            return getFallbackStringValue(key, defaultValue);
        }
    }
    
    public Number getNumberValue(String userId, String key, Number defaultValue) {
        ensureInitialized();
        
        if (!isOpenFeatureAvailable()) {
            return getFallbackNumberValue(key, defaultValue);
        }
        
        try {
            EvaluationContext context = createEvaluationContext(userId);
            FlagEvaluationDetails<Double> details = openFeatureClient.getDoubleDetails(key, defaultValue.doubleValue(), context);
            
            Number flagValue = details.getValue();
            log.debug("🎛️ Feature flag '{}' evaluated to: {} for user: {} (reason: {})", 
                     key, flagValue, userId, details.getReason());
            
            return flagValue;
            
        } catch (Exception e) {
            log.warn("Error getting feature flag '{}' for user '{}': {}", key, userId, e.getMessage());
            return getFallbackNumberValue(key, defaultValue);
        }
    }
    
    public Object getObjectValue(String userId, String key, Object defaultValue) {
        ensureInitialized();
        
        if (!isOpenFeatureAvailable()) {
            return getFallbackObjectValue(key, defaultValue);
        }
        
        try {
            EvaluationContext context = createEvaluationContext(userId);
            FlagEvaluationDetails<dev.openfeature.sdk.Value> details = openFeatureClient.getObjectDetails(key, dev.openfeature.sdk.Value.objectToValue(defaultValue), context);
            
            Object flagValue = details.getValue().asObject();
            log.debug("🎛️ Feature flag '{}' evaluated to: {} for user: {} (reason: {})", 
                     key, flagValue, userId, details.getReason());
            
            return flagValue;
            
        } catch (Exception e) {
            log.warn("Error getting feature flag '{}' for user '{}': {}", key, userId, e.getMessage());
            return getFallbackObjectValue(key, defaultValue);
        }
    }
    
    public Map<String, Object> getAllFeatures(String userId) {
        ensureInitialized();
        
        if (!isOpenFeatureAvailable()) {
            log.debug("OpenFeature not available, returning fallback features map");
            return new HashMap<>(fallbackFlags);
        }
        
        try {
            // OpenFeature doesn't have a direct getAllFeatures method
            // We'll return common feature flags that we know about
            Map<String, Object> features = new HashMap<>();
            
            EvaluationContext context = createEvaluationContext(userId);
            
            // Get known feature flags
            features.put("new-flow", openFeatureClient.getBooleanValue("new-flow", false, context));
            features.put("premium-features", openFeatureClient.getBooleanValue("premium-features", true, context));
            features.put("enhanced-product-details", openFeatureClient.getBooleanValue("enhanced-product-details", true, context));
            features.put("beta-features", openFeatureClient.getBooleanValue("beta-features", false, context));
            features.put("use-neon", openFeatureClient.getBooleanValue("use-neon", false, context));
            
            log.debug("All features for user '{}': {}", userId, features);
            return features;
            
        } catch (Exception e) {
            log.warn("Error getting all features for user '{}': {}", userId, e.getMessage());
            return new HashMap<>(fallbackFlags);
        }
    }
    
    public boolean isInitialized() {
        return initialized && isOpenFeatureAvailable();
    }
    
    private boolean isOpenFeatureAvailable() {
        return openFeatureClient != null && 
               devCycleServerSdkKey != null && 
               !devCycleServerSdkKey.isEmpty() && 
               !devCycleServerSdkKey.startsWith("your-");
    }
    
    // Fallback methods when OpenFeature is not available
    private boolean getFallbackBooleanValue(String key, boolean defaultValue) {
        Object value = fallbackFlags.get(key);
        if (value instanceof Boolean) {
            boolean flagValue = (Boolean) value;
            log.debug("Fallback feature flag '{}' evaluated to: {}", key, flagValue);
            return flagValue;
        }
        log.debug("Fallback feature flag '{}' not found, returning default value: {}", key, defaultValue);
        return defaultValue;
    }
    
    private String getFallbackStringValue(String key, String defaultValue) {
        Object value = fallbackFlags.get(key);
        if (value instanceof String) {
            String flagValue = (String) value;
            log.debug("Fallback feature flag '{}' evaluated to: {}", key, flagValue);
            return flagValue;
        }
        log.debug("Fallback feature flag '{}' not found, returning default value: {}", key, defaultValue);
        return defaultValue;
    }
    
    private Number getFallbackNumberValue(String key, Number defaultValue) {
        Object value = fallbackFlags.get(key);
        if (value instanceof Number) {
            Number flagValue = (Number) value;
            log.debug("Fallback feature flag '{}' evaluated to: {}", key, flagValue);
            return flagValue;
        }
        log.debug("Fallback feature flag '{}' not found, returning default value: {}", key, defaultValue);
        return defaultValue;
    }
    
    private Object getFallbackObjectValue(String key, Object defaultValue) {
        Object value = fallbackFlags.get(key);
        if (value != null) {
            log.debug("Fallback feature flag '{}' evaluated to: {}", key, value);
            return value;
        }
        log.debug("Fallback feature flag '{}' not found, returning default value: {}", key, defaultValue);
        return defaultValue;
    }
    
    // Admin methods to update feature flags at runtime (for fallback flags)
    public void updateFeatureFlag(String key, Object value) {
        ensureInitialized();
        fallbackFlags.put(key, value);
        log.info("Fallback feature flag '{}' updated to: {}", key, value);
    }
    
    public void removeFeatureFlag(String key) {
        ensureInitialized();
        fallbackFlags.remove(key);
        log.info("Fallback feature flag '{}' removed", key);
    }
    
    public Map<String, Object> getAllFeatureFlags() {
        ensureInitialized();
        return new HashMap<>(fallbackFlags);
    }
}