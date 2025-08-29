package com.shopper.config;

import io.opentelemetry.api.logs.Logger;
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
import io.opentelemetry.api.common.AttributeKey;

@RequiredArgsConstructor
@Slf4j
public class DynatraceOneAgentHook implements EvalHook<Object> {

    private Tracer tracer;
    private final Map<HookContext<Object>, Span> spans = new ConcurrentHashMap<>();
    private final Map<HookContext<Object>, Logger> spanLoggers = new ConcurrentHashMap<>();

    public Optional<HookContext<Object>> before(HookContext<Object> ctx) {
        try {
            tracer = GlobalOpenTelemetry.get().getTracer("devcycle-tracer");
            Span span = tracer.spanBuilder("feature_flag_evaluation." + ctx.getKey())
                    .setSpanKind(SpanKind.INTERNAL)
                    .startSpan();

            Logger spanLogger = GlobalOpenTelemetry.get().getLogsBridge().get("hooks-logger");

            if (span != null && spanLogger != null) {
                span.setAttribute("feature_flag.provider.name", "devcycle");
                span.setAttribute("feature_flag.key", ctx.getKey());
                span.setAttribute("feature_flag.value_type", ctx.getDefaultValue().getClass().getSimpleName());
                span.setAttribute("feature_flag.context.id", ctx.getUser().getUserId());
                spanLogger.logRecordBuilder().setAttribute(AttributeKey.stringKey("feature_flag.provider.name"), "devcycle");
                spanLogger.logRecordBuilder().setAttribute(AttributeKey.stringKey("feature_flag.key"), ctx.getKey());
                spanLogger.logRecordBuilder().setAttribute(AttributeKey.stringKey("feature_flag.value_type"), ctx.getDefaultValue().getClass().getSimpleName());
                spanLogger.logRecordBuilder().setAttribute(AttributeKey.stringKey("feature_flag.context.id"), ctx.getUser().getUserId());
                spanLogger.logRecordBuilder().setAttribute(AttributeKey.stringKey("span.id"), span.getSpanContext().getSpanId());
                spanLogger.logRecordBuilder().setAttribute(AttributeKey.stringKey("span.trace_id"), span.getSpanContext().getTraceId());

                if (ctx.getMetadata() != null) {
                    if (ctx.getMetadata().project != null && ctx.getMetadata().project.id != null) {
                        span.setAttribute("feature_flag.project", ctx.getMetadata().project.id);
                        spanLogger.logRecordBuilder().setAttribute(AttributeKey.stringKey("feature_flag.project"), ctx.getMetadata().project.id);
                    }
                    if (ctx.getMetadata().environment != null && ctx.getMetadata().environment.id != null) {
                        span.setAttribute("feature_flag.environment", ctx.getMetadata().environment.id);
                        spanLogger.logRecordBuilder().setAttribute(AttributeKey.stringKey("feature_flag.environment"), ctx.getMetadata().environment.id);
                    }
                }

                log.debug("Feature flag evaluation span started for key: {}", ctx.getKey());
            }

            spans.put(ctx, span);
            spanLoggers.put(ctx, spanLogger);
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
            Logger spanLogger = spanLoggers.remove(ctx);

            if (span != null) {
                log.debug("Completing feature flag evaluation span for key: {}", ctx.getKey());
                log.warn("variableMetadata: {}", variableMetadata != null ? variableMetadata.toString() : "null");

                if (variable.isPresent()) {
                    Variable<Object> var = variable.get();
                    span.setAttribute("feature_flag.result.value", String.valueOf(var.getValue()));
                    spanLogger.logRecordBuilder().setAttribute(AttributeKey.stringKey("feature_flag.result.value"), String.valueOf(var.getValue()));

                    if (var.getEval() != null && var.getEval().getReason() != null) {
                        span.setAttribute("feature_flag.result.reason", var.getEval().getReason());
                        spanLogger.logRecordBuilder().setAttribute(AttributeKey.stringKey("feature_flag.result.reason"), var.getEval().getReason());
                    }

                    if (variableMetadata != null && variableMetadata.featureId != null) {
                        span.setAttribute("feature_flag.set.id", variableMetadata.featureId);
                        spanLogger.logRecordBuilder().setAttribute(AttributeKey.stringKey("feature_flag.set.id"), variableMetadata.featureId);
                        if (ctx.getMetadata() != null && ctx.getMetadata().project != null && ctx.getMetadata().project.id != null) {
                            span.setAttribute("feature_flag.url", "https://app.devcycle.com/r/p/" + ctx.getMetadata().project.id +  "/f/" + variableMetadata.featureId);
                            spanLogger.logRecordBuilder().setAttribute(AttributeKey.stringKey("feature_flag.url"), "https://app.devcycle.com/r/p/" + ctx.getMetadata().project.id +  "/f/" + variableMetadata.featureId);
                        }
                    }

                    log.debug("Feature flag span completed: {} = {}", var.getKey(), var.getValue());
                } else {
                    span.setAttribute("feature_flag.result.value", "null");
                    span.setAttribute("feature_flag.result.reason", "evaluation_failed");
                    log.debug("Feature flag evaluation failed for key: {}", ctx.getKey());
                }

                span.end();
                spanLogger.logRecordBuilder().setAttribute(AttributeKey.stringKey("span.id"), span.getSpanContext().getSpanId());
                spanLogger.logRecordBuilder().setAttribute(AttributeKey.stringKey("span.trace_id"), span.getSpanContext().getTraceId());
                spanLogger.logRecordBuilder().setAttribute(AttributeKey.longKey("span.end_time"), System.currentTimeMillis());
                spanLogger.logRecordBuilder().setAttribute(AttributeKey.stringKey("span.status"), "completed");
                spanLogger.logRecordBuilder().setAttribute(AttributeKey.stringKey("span.status_code"), "200");
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