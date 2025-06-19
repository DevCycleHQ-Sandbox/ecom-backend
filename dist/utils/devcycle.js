"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.getDevCycleClient = exports.initializeDevCycle = void 0;
exports.initializeOpenFeature = initializeOpenFeature;
exports.getFeatureFlagClient = getFeatureFlagClient;
exports.getFeatureFlag = getFeatureFlag;
exports.getStringFlag = getStringFlag;
exports.getNumberFlag = getNumberFlag;
exports.getObjectFlag = getObjectFlag;
const server_sdk_1 = require("@openfeature/server-sdk");
const nodejs_server_sdk_1 = require("@devcycle/nodejs-server-sdk");
let featureFlagClient = null;
async function initializeOpenFeature() {
    const serverKey = process.env.DEVCYCLE_SERVER_SDK_KEY;
    if (!serverKey) {
        console.warn("⚠️  DevCycle server SDK key not found. Feature flags will be disabled.");
        return;
    }
    try {
        const devcycleProvider = new nodejs_server_sdk_1.DevCycleProvider(serverKey);
        await server_sdk_1.OpenFeature.setProvider(devcycleProvider);
        featureFlagClient = server_sdk_1.OpenFeature.getClient();
        console.log("✅ OpenFeature with DevCycle provider initialized successfully");
    }
    catch (error) {
        console.error("❌ Failed to initialize OpenFeature with DevCycle provider:", error);
    }
}
function getFeatureFlagClient() {
    return featureFlagClient;
}
async function getFeatureFlag(userId, flagKey, defaultValue = false) {
    if (!featureFlagClient) {
        console.warn(`Feature flag ${flagKey} requested but OpenFeature not initialized`);
        return defaultValue;
    }
    try {
        const context = {
            targetingKey: userId,
            user_id: userId,
        };
        const result = await featureFlagClient.getBooleanValue(flagKey, defaultValue, context);
        return result;
    }
    catch (error) {
        console.error(`Error getting feature flag ${flagKey}:`, error);
        return defaultValue;
    }
}
async function getStringFlag(userId, flagKey, defaultValue = "") {
    if (!featureFlagClient) {
        return defaultValue;
    }
    try {
        const context = {
            targetingKey: userId,
            user_id: userId,
        };
        const result = await featureFlagClient.getStringValue(flagKey, defaultValue, context);
        return result;
    }
    catch (error) {
        console.error(`Error getting string flag ${flagKey}:`, error);
        return defaultValue;
    }
}
async function getNumberFlag(userId, flagKey, defaultValue = 0) {
    if (!featureFlagClient) {
        return defaultValue;
    }
    try {
        const context = {
            targetingKey: userId,
            user_id: userId,
        };
        const result = await featureFlagClient.getNumberValue(flagKey, defaultValue, context);
        return result;
    }
    catch (error) {
        console.error(`Error getting number flag ${flagKey}:`, error);
        return defaultValue;
    }
}
async function getObjectFlag(userId, flagKey, defaultValue = {}) {
    if (!featureFlagClient) {
        return defaultValue;
    }
    try {
        const context = {
            targetingKey: userId,
            user_id: userId,
        };
        const result = await featureFlagClient.getObjectValue(flagKey, defaultValue, context);
        return result;
    }
    catch (error) {
        console.error(`Error getting object flag ${flagKey}:`, error);
        return defaultValue;
    }
}
exports.initializeDevCycle = initializeOpenFeature;
exports.getDevCycleClient = getFeatureFlagClient;
//# sourceMappingURL=devcycle.js.map