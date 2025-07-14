package com.shopper.config;

import dev.openfeature.sdk.EvaluationContext;
import dev.openfeature.sdk.FlagEvaluationDetails;
import dev.openfeature.sdk.Hook;
import dev.openfeature.sdk.HookContext;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@RequiredArgsConstructor
@Slf4j
public class DynatraceOtelLogHook implements Hook<Object> {

    private final Tracer tracer;
    private final OpenTelemetryConfig.AppMetadata appMetadata;
    private final Map<HookContext<Object>, Span> spans = new ConcurrentHashMap<>();

    @Override
    public Optional<EvaluationContext> before(HookContext<Object> ctx, Map<String, Object> hints) {
        Span span = tracer.spanBuilder("feature_flag_evaluation." + ctx.getFlagKey())
                .setSpanKind(SpanKind.SERVER)
                .startSpan();

        if (span != null) {
            span.setAttribute("feature_flag.key", ctx.getFlagKey());
            span.setAttribute("feature_flag.value_type", ctx.getType().name());
            span.setAttribute("feature_flag.flagset", ctx.getFlagKey());
            span.setAttribute("feature_flag.project", appMetadata.getProject());
            span.setAttribute("feature_flag.environment", appMetadata.getEnvironmentId());

            if (ctx.getClientMetadata() != null && ctx.getClientMetadata().getName() != null) {
                span.setAttribute("openfeature.client.name", ctx.getClientMetadata().getName());
            }

            if (ctx.getProviderMetadata() != null && ctx.getProviderMetadata().getName() != null) {
                span.setAttribute("openfeature.provider.name", "devcycle");
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