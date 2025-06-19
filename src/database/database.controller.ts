import { Controller, Get, Param, Request, UseGuards } from "@nestjs/common"
import { JwtAuthGuard } from "../auth/guards/jwt-auth.guard"
import { RolesGuard } from "../auth/guards/roles.guard"
import { Roles } from "../auth/decorators/roles.decorator"
import { DatabaseManagerService } from "./database-manager.service"
import { DatabaseService } from "./database.service"
import { DatabaseMigrationService } from "./database-migration.service"

@Controller("database")
@UseGuards(JwtAuthGuard)
export class DatabaseController {
  constructor(
    private databaseManagerService: DatabaseManagerService,
    private databaseService: DatabaseService,
    private databaseMigrationService: DatabaseMigrationService
  ) {}

  @Get("status")
  async getDatabaseStatus(@Request() req: any) {
    const userId = req.user.id

    return await this.databaseManagerService.getCurrentDatabaseInfo(userId)
  }

  @Get("metrics")
  @UseGuards(RolesGuard)
  @Roles("admin")
  async getDatabaseMetrics() {
    return await this.databaseManagerService.getDatabaseMetrics()
  }

  @Get("health")
  async checkDatabaseHealth() {
    return await this.databaseManagerService.validateConnection()
  }

  @Get("user/:userId/database-type")
  @UseGuards(RolesGuard)
  @Roles("admin")
  async getUserDatabaseType(@Param("userId") userId: string) {
    const dbType =
      await this.databaseManagerService.getDatabaseTypeForUser(userId)
    const shouldUsePostgres =
      await this.databaseManagerService.shouldUsePostgreSQL(userId)

    return {
      userId,
      databaseType: dbType,
      featureFlag: {
        name: "use_postgres_database",
        value: shouldUsePostgres,
      },
    }
  }

  @Get("connection-info")
  @UseGuards(RolesGuard)
  @Roles("admin")
  async getConnectionInfo() {
    return this.databaseService.getConnectionInfo()
  }

  @Get("stats")
  @UseGuards(RolesGuard)
  @Roles("admin")
  async getDatabaseStats() {
    return await this.databaseMigrationService.getDatabaseStats()
  }
}
