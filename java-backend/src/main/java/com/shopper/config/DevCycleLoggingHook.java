package com.shopper.config;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.api.common.AttributeKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

import com.devcycle.sdk.server.common.model.EvalHook;
import com.devcycle.sdk.server.common.model.HookContext;
import com.devcycle.sdk.server.common.model.Variable;
import com.devcycle.sdk.server.local.model.VariableMetadata;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.logs.Logger;
import io.opentelemetry.api.logs.LogRecordBuilder;
import io.opentelemetry.api.logs.Severity;

import java.time.Instant;

@RequiredArgsConstructor
@Slf4j
public class DevCycleLoggingHook implements EvalHook<Object> {

    public Optional<HookContext<Object>> before(HookContext<Object> ctx) {
        // No action needed on before for logging-only hook
        return Optional.empty();
    }

    public void after(HookContext<Object> ctx, Optional<Variable<Object>> variable, VariableMetadata variableMetadata) {
        // DevCycle calls onFinally instead of after, so we don't handle logging here
    }

    public void onFinally(HookContext<Object> ctx, Optional<Variable<Object>> variable, VariableMetadata variableMetadata) {
        try {
            // Get OpenTelemetry Logger - this will use the bridge we're setting up
            Logger logger = GlobalOpenTelemetry.get().getLogsBridge().get("feature-flags");
            
            // Get current span for trace-log correlation
            Span currentSpan = Span.current();
            
            // Determine evaluation status
            boolean hasError = !variable.isPresent();
            Severity logSeverity = hasError ? Severity.ERROR : Severity.INFO;
            String logBody = hasError ? 
                "Feature flag evaluation failed: " + ctx.getKey() : 
                "Feature flag evaluation: " + ctx.getKey();

            // Create log record builder following OpenTelemetry feature flag semantic conventions
            LogRecordBuilder logBuilder = logger.logRecordBuilder()
                .setTimestamp(Instant.now())
                .setSeverity(logSeverity)
                .setBody(logBody)
                // Required attributes per spec
                .setAttribute(AttributeKey.stringKey("feature_flag.key"), ctx.getKey())
                .setAttribute(AttributeKey.stringKey("feature_flag.provider.name"), "devcycle")
                // Recommended attributes per spec  
                .setAttribute(AttributeKey.stringKey("feature_flag.context.id"), ctx.getUser().getUserId());

            // Add span correlation for trace-log correlation if span is available
            if (currentSpan != null && currentSpan.getSpanContext().isValid()) {
                logBuilder.setAttribute(AttributeKey.stringKey("trace_id"), currentSpan.getSpanContext().getTraceId())
                          .setAttribute(AttributeKey.stringKey("span_id"), currentSpan.getSpanContext().getSpanId());
            }

            // Add result attributes if evaluation was successful
            if (variable.isPresent()) {
                Variable<Object> var = variable.get();
                
                // Conditionally required per spec: result value
                logBuilder.setAttribute(AttributeKey.stringKey("feature_flag.result.value"), String.valueOf(var.getValue()));
                
                // Add reason if available (recommended per spec)
                if (var.getEval() != null && var.getEval().getReason() != null) {
                    logBuilder.setAttribute(AttributeKey.stringKey("feature_flag.result.reason"), var.getEval().getReason());
                }

                // Add variant if determinable (conditionally required per spec)
                Object value = var.getValue();
                if (value instanceof Boolean || value instanceof String) {
                    logBuilder.setAttribute(AttributeKey.stringKey("feature_flag.result.variant"), value.toString());
                }
            } else {
                // Add error attributes for failed evaluation (conditionally required per spec)
                logBuilder.setAttribute(AttributeKey.stringKey("error.type"), "general")
                          .setAttribute(AttributeKey.stringKey("error.message"), "Feature flag evaluation returned no result");
            }

            // Add metadata attributes (recommended per spec)
            if (ctx.getMetadata() != null) {
                if (ctx.getMetadata().project != null && ctx.getMetadata().project.id != null) {
                    logBuilder.setAttribute(AttributeKey.stringKey("feature_flag.set.id"), ctx.getMetadata().project.id);
                }
                if (ctx.getMetadata().environment != null && ctx.getMetadata().environment.id != null) {
                    logBuilder.setAttribute(AttributeKey.stringKey("feature_flag.environment"), ctx.getMetadata().environment.id);
                }
            }

            // Add DevCycle-specific metadata
            if (variableMetadata != null && variableMetadata.featureId != null) {
                logBuilder.setAttribute(AttributeKey.stringKey("feature_flag.feature_id"), variableMetadata.featureId);
                
                if (ctx.getMetadata() != null && ctx.getMetadata().project != null && ctx.getMetadata().project.id != null) {
                    logBuilder.setAttribute(AttributeKey.stringKey("feature_flag.url"), 
                                          "https://app.devcycle.com/r/p/" + ctx.getMetadata().project.id + "/f/" + variableMetadata.featureId);
                }
            }

            // Emit the structured log record via OpenTelemetry Logs API
            logBuilder.emit();
            
            // Also emit to SLF4J for local debugging (this will be caught by the appender bridge)
            if (hasError) {
                log.error("Feature flag evaluation failed for key: {}", ctx.getKey());
            } else {
                log.info("Feature flag evaluation completed for key: {}", ctx.getKey());
            }
            
        } catch (Exception e) {
            log.warn("Failed to emit feature flag log record for key {}: {}", ctx.getKey(), e.getMessage());
        }
    }

