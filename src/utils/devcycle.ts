import { DVCClient, initialize } from "@devcycle/nodejs-server-sdk"

let devCycleClient: DVCClient | null = null

export function initializeDevCycle(): void {
  const serverKey = process.env.DEVCYCLE_SERVER_SDK_KEY

  if (!serverKey) {
    console.warn(
      "⚠️  DevCycle server SDK key not found. Feature flags will be disabled."
    )
    return
  }

  try {
    devCycleClient = initialize(serverKey)
    console.log("✅ DevCycle SDK initialized successfully")
  } catch (error) {
    console.error("❌ Failed to initialize DevCycle SDK:", error)
  }
}

export function getDevCycleClient(): DVCClient | null {
  return devCycleClient
}

export async function getFeatureFlag(
  userId: string,
  flagKey: string,
  defaultValue: boolean = false
): Promise<boolean> {
  if (!devCycleClient) {
    console.warn(
      `Feature flag ${flagKey} requested but DevCycle not initialized`
    )
    return defaultValue
  }

  try {
    const user = {
      user_id: userId,
    }

    const result = await devCycleClient.variableValue(
      user,
      flagKey,
      defaultValue
    )
    return result
  } catch (error) {
    console.error(`Error getting feature flag ${flagKey}:`, error)
    return defaultValue
  }
}
