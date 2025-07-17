package com.shopper.service;

import dev.openfeature.sdk.Client;
import dev.openfeature.sdk.EvaluationContext;
import dev.openfeature.sdk.MutableContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(properties = {
    "app.devcycle.server-sdk-key=your-test-key",
    "app.telemetry.use-local-otlp=false"
})
class FeatureFlagServiceTest {

    @Autowired
    private FeatureFlagService featureFlagService;

    @Autowired
    private Client openFeatureClient;

    @Test
    void testFeatureFlagServiceInitialization() {
        assertNotNull(featureFlagService);
        assertNotNull(openFeatureClient);
    }

    @Test
    void testBooleanFeatureFlag() {
        String userId = "test-user";
        String flagKey = "new-flow";
        boolean defaultValue = false;

        // Test with fallback (since we're using a test key)
        boolean result = featureFlagService.getBooleanValue(userId, flagKey, defaultValue);
        
        // Should return default value when DevCycle is not properly configured
        assertEquals(defaultValue, result);
    }

    @Test
    void testStringFeatureFlag() {
        String userId = "test-user";
        String flagKey = "test-string-flag";
        String defaultValue = "default";

        String result = featureFlagService.getStringValue(userId, flagKey, defaultValue);
        
        // Should return default value when DevCycle is not properly configured
        assertEquals(defaultValue, result);
    }

    @Test
    void testEvaluationContext() {
        String userId = "test-user";
        
        // Create evaluation context as per DevCycle documentation
        EvaluationContext context = new MutableContext(userId)
                .add("user_id", userId)
                .add("email", "test@example.com");

        assertNotNull(context);
        assertEquals(userId, context.getTargetingKey());
        assertTrue(context.asObjectMap().containsKey("user_id"));
    }

    @Test
    void testOpenFeatureClientDirectly() {
        String userId = "test-user";
        EvaluationContext context = new MutableContext(userId).add("user_id", userId);
        
        // Test direct OpenFeature client usage
        boolean result = openFeatureClient.getBooleanValue("test-flag", false, context);
        
        // Should not throw exception even with test configuration
        assertNotNull(result);
    }

    @Test
    void testFeatureFlagServiceFallback() {
        String userId = "test-user";
        
        // Test that service handles unavailable DevCycle gracefully
        assertDoesNotThrow(() -> {
            featureFlagService.getBooleanValue(userId, "test-flag", true);
            featureFlagService.getStringValue(userId, "test-flag", "test");
            featureFlagService.getNumberValue(userId, "test-flag", 42);
            featureFlagService.getObjectValue(userId, "test-flag", null);
        });
    }
}