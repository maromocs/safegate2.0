# ---- Builder stage ----
FROM gradle:8.7-jdk21 AS builder
WORKDIR /workspace

# Copy build files and sources
COPY gradlew ./
COPY gradle ./gradle
COPY build.gradle ./
COPY settings.gradle ./
COPY src ./src

# Ensure wrapper is executable and build only the bootable jar (skip tests)
RUN chmod +x gradlew && ./gradlew --no-daemon clean bootJar -x test

# ---- Runtime stage ----
FROM eclipse-temurin:21-jre
WORKDIR /app

# Copy the built jar from the builder stage
COPY --from=builder /workspace/build/libs/app.jar /app/app.jar

# Expose port
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "/app/app.jar"]