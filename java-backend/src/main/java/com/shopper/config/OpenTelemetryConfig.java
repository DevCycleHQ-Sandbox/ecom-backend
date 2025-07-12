package com.shopper.config;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.semconv.ResourceAttributes;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

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

    @Bean
    public OpenTelemetry openTelemetry(AppMetadata appMetadata) {
        String tracesExporterUrl = "";
        Map<String, String> exporterHeaders = new HashMap<>();

        if (useLocalOtlp) {
            // Use local OTLP endpoint
            tracesExporterUrl = String.format("http://localhost:%d/otlp/v1/traces", localOtlpPort);
            log.info("Using local OTLP endpoint: Traces={}", tracesExporterUrl);
        } else if (!dynatraceEnvUrl.isEmpty() && !dynatraceApiToken.isEmpty()) {
            // Use Dynatrace
            tracesExporterUrl = dynatraceEnvUrl + "/api/v2/otlp/v1/traces";
            exporterHeaders.put("Authorization", "Api-Token " + dynatraceApiToken);
            log.info("Using Dynatrace OTLP endpoint: Traces={}", tracesExporterUrl);
        } else {
            log.info("Neither local OTLP nor Dynatrace endpoints are configured. OpenTelemetry will be no-op.");
            return OpenTelemetry.noop();
        }

        if (tracesExporterUrl.isEmpty()) {
            return OpenTelemetry.noop();
        }

        // Build resource with service information and Dynatrace metadata
        Resource.Builder resourceBuilder = Resource.getDefault().toBuilder()
                .put(ResourceAttributes.SERVICE_NAME, appMetadata.getName())
                .put(ResourceAttributes.SERVICE_VERSION, appMetadata.getVersion())
                .put(ResourceAttributes.DEPLOYMENT_ENVIRONMENT, appMetadata.getEnvironment());

        // Try to read Dynatrace metadata files
        Resource dtMetadata = readDynatraceMetadata();
        if (dtMetadata != null) {
            resourceBuilder = resourceBuilder.merge(dtMetadata);
        }

        Resource resource = resourceBuilder.build();

        // Configure OTLP exporter
        OtlpGrpcSpanExporter.Builder exporterBuilder = OtlpGrpcSpanExporter.builder()
                .setEndpoint(tracesExporterUrl);

        // Add headers if present
        if (!exporterHeaders.isEmpty()) {
            exporterHeaders.forEach(exporterBuilder::addHeader);
        }

        OtlpGrpcSpanExporter spanExporter = exporterBuilder.build();

        // Build OpenTelemetry SDK
        SdkTracerProvider tracerProvider = SdkTracerProvider.builder()
                .addSpanProcessor(BatchSpanProcessor.builder(spanExporter).build())
                .setResource(resource)
                .build();

        OpenTelemetrySdk openTelemetry = OpenTelemetrySdk.builder()
                .setTracerProvider(tracerProvider)
                .build();

        log.info("OpenTelemetry SDK started successfully with Traces export.");
        return openTelemetry;
    }

    @Bean
    public Tracer tracer(OpenTelemetry openTelemetry, AppMetadata appMetadata) {
        return openTelemetry.getTracer(appMetadata.getName(), appMetadata.getVersion());
    }

    private Resource readDynatraceMetadata() {
        String[] metadataFiles = {
                "dt_metadata_e617c525669e072eebe3d0f08212e8f2.json",
                "/var/lib/dynatrace/enrichment/dt_metadata.json",
                "/var/lib/dynatrace/enrichment/dt_host_metadata.json"
        };

        for (String fileName : metadataFiles) {
            try {
                Path filePath;
                if (fileName.startsWith("/var")) {
                    filePath = Paths.get(fileName);
                } else {
                    // First read the file to get the actual path
                    String actualPath = Files.readString(Paths.get(fileName)).trim();
                    filePath = Paths.get(actualPath);
                }

                if (Files.exists(filePath)) {
                    String content = Files.readString(filePath).trim();
                    log.info("Merged Dynatrace metadata from {}", fileName);
                    
                    // Parse JSON and convert to Resource attributes
                    // For simplicity, we'll add basic attributes here
                    // In a real implementation, you'd parse the JSON properly
                    return Resource.create(Attributes.builder()
                            .put("dt.metadata.source", fileName)
                            .build());
                }
            } catch (IOException e) {
                log.debug("Failed to read Dynatrace metadata from {}: {}", fileName, e.getMessage());
            }
        }

        return null;
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