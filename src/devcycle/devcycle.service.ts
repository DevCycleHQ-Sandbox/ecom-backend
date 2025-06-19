import { Injectable } from "@nestjs/common"
import { ConfigService } from "@nestjs/config"

@Injectable()
export class DevCycleService {
  private devcycleClient: any

  constructor(private configService: ConfigService) {
    this.initializeDevCycle()
  }

  private async initializeDevCycle() {
    try {
      const { initializeDevCycle } = await import("@devcycle/nodejs-server-sdk")
      const sdkKey = this.configService.get<string>("DEVCYCLE_SERVER_SDK_KEY")

      if (sdkKey) {
        this.devcycleClient = initializeDevCycle(sdkKey)
      }
    } catch (error) {
      console.warn("DevCycle SDK not available:", error.message)
    }
  }

  async getVariableValue(userId: string, key: string, defaultValue: any) {
    if (!this.devcycleClient) {
      return defaultValue
    }

    try {
      const user = { user_id: userId }
      const variable = await this.devcycleClient.variableValue(
        user,
        key,
        defaultValue
      )
      return variable
    } catch (error) {
      console.warn(`Error getting DevCycle variable ${key}:`, error.message)
      return defaultValue
    }
  }
}
