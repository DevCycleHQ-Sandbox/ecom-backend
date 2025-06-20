import { Module, OnModuleInit } from "@nestjs/common"
import { ConfigModule, ConfigService } from "@nestjs/config"
import { OpenFeatureModule } from "@openfeature/nestjs-sdk"
import { DevCycleNestJSProvider } from "@devcycle/openfeature-nestjs-provider"
import { OpenFeature } from "@openfeature/server-sdk"
import { DynatraceOtelLogHook } from "../dynatraceOtelLogHook"
import { otelSetup } from "../otelSetup"
import { FeatureFlagService } from "./feature-flag.service"

@Module({
  imports: [
    ConfigModule,
    OpenFeatureModule.forRoot({
      contextFactory: () => ({
        targetingKey: "nestjs-app",
      }),
    }),
  ],
  providers: [
    FeatureFlagService,
    {
      provide: "DEVCYCLE_PROVIDER",
      useFactory: (configService: ConfigService) => {
        const sdkKey = configService.get<string>("DEVCYCLE_SERVER_SDK_KEY")
        if (!sdkKey) {
          console.warn("⚠️  DEVCYCLE_SERVER_SDK_KEY not provided")
          return null
        }
        return new DevCycleNestJSProvider(sdkKey)
      },
      inject: [ConfigService],
    },
    {
      provide: "DVC_CLIENT",
      useFactory: (provider: DevCycleNestJSProvider) => {
        return provider?.devcycleClient || null
      },
      inject: ["DEVCYCLE_PROVIDER"],
    },
  ],
  exports: [FeatureFlagService, OpenFeatureModule, "DVC_CLIENT"],
})
export class FeatureFlagModule implements OnModuleInit {
  constructor(
    private configService: ConfigService,
    private featureFlagService: FeatureFlagService
  ) {}

  async onModuleInit() {
    try {
      const sdkKey = this.configService.get<string>("DEVCYCLE_SERVER_SDK_KEY")

      if (!sdkKey) {
        console.warn(
          "⚠️  DEVCYCLE_SERVER_SDK_KEY not provided, feature flags will use default values"
        )
        return
      }

      // Setup custom hooks
      const { getTracer } = otelSetup
      const tracer = getTracer()
      const dynatraceLogHook = new DynatraceOtelLogHook(tracer)
      OpenFeature.addHooks(dynatraceLogHook)

      // Create the DevCycle provider
      const provider = new DevCycleNestJSProvider(sdkKey)

      // Set the provider for OpenFeature
      await OpenFeature.setProviderAndWait(provider)

      console.log(
        "features,",
        await provider.devcycleClient.allFeatures({
          user_id: "admin",
        })
      )

      console.log(
        "✅ OpenFeature with DevCycle NestJS provider initialized successfully"
      )
    } catch (error) {
      console.warn(
        "OpenFeature with DevCycle provider not available:",
        error.message
      )
    }
  }
}
