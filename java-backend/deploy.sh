#!/bin/bash

# OneAgent SDK Java Backend Deployment Script
echo "🚀 Deploying OneAgent SDK Java Backend Application"

# Configuration
APP_NAME="java-backend"
APP_VERSION="1.0.0"
APP_PORT="3002"
DATA_DIR="./data"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

print_status() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Check if Java is installed
check_java() {
    print_status "Checking Java installation..."
    if command -v java &> /dev/null; then
        JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2)
        print_success "Java found: $JAVA_VERSION"
        
        # Check if Java 17+
        MAJOR_VERSION=$(echo $JAVA_VERSION | cut -d'.' -f1)
        if [ "$MAJOR_VERSION" -lt 17 ]; then
            print_error "Java 17 or higher is required. Found: $JAVA_VERSION"
            exit 1
        fi
    else
        print_error "Java not found. Please install Java 17 or higher."
        echo "Ubuntu/Debian: sudo apt install openjdk-17-jre"
        echo "CentOS/RHEL: sudo yum install java-17-openjdk"
        exit 1
    fi
}

# Check if OneAgent is installed
check_oneagent() {
    print_status "Checking Dynatrace OneAgent installation..."
    
    # Check for OneAgent on Linux
    if [ -f "/opt/dynatrace/oneagent/agent/lib64/liboneagentproc.so" ]; then
        print_success "OneAgent found on Linux system"
    elif systemctl is-active --quiet oneagent 2>/dev/null; then
        print_success "OneAgent service is running"
    else
        print_warning "OneAgent not detected!"
        echo ""
        echo "OneAgent is required for telemetry collection. To install:"
        echo "1. Go to your Dynatrace environment"
        echo "2. Navigate to 'Deploy Dynatrace' → 'Start installation'"
        echo "3. Follow the installation instructions for your OS"
        echo ""
        read -p "Continue without OneAgent? (y/N): " -n 1 -r
        echo
        if [[ ! $REPLY =~ ^[Yy]$ ]]; then
            exit 1
        fi
    fi
}

# Create necessary directories
setup_directories() {
    print_status "Setting up directories..."
    mkdir -p "$DATA_DIR"
    print_success "Data directory created: $DATA_DIR"
}

# Check for JAR file
check_jar() {
    JAR_FILE="target/${APP_NAME}-${APP_VERSION}.jar"
    if [ ! -f "$JAR_FILE" ]; then
        print_error "JAR file not found: $JAR_FILE"
        echo "Please build the application first:"
        echo "mvn clean package"
        exit 1
    fi
    print_success "JAR file found: $JAR_FILE"
}

# Set environment variables
setup_environment() {
    print_status "Setting up environment variables..."
    
    # Create .env file if it doesn't exist
    if [ ! -f ".env" ]; then
        cat > .env << EOF
# Application Configuration
NODE_ENV=production
PORT=${APP_PORT}

# Database Configuration
DATABASE_URL=./data/database.sqlite
SECONDARY_DATABASE_ENABLED=false

# JWT Configuration (CHANGE THIS IN PRODUCTION!)
JWT_SECRET=your-production-jwt-secret-change-this-to-something-very-secure-and-long

# DevCycle Configuration (Optional)
DEVCYCLE_SERVER_SDK_KEY=your-devcycle-server-sdk-key

# Telemetry Configuration
TELEMETRY_PROJECT=shopper-backend-prod
TELEMETRY_ENVIRONMENT_ID=production

# Java Configuration
JAVA_OPTS=-Xmx1g -Xms512m
EOF
        print_success "Environment file created: .env"
        print_warning "Please update the JWT_SECRET and other settings in .env file!"
    else
        print_success "Environment file exists: .env"
    fi
}

