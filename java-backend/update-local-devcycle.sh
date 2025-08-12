#!/bin/bash

echo "🔄 Updating Local DevCycle SDK..."

# Step 1: Build and publish the local DevCycle SDK
echo "📦 Building local DevCycle SDK..."
cd ~/dev/java-server-sdk
./gradlew clean build publishToMavenLocal -x test -q

if [ $? -ne 0 ]; then
    echo "❌ Failed to build local DevCycle SDK"
    exit 1
fi

echo "✅ Local DevCycle SDK published to Maven local repository"

# Step 2: Return to project and clean/rebuild
echo "🔨 Rebuilding project with updated local SDK..."
cd ~/dev/shopper/backend/java-backend
mvn clean compile -q

if [ $? -ne 0 ]; then
    echo "❌ Failed to rebuild project"
    exit 1
fi

echo "✅ Project rebuilt with local DevCycle SDK"

# Step 3: Optional - restart server if it's running
if pgrep -f "spring-boot:run" > /dev/null; then
    echo "🔄 Restarting server..."
    pkill -f "spring-boot:run"
    sleep 2
    nohup ./run-with-otel.sh > server.log 2>&1 & disown
    echo "🚀 Server restarted with updated local SDK"
else
    echo "ℹ️  Server not running. Start with: ./run-with-otel.sh"
fi

echo "🎉 Local DevCycle SDK update complete!"
echo ""
echo "📋 Quick test:"
echo "  1. Start server: ./run-with-otel.sh"
echo "  2. Login: curl -H 'Content-Type: application/json' -d '{\"username\":\"admin\",\"password\":\"password\"}' http://localhost:3002/api/auth/login"
echo "  3. Test feature flags: curl -H 'Authorization: Bearer <token>' http://localhost:3002/api/products"
echo "  4. Check logs: tail -f server.log" 