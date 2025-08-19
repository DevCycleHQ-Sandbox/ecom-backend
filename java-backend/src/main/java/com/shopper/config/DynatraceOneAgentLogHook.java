package com.shopper.config;

import com.dynatrace.oneagent.sdk.api.OneAgentSDK;
import com.dynatrace.oneagent.sdk.api.IncomingRemoteCallTracer;
import com.dynatrace.oneagent.sdk.api.CustomServiceTracer;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import com.devcycle.sdk.server.common.model.EvalHook;
import com.devcycle.sdk.server.common.model.HookContext;
import com.devcycle.sdk.server.common.model.Variable;
import com.devcycle.sdk.server.local.model.VariableMetadata;

@RequiredArgsConstructor
@Slf4j
public class DynatraceOneAgentLogHook implements EvalHook<Object> {

    private final OneAgentSDK oneAgentSDK;
    private final Map<HookContext<Object>, CustomServiceTracer> tracers = new ConcurrentHashMap<>();
    private final Map<HookContext<Object>, Map> attributes = new ConcurrentHashMap<>();

    public Optional<HookContext<Object>> before(HookContext<Object> ctx) {
        try {
            // Create a custom service tracer for feature flag evaluation
            CustomServiceTracer tracer = oneAgentSDK.traceCustomService("feature_flag_evaluation", ctx.getKey());

            // log.debug("Feature flag structured header created: {}", featureFlagJson);
            if (tracer != null) {
                tracer.start();
                
                // // Add custom request attributes
                // oneAgentSDK.addCustomRequestAttribute("feature_flag.key", ctx.getKey());
                // oneAgentSDK.addCustomRequestAttribute("feature_flag.value_type", ctx.getDefaultValue().getClass().getSimpleName());
                
                // if (ctx.getMetadata() != null) {
                //     if (ctx.getMetadata().project != null && ctx.getMetadata().project.id != null) {
                //         oneAgentSDK.addCustomRequestAttribute("feature_flag.project", ctx.getMetadata().project.id);
                //     }
                //     if (ctx.getMetadata().environment != null && ctx.getMetadata().environment.id != null) {
                //         oneAgentSDK.addCustomRequestAttribute("feature_flag.environment", ctx.getMetadata().environment.id);
                //     }
                // }

                Map<String, String> featureFlagData = new HashMap<>();
                featureFlagData.put("key", ctx.getKey());
                featureFlagData.put("value_type", ctx.getDefaultValue().getClass().getSimpleName());

                if (ctx.getMetadata() != null) {
                    featureFlagData.put("project", ctx.getMetadata().project.id);
                    featureFlagData.put("environment", ctx.getMetadata().environment.id);
                }

                attributes.put(ctx, featureFlagData);
                log.debug("Feature flag evaluation tracer started for key: {}", ctx.getKey());
            }

            tracers.put(ctx, tracer);
            return Optional.empty();
        } catch (Exception e) {
            log.error("Error starting feature flag tracer for key {}: {}", ctx.getKey(), e.getMessage());
            return Optional.empty();
        }
    }

    public void after(HookContext<Object> ctx, Optional<Variable<Object>> variable, VariableMetadata variableMetadata) {
        // DevCycle calls onFinally instead of after, so we don't handle tracer completion here
        log.debug("after called for key: {} (tracer completion handled in onFinally)", ctx.getKey());
        log.warn("variableMetadata: {}", variableMetadata != null ? variableMetadata.toString() : "null");
        log.warn("variable: {}", variable != null ? variable.get().toString() : "null");
        log.warn("ctx: {}", ctx != null ? ctx.toString() : "null");
    }

    public void onFinally(HookContext<Object> ctx, Optional<Variable<Object>> variable, VariableMetadata variableMetadata) {
        try {
            CustomServiceTracer tracer = tracers.remove(ctx);
            Map attributeMap = attributes.remove(ctx);
            if (tracer != null) {
                log.debug("Completing feature flag evaluation tracer for key: {}", ctx.getKey());
                log.warn("variableMetadata: {}", variableMetadata != null ? variableMetadata.toString() : "null");

//                if (variable.isPresent()) {
//                    Variable<Object> var = variable.get();
//                    oneAgentSDK.addCustomRequestAttribute("feature_flag.value", String.valueOf(var.getValue()));
//
//                    if (var.getEval() != null && var.getEval().getReason() != null) {
//                        oneAgentSDK.addCustomRequestAttribute("feature_flag.reason", var.getEval().getReason());
//                    }
//
//                    if (variableMetadata != null && variableMetadata.featureId != null) {
//                        oneAgentSDK.addCustomRequestAttribute("feature_flag.flagset", variableMetadata.featureId);
//                    }
//
//                    log.debug("Feature flag tracer completed: {} = {}", var.getKey(), var.getValue());
//                } else {
//                    oneAgentSDK.addCustomRequestAttribute("feature_flag.value", "null");
//                    oneAgentSDK.addCustomRequestAttribute("feature_flag.reason", "evaluation_failed");
//                    tracer.error("Feature flag evaluation failed");
//                    log.debug("Feature flag evaluation failed for key: {}", ctx.getKey());
//                }
                oneAgentSDK.addCustomRequestAttribute("feature_flag", attributeMap.toString());
                
                tracer.end();
                log.debug("Feature flag tracer ended for key: {}", ctx.getKey());

            } else {
                log.warn("No tracer found for feature flag key: {}", ctx.getKey());
            }
        } catch (Exception e) {
            log.error("Error completing feature flag tracer for key {}: {}", ctx.getKey(), e.getMessage());
        }
    }

    public void error(HookContext<Object> ctx, Exception e) {
        try {
            CustomServiceTracer tracer = tracers.get(ctx);
            if (tracer != null) {
                tracer.error(e.getMessage());
            }
            log.warn("Feature flag evaluation error for key: {}, error: {}", ctx.getKey(), e.getMessage());
        } catch (Exception ex) {
            log.error("Error handling feature flag error for key {}: {}", ctx.getKey(), ex.getMessage());
        }
    }
}