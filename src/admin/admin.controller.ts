import {
  Controller,
  Get,
  HttpException,
  HttpStatus,
  Param,
  Post,
  UseGuards,
} from "@nestjs/common"
import { SyncService } from "../database/sync.service"
import { JwtAuthGuard } from "../auth/guards/jwt-auth.guard"
import { RolesGuard } from "../auth/guards/roles.guard"
import { Roles } from "../auth/decorators/roles.decorator"

@Controller("admin")
@UseGuards(JwtAuthGuard, RolesGuard)
@Roles("admin")
export class AdminController {
  constructor(private readonly syncService: SyncService) {}

  @Post("sync/all")
  async syncAllData() {
    try {
      const results = await this.syncService.syncAllData()

      const summary = {
        timestamp: new Date().toISOString(),
        totalEntities: results.length,
        totalSynced: results.reduce((sum, r) => sum + r.synced, 0),
        totalErrors: results.reduce((sum, r) => sum + r.errors.length, 0),
        results,
      }

      return {
        success: true,
        message: "Database sync completed",
        data: summary,
      }
    } catch (error) {
      throw new HttpException(
        {
          success: false,
          message: "Sync failed",
          error: error.message,
        },
        HttpStatus.INTERNAL_SERVER_ERROR
      )
    }
  }

  @Post("sync/:entity")
  async syncSpecificEntity(@Param("entity") entity: string) {
    try {
      const result = await this.syncService.syncSpecificEntity(entity)

      return {
        success: true,
        message: `Sync completed for ${entity}`,
        data: {
          timestamp: new Date().toISOString(),
          ...result,
        },
      }
    } catch (error) {
      throw new HttpException(
        {
          success: false,
          message: `Sync failed for ${entity}`,
          error: error.message,
        },
        HttpStatus.INTERNAL_SERVER_ERROR
      )
    }
  }

  @Get("database/stats")
  async getDatabaseStats() {
    try {
      const stats = await this.syncService.getDatabaseStats()

      return {
        success: true,
        message: "Database statistics retrieved",
        data: {
          timestamp: new Date().toISOString(),
          ...stats,
        },
      }
    } catch (error) {
      throw new HttpException(
        {
          success: false,
          message: "Failed to retrieve database stats",
          error: error.message,
        },
        HttpStatus.INTERNAL_SERVER_ERROR
      )
    }
  }

  @Get("sync/status")
  async getSyncStatus() {
    try {
      const stats = await this.syncService.getDatabaseStats()

      // Calculate sync status by comparing counts
      const syncStatus = {}
      const entities = Object.keys(stats.sqlite)

      for (const entity of entities) {
        const sqliteCount = stats.sqlite[entity]
        const postgresCount = stats.postgres[entity]

        syncStatus[entity] = {
          sqlite: sqliteCount,
          postgres: postgresCount,
          inSync: sqliteCount === postgresCount,
          difference:
            typeof sqliteCount === "number" && typeof postgresCount === "number"
              ? sqliteCount - postgresCount
              : "Unknown",
        }
      }

      const allInSync = Object.values(syncStatus).every(
        (status: any) => status.inSync
      )

      return {
        success: true,
        message: "Sync status retrieved",
        data: {
          timestamp: new Date().toISOString(),
          overallStatus: allInSync ? "IN_SYNC" : "OUT_OF_SYNC",
          entities: syncStatus,
        },
      }
    } catch (error) {
      throw new HttpException(
        {
          success: false,
          message: "Failed to retrieve sync status",
          error: error.message,
        },
        HttpStatus.INTERNAL_SERVER_ERROR
      )
    }
  }
}
