#!/bin/bash

# Java Backend Run Script
echo "🚀 Starting Java Backend Server..."

# Load environment variables if .env file exists
if [ -f .env ]; then
    echo "📋 Loading environment variables from .env file..."
    export $(cat .env | xargs)
fi

# Set default values
export PORT=${PORT:-3002}
export NODE_ENV=${NODE_ENV:-development}
export JWT_SECRET=${JWT_SECRET:-your-jwt-secret-key-here-make-it-really-long-and-secure-enough-for-jwt-hmac-sha-algorithms-minimum-256-bits-required}
export DATABASE_URL=${DATABASE_URL:-jdbc:sqlite:./database.sqlite}
export FRONTEND_URL=${FRONTEND_URL:-http://localhost:3000}

echo "🔧 Environment Configuration:"
echo "  Port: $PORT"
echo "  Environment: $NODE_ENV"
echo "  Database: $DATABASE_URL"
echo "  Frontend URL: $FRONTEND_URL"
echo ""

# Check if Maven is installed
if ! command -v mvn &> /dev/null; then
    echo "❌ Maven is not installed. Please install Maven to run this application."
    exit 1
fi

# Check if Java is installed
if ! command -v java &> /dev/null; then
    echo "❌ Java is not installed. Please install Java 17 or later."
    exit 1
fi

# Check Java version
JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | awk -F '.' '{print $1}')
if [ "$JAVA_VERSION" -lt 17 ]; then
    echo "❌ Java 17 or later is required. Current version: $JAVA_VERSION"
    exit 1
fi

echo "✅ Java version: $(java -version 2>&1 | head -n 1)"
echo "✅ Maven version: $(mvn -version | head -n 1)"
echo ""

# Build the project if target directory doesn't exist
if [ ! -d "target" ]; then
    echo "🔨 Building the project..."
    mvn clean compile
    if [ $? -ne 0 ]; then
        echo "❌ Build failed!"
        exit 1
    fi
fi

# Run the application
echo "🏃 Running the application..."
mvn spring-boot:run

echo "🛑 Java Backend Server stopped."