package com.devcycle.hooks;

import dev.openfeature.sdk.*;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OTelSpanHookTest {

    @Mock
    private Tracer tracer;

    @Mock
    private Span span;

    @Mock
    private SpanBuilder spanBuilder;

    @Mock
    private HookContext<Object> hookContext;

    @Mock
    private FlagEvaluationDetails<Object> flagEvaluationDetails;

    @Mock
    private ClientMetadata clientMetadata;

    private OTelSpanHook hook;

    @BeforeEach
    void setUp() {
        hook = new OTelSpanHook(tracer);

        // Setup mocks
        when(tracer.spanBuilder(anyString())).thenReturn(spanBuilder);
        when(spanBuilder.setSpanKind(any(SpanKind.class))).thenReturn(spanBuilder);
        when(spanBuilder.startSpan()).thenReturn(span);
        
        when(hookContext.getFlagKey()).thenReturn("test-flag");
        when(hookContext.getType()).thenReturn(FlagValueType.BOOLEAN);
        when(hookContext.getClientMetadata()).thenReturn(clientMetadata);
        
        when(clientMetadata.getName()).thenReturn("test-client");
    }

    @Test
    void testBeforeHook() {
        Map<String, Object> hints = new HashMap<>();
        
        hook.before(hookContext, hints);
        
        verify(tracer).spanBuilder("feature_flag_evaluation.test-flag");
        verify(spanBuilder).setSpanKind(SpanKind.SERVER);
        verify(spanBuilder).startSpan();
        
        verify(span).setAttribute("feature_flag.key", "test-flag");
        verify(span).setAttribute("feature_flag.value_type", "BOOLEAN");
        verify(span).setAttribute("feature_flag.flagset", "test-flag");
        verify(span).setAttribute("openfeature.client.name", "test-client");
    }

    @Test
    void testAfterHook() {
        Map<String, Object> hints = new HashMap<>();
        
        // Setup flag evaluation details
        when(flagEvaluationDetails.getValue()).thenReturn(true);
        when(flagEvaluationDetails.getReason()).thenReturn("TARGETING_MATCH");
        when(flagEvaluationDetails.getVariant()).thenReturn("test-variant");
        when(flagEvaluationDetails.getErrorCode()).thenReturn(null);
        when(flagEvaluationDetails.getErrorMessage()).thenReturn(null);
        
        // First call before to set up the span
        hook.before(hookContext, hints);
        
        // Then call after
        hook.after(hookContext, flagEvaluationDetails, hints);
        
        verify(span).setAttribute("feature_flag.value", "true");
        verify(span).setAttribute("feature_flag.reason", "TARGETING_MATCH");
        verify(span).setAttribute("feature_flag.variant", "test-variant");
        verify(span).end();
    }

    @Test
    void testErrorHook() {
        Map<String, Object> hints = new HashMap<>();
        Exception testException = new RuntimeException("Test error");
        
        // First call before to set up the span
        hook.before(hookContext, hints);
        
        // Then call error
        hook.error(hookContext, testException, hints);
        
        verify(span).recordException(testException);
        verify(span).setAttribute("feature_flag.error_message", "Test error");
        verify(span).end();
    }

    @Test
    void testFinallyAfterHook() {
        Map<String, Object> hints = new HashMap<>();
        
        // First call before to set up the span
        hook.before(hookContext, hints);
        
        // Then call finallyAfter
        hook.finallyAfter(hookContext, hints);
        
        verify(span).end();
    }

    @Test
    void testWithProviderMetadata() {
        Map<String, Object> hints = new HashMap<>();
        
        // Mock provider metadata
        var mockProviderMetadata = mock(dev.openfeature.sdk.ProviderMetadata.class);
        when(mockProviderMetadata.getName()).thenReturn("test-provider");
        when(hookContext.getProviderMetadata()).thenReturn(mockProviderMetadata);
        
        hook.before(hookContext, hints);
        
        verify(span).setAttribute("openfeature.provider.name", "devcycle");
    }

    @Test
    void testWithNullClientMetadata() {
        Map<String, Object> hints = new HashMap<>();
        when(hookContext.getClientMetadata()).thenReturn(null);
        
        hook.before(hookContext, hints);
        
        verify(tracer).spanBuilder("feature_flag_evaluation.test-flag");
        verify(span).setAttribute("feature_flag.key", "test-flag");
        verify(span).setAttribute("feature_flag.value_type", "BOOLEAN");
        verify(span).setAttribute("feature_flag.flagset", "test-flag");
        
        // Should not set client name attribute
        verify(span, never()).setAttribute(eq("openfeature.client.name"), anyString());
    }
}