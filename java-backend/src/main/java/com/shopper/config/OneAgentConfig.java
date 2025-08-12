package com.shopper.config;

import com.dynatrace.oneagent.sdk.OneAgentSDK;
import com.dynatrace.oneagent.sdk.OneAgentSDKFactory;
import com.dynatrace.oneagent.sdk.api.LoggingCallback;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

@Configuration
@Slf4j
public class OneAgentConfig {

    @Value("${spring.application.name}")
    private String serviceName;

    @Value("${app.version:1.0.0}")
    private String serviceVersion;

    @Value("${spring.profiles.active:development}")
    private String environment;

    @Value("${app.telemetry.project:new-parth-project}")
    private String project;

    @Value("${app.telemetry.environment-id:66ccc3628c118d9a6da306e0}")
    private String environmentId;

    @PostConstruct
    public void logTelemetryConfiguration() {
        OneAgentSDK oneAgentSDK = OneAgentSDKFactory.createInstance();
        
        switch (oneAgentSDK.getCurrentState()) {
            case ACTIVE:
                log.info("✅ OneAgent SDK is active and ready for instrumentation");
                log.info("📊 OneAgent SDK enabled for service: {} v{} ({})", 
                        serviceName, serviceVersion, environment);
                break;
            case PERMANENTLY_INACTIVE:
                log.warn("⚠️  OneAgent SDK is permanently inactive. Ensure Dynatrace OneAgent is installed and running.");
                break;
            case TEMPORARILY_INACTIVE:
                log.warn("⚠️  OneAgent SDK is temporarily inactive. It may become active later.");
                break;
            default:
                log.warn("⚠️  OneAgent SDK state is unknown.");
                break;
        }
    }

    @Bean
    public OneAgentSDK oneAgentSDK() {
        OneAgentSDK oneAgentSDK = OneAgentSDKFactory.createInstance();
        
        // Set up logging callback for OneAgent SDK
        oneAgentSDK.setLoggingCallback(new LoggingCallback() {
            @Override
            public void warn(String message) {
                log.warn("OneAgent SDK: {}", message);
            }

            @Override
            public void error(String message) {
                log.error("OneAgent SDK: {}", message);
            }
        });

        return oneAgentSDK;
    }

    @Bean
    public AppMetadata appMetadata() {
        return AppMetadata.builder()
                .name(serviceName)
                .version(serviceVersion)
                .environment(environment)
                .project(project)
                .environmentId(environmentId)
                .build();
    }

    public static class AppMetadata {
        private final String name;
        private final String version;
        private final String environment;
        private final String project;
        private final String environmentId;

        private AppMetadata(Builder builder) {
            this.name = builder.name;
            this.version = builder.version;
            this.environment = builder.environment;
            this.project = builder.project;
            this.environmentId = builder.environmentId;
        }

        public static Builder builder() {
            return new Builder();
        }

        // Getters
        public String getName() { return name; }
        public String getVersion() { return version; }
        public String getEnvironment() { return environment; }
        public String getProject() { return project; }
        public String getEnvironmentId() { return environmentId; }

        public static class Builder {
            private String name;
            private String version;
            private String environment;
            private String project;
            private String environmentId;

            public Builder name(String name) {
                this.name = name;
                return this;
            }

            public Builder version(String version) {
                this.version = version;
                return this;
            }

            public Builder environment(String environment) {
                this.environment = environment;
                return this;
            }

            public Builder project(String project) {
                this.project = project;
                return this;
            }

            public Builder environmentId(String environmentId) {
                this.environmentId = environmentId;
                return this;
            }

            public AppMetadata build() {
                return new AppMetadata(this);
            }
        }
    }
}