# Create systemd service (Linux)
create_service() {
    if command -v systemctl &> /dev/null; then
        print_status "Creating systemd service..."
        
        CURRENT_DIR=$(pwd)
        JAR_FILE="${CURRENT_DIR}/target/${APP_NAME}-${APP_VERSION}.jar"
        
        sudo tee /etc/systemd/system/${APP_NAME}.service > /dev/null << EOF
[Unit]
Description=OneAgent SDK Java Backend Application
After=network.target

[Service]
Type=simple
User=$USER
WorkingDirectory=${CURRENT_DIR}
ExecStart=/usr/bin/java -jar ${JAR_FILE}
EnvironmentFile=${CURRENT_DIR}/.env
Restart=always
RestartSec=10

# Logging
StandardOutput=journal
StandardError=journal
SyslogIdentifier=${APP_NAME}

# Security
NoNewPrivileges=yes
PrivateTmp=yes
ProtectSystem=strict
ReadWritePaths=${CURRENT_DIR}

[Install]
WantedBy=multi-user.target
EOF
        
        sudo systemctl daemon-reload
        print_success "Systemd service created: ${APP_NAME}.service"
    fi
}

# Start the application
start_application() {
    print_status "Starting the application..."
    
    # Load environment variables
    if [ -f ".env" ]; then
        set -a
        source .env
        set +a
    fi
    
    JAR_FILE="target/${APP_NAME}-${APP_VERSION}.jar"
    
    if command -v systemctl &> /dev/null && [ -f "/etc/systemd/system/${APP_NAME}.service" ]; then
        # Use systemd service
        sudo systemctl enable ${APP_NAME}
        sudo systemctl start ${APP_NAME}
        print_success "Application started as systemd service"
        echo "Check status: sudo systemctl status ${APP_NAME}"
        echo "View logs: sudo journalctl -u ${APP_NAME} -f"
    else
        # Run directly
        print_status "Starting application directly..."
        echo "Starting on port ${APP_PORT}..."
        
        # Check if port is available
        if lsof -Pi :${APP_PORT} -sTCP:LISTEN -t >/dev/null 2>&1; then
            print_error "Port ${APP_PORT} is already in use!"
            echo "Please stop the existing service or change the port."
            exit 1
        fi
        
        # Start in background
        nohup java ${JAVA_OPTS:-} -jar "$JAR_FILE" > application.log 2>&1 &
        APP_PID=$!
        echo $APP_PID > app.pid
        
        sleep 5
        
        # Check if application started successfully
        if kill -0 $APP_PID 2>/dev/null; then
            print_success "Application started successfully (PID: $APP_PID)"
            echo "Application URL: http://localhost:${APP_PORT}/api"
            echo "Health check: http://localhost:${APP_PORT}/api/actuator/health"
            echo "View logs: tail -f application.log"
            echo "Stop application: kill $APP_PID"
        else
            print_error "Application failed to start. Check application.log for details."
            exit 1
        fi
    fi
}

# Main deployment flow
main() {
    echo "======================================"
    echo "  OneAgent SDK Java Backend Deploy   "
    echo "======================================"
    echo ""
    
    check_java
    check_oneagent
    setup_directories
    check_jar
    setup_environment
    
    # Ask if user wants to create systemd service
    if command -v systemctl &> /dev/null; then
        echo ""
        read -p "Create systemd service? (Y/n): " -n 1 -r
        echo
        if [[ ! $REPLY =~ ^[Nn]$ ]]; then
            create_service
        fi
    fi
    
    start_application
    
    echo ""
    print_success "Deployment completed!"
    echo ""
    echo "Next steps:"
    echo "1. Test the application: curl http://localhost:${APP_PORT}/api/actuator/health"
    echo "2. Check OneAgent SDK status in application logs"
    echo "3. Verify telemetry in your Dynatrace environment"
    echo "4. Update .env file with your production settings"
    echo ""
}

# Handle script arguments
case "${1:-}" in
    "start")
        start_application
        ;;
    "stop")
        if [ -f "app.pid" ]; then
            PID=$(cat app.pid)
            kill $PID 2>/dev/null && echo "Application stopped (PID: $PID)" || echo "Application not running"
            rm -f app.pid
        elif command -v systemctl &> /dev/null; then
            sudo systemctl stop ${APP_NAME}
            echo "Systemd service stopped"
        fi
        ;;
    "status")
        if command -v systemctl &> /dev/null && [ -f "/etc/systemd/system/${APP_NAME}.service" ]; then
            sudo systemctl status ${APP_NAME}
        elif [ -f "app.pid" ]; then
            PID=$(cat app.pid)
            if kill -0 $PID 2>/dev/null; then
                echo "Application is running (PID: $PID)"
            else
                echo "Application is not running"
            fi
        else
            echo "Application status unknown"
        fi
        ;;
    *)
        main
        ;;
esac