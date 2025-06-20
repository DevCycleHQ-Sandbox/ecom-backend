import { Module } from "@nestjs/common"
import { AdminController } from "./admin.controller"
import { SyncService } from "../database/sync.service"
import { DatabaseModule } from "../database/database.module"
import { AuthModule } from "../auth/auth.module"

@Module({
  imports: [DatabaseModule, AuthModule],
  controllers: [AdminController],
  providers: [SyncService],
  exports: [SyncService],
})
export class AdminModule {}
