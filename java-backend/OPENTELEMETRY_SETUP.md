# OpenTelemetry Integration for Dynatrace

This document explains how to set up OpenTelemetry automatic instrumentation for the Java Backend application with Dynatrace monitoring.

## Overview

The application is configured to use OpenTelemetry automatic instrumentation with the Dynatrace OneAgent OTLP endpoint. This provides:

- ✅ **Automatic traces** for HTTP requests, database queries, and method calls
- ✅ **Metrics collection** for application performance monitoring
- ✅ **Logs correlation** with trace and span context
- ✅ **Zero-code instrumentation** using the OpenTelemetry Java Agent

## Architecture

```
Java Application
    ↓ (auto-instrumented)
OpenTelemetry Java Agent
    ↓ (OTLP)
Dynatrace OneAgent (localhost:14499)
    ↓
Dynatrace Environment
```

## Files Changed

### 1. Downloaded OpenTelemetry Java Agent

- `opentelemetry-javaagent.jar` - Latest auto-instrumentation agent

### 2. Updated Configuration Files

- `src/main/java/com/shopper/config/OpenTelemetryConfig.java` - Simplified for auto-instrumentation
- `src/main/resources/application.yml` - Added OTLP configuration
- `pom.xml` - Added `-javaagent` parameter to Maven plugin

### 3. Created Run Script

- `run-with-otel.sh` - Script with proper environment variables

## Configuration

### Environment Variables

For **Dynatrace OneAgent** (localhost OTLP endpoint):

```bash
export OTEL_EXPORTER_OTLP_ENDPOINT="http://localhost:14499/otlp"
export OTEL_SERVICE_NAME="java-backend"
export OTEL_SERVICE_VERSION="1.0.0"
export OTEL_RESOURCE_ATTRIBUTES="service.name=java-backend,service.version=1.0.0,deployment.environment=development"
export OTEL_EXPORTER_OTLP_METRICS_TEMPORALITY_PREFERENCE="delta"
```

For **Direct Dynatrace** (with API token):

```bash
export OTEL_EXPORTER_OTLP_ENDPOINT="https://your-env.live.dynatrace.com/api/v2/otlp"
export OTEL_EXPORTER_OTLP_HEADERS="Authorization=Api-Token YOUR_API_TOKEN"
export OTEL_SERVICE_NAME="java-backend"
export OTEL_SERVICE_VERSION="1.0.0"
export OTEL_RESOURCE_ATTRIBUTES="service.name=java-backend,service.version=1.0.0,deployment.environment=production"
export OTEL_EXPORTER_OTLP_METRICS_TEMPORALITY_PREFERENCE="delta"
```

### Application Configuration (application.yml)

```yaml
app:
  telemetry:
    use-local-otlp: ${USE_LOCAL_OTLP:true}
    local-otlp-port: ${LOCAL_OTLP_PORT:14499}
    dynatrace:
      env-url: ${DYNATRACE_ENV_URL:}
      api-token: ${DYNATRACE_API_TOKEN:}

otel:
  exporter:
    otlp:
      endpoint: ${OTEL_EXPORTER_OTLP_ENDPOINT:http://localhost:14499/otlp}
      headers: ${OTEL_EXPORTER_OTLP_HEADERS:}
      metrics:
        temporality:
          preference: delta
  resource:
    attributes: "service.name=${spring.application.name},service.version=${app.version:1.0.0},deployment.environment=${spring.profiles.active:development}"
  service:
    name: ${spring.application.name}
```

## Running the Application

### Option 1: Using the OpenTelemetry Script (Recommended)

```bash
./run-with-otel.sh
```

### Option 2: Using Maven with Environment Variables

```bash
export OTEL_EXPORTER_OTLP_ENDPOINT="http://localhost:14499/otlp"
export OTEL_SERVICE_NAME="java-backend"
export USE_LOCAL_OTLP=true
mvn spring-boot:run
```

