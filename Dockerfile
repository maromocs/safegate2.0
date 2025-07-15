FROM openjdk:21-jdk-slim

# Set working directory
WORKDIR /app

# Copy gradle wrapper and build files
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .

# Copy source code
COPY src src

# Make gradlew executable
RUN chmod +x gradlew

# Build the application
RUN ./gradlew build -x test

# Expose port
EXPOSE 8080

# More lenient health check - wait longer for startup
HEALTHCHECK --interval=30s --timeout=10s --start-period=120s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# Run the application directly from build/libs
ENTRYPOINT ["java", "-jar", "build/libs/SafeGate-0.0.1-SNAPSHOT.jar"]