import {
  Body,
  Controller,
  Delete,
  Get,
  Param,
  Patch,
  Post,
  UseGuards,
} from "@nestjs/common"
import { ApiResponse, ApiTags } from "@nestjs/swagger"
import { Observable } from "rxjs"
import { switchMap } from "rxjs/operators"
import { ProductsService } from "./products.service"
import { CreateProductDto } from "./dto/create-product.dto"
import { UpdateProductDto } from "./dto/update-product.dto"
import { JwtAuthGuard } from "../auth/guards/jwt-auth.guard"
import { RolesGuard } from "../auth/guards/roles.guard"
import { Roles } from "../auth/decorators/roles.decorator"
import { Username } from "../auth/decorators/username.decorator"
import {
  BooleanFeatureFlag,
  EvaluationDetails,
  RequireFlagsEnabled,
} from "@openfeature/nestjs-sdk"

@ApiTags("products")
@Controller("products")
export class ProductsController {
  constructor(private readonly productsService: ProductsService) {}

  @Post()
  @UseGuards(JwtAuthGuard, RolesGuard)
  @Roles("admin")
  create(@Body() createProductDto: CreateProductDto) {
    return this.productsService.create(createProductDto)
  }

  @Get()
  @ApiResponse({
    status: 200,
    description: "Returns all products successfully",
  })
  @ApiResponse({
    status: 500,
    description: "Internal server error",
  })
  findAll(@Username() username: string) {
    // Use username for feature flag targeting
    return this.productsService.findAll(username)
  }

  // Example using the new OpenFeature NestJS SDK decorator approach
  @Get("with-feature-flag")
  @ApiResponse({
    status: 200,
    description: "Returns products with feature flag evaluation",
  })
  findAllWithFeatureFlag(
    @Username() username: string,
    @BooleanFeatureFlag({
      flagKey: "new-flow",
      defaultValue: false,
    })
    newFlowFeature: Observable<EvaluationDetails<boolean>>
  ): Observable<any> {
    return newFlowFeature.pipe(
      switchMap((flagDetails) => {
        console.log(
          `🎛️ Feature flag 'new-flow' evaluated via decorator: ${flagDetails.value} for user ${username}`
        )

        if (flagDetails.value) {
          console.log("✅ New flow enabled via decorator")
        }

        // Call the service method and combine with flag details
        return this.productsService.findAll(username).then((products) => ({
          products,
          featureFlags: {
            newFlow: flagDetails.value,
            reason: flagDetails.reason,
            variant: flagDetails.variant,
          },
        }))
      })
    )
  }

  // Example using RequireFlagsEnabled decorator
  @Get("premium-only")
  @RequireFlagsEnabled({
    flags: [{ flagKey: "premium-features" }],
  })
  @ApiResponse({
    status: 200,
    description:
      "Premium feature - only available when feature flag is enabled",
  })
  @ApiResponse({
    status: 403,
    description: "Feature not enabled",
  })
  getPremiumProducts(@Username() username: string) {
    // This endpoint will only be accessible when the 'premium-features' flag is enabled
    return this.productsService.findAll(username).then((products) => ({
      products: products.slice(0, 5), // Premium users get first 5 products
      isPremium: true,
    }))
  }

  @Get(":id")
  findOne(@Param("id") id: string, @Username() username: string) {
    // Use username for feature flag targeting
    return this.productsService.findOne(id, username)
  }

  @Patch(":id")
  @UseGuards(JwtAuthGuard, RolesGuard)
  @Roles("admin")
  update(
    @Param("id") id: string,
    @Body() updateProductDto: UpdateProductDto,
    @Username() username: string
  ) {
    // Use username for feature flag targeting
    return this.productsService.update(id, updateProductDto, username)
  }

  @Delete(":id")
  @UseGuards(JwtAuthGuard, RolesGuard)
  @Roles("admin")
  remove(@Param("id") id: string) {
    return this.productsService.remove(id)
  }
}
