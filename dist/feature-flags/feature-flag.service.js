"use strict";
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
var __metadata = (this && this.__metadata) || function (k, v) {
    if (typeof Reflect === "object" && typeof Reflect.metadata === "function") return Reflect.metadata(k, v);
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.FeatureFlagService = void 0;
const common_1 = require("@nestjs/common");
const config_1 = require("@nestjs/config");
const server_sdk_1 = require("@openfeature/server-sdk");
const nodejs_server_sdk_1 = require("@devcycle/nodejs-server-sdk");
let FeatureFlagService = class FeatureFlagService {
    constructor(configService) {
        this.configService = configService;
        this.initializeOpenFeature();
    }
    async initializeOpenFeature() {
        try {
            const sdkKey = this.configService.get("DEVCYCLE_SERVER_SDK_KEY");
            if (sdkKey) {
                const devcycleProvider = new nodejs_server_sdk_1.DevCycleProvider(sdkKey);
                await server_sdk_1.OpenFeature.setProvider(devcycleProvider);
                this.featureFlagClient = server_sdk_1.OpenFeature.getClient();
                console.log("✅ OpenFeature with DevCycle provider initialized successfully");
            }
        }
        catch (error) {
            console.warn("OpenFeature with DevCycle provider not available:", error.message);
        }
    }
    async getBooleanValue(userId, key, defaultValue = false) {
        if (!this.featureFlagClient) {
            return defaultValue;
        }
        try {
            const context = {
                targetingKey: userId,
                user_id: userId,
            };
            const result = await this.featureFlagClient.getBooleanValue(key, defaultValue, context);
            return result;
        }
        catch (error) {
            console.warn(`Error getting feature flag ${key}:`, error.message);
            return defaultValue;
        }
    }
    async getStringValue(userId, key, defaultValue = "") {
        if (!this.featureFlagClient) {
            return defaultValue;
        }
        try {
            const context = {
                targetingKey: userId,
                user_id: userId,
            };
            const result = await this.featureFlagClient.getStringValue(key, defaultValue, context);
            return result;
        }
        catch (error) {
            console.warn(`Error getting string variable ${key}:`, error.message);
            return defaultValue;
        }
    }
    async getNumberValue(userId, key, defaultValue = 0) {
        if (!this.featureFlagClient) {
            return defaultValue;
        }
        try {
            const context = {
                targetingKey: userId,
                user_id: userId,
            };
            const result = await this.featureFlagClient.getNumberValue(key, defaultValue, context);
            return result;
        }
        catch (error) {
            console.warn(`Error getting number variable ${key}:`, error.message);
            return defaultValue;
        }
    }
    async getObjectValue(userId, key, defaultValue = {}) {
        if (!this.featureFlagClient) {
            return defaultValue;
        }
        try {
            const context = {
                targetingKey: userId,
                user_id: userId,
            };
            const result = await this.featureFlagClient.getObjectValue(key, defaultValue, context);
            return result;
        }
        catch (error) {
            console.warn(`Error getting object variable ${key}:`, error.message);
            return defaultValue;
        }
    }
    async getVariableValue(userId, key, defaultValue) {
        if (typeof defaultValue === "boolean") {
            return this.getBooleanValue(userId, key, defaultValue);
        }
        else if (typeof defaultValue === "string") {
            return this.getStringValue(userId, key, defaultValue);
        }
        else if (typeof defaultValue === "number") {
            return this.getNumberValue(userId, key, defaultValue);
        }
        else {
            return this.getObjectValue(userId, key, defaultValue);
        }
    }
};
exports.FeatureFlagService = FeatureFlagService;
exports.FeatureFlagService = FeatureFlagService = __decorate([
    (0, common_1.Injectable)(),
    __metadata("design:paramtypes", [config_1.ConfigService])
], FeatureFlagService);
//# sourceMappingURL=feature-flag.service.js.map