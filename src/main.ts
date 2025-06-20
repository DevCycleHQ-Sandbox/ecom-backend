import { NestFactory } from "@nestjs/core"
import { ValidationPipe } from "@nestjs/common"
import { AppModule } from "./app.module"
import { seedDatabase } from "./database/seed"
import { getDataSourceToken } from "@nestjs/typeorm"
import { LoggingInterceptor } from "./interceptors/logging.interceptor"

async function bootstrap() {
  const app = await NestFactory.create(AppModule)

  // Global validation pipe
  app.useGlobalPipes(new ValidationPipe())

  // Global logging interceptor
  app.useGlobalInterceptors(new LoggingInterceptor())

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
    // Use the named SQLite connection for seeding
    const sqliteDataSource = app.get(getDataSourceToken("sqlite"))
    await seedDatabase(sqliteDataSource)
    console.log("✅ Database seeded successfully")
  } catch (error) {
    console.log("⚠️  Error seeding database:", error.message)
  }
}

bootstrap()
