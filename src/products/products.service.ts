import {
  Injectable,
  InternalServerErrorException,
  NotFoundException,
} from "@nestjs/common"
import { Product } from "../entities/product.entity"
import { CreateProductDto } from "./dto/create-product.dto"
import { UpdateProductDto } from "./dto/update-product.dto"
import { DualDatabaseService } from "../database/dual-database.service"
import { FeatureFlagService } from "../feature-flags/feature-flag.service"

@Injectable()
export class ProductsService {
  constructor(
    private dualDatabaseService: DualDatabaseService,
    private featureFlagService: FeatureFlagService
  ) {}

  async create(createProductDto: CreateProductDto): Promise<Product> {
    // Create a product entity from the DTO
    const product = new Product()
    Object.assign(product, createProductDto)

    // Dual write: save to both databases
    return await this.dualDatabaseService.dualSave(Product, product)
  }

  async findAll(userId: string = "system"): Promise<Product[]> {
    console.log(`📦 Products.findAll called for userId: ${userId}`)

    // Check the new-flow feature flag for metric tracking
    const isNewFlowEnabled = await this.featureFlagService.getBooleanValue(
      userId,
      "new-flow",
      false
    )

    console.log(
      `🎛️ Feature flag 'new-flow' for user ${userId}: ${isNewFlowEnabled}`
    )

    if (isNewFlowEnabled) {
      // Generate random number between 0 and 1
      const randomValue = Math.random()

      // Throw error in 10% of cases for metric tracking
      if (randomValue < 0.1) {
        console.error(`🚨 Error - userId: ${userId}, random: ${randomValue}`)
        throw new InternalServerErrorException(
          "Service temporarily unavailable - new flow processing error"
        )
      }
      console.log(
        `✅ New flow passed for user ${userId} (random: ${randomValue.toFixed(3)})`
      )
    }

    // Read from the database determined by feature flag
    const products = await this.dualDatabaseService.findMany(userId, Product)
    console.log(`📊 Retrieved ${products.length} products for user ${userId}`)

    return products
  }

  async findOne(id: string, userId: string = "system"): Promise<Product> {
    // Read from the database determined by feature flag
    const product = await this.dualDatabaseService.findOne(userId, Product, {
      where: { id },
    })
    if (!product) {
      throw new NotFoundException("Product not found")
    }
    return product
  }

  async update(
    id: string,
    updateProductDto: UpdateProductDto,
    userId: string = "system"
  ): Promise<Product> {
    // Dual write: update in both databases
    await this.dualDatabaseService.dualUpdate(Product, id, updateProductDto)
    const updatedProduct = await this.findOne(id, userId)
    return updatedProduct
  }

  async remove(id: string): Promise<void> {
    // Dual write: delete from both databases
    await this.dualDatabaseService.dualDelete(Product, id)
  }
}
