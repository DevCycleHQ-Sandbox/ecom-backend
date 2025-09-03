package com.shopper.config;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.extension.trace.propagation.B3Propagator;
import io.opentelemetry.extension.trace.propagation.JaegerPropagator;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.exporter.otlp.metrics.OtlpGrpcMetricExporter;
import io.opentelemetry.exporter.otlp.logs.OtlpGrpcLogRecordExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReader;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.logs.SdkLoggerProvider;
import io.opentelemetry.sdk.logs.export.BatchLogRecordProcessor;
import io.opentelemetry.semconv.ResourceAttributes;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Configuration
@Slf4j
public class OpenTelemetryConfig {

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

    @Value("${otel.exporter.otlp.endpoint:http://localhost:14499/otlp}")
    private String otlpEndpoint;

    @Value("${otel.exporter.otlp.headers:}")
    private String otlpHeaders;

    private OpenTelemetrySdk openTelemetrySdk;

    @PostConstruct
    public void logTelemetryConfiguration() {
        log.info("📊 OpenTelemetry Auto-Instrumentation enabled for service: {} v{} ({})", 
                serviceName, serviceVersion, environment);
        log.info("🔗 OpenTelemetry configured for OTLP endpoint: {}", otlpEndpoint);
        if (!otlpHeaders.isEmpty()) {
            log.info("🔐 OTLP headers configured for authentication");
        }
    }

    @Bean
    public OpenTelemetry openTelemetry() {
        if (openTelemetrySdk != null) {
            return openTelemetrySdk;
        }

        Resource resource = Resource.getDefault()
                .merge(Resource.create(Attributes.of(
                        ResourceAttributes.SERVICE_NAME, serviceName,
                        ResourceAttributes.SERVICE_VERSION, serviceVersion,
                        ResourceAttributes.DEPLOYMENT_ENVIRONMENT, environment,
                        ResourceAttributes.SERVICE_NAMESPACE, project
                )));

        // Configure OTLP exporters only if endpoint is provided
        SdkTracerProvider.Builder tracerProviderBuilder = SdkTracerProvider.builder()
                .setResource(resource);

        SdkMeterProvider.Builder meterProviderBuilder = SdkMeterProvider.builder()
                .setResource(resource);

        SdkLoggerProvider.Builder loggerProviderBuilder = SdkLoggerProvider.builder()
                .setResource(resource);

        if (!otlpEndpoint.isEmpty() && !otlpEndpoint.equals("disabled")) {
            // Configure trace exporter
            OtlpGrpcSpanExporter.Builder spanExporterBuilder = OtlpGrpcSpanExporter.builder()
                    .setEndpoint(otlpEndpoint)
                    .setTimeout(Duration.ofSeconds(30));

            if (!otlpHeaders.isEmpty()) {
                spanExporterBuilder.addHeader("Authorization", otlpHeaders.replace("Authorization=", ""));
            }

            tracerProviderBuilder.addSpanProcessor(
                    BatchSpanProcessor.builder(spanExporterBuilder.build())
                            .setMaxExportBatchSize(512)
                            .setExportTimeout(Duration.ofSeconds(30))
                            .setScheduleDelay(Duration.ofSeconds(5))
                            .build());

            // Configure metrics exporter
            OtlpGrpcMetricExporter.Builder metricExporterBuilder = OtlpGrpcMetricExporter.builder()
                    .setEndpoint(otlpEndpoint)
                    .setTimeout(Duration.ofSeconds(30));

            if (!otlpHeaders.isEmpty()) {
                metricExporterBuilder.addHeader("Authorization", otlpHeaders.replace("Authorization=", ""));
            }

            meterProviderBuilder.registerMetricReader(
                    PeriodicMetricReader.builder(metricExporterBuilder.build())
                            .setInterval(Duration.ofSeconds(30))
                            .build());

            // Configure logs exporter
            OtlpGrpcLogRecordExporter.Builder logExporterBuilder = OtlpGrpcLogRecordExporter.builder()
                    .setEndpoint(otlpEndpoint)
                    .setTimeout(Duration.ofSeconds(30));

            if (!otlpHeaders.isEmpty()) {
                logExporterBuilder.addHeader("Authorization", otlpHeaders.replace("Authorization=", ""));
            }

            loggerProviderBuilder.addLogRecordProcessor(
                    BatchLogRecordProcessor.builder(logExporterBuilder.build())
                            .build());

            log.info("✅ OTLP exporters configured for endpoint: {}", otlpEndpoint);
        } else {
            log.warn("⚠️  No OTLP endpoint configured, telemetry will not be exported");
        }

        openTelemetrySdk = OpenTelemetrySdk.builder()
                .setTracerProvider(tracerProviderBuilder.build())
                .setMeterProvider(meterProviderBuilder.build())
                .setLoggerProvider(loggerProviderBuilder.build())
                .buildAndRegisterGlobal();

        return openTelemetrySdk;
    }

    @Bean
    public Tracer tracer() {
        return GlobalOpenTelemetry.get().getTracer("java-backend", serviceVersion);
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

    @PreDestroy
    public void cleanup() {
        if (openTelemetrySdk != null) {
            openTelemetrySdk.getSdkTracerProvider().close();
            openTelemetrySdk.getSdkMeterProvider().close();
            openTelemetrySdk.getSdkLoggerProvider().close();
        }
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