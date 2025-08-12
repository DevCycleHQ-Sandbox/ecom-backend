# DevCycle Java Server Hooks

[![CI](https://github.com/DevCycleHQ/java-server-hooks/actions/workflows/ci.yml/badge.svg)](https://github.com/DevCycleHQ/java-server-hooks/actions/workflows/ci.yml)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.devcycle/java-server-hooks/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.devcycle/java-server-hooks)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

OpenFeature hooks for DevCycle Java Server SDK with OpenTelemetry integration.

## Features

- **OpenTelemetry Integration**: Automatic span creation for feature flag evaluations
- **Rich Metadata**: Detailed span attributes including flag keys, values, variants, and application metadata
- **Error Handling**: Comprehensive error tracking and exception recording
- **Flexible Configuration**: Customizable application metadata through simple interfaces
- **Production Ready**: Thread-safe implementation with proper resource cleanup

## Requirements

- Java 11 or higher
- OpenFeature SDK 1.10.0+
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
import dev.openfeature.sdk.OpenFeatureAPI;
import io.opentelemetry.api.OpenTelemetry;

// Initialize OpenTelemetry tracer
OpenTelemetry openTelemetry = // ... your OpenTelemetry instance
Tracer tracer = openTelemetry.getTracer("your-service-name");

// Create application metadata
OTelSpanHook.AppMetadata appMetadata = OTelSpanHook.SimpleAppMetadata.builder()
    .name("my-service")
    .version("1.0.0")
    .environment("production")
    .project("my-project")
    .environmentId("prod-env-123")
    .build();

// Create and register the hook
OTelSpanHook hook = new OTelSpanHook(tracer, appMetadata);
OpenFeatureAPI.getInstance().addHook(hook);
```

### With DevCycle SDK

```java
import com.devcycle.sdk.server.local.api.DevCycleLocalClient;
import com.devcycle.hooks.OTelSpanHook;
import dev.openfeature.sdk.OpenFeatureAPI;

// Initialize DevCycle client
DevCycleLocalClient devCycleClient = new DevCycleLocalClient("your-sdk-key");

// Set up OpenFeature with DevCycle provider
OpenFeatureAPI api = OpenFeatureAPI.getInstance();
api.setProvider(devCycleClient.getOpenFeatureProvider());

// Add the OTel span hook
OTelSpanHook hook = new OTelSpanHook(tracer, appMetadata);
api.addHook(hook);

// Now all feature flag evaluations will create OTel spans
Client client = api.getClient();
boolean flagValue = client.getBooleanValue("my-flag", false);
```

## Configuration

### Application Metadata

The `AppMetadata` interface allows you to provide application-specific information that will be included in spans:

```java
public interface AppMetadata {
    String getName();        // Service/application name
    String getVersion();     // Application version
    String getEnvironment(); // Environment (dev, staging, prod)
    String getProject();     // Project identifier
    String getEnvironmentId(); // Environment ID
}
```

### Custom Metadata Implementation

You can implement your own `AppMetadata`:

```java
public class MyAppMetadata implements OTelSpanHook.AppMetadata {
    @Override
    public String getName() {
        return System.getProperty("app.name", "my-service");
    }
    
    @Override
    public String getVersion() {
        return getClass().getPackage().getImplementationVersion();
    }
    
    // ... implement other methods
}
```

### Minimal Configuration

If you don't need application metadata, you can pass `null`:

```java
OTelSpanHook hook = new OTelSpanHook(tracer, null);
```

## Span Attributes

The hook creates spans with the following attributes:

### Core Feature Flag Attributes
- `feature_flag.key` - The feature flag key
- `feature_flag.value` - The resolved flag value
- `feature_flag.value_type` - The type of the flag value (BOOLEAN, STRING, etc.)
- `feature_flag.reason` - The evaluation reason
- `feature_flag.variant` - The variant name (if applicable)
- `feature_flag.flagset` - The flagset (same as key)

### Application Metadata Attributes
- `service.name` - Application/service name
- `service.version` - Application version
- `deployment.environment` - Deployment environment
- `feature_flag.project` - Project identifier
- `feature_flag.environment` - Environment ID

### OpenFeature Metadata
- `openfeature.client.name` - OpenFeature client name
- `openfeature.provider.name` - Provider name (e.g., "devcycle")

### Error Attributes (when applicable)
- `feature_flag.error_code` - Error code if evaluation failed
- `feature_flag.error_message` - Error message if evaluation failed

## Span Naming

Spans are named using the pattern: `feature_flag_evaluation.{flag_key}`

For example: `feature_flag_evaluation.my-awesome-flag`

## Integration Examples

### Spring Boot Application

```java
@Configuration
public class OpenFeatureConfig {
    
    @Bean
    public OTelSpanHook otelSpanHook(
            @Value("${spring.application.name}") String serviceName,
            @Value("${app.version:1.0.0}") String version,
            @Value("${spring.profiles.active:development}") String environment
    ) {
        OpenTelemetry openTelemetry = GlobalOpenTelemetry.get();
        Tracer tracer = openTelemetry.getTracer(serviceName);
        
        OTelSpanHook.AppMetadata metadata = OTelSpanHook.SimpleAppMetadata.builder()
            .name(serviceName)
            .version(version)
            .environment(environment)
            .project("my-project")
            .environmentId("env-123")
            .build();
            
        return new OTelSpanHook(tracer, metadata);
    }
    
    @PostConstruct
    public void setupOpenFeature(OTelSpanHook hook) {
        OpenFeatureAPI.getInstance().addHook(hook);
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
- Spans are automatically cleaned up in the `finallyAfter` hook to prevent memory leaks

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