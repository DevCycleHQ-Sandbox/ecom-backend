package com.shopper.config;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

@Configuration
@Slf4j
public class OpenTelemetryConfig {

    @Value("${app.telemetry.use-local-otlp:false}")
    private boolean useLocalOtlp;

    @Value("${app.telemetry.local-otlp-port:14499}")
    private int localOtlpPort;

    @Value("${app.telemetry.dynatrace.env-url:}")
    private String dynatraceEnvUrl;

    @Value("${app.telemetry.dynatrace.api-token:}")
    private String dynatraceApiToken;

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
        if (useLocalOtlp) {
            String endpoint = String.format("http://localhost:%d/otlp", localOtlpPort);
            log.info("🔗 OpenTelemetry configured for local OTLP endpoint: {}", endpoint);

            // Set system properties for auto-instrumentation
            System.setProperty("otel.exporter.otlp.endpoint", endpoint);
            System.setProperty("otel.service.name", serviceName);
            System.setProperty("otel.service.version", serviceVersion);
            System.setProperty("otel.resource.attributes",
                    String.format("service.name=%s,service.version=%s,deployment.environment=%s",
                            serviceName, serviceVersion, environment));
            System.setProperty("otel.exporter.otlp.metrics.temporality.preference", "delta");

        } else if (dynatraceEnvUrl != null && !dynatraceEnvUrl.isEmpty() && dynatraceApiToken != null && !dynatraceApiToken.isEmpty()) {
            String endpoint = dynatraceEnvUrl + "/api/v2/otlp";
            log.info("🔗 OpenTelemetry configured for Dynatrace endpoint: {}", endpoint);

            // Set system properties for auto-instrumentation
            System.setProperty("otel.exporter.otlp.endpoint", endpoint);
            System.setProperty("otel.exporter.otlp.headers", "Authorization=Api-Token " + dynatraceApiToken);
            System.setProperty("otel.service.name", serviceName);
            System.setProperty("otel.service.version", serviceVersion);
            System.setProperty("otel.resource.attributes",
                    String.format("service.name=%s,service.version=%s,deployment.environment=%s",
                            serviceName, serviceVersion, environment));
            System.setProperty("otel.exporter.otlp.metrics.temporality.preference", "delta");

        } else {
            log.info("⚠️  Neither local OTLP nor Dynatrace endpoints are configured. OpenTelemetry auto-instrumentation will use default settings.");
        }

        log.info("📊 OpenTelemetry Auto-Instrumentation enabled for service: {} v{} ({})",
                serviceName, serviceVersion, environment);
    }

    @Bean
    public OpenTelemetry openTelemetry() {
        return GlobalOpenTelemetry.get();
    }

    @Bean
    public Tracer tracer(OpenTelemetry openTelemetry) {
        return openTelemetry.getTracer(serviceName);
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