### Option 3: Direct Java Execution (Production)

```bash
java -javaagent:opentelemetry-javaagent.jar \
     -Dotel.exporter.otlp.endpoint=http://localhost:14499/otlp \
     -Dotel.service.name=java-backend \
     -Dotel.service.version=1.0.0 \
     -Dotel.resource.attributes="service.name=java-backend,service.version=1.0.0,deployment.environment=production" \
     -jar target/java-backend-1.0.0.jar
```

## Verification

### 1. Check Application Startup Logs

Look for these log messages indicating successful OpenTelemetry configuration:

```
🔗 OpenTelemetry configured for local OTLP endpoint: http://localhost:14499/otlp
📊 OpenTelemetry Auto-Instrumentation enabled for service: java-backend v1.0.0 (development)
```

### 2. Test API Endpoints

Generate telemetry data by making API calls:

```bash
# Health check
curl http://localhost:3002/api/actuator/health

# Authentication (generates database traces)
curl -X POST http://localhost:3002/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"password"}'

# Products API (generates database traces)
curl http://localhost:3002/api/products
```

### 3. Verify in Dynatrace

1. Open your Dynatrace environment
2. Go to **Applications & Microservices** → **Services**
3. Look for the `java-backend` service
4. Check **Service flow** and **Response time** metrics
5. View **Distributed traces** for API calls

## Troubleshooting

### Common Issues

**1. "Connection refused" to OTLP endpoint**

- Ensure Dynatrace OneAgent is running
- Check that port 14499 is accessible: `curl http://localhost:14499/otlp/v1/traces`
- Verify OneAgent configuration

**2. No telemetry data in Dynatrace**

- Check application startup logs for OpenTelemetry configuration
- Verify environment variables are set correctly
- Test with debug logging: `export OTEL_JAVAAGENT_DEBUG=true`

**3. Java Agent not loaded**

- Ensure `opentelemetry-javaagent.jar` exists in project root
- Check Maven plugin configuration in `pom.xml`
- Verify JVM arguments include `-javaagent` parameter

### Debug Logging

Enable OpenTelemetry debug logging:

```bash
export OTEL_JAVAAGENT_DEBUG=true
export OTEL_JAVAAGENT_LOGGING=application
./run-with-otel.sh
```

## Production Deployment

### Docker Configuration

```dockerfile
FROM openjdk:17-jre-slim

# Download OpenTelemetry agent
ADD https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/latest/download/opentelemetry-javaagent.jar /app/

COPY target/java-backend-1.0.0.jar /app/app.jar

# Set environment variables
ENV OTEL_SERVICE_NAME=java-backend
ENV OTEL_SERVICE_VERSION=1.0.0
ENV OTEL_EXPORTER_OTLP_ENDPOINT=http://localhost:14499/otlp

ENTRYPOINT ["java", "-javaagent:/app/opentelemetry-javaagent.jar", "-jar", "/app/app.jar"]
```

### Kubernetes Deployment

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: java-backend
spec:
  template:
    spec:
      containers:
        - name: java-backend
          image: java-backend:latest
          env:
            - name: OTEL_EXPORTER_OTLP_ENDPOINT
              value: "http://localhost:14499/otlp"
            - name: OTEL_SERVICE_NAME
              value: "java-backend"
            - name: OTEL_SERVICE_VERSION
              value: "1.0.0"
            - name: OTEL_RESOURCE_ATTRIBUTES
              value: "service.name=java-backend,service.version=1.0.0,deployment.environment=production"
```

## References

- [Dynatrace OpenTelemetry Java Auto-Instrumentation](https://docs.dynatrace.com/docs/ingest-from/opentelemetry/walkthroughs/java/java-auto)
- [OpenTelemetry Java Agent Configuration](https://opentelemetry.io/docs/instrumentation/java/automatic/agent-config/)
- [Dynatrace OTLP Ingestion](https://docs.dynatrace.com/docs/ingest-from/opentelemetry)
