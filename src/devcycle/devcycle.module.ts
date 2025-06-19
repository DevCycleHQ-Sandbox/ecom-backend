import { Module } from "@nestjs/common"
import { DevCycleService } from "./devcycle.service"

@Module({
  providers: [DevCycleService],
  exports: [DevCycleService],
})
export class DevCycleModule {}
