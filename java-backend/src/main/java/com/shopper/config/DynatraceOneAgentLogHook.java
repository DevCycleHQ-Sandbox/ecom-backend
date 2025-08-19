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
public class DynatraceOneAgentLogHook implements EvalHook<Object> {

    private Tracer tracer;
    private final Map<HookContext<Object>, Span> spans = new ConcurrentHashMap<>();

    public Optional<HookContext<Object>> before(HookContext<Object> ctx) {
        try {
            tracer = GlobalOpenTelemetry.get().getTracer("devcycle-tracer");
            Span span = tracer.spanBuilder("feature_flag_evaluation." + ctx.getKey())
                    .setSpanKind(SpanKind.INTERNAL)
                    .startSpan();

            if (span != null) {
                span.setAttribute("feature_flag.key", ctx.getKey());
                span.setAttribute("feature_flag.value_type", ctx.getDefaultValue().getClass().getSimpleName());

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
        log.warn("variableMetadata: {}", variableMetadata != null ? variableMetadata.toString() : "null");
        log.warn("variable: {}", variable != null ? variable.get().toString() : "null");
        log.warn("ctx: {}", ctx != null ? ctx.toString() : "null");
    }

    public void onFinally(HookContext<Object> ctx, Optional<Variable<Object>> variable, VariableMetadata variableMetadata) {
        try {
            Span span = spans.remove(ctx);

            if (span != null) {
                log.debug("Completing feature flag evaluation span for key: {}", ctx.getKey());
                log.warn("variableMetadata: {}", variableMetadata != null ? variableMetadata.toString() : "null");

                if (variable.isPresent()) {
                    Variable<Object> var = variable.get();
                    span.setAttribute("feature_flag.value", String.valueOf(var.getValue()));

                    if (var.getEval() != null && var.getEval().getReason() != null) {
                        span.setAttribute("feature_flag.reason", var.getEval().getReason());
                    }

                    if (variableMetadata != null && variableMetadata.featureId != null) {
                        span.setAttribute("feature_flag.flagset", variableMetadata.featureId);
                    }

                    log.debug("Feature flag span completed: {} = {}", var.getKey(), var.getValue());
                } else {
                    span.setAttribute("feature_flag.value", "null");
                    span.setAttribute("feature_flag.reason", "evaluation_failed");
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
        log.warn("error: {}", ctx.getKey());
        log.warn("e: {}", e.getMessage());
    }
}