    public void error(HookContext<Object> ctx, Exception e) {
        try {
            // Get OpenTelemetry Logger - this will use the bridge we're setting up
            Logger logger = GlobalOpenTelemetry.get().getLogsBridge().get("feature-flags");

            // Get current span for trace-log correlation
            Span currentSpan = Span.current();

            // Create error log record following OpenTelemetry feature flag semantic conventions
            LogRecordBuilder logBuilder = logger.logRecordBuilder()
                .setTimestamp(Instant.now())
                .setSeverity(Severity.ERROR)
                .setBody("Feature flag evaluation error: " + ctx.getKey())
                // Required attributes per spec
                .setAttribute(AttributeKey.stringKey("feature_flag.key"), ctx.getKey())
                .setAttribute(AttributeKey.stringKey("feature_flag.provider.name"), "devcycle")
                // Recommended attributes per spec
                .setAttribute(AttributeKey.stringKey("feature_flag.context.id"), ctx.getUser().getUserId())
                // Error attributes (conditionally required per spec)
                .setAttribute(AttributeKey.stringKey("error.type"), "general")
                .setAttribute(AttributeKey.stringKey("error.message"), e.getMessage());

            // Add span correlation for trace-log correlation if span is available
            if (currentSpan != null && currentSpan.getSpanContext().isValid()) {
                logBuilder.setAttribute(AttributeKey.stringKey("trace_id"), currentSpan.getSpanContext().getTraceId())
                          .setAttribute(AttributeKey.stringKey("span_id"), currentSpan.getSpanContext().getSpanId());
            }

            // Add metadata attributes if available (recommended per spec)
            if (ctx.getMetadata() != null) {
                if (ctx.getMetadata().project != null && ctx.getMetadata().project.id != null) {
                    logBuilder.setAttribute(AttributeKey.stringKey("feature_flag.set.id"), ctx.getMetadata().project.id);
                }
                if (ctx.getMetadata().environment != null && ctx.getMetadata().environment.id != null) {
                    logBuilder.setAttribute(AttributeKey.stringKey("feature_flag.environment"), ctx.getMetadata().environment.id);
                }
            }

            // Emit the error log record via OpenTelemetry Logs API
            logBuilder.emit();
            
            // Also emit to SLF4J for local debugging (this will be caught by the appender bridge)
            log.error("Feature flag evaluation error for key {}: {}", ctx.getKey(), e.getMessage());
            
        } catch (Exception logError) {
            log.warn("Failed to emit feature flag error log record: {}", logError.getMessage());
        }
    }
}
