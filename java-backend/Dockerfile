FROM openjdk:17-jre-slim

# Install curl for OneAgent download
RUN apt-get update && apt-get install -y curl wget && rm -rf /var/lib/apt/lists/*

# Create app directory
WORKDIR /app

# Copy the JAR file
COPY target/java-backend-1.0.0.jar /app/app.jar

# Copy configuration files
COPY src/main/resources/application.yml /app/application.yml
COPY src/main/resources/application.properties /app/application.properties

# Create directory for database
RUN mkdir -p /app/data

# Expose the application port
EXPOSE 3002

# Add health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:3002/api/actuator/health || exit 1

# Set environment variables
ENV JAVA_OPTS="-Xmx512m -Xms256m"
ENV DATABASE_URL="./data/database.sqlite"

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]