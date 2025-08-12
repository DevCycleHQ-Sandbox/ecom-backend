#!/bin/bash

# Java Backend Run Script with OpenTelemetry
echo "🚀 Starting Java Backend Server with OpenTelemetry..."

# Load environment variables if .env file exists
if [ -f .env ]; then
    echo "📋 Loading environment variables from .env file..."
    export $(cat .env | xargs)
fi

# Set default values for application
export PORT=${PORT:-3002}
export NODE_ENV=${NODE_ENV:-development}
export JWT_SECRET=${JWT_SECRET:-your-jwt-secret-key-here-make-it-really-long-and-secure-enough}
export DATABASE_URL=${DATABASE_URL:-jdbc:sqlite:./database.sqlite}
export FRONTEND_URL=${FRONTEND_URL:-http://localhost:3000}

# Set default telemetry values
export USE_LOCAL_OTLP=${USE_LOCAL_OTLP:-false}
export LOCAL_OTLP_PORT=${LOCAL_OTLP_PORT:-14499}
export DYNATRACE_ENV_URL=${DYNATRACE_ENV_URL:-https://dynatrace.com}
export DYNATRACE_API_TOKEN=${DYNATRACE_API_TOKEN:-}

# Determine the correct OTLP endpoint
if [ "$USE_LOCAL_OTLP" = "true" ]; then
    OTLP_ENDPOINT="http://localhost:${LOCAL_OTLP_PORT}/otlp"
    OTLP_HEADERS=""
    echo "📡 Using LOCAL OTLP endpoint: $OTLP_ENDPOINT"
elif [ -n "$DYNATRACE_ENV_URL" ] && [ -n "$DYNATRACE_API_TOKEN" ]; then
    OTLP_ENDPOINT="${DYNATRACE_ENV_URL}/api/v2/otlp"
    OTLP_HEADERS="Authorization=Api-Token ${DYNATRACE_API_TOKEN}"
    echo "📡 Using DYNATRACE OTLP endpoint: $OTLP_ENDPOINT"
else
    echo "⚠️  No telemetry endpoint configured. Set either:"
    echo "   - USE_LOCAL_OTLP=true for local OneAgent"
    echo "   - DYNATRACE_ENV_URL and DYNATRACE_API_TOKEN for direct Dynatrace"
    echo "   Proceeding without telemetry export..."
    OTLP_ENDPOINT=""
    OTLP_HEADERS=""
fi

# OpenTelemetry configuration
export OTEL_SERVICE_NAME="java-backend"
export OTEL_SERVICE_VERSION="1.0.0"
export OTEL_RESOURCE_ATTRIBUTES="service.name=java-backend,service.version=1.0.0,deployment.environment=${NODE_ENV}"

# Only set endpoint if we have one configured
if [ -n "$OTLP_ENDPOINT" ]; then
    export OTEL_EXPORTER_OTLP_ENDPOINT="$OTLP_ENDPOINT"
    if [ -n "$OTLP_HEADERS" ]; then
        export OTEL_EXPORTER_OTLP_HEADERS="$OTLP_HEADERS"
    fi
    export OTEL_EXPORTER_OTLP_METRICS_TEMPORALITY_PREFERENCE="delta"
    export OTEL_EXPORTER_OTLP_COMPRESSION="gzip"
else
    # Disable OTLP exporter if no endpoint configured
    export OTEL_TRACES_EXPORTER="none"
    export OTEL_METRICS_EXPORTER="none"
    export OTEL_LOGS_EXPORTER="none"
fi

echo "🔧 Configuration:"
echo "  Port: $PORT"
echo "  Environment: $NODE_ENV"
echo "  Database: $DATABASE_URL"
echo "  Frontend URL: $FRONTEND_URL"
echo "  OTLP Endpoint: ${OTLP_ENDPOINT:-disabled}"
echo ""

# Check dependencies
if ! command -v mvn &> /dev/null; then
    echo "❌ Maven is not installed."
    exit 1
fi

if ! command -v java &> /dev/null; then
    echo "❌ Java is not installed."
    exit 1
fi

# Check if OpenTelemetry agent exists
if [ ! -f "opentelemetry-javaagent.jar" ]; then
    echo "📥 Downloading OpenTelemetry Java Agent..."
    curl -L -o opentelemetry-javaagent.jar https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/latest/download/opentelemetry-javaagent.jar
fi

echo "✅ Starting with OpenTelemetry instrumentation..."
echo ""

# Run with OpenTelemetry Java Agent
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-javaagent:opentelemetry-javaagent.jar"

echo "🛑 Java Backend Server stopped." 
