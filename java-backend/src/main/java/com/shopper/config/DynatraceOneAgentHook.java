package com.shopper.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;


import com.devcycle.sdk.server.common.model.EvalHook;
import com.devcycle.sdk.server.common.model.HookContext;
import com.devcycle.sdk.server.common.model.Variable;
import com.devcycle.sdk.server.local.model.VariableMetadata;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.GlobalOpenTelemetry;


@RequiredArgsConstructor
@Slf4j
public class DynatraceOneAgentHook implements EvalHook<Object> {

    private Tracer tracer;
    private final Map<HookContext<Object>, Span> spans = new ConcurrentHashMap<>();

    public Optional<HookContext<Object>> before(HookContext<Object> ctx) {
        try {
            tracer = GlobalOpenTelemetry.get().getTracer("devcycle-feature-flags");
            
            // Create span for feature flag evaluation
            Span span = tracer.spanBuilder("feature_flag_evaluation")
                    .setSpanKind(SpanKind.INTERNAL)
                    .startSpan();

            if (span != null) {
                // Set span attributes following OpenTelemetry semantic conventions
                span.setAttribute("feature_flag.key", ctx.getKey());
                span.setAttribute("feature_flag.provider.name", "devcycle");
                span.setAttribute("feature_flag.value_type", ctx.getDefaultValue().getClass().getSimpleName());
                span.setAttribute("feature_flag.context.id", ctx.getUser().getUserId());
                
                if (ctx.getMetadata() != null) {
                    if (ctx.getMetadata().project != null && ctx.getMetadata().project.id != null) {
                        span.setAttribute("feature_flag.project", ctx.getMetadata().project.id);
                    }
                    if (ctx.getMetadata().environment != null && ctx.getMetadata().environment.id != null) {
                        span.setAttribute("feature_flag.environment", ctx.getMetadata().environment.id);
                    }
                }


                log.debug("Feature flag evaluation span started for key: {}", ctx.getKey());
            }

            spans.put(ctx, span);
            return Optional.empty();
        } catch (Exception e) {
            log.error("Error starting feature flag span for key {}: {}", ctx.getKey(), e.getMessage());
            return Optional.empty();
        }
    }

    public void after(HookContext<Object> ctx, Optional<Variable<Object>> variable, VariableMetadata variableMetadata) {
        // DevCycle calls onFinally instead of after, so we don't handle span completion here
        log.debug("after called for key: {} (span completion handled in onFinally)", ctx.getKey());
    }

    public void onFinally(HookContext<Object> ctx, Optional<Variable<Object>> variable, VariableMetadata variableMetadata) {
        try {
            Span span = spans.remove(ctx);

            if (span != null) {
                log.debug("Completing feature flag evaluation span for key: {}", ctx.getKey());

                String reason = "unknown";
                Object resultValue = null;
                String variant = null;
                boolean hasError = false;

                if (variable.isPresent()) {
                    Variable<Object> var = variable.get();
                    resultValue = var.getValue();
                    
                    // Set span attributes for successful evaluation
                    span.setAttribute("feature_flag.result.value", String.valueOf(resultValue));
                    
                    if (var.getEval() != null && var.getEval().getReason() != null) {
                        reason = var.getEval().getReason();
                        span.setAttribute("feature_flag.result.reason", reason);
                    }

                    // Add variant if available (DevCycle doesn't expose this directly, but we can infer)
                    if (resultValue instanceof Boolean) {
                        variant = resultValue.toString();
                    } else if (resultValue instanceof String) {
                        variant = (String) resultValue;
                    }
                    
                    if (variant != null) {
                        span.setAttribute("feature_flag.result.variant", variant);
                    }

                    // Add feature metadata
                    if (variableMetadata != null && variableMetadata.featureId != null) {
                        span.setAttribute("feature_flag.set.id", variableMetadata.featureId);
                        if (ctx.getMetadata() != null && ctx.getMetadata().project != null && ctx.getMetadata().project.id != null) {
                            span.setAttribute("feature_flag.url", "https://app.devcycle.com/r/p/" + ctx.getMetadata().project.id +  "/f/" + variableMetadata.featureId);
                        }
                    }

                    log.debug("Feature flag span completed: {} = {}", var.getKey(), var.getValue());
                } else {
                    // Handle evaluation failure
                    hasError = true;
                    reason = "evaluation_failed";
                    span.setAttribute("feature_flag.result.reason", reason);
                    span.setAttribute("error.type", "general");
                    span.setAttribute("error.message", "Feature flag evaluation returned no result");
                    log.debug("Feature flag evaluation failed for key: {}", ctx.getKey());
                }


                span.end();
                log.debug("Feature flag span ended for key: {}", ctx.getKey());

            } else {
                log.warn("No span found for feature flag key: {}", ctx.getKey());
            }
        } catch (Exception e) {
            log.error("Error completing feature flag span for key {}: {}", ctx.getKey(), e.getMessage());
        }
    }

    public void error(HookContext<Object> ctx, Exception e) {
        log.warn("Feature flag evaluation error for key {}: {}", ctx.getKey(), e.getMessage());
        
        Span span = spans.get(ctx);
        if (span != null) {
            span.setAttribute("error.type", "general");
            span.setAttribute("error.message", e.getMessage());
        }
    }

}