package com.devcycle.hooks;

import dev.openfeature.sdk.EvaluationContext;
import dev.openfeature.sdk.FlagEvaluationDetails;
import dev.openfeature.sdk.Hook;
import dev.openfeature.sdk.HookContext;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * OpenFeature Hook that creates OpenTelemetry spans for feature flag evaluations.
 * This hook creates spans with detailed attributes for feature flag operations,
 * providing observability into feature flag usage and performance.
 */
@RequiredArgsConstructor
public class OTelSpanHook implements Hook<Object> {

    private static final Logger log = LoggerFactory.getLogger(OTelSpanHook.class);

    private final Tracer tracer;
    private final AppMetadata appMetadata;
    private final Map<HookContext<Object>, Span> spans = new ConcurrentHashMap<>();

    /**
     * Interface for providing application metadata to be included in spans.
     * Implement this interface to provide application-specific metadata.
     */
    public interface AppMetadata {
        /**
         * @return The application/service name
         */
        String getName();

        /**
         * @return The application version
         */
        String getVersion();

        /**
         * @return The environment (e.g., development, staging, production)
         */
        String getEnvironment();

        /**
         * @return The project identifier
         */
        String getProject();

        /**
         * @return The environment ID
         */
        String getEnvironmentId();
    }

    /**
     * Simple implementation of AppMetadata for basic use cases.
     */
    @RequiredArgsConstructor
    public static class SimpleAppMetadata implements AppMetadata {
        private final String name;
        private final String version;
        private final String environment;
        private final String project;
        private final String environmentId;

        @Override
        public String getName() { return name; }

        @Override
        public String getVersion() { return version; }

        @Override
        public String getEnvironment() { return environment; }

        @Override
        public String getProject() { return project; }

        @Override
        public String getEnvironmentId() { return environmentId; }

        public static Builder builder() {
            return new Builder();
        }

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

            public SimpleAppMetadata build() {
                return new SimpleAppMetadata(name, version, environment, project, environmentId);
            }
        }
    }

    @Override
    public Optional<EvaluationContext> before(HookContext<Object> ctx, Map<String, Object> hints) {
        Span span = tracer.spanBuilder("feature_flag_evaluation." + ctx.getFlagKey())
                .setSpanKind(SpanKind.SERVER)
                .startSpan();

        if (span != null) {
            span.setAttribute("feature_flag.key", ctx.getFlagKey());
            span.setAttribute("feature_flag.value_type", ctx.getType().name());
            span.setAttribute("feature_flag.flagset", ctx.getFlagKey());
            
            if (appMetadata != null) {
                if (appMetadata.getProject() != null) {
                    span.setAttribute("feature_flag.project", appMetadata.getProject());
                }
                if (appMetadata.getEnvironmentId() != null) {
                    span.setAttribute("feature_flag.environment", appMetadata.getEnvironmentId());
                }
                if (appMetadata.getName() != null) {
                    span.setAttribute("service.name", appMetadata.getName());
                }
                if (appMetadata.getVersion() != null) {
                    span.setAttribute("service.version", appMetadata.getVersion());
                }
                if (appMetadata.getEnvironment() != null) {
                    span.setAttribute("deployment.environment", appMetadata.getEnvironment());
                }
            }

            if (ctx.getClientMetadata() != null && ctx.getClientMetadata().getName() != null) {
                span.setAttribute("openfeature.client.name", ctx.getClientMetadata().getName());
            }

            if (ctx.getProviderMetadata() != null && ctx.getProviderMetadata().getName() != null) {
                span.setAttribute("openfeature.provider.name", ctx.getProviderMetadata().getName());
            }
        }

        spans.put(ctx, span);
        return Optional.empty();
    }

    @Override
    public void after(HookContext<Object> ctx, FlagEvaluationDetails<Object> details, Map<String, Object> hints) {
        Span span = spans.remove(ctx);

        if (span != null) {
            log.debug("evaluationDetails: {}", details);

            if (details.getErrorCode() != null) {
                span.setAttribute("feature_flag.error_code", details.getErrorCode().toString());
            }

            if (details.getErrorMessage() != null) {
                span.setAttribute("feature_flag.error_message", details.getErrorMessage());
            }

            span.setAttribute("feature_flag.value", String.valueOf(details.getValue()));
            span.setAttribute("feature_flag.reason", details.getReason() != null ? details.getReason() : "");

            if (details.getVariant() != null) {
                span.setAttribute("feature_flag.variant", details.getVariant());
            }

            log.debug("Feature flag span completed: {}", ctx.getFlagKey());
            span.end();
        }
    }

    @Override
    public void error(HookContext<Object> ctx, Exception error, Map<String, Object> hints) {
        Span span = spans.remove(ctx);
        if (span != null) {
            span.recordException(error);
            span.setAttribute("feature_flag.error_message", error.getMessage());
            span.end();
        }
    }

    @Override
    public void finallyAfter(HookContext<Object> ctx, Map<String, Object> hints) {
        // Ensure span is always cleaned up
        Span span = spans.remove(ctx);
        if (span != null) {
            span.end();
        }
    }
}