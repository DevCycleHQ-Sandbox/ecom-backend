import { NestFactory } from "@nestjs/core"
import { ValidationPipe } from "@nestjs/common"
import { AppModule } from "./app.module"
import { seedDatabase } from "./database/seed"

async function bootstrap() {
  const app = await NestFactory.create(AppModule)

  // Global validation pipe
  app.useGlobalPipes(new ValidationPipe())

  // CORS configuration
  app.enableCors({
    origin: process.env.FRONTEND_URL || "http://localhost:3000",
    credentials: true,
  })

  // Global prefix for all routes
  app.setGlobalPrefix("api")

  const port = process.env.PORT || 3001
  await app.listen(port)

  console.log(`🚀 Server running on port ${port}`)
  console.log(`📱 Environment: ${process.env.NODE_ENV || "development"}`)

  // Seed database with sample data
  try {
    const { DataSource } = await import("typeorm")
    const dataSource = app.get(DataSource)
    await seedDatabase(dataSource)
  } catch (error) {
    console.log("⚠️  Error seeding database:", error.message)
  }
}

bootstrap()
