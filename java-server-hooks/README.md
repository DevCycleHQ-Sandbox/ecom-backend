# DevCycle Java Server Hooks

[![CI](https://github.com/DevCycleHQ/java-server-hooks/actions/workflows/ci.yml/badge.svg)](https://github.com/DevCycleHQ/java-server-hooks/actions/workflows/ci.yml)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.devcycle/java-server-hooks/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.devcycle/java-server-hooks)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

DevCycle EvalHook for DevCycle Java Server SDK with OpenTelemetry integration.

> **Note**: This implementation provides the structure and functionality for DevCycle's native EvalHook interface. When the full DevCycle SDK EvalHook interface becomes available, this implementation can be easily updated to use the proper interfaces and types.

## Features

- **OpenTelemetry Integration**: Automatic span creation for DevCycle feature flag evaluations
- **Native DevCycle Hook**: Uses DevCycle SDK's native EvalHook interface for optimal performance
- **Rich Metadata**: Detailed span attributes including flag keys, values, evaluation reasons, and project/environment metadata
- **Error Handling**: Comprehensive error tracking and logging
- **Production Ready**: Thread-safe implementation with proper resource cleanup

## Requirements

- Java 11 or higher
- DevCycle Java Server SDK 2.2.0+
- OpenTelemetry API 1.31.0+

## Installation

### Gradle

```gradle
dependencies {
    implementation 'com.devcycle:java-server-hooks:1.0.0'
}
```

### Maven

```xml
<dependency>
    <groupId>com.devcycle</groupId>
    <artifactId>java-server-hooks</artifactId>
    <version>1.0.0</version>
</dependency>
```

## Quick Start

### Basic Usage

```java
import com.devcycle.hooks.OTelSpanHook;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;

// Initialize OpenTelemetry tracer
OpenTelemetry openTelemetry = // ... your OpenTelemetry instance
Tracer tracer = openTelemetry.getTracer("your-service-name");

// Create the hook
OTelSpanHook hook = new OTelSpanHook(tracer);

// Manual usage example (when DevCycle SDK integration is available):
// hook.before("my-flag-key");
// hook.onFinally("my-flag-key", Optional.of("flag-value"), null);
```

### Integration with DevCycle SDK (Future)

When the DevCycle SDK EvalHook interface becomes available, the integration will look like:

```java
import com.devcycle.sdk.server.local.api.DevCycleLocalClient;
import com.devcycle.sdk.server.common.model.DevCycleUser;

// Initialize DevCycle client and add the hook
DevCycleLocalClient client = new DevCycleLocalClient("your-sdk-key");
client.addEvalHook(hook);

// Now all DevCycle evaluations will create OTel spans
DevCycleUser user = DevCycleUser.builder().userId("user123").build();
boolean flagValue = client.variableValue(user, "my-flag", false);
```



## Configuration

### Simple Configuration

The `OTelSpanHook` requires only a `Tracer` instance from your OpenTelemetry setup:

```java
// Create the hook with your tracer
OTelSpanHook hook = new OTelSpanHook(tracer);
```

This will automatically capture feature flag evaluation data and create spans with relevant attributes.

## Span Attributes

The hook creates spans with the following attributes:

### Core Feature Flag Attributes
- `feature_flag.key` - The DevCycle variable key
- `feature_flag.value` - The evaluated variable value
- `feature_flag.value_type` - The type of the default value (Boolean, String, etc.)
- `feature_flag.reason` - The evaluation reason from DevCycle
- `feature_flag.flagset` - The DevCycle feature ID (from metadata)

### DevCycle Metadata Attributes
- `feature_flag.project` - DevCycle project ID
- `feature_flag.environment` - DevCycle environment ID

### Error Handling
- When evaluation fails: `feature_flag.value` is set to "null" and `feature_flag.reason` is set to "evaluation_failed"

## Span Naming

Spans are named using the pattern: `feature_flag_evaluation.{flag_key}`

For example: `feature_flag_evaluation.my-awesome-flag`

## Integration Examples

### Spring Boot Application

```java
@Configuration
public class DevCycleConfig {
    
    @Bean
    public OTelSpanHook otelSpanHook(@Value("${spring.application.name}") String serviceName) {
        OpenTelemetry openTelemetry = GlobalOpenTelemetry.get();
        Tracer tracer = openTelemetry.getTracer(serviceName);
        
        return new OTelSpanHook(tracer);
    }
    
    @Bean
    public DevCycleLocalClient devCycleClient(
            @Value("${devcycle.server.sdk.key}") String sdkKey,
            OTelSpanHook otelSpanHook) {
        DevCycleLocalClient client = new DevCycleLocalClient(sdkKey);
        client.addEvalHook(otelSpanHook);
        return client;
    }
}
```

### Observability Setup

This hook works with any OpenTelemetry-compatible observability platform:

- **Jaeger** - Distributed tracing
- **Zipkin** - Distributed tracing  
- **Dynatrace** - Full-stack monitoring
- **New Relic** - Application performance monitoring
- **Datadog** - Cloud monitoring
- **AWS X-Ray** - Distributed tracing for AWS

## Thread Safety

The `OTelSpanHook` is thread-safe and can be safely used in concurrent environments. It uses a `ConcurrentHashMap` to track spans across hook lifecycle events.

## Performance Considerations

- Span creation is lightweight and designed for high-throughput applications
- The hook uses efficient data structures to minimize memory overhead
- Spans are automatically cleaned up in the `onFinally` hook to prevent memory leaks

## Development

### Building

```bash
./gradlew build
```

### Running Tests

```bash
./gradlew test
```

### Publishing

The project is configured to publish to Maven Central. To publish:

1. Set up your Sonatype OSSRH credentials
2. Configure signing keys
3. Create a GitHub release

GitHub Actions will automatically publish to Maven Central when a release is created.

## Contributing

1. Fork the repository
2. Create a feature branch
3. Add tests for new functionality
4. Ensure all tests pass
5. Submit a pull request

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Support

For questions or issues:

- Create an issue on GitHub
- Contact DevCycle support
- Check the [DevCycle documentation](https://docs.devcycle.com/)

## Related Projects

- [DevCycle Java Server SDK](https://github.com/DevCycleHQ/java-server-sdk)
- [OpenFeature Java SDK](https://github.com/open-feature/java-sdk)
- [OpenTelemetry Java](https://github.com/open-telemetry/opentelemetry-java)