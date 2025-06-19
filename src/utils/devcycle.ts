import { Client, OpenFeature } from "@openfeature/server-sdk"
import { DevCycleProvider } from "@devcycle/nodejs-server-sdk"

let featureFlagClient: Client | null = null

export async function initializeOpenFeature(): Promise<void> {
  const serverKey = process.env.DEVCYCLE_SERVER_SDK_KEY

  if (!serverKey) {
    console.warn(
      "⚠️  DevCycle server SDK key not found. Feature flags will be disabled."
    )
    return
  }

  try {
    // Initialize DevCycle provider
    const devcycleProvider = new DevCycleProvider(serverKey)

    // Set the provider for OpenFeature
    await OpenFeature.setProvider(devcycleProvider)

    // Get the OpenFeature client
    featureFlagClient = OpenFeature.getClient()

    console.log(
      "✅ OpenFeature with DevCycle provider initialized successfully"
    )
  } catch (error) {
    console.error(
      "❌ Failed to initialize OpenFeature with DevCycle provider:",
      error
    )
  }
}

export function getFeatureFlagClient(): Client | null {
  return featureFlagClient
}

export async function getFeatureFlag(
  userId: string,
  flagKey: string,
  defaultValue: boolean = false
): Promise<boolean> {
  if (!featureFlagClient) {
    console.warn(
      `Feature flag ${flagKey} requested but OpenFeature not initialized`
    )
    return defaultValue
  }

  try {
    const context = {
      targetingKey: userId,
      user_id: userId,
    }

    const result = await featureFlagClient.getBooleanValue(
      flagKey,
      defaultValue,
      context
    )
    return result
  } catch (error) {
    console.error(`Error getting feature flag ${flagKey}:`, error)
    return defaultValue
  }
}

// Additional utility functions for different data types
export async function getStringFlag(
  userId: string,
  flagKey: string,
  defaultValue: string = ""
): Promise<string> {
  if (!featureFlagClient) {
    return defaultValue
  }

  try {
    const context = {
      targetingKey: userId,
      user_id: userId,
    }

    const result = await featureFlagClient.getStringValue(
      flagKey,
      defaultValue,
      context
    )
    return result
  } catch (error) {
    console.error(`Error getting string flag ${flagKey}:`, error)
    return defaultValue
  }
}

export async function getNumberFlag(
  userId: string,
  flagKey: string,
  defaultValue: number = 0
): Promise<number> {
  if (!featureFlagClient) {
    return defaultValue
  }

  try {
    const context = {
      targetingKey: userId,
      user_id: userId,
    }

    const result = await featureFlagClient.getNumberValue(
      flagKey,
      defaultValue,
      context
    )
    return result
  } catch (error) {
    console.error(`Error getting number flag ${flagKey}:`, error)
    return defaultValue
  }
}

export async function getObjectFlag(
  userId: string,
  flagKey: string,
  defaultValue: any = {}
): Promise<any> {
  if (!featureFlagClient) {
    return defaultValue
  }

  try {
    const context = {
      targetingKey: userId,
      user_id: userId,
    }

    const result = await featureFlagClient.getObjectValue(
      flagKey,
      defaultValue,
      context
    )
    return result
  } catch (error) {
    console.error(`Error getting object flag ${flagKey}:`, error)
    return defaultValue
  }
}

// Legacy function names for backward compatibility
export const initializeDevCycle = initializeOpenFeature
export const getDevCycleClient = getFeatureFlagClient
