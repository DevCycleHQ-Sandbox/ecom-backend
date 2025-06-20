import { Injectable } from "@nestjs/common"
import { ConfigService } from "@nestjs/config"
import { Client, OpenFeature } from "@openfeature/server-sdk"
import { DevCycleProvider } from "@devcycle/nodejs-server-sdk"
import { otelSetup } from "src/otelSetup"
import { DynatraceOtelLogHook } from "src/dynatraceOtelLogHook"

@Injectable()
export class FeatureFlagService {
  private featureFlagClient: Client
  private initialized = false
  private initializationPromise: Promise<void>

  constructor(private configService: ConfigService) {
    this.initializationPromise = this.initializeOpenFeature()
  }

  private async initializeOpenFeature(): Promise<void> {
    try {
      const sdkKey = this.configService.get<string>("DEVCYCLE_SERVER_SDK_KEY")

      const { getTracer } = otelSetup
      const tracer = getTracer()

      const dynatraceLogHook = new DynatraceOtelLogHook(tracer)

      if (sdkKey) {
        // Initialize DevCycle provider
        const devcycleProvider = new DevCycleProvider(sdkKey)
        OpenFeature.addHooks(dynatraceLogHook)

        // Set the provider for OpenFeature
        await OpenFeature.setProviderAndWait(devcycleProvider)

        // Get the OpenFeature client
        this.featureFlagClient = OpenFeature.getClient()

        this.initialized = true
        console.log(
          "✅ OpenFeature with DevCycle provider initialized successfully"
        )
      } else {
        console.warn(
          "⚠️  DEVCYCLE_SERVER_SDK_KEY not provided, feature flags will use default values"
        )
        this.initialized = false
      }
    } catch (error) {
      console.warn(
        "OpenFeature with DevCycle provider not available:",
        error.message
      )
      this.initialized = false
    }
  }

  async isInitialized(): Promise<boolean> {
    await this.initializationPromise
    return this.initialized
  }

  async waitForInitialization(): Promise<void> {
    await this.initializationPromise
  }

  async getBooleanValue(
    userId: string,
    key: string,
    defaultValue: boolean = false
  ) {
    await this.waitForInitialization()

    if (!this.featureFlagClient) {
      return defaultValue
    }

    try {
      const context = {
        targetingKey: userId,
        user_id: userId,
      }

      const result = await this.featureFlagClient.getBooleanValue(
        key,
        defaultValue,
        context
      )
      return result
    } catch (error) {
      console.warn(`Error getting feature flag ${key}:`, error.message)
      return defaultValue
    }
  }

  async getStringValue(userId: string, key: string, defaultValue: string = "") {
    await this.waitForInitialization()

    if (!this.featureFlagClient) {
      return defaultValue
    }

    try {
      const context = {
        targetingKey: userId,
        user_id: userId,
      }

      const result = await this.featureFlagClient.getStringValue(
        key,
        defaultValue,
        context
      )
      return result
    } catch (error) {
      console.warn(`Error getting string variable ${key}:`, error.message)
      return defaultValue
    }
  }

  async getNumberValue(userId: string, key: string, defaultValue: number = 0) {
    await this.waitForInitialization()

    if (!this.featureFlagClient) {
      return defaultValue
    }

    try {
      const context = {
        targetingKey: userId,
        user_id: userId,
      }

      const result = await this.featureFlagClient.getNumberValue(
        key,
        defaultValue,
        context
      )
      return result
    } catch (error) {
      console.warn(`Error getting number variable ${key}:`, error.message)
      return defaultValue
    }
  }

  async getObjectValue(userId: string, key: string, defaultValue: any = {}) {
    await this.waitForInitialization()

    if (!this.featureFlagClient) {
      return defaultValue
    }

    try {
      const context = {
        targetingKey: userId,
        user_id: userId,
      }

      const result = await this.featureFlagClient.getObjectValue(
        key,
        defaultValue,
        context
      )
      return result
    } catch (error) {
      console.warn(`Error getting object variable ${key}:`, error.message)
      return defaultValue
    }
  }

  // Legacy method for backward compatibility
  async getVariableValue(userId: string, key: string, defaultValue: any) {
    // Determine the type and call appropriate method
    if (typeof defaultValue === "boolean") {
      return this.getBooleanValue(userId, key, defaultValue)
    } else if (typeof defaultValue === "string") {
      return this.getStringValue(userId, key, defaultValue)
    } else if (typeof defaultValue === "number") {
      return this.getNumberValue(userId, key, defaultValue)
    } else {
      return this.getObjectValue(userId, key, defaultValue)
    }
  }
}
