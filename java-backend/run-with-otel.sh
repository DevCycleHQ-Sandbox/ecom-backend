#!/bin/bash
set -e  # Exit on any command failure

# Java Backend Run Script with OpenTelemetry
echo "🚀 Starting Java Backend Server with OpenTelemetry..."

# Cleanup function for graceful shutdown
cleanup() {
    echo ""
    echo "🧹 Cleaning up..."
    # Kill any background processes if needed
    jobs -p | xargs -r kill 2>/dev/null || true
}

# Set up trap for cleanup on exit
trap cleanup EXIT INT TERM

# Load environment variables if .env file exists
if [ -f .env ]; then
    echo "📋 Loading environment variables from .env file..."
    # Safely load .env file, handling multi-line values
    while IFS= read -r line || [ -n "$line" ]; do
        # Skip empty lines and comments
        [[ -z "$line" || "$line" =~ ^[[:space:]]*# ]] && continue
        
        # Handle multi-line values by continuing to read until we find a complete assignment
        if [[ "$line" =~ ^[A-Za-z_][A-Za-z0-9_]*= ]]; then
            # If we have a pending multi-line variable, export it
            if [ -n "$current_var" ] && [ -n "$current_value" ]; then
                export "$current_var"="$current_value"
            fi
            
            # Start new variable
            current_var="${line%%=*}"
            current_value="${line#*=}"
        else
            # Continue multi-line value
            current_value="$current_value$line"
        fi
    done < .env
    
    # Export the last variable if exists
    if [ -n "$current_var" ] && [ -n "$current_value" ]; then
        export "$current_var"="$current_value"
    fi
    
    # Clean up
    unset current_var current_value
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

# OpenTelemetry Logback Bridge Configuration
export OTEL_LOGS_EXPORTER="otlp"
export OTEL_INSTRUMENTATION_LOGBACK_APPENDER_ENABLED="true"


# Only set endpoint if we have one configured
if [ -n "$OTLP_ENDPOINT" ]; then
    export OTEL_EXPORTER_OTLP_ENDPOINT="$OTLP_ENDPOINT"
    if [ -n "$OTLP_HEADERS" ]; then
        export OTEL_EXPORTER_OTLP_HEADERS="$OTLP_HEADERS"
    fi
    export OTEL_EXPORTER_OTLP_METRICS_TEMPORALITY_PREFERENCE="delta"
    export OTEL_EXPORTER_OTLP_COMPRESSION="gzip"
    
    # Enable all telemetry signals including logs
    export OTEL_TRACES_EXPORTER="otlp"
    export OTEL_METRICS_EXPORTER="otlp"  
    export OTEL_LOGS_EXPORTER="otlp"
    echo "📊 Enabled OTLP export for traces, metrics, and logs with bridge"
else
    # Disable OTLP exporter if no endpoint configured
    export OTEL_TRACES_EXPORTER="none"
    export OTEL_METRICS_EXPORTER="none"
    export OTEL_LOGS_EXPORTER="none"
    echo "⚠️  Telemetry export disabled (no endpoint configured)"
fi

echo "🔧 Configuration:"
echo "  Port: $PORT"
echo "  Environment: $NODE_ENV"
echo "  Database: $DATABASE_URL"
echo "  Frontend URL: $FRONTEND_URL"
echo "  OTLP Endpoint: ${OTLP_ENDPOINT:-disabled}"
echo ""

# Check dependencies
echo "🔍 Checking dependencies..."

if ! command -v mvn &> /dev/null; then
    echo "❌ Maven is not installed. Please install Maven first."
    echo "   On macOS: brew install maven"
    echo "   On Ubuntu: sudo apt-get install maven"
    exit 1
fi

if ! command -v java &> /dev/null; then
    echo "❌ Java is not installed. Please install Java first."
    echo "   On macOS: brew install openjdk"
    echo "   On Ubuntu: sudo apt-get install default-jdk"
    exit 1
fi

# Check Java version
JAVA_VERSION=$(java -version 2>&1 | head -n1 | awk -F '"' '{print $2}' | awk -F '.' '{print $1}')
if [ "$JAVA_VERSION" -lt 17 ]; then
    echo "⚠️  Warning: Java $JAVA_VERSION detected. Java 17+ is recommended for Spring Boot 3.x"
fi

# Check if pom.xml exists
if [ ! -f "pom.xml" ]; then
    echo "❌ pom.xml not found. Make sure you're in the correct directory."
    exit 1
fi

# Check if OpenTelemetry agent exists
if [ ! -f "opentelemetry-javaagent.jar" ]; then
    echo "📥 Downloading OpenTelemetry Java Agent..."
    if ! curl -L -o opentelemetry-javaagent.jar https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/latest/download/opentelemetry-javaagent.jar; then
        echo "❌ Failed to download OpenTelemetry agent."
        exit 1
    fi
    echo "✅ OpenTelemetry agent downloaded successfully."
fi

echo "✅ Starting with OpenTelemetry instrumentation..."
echo ""

# Run with OpenTelemetry Java Agent
echo "💡 Running Maven with Spring Boot and OpenTelemetry agent..."
mvn clean spring-boot:run \
    -Dspring-boot.run.jvmArguments="-javaagent:opentelemetry-javaagent.jar" \
    -Dspring-boot.run.profiles=${NODE_ENV} \
    -Dserver.port=${PORT}

echo "🛑 Java Backend Server stopped." 
