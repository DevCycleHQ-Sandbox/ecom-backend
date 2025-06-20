import { Inject, Injectable } from "@nestjs/common"
import { ConfigService } from "@nestjs/config"
import { Client, OpenFeatureClient } from "@openfeature/nestjs-sdk"
import type { DevCycleClient } from "@devcycle/nodejs-server-sdk"

@Injectable()
export class FeatureFlagService {
  private initialized = false
  private initializationPromise: Promise<void>

  constructor(
    private configService: ConfigService,
    @OpenFeatureClient() private client: Client,
    @Inject("DVC_CLIENT") private devcycleClient: DevCycleClient | null
  ) {
    this.initialized = !!this.devcycleClient
  }

  async isInitialized(): Promise<boolean> {
    return this.initialized
  }

  async waitForInitialization(): Promise<void> {
    // Already initialized in constructor
    return Promise.resolve()
  }

  async getBooleanValue(
    userId: string,
    key: string,
    defaultValue: boolean = false
  ) {
    if (!this.initialized) {
      return defaultValue
    }

    try {
      const context = {
        targetingKey: userId,
        user_id: userId,
      }

      const result = await this.client.getBooleanValue(
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
    if (!this.initialized) {
      return defaultValue
    }

    try {
      const context = {
        targetingKey: userId,
        user_id: userId,
      }

      const result = await this.client.getStringValue(
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
    if (!this.initialized) {
      return defaultValue
    }

    try {
      const context = {
        targetingKey: userId,
        user_id: userId,
      }

      const result = await this.client.getNumberValue(
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
    if (!this.initialized) {
      return defaultValue
    }

    try {
      const context = {
        targetingKey: userId,
        user_id: userId,
      }

      const result = await this.client.getObjectValue(
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

  // Additional method to access DevCycle client directly if needed
  getDevCycleClient(): DevCycleClient | null {
    return this.devcycleClient
  }

  // Method to get all features for a user (using DevCycle client directly)
  async getAllFeatures(userId: string) {
    if (!this.devcycleClient) {
      console.warn("DevCycle client not available")
      return {}
    }

    try {
      return await this.devcycleClient.allFeatures({ user_id: userId })
    } catch (error) {
      console.warn(`Error getting all features:`, error.message)
      return {}
    }
  }
}
