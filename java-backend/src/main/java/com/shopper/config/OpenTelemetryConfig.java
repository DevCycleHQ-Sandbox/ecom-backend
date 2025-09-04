package com.shopper.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

@Configuration
@Slf4j
public class OpenTelemetryConfig {

    @Value("${spring.application.name:java-backend}")
    private String serviceName;

    @Value("${app.version:1.0.0}")
    private String serviceVersion;

    @Value("${spring.profiles.active:development}")
    private String environment;

    @Value("${app.telemetry.project:new-parth-project}")
    private String project;

    @Value("${app.telemetry.environment-id:66ccc3628c118d9a6da306e0}")
    private String environmentId;

    @Value("${otel.exporter.otlp.endpoint:}")
    private String otlpEndpoint;

    @Value("${otel.exporter.otlp.headers:}")
    private String otlpHeaders;

    @PostConstruct
    public void logTelemetryConfiguration() {
        log.info("📊 OpenTelemetry Java Agent enabled for service: {} v{} ({})", 
                serviceName, serviceVersion, environment);
        if (!otlpEndpoint.isEmpty()) {
            log.info("🔗 OpenTelemetry configured for OTLP endpoint: {}", otlpEndpoint);
        }
        if (!otlpHeaders.isEmpty()) {
            log.info("🔐 OTLP headers configured for authentication");
        }
        log.info("✅ Using OpenTelemetry Java Agent for auto-instrumentation");
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

        private AppMetadata(String name, String version, String environment, String project, String environmentId) {
            this.name = name;
            this.version = version;
            this.environment = environment;
            this.project = project;
            this.environmentId = environmentId;
        }

        public static Builder builder() {
            return new Builder();
        }

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
                return new AppMetadata(name, version, environment, project, environmentId);
            }
        }
    }
}