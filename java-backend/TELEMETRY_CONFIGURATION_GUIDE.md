# OpenTelemetry Configuration Guide

## Overview

Your Java Backend application now supports **flexible OpenTelemetry configuration** that automatically chooses the right telemetry endpoint based on environment variables. This enables seamless switching between local development, Dynatrace OneAgent, and direct Dynatrace environments.

## 🔧 Configuration Modes

### 1. **No Telemetry (Default)**

When no telemetry environment variables are set, the application runs without telemetry export.

```bash
./run-with-otel.sh
```

**Output:**

```
⚠️  No telemetry endpoint configured. Set either:
   - USE_LOCAL_OTLP=true for local OneAgent
   - DYNATRACE_ENV_URL and DYNATRACE_API_TOKEN for direct Dynatrace
   Proceeding without telemetry export...
```

### 2. **Local OneAgent Mode**

When Dynatrace OneAgent is running locally on port 14499.

```bash
USE_LOCAL_OTLP=true ./run-with-otel.sh
```

**Output:**

```
📡 Using LOCAL OTLP endpoint: http://localhost:14499/otlp
```

**Environment Variables Used:**

- `OTEL_EXPORTER_OTLP_ENDPOINT=http://localhost:14499/otlp`
- `OTEL_EXPORTER_OTLP_COMPRESSION=gzip`

### 3. **Direct Dynatrace Mode**

When sending telemetry directly to your Dynatrace environment.

```bash
DYNATRACE_ENV_URL="https://abc12345.live.dynatrace.com" \
DYNATRACE_API_TOKEN="dt0c01.SAMPLE12345ABCDEF.token" \
./run-with-otel.sh
```

**Output:**

```
📡 Using DYNATRACE OTLP endpoint: https://abc12345.live.dynatrace.com/api/v2/otlp
```

**Environment Variables Used:**

- `OTEL_EXPORTER_OTLP_ENDPOINT=https://abc12345.live.dynatrace.com/api/v2/otlp`
- `OTEL_EXPORTER_OTLP_HEADERS=Authorization=Api-Token dt0c01.SAMPLE12345ABCDEF.token`
- `OTEL_EXPORTER_OTLP_COMPRESSION=gzip`

## 🚀 Usage Examples

### Development with OneAgent

If you have Dynatrace OneAgent installed locally:

```bash
# Set environment variable and run
export USE_LOCAL_OTLP=true
./run-with-otel.sh
```

### Production with Direct Dynatrace

For production environments with direct Dynatrace integration:

```bash
# Set your Dynatrace credentials
export DYNATRACE_ENV_URL="https://your-tenant.live.dynatrace.com"
export DYNATRACE_API_TOKEN="your-api-token-here"
./run-with-otel.sh
```

### Using .env File

Create a `.env` file in the project root:

```env
# For OneAgent
USE_LOCAL_OTLP=true

# OR for Direct Dynatrace
# DYNATRACE_ENV_URL=https://your-tenant.live.dynatrace.com
# DYNATRACE_API_TOKEN=your-api-token-here
```

Then run:

```bash
./run-with-otel.sh
```

## 📊 What Gets Monitored

With OpenTelemetry automatic instrumentation enabled, you get:

### **Traces** 🔍

- HTTP requests (incoming and outgoing)
- Database queries (SQLite)
- Spring components and services
- JWT authentication flows
- Method-level tracing with `@WithSpan` annotations

### **Metrics** 📈

- HTTP request duration and count
- Database connection pool metrics
- JVM metrics (memory, GC, threads)
- Custom business metrics

### **Logs** 📝

- Application logs with trace correlation
- Structured logging with span context
- Error logs with stack traces

## 🔧 Environment Variables Reference

| Variable              | Description                | Default | Example                               |
| --------------------- | -------------------------- | ------- | ------------------------------------- |
| `USE_LOCAL_OTLP`      | Enable local OneAgent mode | `false` | `true`                                |
| `LOCAL_OTLP_PORT`     | OneAgent OTLP port         | `14499` | `14499`                               |
| `DYNATRACE_ENV_URL`   | Dynatrace environment URL  | -       | `https://abc12345.live.dynatrace.com` |
| `DYNATRACE_API_TOKEN` | Dynatrace API token        | -       | `dt0c01.SAMPLE12345...`               |

## 🐛 Troubleshooting

### Connection Refused Errors

If you see connection refused errors:

```
Failed to connect to localhost/127.0.0.1:14499
```

**Solutions:**

1. **For OneAgent:** Ensure Dynatrace OneAgent is running and listening on port 14499
2. **For Direct:** Check your `DYNATRACE_ENV_URL` and `DYNATRACE_API_TOKEN` are correct
3. **For Development:** Use no telemetry mode by not setting any environment variables

### Application Not Starting

If the application fails to start:

1. Check if port 3002 is already in use:

   ```bash
   lsof -i :3002
   ```

2. Kill existing processes:

   ```bash
   kill <process-id>
   ```

3. Ensure OpenTelemetry agent is downloaded:
   ```bash
   ls -la opentelemetry-javaagent.jar
   ```

### Telemetry Not Appearing in Dynatrace

1. Verify your API token has the correct permissions:

   - `metrics.ingest`
   - `logs.ingest`
   - `openTelemetryTrace.ingest`

2. Check the application logs for export errors

3. Verify the environment URL format:
   ```
   https://{your-environment-id}.{your-domain}
   ```

## 🎯 Best Practices

### Development

- Use `USE_LOCAL_OTLP=true` with OneAgent for full local debugging
- Use no telemetry mode for basic development when telemetry isn't needed

### Production

- Always use direct Dynatrace mode with proper API tokens
- Set up proper log aggregation with trace correlation
- Monitor the telemetry export success rates

### Security

- Store `DYNATRACE_API_TOKEN` in secure environment variables
- Never commit API tokens to version control
- Use least-privilege API tokens (only required scopes)

## 📚 Next Steps

1. **Custom Instrumentation:** Add `@WithSpan` annotations to important business methods
2. **Custom Metrics:** Use OpenTelemetry APIs to create business-specific metrics
3. **Dashboards:** Create Dynatrace dashboards for your application metrics
4. **Alerting:** Set up alerts for error rates, response times, and business KPIs

---

✅ **Configuration Complete!** Your application now supports flexible OpenTelemetry monitoring with automatic endpoint detection.
