package com.devcycle.hooks;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class OTelSpanHookTest {

    @Mock
    private Tracer tracer;

    @Mock
    private Span span;

    @Mock
    private SpanBuilder spanBuilder;

    private OTelSpanHook hook;

    @BeforeEach
    void setUp() {
        hook = new OTelSpanHook(tracer);

        // Setup mocks with lenient to avoid unnecessary stubbing exceptions
        lenient().when(tracer.spanBuilder(anyString())).thenReturn(spanBuilder);
        lenient().when(spanBuilder.setSpanKind(any(SpanKind.class))).thenReturn(spanBuilder);
        lenient().when(spanBuilder.startSpan()).thenReturn(span);
    }

    @Test
    void testBeforeHook() {
        String testContext = "test-flag";
        
        hook.before(testContext);
        
        verify(tracer).spanBuilder("feature_flag_evaluation.test-flag");
        verify(spanBuilder).setSpanKind(SpanKind.INTERNAL);
        verify(spanBuilder).startSpan();
        
        verify(span).setAttribute("feature_flag.key", "test-flag");
        verify(span).setAttribute("feature_flag.value_type", "Object");
    }

    @Test
    void testOnFinallyWithVariable() {
        String testContext = "test-flag";
        String testVariable = "test-value";
        String testMetadata = "metadata";
        
        // First call before to set up the span
        hook.before(testContext);
        
        // Then call onFinally
        hook.onFinally(testContext, Optional.of(testVariable), testMetadata);
        
        verify(span).setAttribute("feature_flag.value", "test-value");
        verify(span).end();
    }

    @Test
    void testOnFinallyWithoutVariable() {
        String testContext = "test-flag";
        String testMetadata = "metadata";
        
        // First call before to set up the span
        hook.before(testContext);
        
        // Then call onFinally with empty Optional
        hook.onFinally(testContext, Optional.empty(), testMetadata);
        
        verify(span).setAttribute("feature_flag.value", "null");
        verify(span).setAttribute("feature_flag.reason", "evaluation_failed");
        verify(span).end();
    }

    @Test
    void testAfterHook() {
        String testContext = "test-flag";
        String testVariable = "test-value";
        String testMetadata = "metadata";
        
        // The after hook should just log and not affect spans
        hook.after(testContext, Optional.of(testVariable), testMetadata);
        
        // No span operations should occur in after
        verify(span, never()).setAttribute(anyString(), anyString());
        verify(span, never()).end();
    }

    @Test
    void testErrorHook() {
        String testContext = "test-flag";
        Exception testException = new RuntimeException("Test error");
        
        hook.error(testContext, testException);
        
        // Error hook should just log, no span operations
        verify(span, never()).setAttribute(anyString(), anyString());
        verify(span, never()).end();
    }

    @Test
    void testBeforeHookException() {
        String testContext = "test-flag";
        
        // Make spanBuilder throw an exception
        when(tracer.spanBuilder(anyString())).thenThrow(new RuntimeException("Test error"));
        
        Optional<Object> result = hook.before(testContext);
        
        // Should return empty optional and not throw
        assert result.isEmpty();
    }

    @Test
    void testWithNullContext() {
        hook.before(null);
        
        verify(tracer).spanBuilder("feature_flag_evaluation.unknown");
        verify(span).setAttribute("feature_flag.key", "unknown");
        verify(span).setAttribute("feature_flag.value_type", "Object");
    }
}