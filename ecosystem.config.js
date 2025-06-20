module.exports = {
  apps: [
    {
      name: "shopper-backend",
      script: "dist/main.js",
      cwd: "/root/ecom-backend", // Update this to your actual backend path on the server
      instances: "max", // You can set this to 'max' to use all CPU cores
      exec_mode: "cluster", // Use 'fork' for single instance
      watch: false, // Set to true if you want to watch for file changes
      max_memory_restart: "1G",
      env: {
        NODE_ENV: "production",
        PORT: process.env.PORT || 3001,
        JWT_SECRET: process.env.JWT_SECRET,
        DATABASE_URL: process.env.DATABASE_URL || "./database.sqlite",
        DEVCYCLE_SERVER_SDK_KEY: process.env.DEVCYCLE_SERVER_SDK_KEY,
        DEVCYCLE_CLIENT_SDK_KEY: process.env.DEVCYCLE_CLIENT_SDK_KEY,
      },
      env_development: {
        NODE_ENV: "development",
        PORT: process.env.PORT || 3001,
        JWT_SECRET: process.env.JWT_SECRET,
        DATABASE_URL: process.env.DATABASE_URL || "./database.sqlite",
        DEVCYCLE_SERVER_SDK_KEY: process.env.DEVCYCLE_SERVER_SDK_KEY,
        DEVCYCLE_CLIENT_SDK_KEY: process.env.DEVCYCLE_CLIENT_SDK_KEY,
      },
      log_date_format: "YYYY-MM-DD HH:mm Z",
      error_file: "logs/err.log",
      out_file: "logs/out.log",
      log_file: "logs/combined.log",
      time: true,
      autorestart: true,
      max_restarts: 10,
      min_uptime: "10s",
      restart_delay: 4000,
    },
    // OpenTelemetry enabled version
    {
      name: "shopper-backend-otel",
      script: "dist/main.js",
      node_args: "--require ./dist/otelSetup.js",
      cwd: "/root/ecom-backend", // Update this to your actual backend path on the server
      instances: "max",
      exec_mode: "cluster",
      watch: false,
      max_memory_restart: "1G",
      env: {
        NODE_ENV: "production",
        PORT: process.env.PORT || 3001,
        JWT_SECRET: process.env.JWT_SECRET,
        DATABASE_URL: process.env.DATABASE_URL || "./database.sqlite",
        DEVCYCLE_SERVER_SDK_KEY: process.env.DEVCYCLE_SERVER_SDK_KEY,
        DEVCYCLE_CLIENT_SDK_KEY: process.env.DEVCYCLE_CLIENT_SDK_KEY,
        // OpenTelemetry configuration
        OTEL_SERVICE_NAME: process.env.OTEL_SERVICE_NAME || "shopper-backend",
        OTEL_SERVICE_VERSION: process.env.OTEL_SERVICE_VERSION || "1.0.0",
        DYNATRACE_ENV_URL: process.env.DYNATRACE_ENV_URL,
        DYNATRACE_API_TOKEN: process.env.DYNATRACE_API_TOKEN,
      },
      env_development: {
        NODE_ENV: "development",
        PORT: process.env.PORT || 3001,
        JWT_SECRET: process.env.JWT_SECRET,
        DATABASE_URL: process.env.DATABASE_URL || "./database.sqlite",
        DEVCYCLE_SERVER_SDK_KEY: process.env.DEVCYCLE_SERVER_SDK_KEY,
        DEVCYCLE_CLIENT_SDK_KEY: process.env.DEVCYCLE_CLIENT_SDK_KEY,
        // OpenTelemetry configuration for development
        OTEL_SERVICE_NAME:
          process.env.OTEL_SERVICE_NAME || "shopper-backend-dev",
        OTEL_SERVICE_VERSION: process.env.OTEL_SERVICE_VERSION || "1.0.0",
        USE_LOCAL_OTLP: process.env.USE_LOCAL_OTLP || "true",
        LOCAL_OTLP_PORT: process.env.LOCAL_OTLP_PORT || "14499",
      },
      log_date_format: "YYYY-MM-DD HH:mm Z",
      error_file: "logs/otel-err.log",
      out_file: "logs/otel-out.log",
      log_file: "logs/otel-combined.log",
      time: true,
      autorestart: true,
      max_restarts: 10,
      min_uptime: "10s",
      restart_delay: 4000,
    },
    {
      name: "shopper-backend-local-otlp",
      script: "dist/main.js",
      node_args: "--require ./dist/otelSetup.js",
      cwd: "/root/ecom-backend", // Update this to your actual backend path on the server
      instances: 1, // Single instance for local development
      exec_mode: "fork",
      watch: false,
      max_memory_restart: "1G",
      env: {
        NODE_ENV: "development",
        PORT: process.env.PORT || 3001,
        JWT_SECRET: process.env.JWT_SECRET,
        DATABASE_URL: process.env.DATABASE_URL || "./database.sqlite",
        DEVCYCLE_SERVER_SDK_KEY: process.env.DEVCYCLE_SERVER_SDK_KEY,
        DEVCYCLE_CLIENT_SDK_KEY: process.env.DEVCYCLE_CLIENT_SDK_KEY,
        // Local OTLP configuration
        OTEL_SERVICE_NAME:
          process.env.OTEL_SERVICE_NAME || "shopper-backend-local",
        OTEL_SERVICE_VERSION: process.env.OTEL_SERVICE_VERSION || "1.0.0",
        USE_LOCAL_OTLP: process.env.USE_LOCAL_OTLP || "true",
        LOCAL_OTLP_PORT: process.env.LOCAL_OTLP_PORT || "14499",
      },
      log_date_format: "YYYY-MM-DD HH:mm Z",
      error_file: "logs/local-otlp-err.log",
      out_file: "logs/local-otlp-out.log",
      log_file: "logs/local-otlp-combined.log",
      time: true,
      autorestart: true,
      max_restarts: 10,
      min_uptime: "10s",
      restart_delay: 4000,
    },
  ],
}
