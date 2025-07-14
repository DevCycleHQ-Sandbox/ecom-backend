package com.shopper.config;

import com.devcycle.sdk.server.local.api.DevCycleLocalClient;
import com.devcycle.sdk.server.local.model.DevCycleLocalOptions;
import dev.openfeature.sdk.OpenFeatureAPI;
import dev.openfeature.sdk.Client;
import dev.openfeature.sdk.FeatureProvider;
import io.opentelemetry.api.trace.Tracer;
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

    private final Tracer tracer;
    private final OpenTelemetryConfig.AppMetadata appMetadata;

    @PostConstruct
    public void initializeOpenFeature() {
        try {
            if (devCycleServerSdkKey == null || devCycleServerSdkKey.isEmpty() || devCycleServerSdkKey.startsWith("your-")) {
                log.warn("⚠️  DEVCYCLE_SERVER_SDK_KEY not provided, feature flags will use default values");
                return;
            }

            // Create DevCycle client with options
            DevCycleLocalOptions options = DevCycleLocalOptions.builder()
                    .enableEdgeDB(false)
                    .enableCloudBucketing(false)
                    .build();

            DevCycleLocalClient devCycleClient = new DevCycleLocalClient(devCycleServerSdkKey, options);

            // Get OpenFeature provider from DevCycle client
            FeatureProvider provider = devCycleClient.getOpenFeatureProvider();

            // Add Dynatrace OpenTelemetry hook
            DynatraceOtelLogHook hook = new DynatraceOtelLogHook(tracer, appMetadata);
            OpenFeatureAPI.getInstance().addHooks(hook);

            // Set the provider
            OpenFeatureAPI.getInstance().setProviderAndWait(provider);

            log.info("✅ OpenFeature with DevCycle provider initialized successfully");

            // Test the provider
            Client client = OpenFeatureAPI.getInstance().getClient();
            log.info("OpenFeature client ready: {}", client.getMetadata().getName());

            // Test a sample feature flag
            try {
                var context = new dev.openfeature.sdk.MutableContext("admin").add("user_id", "admin");
                var result = client.getBooleanValue("new-flow", false, context);
                log.info("Sample feature flag 'new-flow' evaluated to: {}", result);
            } catch (Exception e) {
                log.debug("Could not evaluate sample feature flag: {}", e.getMessage());
            }

        } catch (Exception e) {
            log.warn("OpenFeature with DevCycle provider not available: {}", e.getMessage());
        }
    }

    @Bean
    public Client openFeatureClient() {
        return OpenFeatureAPI.getInstance().getClient();
    }
}