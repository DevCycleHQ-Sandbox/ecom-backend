import { Injectable, NotFoundException } from "@nestjs/common"
import { InjectRepository } from "@nestjs/typeorm"
import { Repository } from "typeorm"
import { Product } from "../entities/product.entity"
import { CreateProductDto } from "./dto/create-product.dto"
import { UpdateProductDto } from "./dto/update-product.dto"
import { DatabaseService } from "../database/database.service"
import { DatabaseManagerService } from "../database/database-manager.service"

@Injectable()
export class ProductsService {
  constructor(
    @InjectRepository(Product)
    private productRepository: Repository<Product>,
    private databaseService: DatabaseService,
    private databaseManagerService: DatabaseManagerService
  ) {}

  async create(createProductDto: CreateProductDto): Promise<Product> {
    const product = this.productRepository.create(createProductDto)
    return await this.productRepository.save(product)
  }

  async findAll(): Promise<Product[]> {
    return await this.productRepository.find()
  }

  async findOne(id: string): Promise<Product> {
    const product = await this.productRepository.findOne({ where: { id } })
    if (!product) {
      throw new NotFoundException("Product not found")
    }
    return product
  }

  async update(
    id: string,
    updateProductDto: UpdateProductDto
  ): Promise<Product> {
    await this.productRepository.update(id, updateProductDto)
    const updatedProduct = await this.findOne(id)
    return updatedProduct
  }

  async remove(id: string): Promise<void> {
    const result = await this.productRepository.delete(id)
    if (result.affected === 0) {
      throw new NotFoundException("Product not found")
    }
  }

  /**
   * Search products using database-specific methods
   * Demonstrates feature flag usage for database switching
   */
  async searchProducts(
    searchTerm: string,
    userId?: string
  ): Promise<Product[]> {
    // Log database usage for analytics
    if (userId) {
      await this.databaseManagerService.logDatabaseUsage(
        userId,
        "product_search"
      )
    }

    const dbType = this.databaseService.getCurrentDatabaseType()

    if (dbType === "postgres") {
      // Use PostgreSQL full-text search if available
      try {
        return await this.productRepository
          .createQueryBuilder("product")
          .where("product.search_vector @@ plainto_tsquery(:search)", {
            search: searchTerm,
          })
          .orWhere("product.name ILIKE :searchPattern", {
            searchPattern: `%${searchTerm}%`,
          })
          .orWhere("product.description ILIKE :searchPattern", {
            searchPattern: `%${searchTerm}%`,
          })
          .getMany()
      } catch {
        // Fallback to basic search if full-text search is not set up
        return await this.basicSearch(searchTerm)
      }
    } else {
      // Use SQLite basic search
      return await this.basicSearch(searchTerm)
    }
  }

  private async basicSearch(searchTerm: string): Promise<Product[]> {
    return await this.productRepository
      .createQueryBuilder("product")
      .where("product.name LIKE :searchPattern", {
        searchPattern: `%${searchTerm}%`,
      })
      .orWhere("product.description LIKE :searchPattern", {
        searchPattern: `%${searchTerm}%`,
      })
      .getMany()
  }

  /**
   * Find products with database-aware pagination
   */
  async findWithPagination(
    page: number = 1,
    limit: number = 10,
    userId?: string
  ): Promise<{ products: Product[]; total: number }> {
    if (userId) {
      await this.databaseManagerService.logDatabaseUsage(
        userId,
        "product_pagination"
      )
    }

    const [products, total] = await this.productRepository.findAndCount({
      skip: (page - 1) * limit,
      take: limit,
      order: {
        created_at: "DESC",
      },
    })

    return { products, total }
  }
}
