import { Injectable } from "@nestjs/common"
import { InjectDataSource } from "@nestjs/typeorm"
import { DataSource } from "typeorm"
import { User } from "../entities/user.entity"
import { Product } from "../entities/product.entity"
import { CartItem } from "../entities/cart-item.entity"
import { Order } from "../entities/order.entity"
import { OrderItem } from "../entities/order-item.entity"

export interface SyncResult {
  entity: string
  synced: number
  errors: string[]
}

@Injectable()
export class SyncService {
  constructor(
    @InjectDataSource("sqlite") private sqliteDataSource: DataSource,
    @InjectDataSource("postgres") private postgresDataSource: DataSource
  ) {}

  async syncAllData(): Promise<SyncResult[]> {
    const results: SyncResult[] = []

    console.log("🔄 Starting database sync from SQLite to PostgreSQL...")

    try {
      // First, clear all PostgreSQL tables in reverse dependency order to avoid foreign key issues
      await this.clearPostgresTables()

      // Sync in order to handle foreign key relationships
      const syncOrder = [
        { entity: User, name: "Users" },
        { entity: Product, name: "Products" },
        { entity: Order, name: "Orders" },
        { entity: OrderItem, name: "OrderItems" },
        { entity: CartItem, name: "CartItems" },
      ]

      for (const { entity, name } of syncOrder) {
        try {
          console.log(`📋 Syncing ${name}...`)
          const result = await this.syncEntity(entity, name, false) // Don't clear individually
          results.push(result)
          console.log(`✅ ${name}: ${result.synced} records synced`)
        } catch (error) {
          console.error(`❌ Error syncing ${name}:`, error.message)
          results.push({
            entity: name,
            synced: 0,
            errors: [error.message],
          })
        }
      }
    } catch (error) {
      console.error("❌ Error during bulk clear:", error.message)
      return [
        {
          entity: "ALL",
          synced: 0,
          errors: [`Failed to clear tables: ${error.message}`],
        },
      ]
    }

    console.log("🎉 Database sync completed!")
    return results
  }

  private async clearPostgresTables(): Promise<void> {
    console.log("🧹 Clearing PostgreSQL tables in safe order...")

    // Clear in reverse dependency order (child tables first)
    const clearOrder = [
      "cart_items",
      "order_items",
      "orders",
      "products",
      "users",
    ]

    try {
      for (const tableName of clearOrder) {
        try {
          // Try TRUNCATE with CASCADE first
          await this.postgresDataSource.query(
            `TRUNCATE TABLE "${tableName}" RESTART IDENTITY CASCADE`
          )
          console.log(`🗑️  Cleared table: ${tableName}`)
        } catch (truncateError) {
          console.warn(
            `⚠️  TRUNCATE failed for ${tableName}, trying DELETE:`,
            truncateError.message
          )
          // Fallback to DELETE
          try {
            await this.postgresDataSource.query(`DELETE FROM "${tableName}"`)
            console.log(`🗑️  Cleared table with DELETE: ${tableName}`)
          } catch (deleteError) {
            console.warn(
              `⚠️  DELETE also failed for ${tableName}:`,
              deleteError.message
            )
            // Final fallback - try to use repository delete
            const entities = {
              cart_items: CartItem,
              order_items: OrderItem,
              orders: Order,
              products: Product,
              users: User,
            }

            const EntityClass = entities[tableName]
            if (EntityClass) {
              const repo = this.postgresDataSource.getRepository(EntityClass)
              await repo.delete({})
              console.log(
                `🗑️  Cleared table with repository delete: ${tableName}`
              )
            }
          }
        }
      }
    } catch (error) {
      throw error
    }
  }

  private async syncEntity(
    EntityClass: any,
    entityName: string,
    shouldClear: boolean = true
  ): Promise<SyncResult> {
    const errors: string[] = []
    let syncedCount = 0

    try {
      // Get all records from SQLite
      const sqliteRepo = this.sqliteDataSource.getRepository(EntityClass)
      const postgresRepo = this.postgresDataSource.getRepository(EntityClass)

      const sqliteRecords = await sqliteRepo.find()

      if (sqliteRecords.length === 0) {
        return { entity: entityName, synced: 0, errors: [] }
      }

      // Only clear if requested (for individual entity syncs)
      if (shouldClear) {
        try {
          await postgresRepo.clear()
        } catch {
          // If clear fails due to foreign keys, try DELETE
          await postgresRepo.delete({})
        }
      }

      // Insert all records into PostgreSQL
      for (const record of sqliteRecords) {
        try {
          // Remove any auto-generated fields that might cause conflicts
          const { created_at, updated_at, ...recordData } = record as any

          // Create new entity instance
          const newEntity = postgresRepo.create({
            ...recordData,
            // Preserve original timestamps if they exist
            ...(created_at && { created_at }),
            ...(updated_at && { updated_at }),
          })

          await postgresRepo.save(newEntity)
          syncedCount++
        } catch (error) {
          errors.push(`Failed to sync record ${record.id}: ${error.message}`)
        }
      }

      return {
        entity: entityName,
        synced: syncedCount,
        errors,
      }
    } catch (error) {
      return {
        entity: entityName,
        synced: 0,
        errors: [error.message],
      }
    }
  }

  async syncSpecificEntity(entityName: string): Promise<SyncResult> {
    const entityMap = {
      users: { entity: User, name: "Users" },
      products: { entity: Product, name: "Products" },
      orders: { entity: Order, name: "Orders" },
      orderitems: { entity: OrderItem, name: "OrderItems" },
      cartitems: { entity: CartItem, name: "CartItems" },
    }

    const entityConfig = entityMap[entityName.toLowerCase()]
    if (!entityConfig) {
      throw new Error(`Unknown entity: ${entityName}`)
    }

    console.log(`📋 Syncing specific entity: ${entityConfig.name}...`)
    return await this.syncEntity(entityConfig.entity, entityConfig.name, true)
  }

  async getDatabaseStats(): Promise<{ sqlite: any; postgres: any }> {
    const entities = [
      { entity: User, name: "users" },
      { entity: Product, name: "products" },
      { entity: Order, name: "orders" },
      { entity: OrderItem, name: "orderItems" },
      { entity: CartItem, name: "cartItems" },
    ]

    const sqliteStats = {}
    const postgresStats = {}

    for (const { entity, name } of entities) {
      try {
        const sqliteCount = await this.sqliteDataSource
          .getRepository(entity)
          .count()
        const postgresCount = await this.postgresDataSource
          .getRepository(entity)
          .count()

        sqliteStats[name] = sqliteCount
        postgresStats[name] = postgresCount
      } catch (error) {
        sqliteStats[name] = `Error: ${error.message}`
        postgresStats[name] = `Error: ${error.message}`
      }
    }

    return { sqlite: sqliteStats, postgres: postgresStats }
  }
}
