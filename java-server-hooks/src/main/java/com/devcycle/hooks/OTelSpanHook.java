package com.devcycle.hooks;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * DevCycle EvalHook that creates OpenTelemetry spans for feature flag evaluations.
 * This hook creates spans with detailed attributes for feature flag operations,
 * providing observability into feature flag usage and performance.
 * 
 * Note: This implementation requires the DevCycle SDK EvalHook interface.
 * When the full DevCycle SDK is available, this should implement:
 * - com.devcycle.sdk.server.common.model.EvalHook<Object>
 * 
 * And use the following imports:
 * - import com.devcycle.sdk.server.common.model.HookContext;
 * - import com.devcycle.sdk.server.common.model.Variable;
 * - import com.devcycle.sdk.server.local.model.VariableMetadata;
 */
@RequiredArgsConstructor
@Slf4j
public class OTelSpanHook {

    private final Tracer tracer;
    private final Map<Object, Span> spans = new ConcurrentHashMap<>();

    /**
     * Called before feature flag evaluation.
     * This method matches the DevCycle EvalHook.before signature:
     * public Optional&lt;HookContext&lt;Object&gt;&gt; before(HookContext&lt;Object&gt; ctx)
     */
    public Optional<Object> before(Object ctx) {
        try {
            String flagKey = getKey(ctx);
            Span span = tracer.spanBuilder("feature_flag_evaluation." + flagKey)
                    .setSpanKind(SpanKind.INTERNAL)
                    .startSpan();

            if (span != null) {
                span.setAttribute("feature_flag.key", flagKey);
                span.setAttribute("feature_flag.value_type", getDefaultValueType(ctx));
                
                // Set project and environment from metadata if available
                setMetadataAttributes(span, ctx);

                log.debug("Feature flag evaluation span started for key: {}", flagKey);
            }

            spans.put(ctx, span);
            return Optional.empty();
        } catch (Exception e) {
            log.error("Error starting feature flag span for key {}: {}", getKey(ctx), e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Called after feature flag evaluation.
     * This method matches the DevCycle EvalHook.after signature:
     * public void after(HookContext&lt;Object&gt; ctx, Optional&lt;Variable&lt;Object&gt;&gt; variable, VariableMetadata variableMetadata)
     */
    public void after(Object ctx, Optional<Object> variable, Object variableMetadata) {
        // DevCycle calls onFinally instead of after, so we don't handle span completion here
        log.debug("after called for key: {} (span completion handled in onFinally)", getKey(ctx));
        log.warn("variableMetadata: {}", variableMetadata != null ? variableMetadata.toString() : "null");
        log.warn("variable: {}", variable != null ? variable.toString() : "null");
        log.warn("ctx: {}", ctx != null ? ctx.toString() : "null");
    }

    /**
     * Called finally after feature flag evaluation.
     * This method matches the DevCycle EvalHook.onFinally signature:
     * public void onFinally(HookContext&lt;Object&gt; ctx, Optional&lt;Variable&lt;Object&gt;&gt; variable, VariableMetadata variableMetadata)
     */
    public void onFinally(Object ctx, Optional<Object> variable, Object variableMetadata) {
        try {
            Span span = spans.remove(ctx);

            if (span != null) {
                log.debug("Completing feature flag evaluation span for key: {}", getKey(ctx));
                log.warn("variableMetadata: {}", variableMetadata != null ? variableMetadata.toString() : "null");

                if (variable.isPresent()) {
                    Object var = variable.get();
                    span.setAttribute("feature_flag.value", String.valueOf(getValue(var)));
                    
                    String reason = getReason(var);
                    if (reason != null) {
                        span.setAttribute("feature_flag.reason", reason);
                    }
                    
                    String featureId = getFeatureId(variableMetadata);
                    if (featureId != null) {
                        span.setAttribute("feature_flag.flagset", featureId);
                    }
                    
                    log.debug("Feature flag span completed: {} = {}", getKey(var), getValue(var));
                } else {
                    span.setAttribute("feature_flag.value", "null");
                    span.setAttribute("feature_flag.reason", "evaluation_failed");
                    log.debug("Feature flag evaluation failed for key: {}", getKey(ctx));
                }
                
                span.end();
                log.debug("Feature flag span ended for key: {}", getKey(ctx));

            } else {
                log.warn("No span found for feature flag key: {}", getKey(ctx));
            }
        } catch (Exception e) {
            log.error("Error completing feature flag span for key {}: {}", getKey(ctx), e.getMessage());
        }
    }

    /**
     * Called on error during feature flag evaluation.
     * This method matches the DevCycle EvalHook.error signature:
     * public void error(HookContext&lt;Object&gt; ctx, Exception e)
     */
    public void error(Object ctx, Exception e) {
        log.warn("error: {}", getKey(ctx));
        log.warn("e: {}", e.getMessage());
    }

    // Helper methods - these would use actual DevCycle SDK classes in the real implementation
    
    private String getKey(Object ctx) {
        // In real implementation: return ctx.getKey();
        return ctx != null ? ctx.toString() : "unknown";
    }
    
    private String getDefaultValueType(Object ctx) {
        // In real implementation: return ctx.getDefaultValue().getClass().getSimpleName();
        return "Object";
    }
    
    private Object getValue(Object variable) {
        // In real implementation: return variable.getValue();
        return variable;
    }
    
    private String getReason(Object variable) {
        // In real implementation: 
        // if (variable.getEval() != null && variable.getEval().getReason() != null) {
        //     return variable.getEval().getReason();
        // }
        return null;
    }
    
    private String getFeatureId(Object variableMetadata) {
        // In real implementation: return variableMetadata.featureId;
        return null;
    }
    
    private void setMetadataAttributes(Span span, Object ctx) {
        // In real implementation:
        // if (ctx.getMetadata() != null) {
        //     if (ctx.getMetadata().project != null && ctx.getMetadata().project.id != null) {
        //         span.setAttribute("feature_flag.project", ctx.getMetadata().project.id);
        //     }
        //     if (ctx.getMetadata().environment != null && ctx.getMetadata().environment.id != null) {
        //         span.setAttribute("feature_flag.environment", ctx.getMetadata().environment.id);
        //     }
        // }
    }
}