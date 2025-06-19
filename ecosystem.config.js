module.exports = {
  apps: [
    {
      name: "shopper-backend",
      script: "dist/main.js",
      cwd: "/path/to/your/backend", // Update this to your actual backend path on the server
      instances: "max", // You can set this to 'max' to use all CPU cores
      exec_mode: "cluster", // Use 'fork' for single instance
      watch: false, // Set to true if you want to watch for file changes
      max_memory_restart: "1G",
      env: {
        NODE_ENV: "production",
        PORT: 3001,
        JWT_SECRET: "your-production-jwt-secret",
        DATABASE_URL: "./database.sqlite",
        DEVCYCLE_SERVER_SDK_KEY: "your-production-devcycle-server-key",
        DEVCYCLE_CLIENT_SDK_KEY: "your-production-devcycle-client-key",
      },
      env_development: {
        NODE_ENV: "development",
        PORT: 3001,
        JWT_SECRET: "your-dev-jwt-secret",
        DATABASE_URL: "./database.sqlite",
        DEVCYCLE_SERVER_SDK_KEY: "your-dev-devcycle-server-key",
        DEVCYCLE_CLIENT_SDK_KEY: "your-dev-devcycle-client-key",
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
  ],
}
