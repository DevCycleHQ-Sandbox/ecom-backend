# Dynatrace OneAgent SDK Integration

This document explains how to set up Dynatrace OneAgent SDK for the Java Backend application with Dynatrace monitoring.

## Overview

The application is configured to use Dynatrace OneAgent SDK for custom instrumentation. This provides:

- ✅ **Custom service tracing** for authentication and business operations
- ✅ **Database request tracing** for dual database operations  
- ✅ **Feature flag evaluation tracing** for DevCycle integration
- ✅ **Zero-configuration setup** when OneAgent is installed
- ✅ **Automatic instrumentation** of frameworks and libraries

## Architecture

```
Java Application (OneAgent SDK)
    ↓ (custom instrumentation)
Dynatrace OneAgent (local agent)
    ↓ (automatic data collection)
Dynatrace Environment
```

## Files Changed

### 1. Updated Maven Dependencies

- `pom.xml` - Replaced OpenTelemetry dependencies with OneAgent SDK
- Added `com.dynatrace.oneagent.sdk.java:oneagent-sdk:1.9.0`

### 2. New Configuration Classes

- `src/main/java/com/shopper/config/OneAgentConfig.java` - OneAgent SDK configuration
- `src/main/java/com/shopper/config/DynatraceOneAgentLogHook.java` - Feature flag tracing hook

### 3. Enhanced Service Classes

- `src/main/java/com/shopper/service/UserService.java` - Authentication tracing
- `src/main/java/com/shopper/service/DualDatabaseStrategyImpl.java` - Database request tracing

### 4. Updated Configuration Files

- `src/main/resources/application.yml` - Removed OpenTelemetry configuration
- `src/main/resources/application.properties` - Updated telemetry configuration

## Requirements

- **Dynatrace OneAgent** installed and running on the host system
- **JRE 17+** (already satisfied by Spring Boot 3.2.0)
- **OneAgent SDK 1.9.0** (included in Maven dependencies)

## Installation

### 1. Install Dynatrace OneAgent

Download and install Dynatrace OneAgent on your host system:

1. Go to your Dynatrace environment
2. Navigate to **Deploy Dynatrace** → **Start installation**
3. Select **Linux** or **Windows** based on your OS
4. Follow the installation instructions
5. Verify OneAgent is running: `sudo systemctl status oneagent` (Linux)

### 2. Verify OneAgent Installation

Check that OneAgent is active and monitoring:

```bash
# Linux
sudo systemctl status oneagent

# Windows (PowerShell as Administrator)
Get-Service oneagent*
```

## Configuration

### Application Configuration

The OneAgent SDK automatically detects the local OneAgent and uses it for telemetry:

```yaml
app:
  telemetry:
    project: shopper-backend
    environment-id: ${ENVIRONMENT_ID:66ccc3628c118d9a6da306e0}

# OneAgent SDK configuration is handled automatically by the OneAgent
# Ensure Dynatrace OneAgent is installed and running on the host
```

### Service Configuration

The application includes custom instrumentation for:

1. **Authentication Operations**
   - User registration with role assignment
   - User login with authentication validation

2. **Database Operations**
   - Primary SQLite database operations
   - Secondary Neon PostgreSQL operations
   - Dual database write/read routing

3. **Feature Flag Evaluation**
   - DevCycle feature flag evaluation
   - Custom attributes for flag context

## Running the Application

### Option 1: Standard Maven Execution

```bash
mvn spring-boot:run
```

### Option 2: Direct Java Execution

```bash
mvn clean package
java -jar target/java-backend-1.0.0.jar
```

### Option 3: IDE Execution

Run the main class `com.shopper.JavaBackendApplication` from your IDE.

## Verification

### 1. Check Application Startup Logs

Look for these log messages indicating successful OneAgent SDK configuration:

```
✅ OneAgent SDK is active and ready for instrumentation
📊 OneAgent SDK enabled for service: java-backend v1.0.0 (development)
✅ OpenFeature with DevCycle provider initialized successfully
🔍 DevCycle hook registered for OneAgent SDK tracing
```

### 2. Test API Endpoints

Generate telemetry data by making API calls:

```bash
# Health check
curl http://localhost:3002/api/actuator/health

# User registration (generates custom service traces)
curl -X POST http://localhost:3002/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","email":"test@example.com","password":"password123"}'

# User authentication (generates custom service traces)
curl -X POST http://localhost:3002/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","password":"password123"}'

# Products API (generates database request traces)
curl http://localhost:3002/api/products
```

### 3. Verify in Dynatrace

1. Open your Dynatrace environment
2. Go to **Applications & Microservices** → **Services**
3. Look for the `java-backend` service
4. Check **Service flow** and **Response time** metrics
5. View **Distributed traces** for API calls
6. Look for custom services:
   - `user_registration.register`
   - `user_authentication.login`
   - `feature_flag_evaluation.*`

## Custom Instrumentation Details

### Authentication Tracing

The application creates custom service traces for:

- **User Registration**: `user_registration.register`
  - Attributes: `username`, `email`, `assigned_role`, `user_id`
  - Error handling for duplicate users

- **User Login**: `user_authentication.login`
  - Attributes: `username`, `user_id`, `user_role`
  - Error handling for authentication failures

### Database Request Tracing

Database operations are traced with:

- **Primary Database**: SQLite operations
- **Secondary Database**: Neon PostgreSQL operations
- **Operation Types**: SELECT, INSERT/UPDATE, SELECT (fallback)
- **Database Info**: Vendor, connection details

### Feature Flag Tracing

DevCycle feature flag evaluations are traced with:

- **Service Name**: `feature_flag_evaluation.<flag_key>`
- **Attributes**: `feature_flag.key`, `feature_flag.value`, `feature_flag.reason`
- **Context**: Project and environment information

## Troubleshooting

### Common Issues

**1. OneAgent SDK is inactive**

```
⚠️  OneAgent SDK is permanently inactive. Ensure Dynatrace OneAgent is installed and running.
```

- Verify OneAgent installation and status
- Check OneAgent logs: `/var/log/dynatrace/oneagent/` (Linux)
- Restart OneAgent service if needed

**2. No custom traces in Dynatrace**

- Verify OneAgent SDK initialization in application logs
- Check that custom services are being called
- Ensure sufficient load to trigger trace collection

**3. Database traces not appearing**

- Verify database operations are going through `DualDatabaseStrategyImpl`
- Check that OneAgent SDK is active during database operations
- Ensure parent traces exist (database traces require context)

### Debug Information

Enable debug logging for OneAgent SDK:

```yaml
logging:
  level:
    com.dynatrace.oneagent: DEBUG
    com.shopper: DEBUG
```

## Production Deployment

### Docker Configuration

```dockerfile
FROM openjdk:17-jre-slim

# Install OneAgent (requires Dynatrace environment URL and token)
RUN wget -O Dynatrace-OneAgent.sh "https://YOUR_DYNATRACE_ENV.live.dynatrace.com/api/v1/deployment/installer/agent/unix/default/latest?arch=x86&flavor=default" --header="Authorization: Api-Token YOUR_PAAS_TOKEN"
RUN /bin/sh Dynatrace-OneAgent.sh --set-app-log-content-access=true

COPY target/java-backend-1.0.0.jar /app/app.jar

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
```

### Kubernetes Deployment

Use Dynatrace Operator for automatic OneAgent injection:

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: java-backend
  annotations:
    dynatrace.com/inject: "true"
spec:
  template:
    spec:
      containers:
        - name: java-backend
          image: java-backend:latest
```

## Benefits Over OpenTelemetry

1. **Zero Configuration**: No manual SDK setup or endpoint configuration
2. **Automatic Discovery**: OneAgent automatically detects and instruments applications
3. **Reduced Overhead**: Native Dynatrace integration with optimized performance
4. **Rich Context**: Automatic correlation with infrastructure and user experience data
5. **Production Ready**: No additional configuration for production deployments

## References

- [Dynatrace OneAgent SDK for Java](https://github.com/Dynatrace/OneAgent-SDK-for-Java)
- [OneAgent SDK Documentation](https://docs.dynatrace.com/docs/extend-dynatrace/oneagent-sdk)
- [OneAgent Installation Guide](https://docs.dynatrace.com/docs/setup-and-configuration/dynatrace-oneagent)