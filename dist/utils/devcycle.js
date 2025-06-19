"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.initializeDevCycle = initializeDevCycle;
exports.getDevCycleClient = getDevCycleClient;
exports.getFeatureFlag = getFeatureFlag;
const nodejs_server_sdk_1 = require("@devcycle/nodejs-server-sdk");
let devCycleClient = null;
function initializeDevCycle() {
    const serverKey = process.env.DEVCYCLE_SERVER_SDK_KEY;
    if (!serverKey) {
        console.warn("⚠️  DevCycle server SDK key not found. Feature flags will be disabled.");
        return;
    }
    try {
        devCycleClient = (0, nodejs_server_sdk_1.initialize)(serverKey);
        console.log("✅ DevCycle SDK initialized successfully");
    }
    catch (error) {
        console.error("❌ Failed to initialize DevCycle SDK:", error);
    }
}
function getDevCycleClient() {
    return devCycleClient;
}
async function getFeatureFlag(userId, flagKey, defaultValue = false) {
    if (!devCycleClient) {
        console.warn(`Feature flag ${flagKey} requested but DevCycle not initialized`);
        return defaultValue;
    }
    try {
        const user = {
            user_id: userId,
        };
        const result = await devCycleClient.variableValue(user, flagKey, defaultValue);
        return result;
    }
    catch (error) {
        console.error(`Error getting feature flag ${flagKey}:`, error);
        return defaultValue;
    }
}
//# sourceMappingURL=devcycle.js.map