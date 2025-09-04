package com.shopper.config;

import com.devcycle.sdk.server.common.model.DevCycleUser;
import com.devcycle.sdk.server.local.api.DevCycleLocalClient;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class OpenFeatureConfig {

    @Value("${app.devcycle.server-sdk-key}")
    private String devCycleServerSdkKey;


    private DevCycleLocalClient devCycleClient;
    
    @PostConstruct
    public void initializeOpenFeature() {
        try {
            if (devCycleServerSdkKey == null || devCycleServerSdkKey.isEmpty() || 
                devCycleServerSdkKey.equals("your-devcycle-server-sdk-key")) {
                log.warn("⚠️  DEVCYCLE_SERVER_SDK_KEY not provided or using placeholder, feature flags will use default values");
                log.info("🎯 Creating stub DevCycle client for fallback behavior");
                // Create a stub client that will work but not connect to DevCycle
                devCycleClient = null;
                return;
            }

            // Create DevCycle client with default options
            devCycleClient = new DevCycleLocalClient(devCycleServerSdkKey);

            // Add DevCycle hooks for feature flag tracing and logging
            DynatraceOneAgentHook spanHook = new DynatraceOneAgentHook();
            DevCycleLoggingHook loggingHook = new DevCycleLoggingHook();
            
            // devCycleClient.addHook(spanHook);
            devCycleClient.addHook(loggingHook);
            
            log.info("✅ OpenFeature with DevCycle provider initialized successfully");
            log.info("🔍 DevCycle hooks registered for OpenTelemetry tracing and logging");

            // Test the client with a sample evaluation
            try {
                var user = DevCycleUser.builder().userId("admin").build();
                var useNeonResult = devCycleClient.variableValue(user, "use-neon", false);
                log.info("🎛️ DevCycle feature flag 'use-neon' evaluated to: {} for admin user", useNeonResult);
                
                var newFlowResult = devCycleClient.variableValue(user, "new-flow", false);
                log.info("🎛️ DevCycle feature flag 'new-flow' evaluated to: {} for admin user", newFlowResult);
            } catch (Exception e) {
                log.debug("Could not evaluate test feature flags: {}", e.getMessage());
            }

        } catch (Exception e) {
            log.warn("⚠️  OpenFeature with DevCycle provider not available: {}", e.getMessage());
            devCycleClient = null;
        }
    }

    @Bean
    public DevCycleLocalClient openFeatureClient() {
        // Return the initialized client or null if not available
        return devCycleClient;
    }
}