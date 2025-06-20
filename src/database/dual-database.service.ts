import { Injectable } from "@nestjs/common"
import { InjectDataSource } from "@nestjs/typeorm"
import {
  DataSource,
  DeepPartial,
  EntityTarget,
  FindManyOptions,
  FindOneOptions,
  Repository,
} from "typeorm"
import { FeatureFlagService } from "../feature-flags/feature-flag.service"

@Injectable()
export class DualDatabaseService {
  constructor(
    @InjectDataSource("sqlite") private sqliteDataSource: DataSource,
    @InjectDataSource("postgres") private postgresDataSource: DataSource,
    private featureFlagService: FeatureFlagService
  ) {}

  /**
   * Get the appropriate repository for reading based on feature flag
   */
  async getReadRepository<T>(
    userId: string,
    entity: EntityTarget<T>
  ): Promise<Repository<T>> {
    const useNeon = await this.featureFlagService.getBooleanValue(
      userId,
      "use-neon",
      false
    )

    if (useNeon) {
      console.log(
        `📖 Reading ${entity.toString()} from PostgreSQL for user ${userId}`
      )
      return this.postgresDataSource.getRepository(entity)
    } else {
      console.log(
        `📖 Reading ${entity.toString()} from SQLite for user ${userId}`
      )
      return this.sqliteDataSource.getRepository(entity)
    }
  }

  /**
   * Get both repositories for writing to both databases
   */
  getWriteRepositories<T>(entity: EntityTarget<T>): {
    sqlite: Repository<T>
    postgres: Repository<T>
  } {
    return {
      sqlite: this.sqliteDataSource.getRepository(entity),
      postgres: this.postgresDataSource.getRepository(entity),
    }
  }

  /**
   * Save to both databases
   */
  async dualSave<T>(entity: EntityTarget<T>, data: DeepPartial<T>): Promise<T> {
    const { sqlite, postgres } = this.getWriteRepositories(entity)

    console.log(`💾 Dual write: Saving ${entity.toString()} to both databases`)

    // Save to both databases in parallel
    const [sqliteResult] = await Promise.all([
      sqlite.save(data),
      postgres.save(data),
    ])

    console.log(`✅ Dual write completed for ${entity.toString()}`)

    // Return the SQLite result by default (could be configurable)
    return sqliteResult
  }

  /**
   * Update in both databases
   */
  async dualUpdate<T>(
    entity: EntityTarget<T>,
    criteria: any,
    partialEntity: any
  ): Promise<void> {
    const { sqlite, postgres } = this.getWriteRepositories(entity)

    console.log(
      `🔄 Dual update: Updating ${entity.toString()} in both databases`
    )

    // Update in both databases in parallel
    await Promise.all([
      sqlite.update(criteria, partialEntity),
      postgres.update(criteria, partialEntity),
    ])

    console.log(`✅ Dual update completed for ${entity.toString()}`)
  }

  /**
   * Delete from both databases
   */
  async dualDelete<T>(entity: EntityTarget<T>, criteria: any): Promise<void> {
    const { sqlite, postgres } = this.getWriteRepositories(entity)

    console.log(
      `🗑️  Dual delete: Deleting ${entity.toString()} from both databases`
    )

    // Delete from both databases in parallel
    await Promise.all([sqlite.delete(criteria), postgres.delete(criteria)])

    console.log(`✅ Dual delete completed for ${entity.toString()}`)
  }

  /**
   * Find one record from the appropriate database based on feature flag
   */
  async findOne<T>(
    userId: string,
    entity: EntityTarget<T>,
    options?: FindOneOptions<T>
  ): Promise<T | null> {
    const repo = await this.getReadRepository(userId, entity)
    return repo.findOne(options)
  }

  /**
   * Find many records from the appropriate database based on feature flag
   */
  async findMany<T>(
    userId: string,
    entity: EntityTarget<T>,
    options?: FindManyOptions<T>
  ): Promise<T[]> {
    const repo = await this.getReadRepository(userId, entity)
    return repo.find(options)
  }

  /**
   * Count records from the appropriate database based on feature flag
   */
  async count<T>(
    userId: string,
    entity: EntityTarget<T>,
    options?: FindManyOptions<T>
  ): Promise<number> {
    const repo = await this.getReadRepository(userId, entity)
    return repo.count(options)
  }

  /**
   * Execute a query on the appropriate database based on feature flag
   */
  async query(userId: string, sql: string, parameters?: any[]): Promise<any> {
    const useNeon = await this.featureFlagService.getBooleanValue(
      userId,
      "use-neon",
      false
    )

    if (useNeon) {
      console.log(`🔍 Executing query on PostgreSQL for user ${userId}`)
      return this.postgresDataSource.query(sql, parameters)
    } else {
      console.log(`🔍 Executing query on SQLite for user ${userId}`)
      return this.sqliteDataSource.query(sql, parameters)
    }
  }

  /**
   * Execute a query on both databases (for maintenance operations)
   */
  async dualQuery(
    sql: string,
    parameters?: any[]
  ): Promise<{ sqlite: any; postgres: any }> {
    console.log(`🔧 Executing dual query on both databases`)

    const [sqliteResult, postgresResult] = await Promise.all([
      this.sqliteDataSource.query(sql, parameters),
      this.postgresDataSource.query(sql, parameters),
    ])

    return {
      sqlite: sqliteResult,
      postgres: postgresResult,
    }
  }
}
