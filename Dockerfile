# -----------------------------------------------------------------------------
# Stage 1: Build Application
# -----------------------------------------------------------------------------
FROM eclipse-temurin:25-jdk-alpine AS builder

WORKDIR /workspace

# Copy Gradle wrapper and build configurations first for layer caching
COPY gradlew gradlew.bat settings.gradle build.gradle ./
COPY gradle ./gradle

# Grant execute permission for gradlew
RUN chmod +x gradlew

# Download dependencies (offline cache layer)
RUN ./gradlew dependencies --no-daemon

# Copy source code
COPY src ./src

# Build production jar without running integration tests inside docker build
RUN ./gradlew bootJar --no-daemon -x test

# -----------------------------------------------------------------------------
# Stage 2: Minimal Production Runtime
# -----------------------------------------------------------------------------
FROM eclipse-temurin:25-jre-alpine

WORKDIR /app

# Install curl for health check
RUN apk add --no-cache curl

# Create non-privileged user for security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser:appgroup

# Copy compiled JAR from builder stage
COPY --from=builder --chown=appuser:appgroup /workspace/build/libs/*.jar app.jar

# Expose HTTP port
EXPOSE 8080

# Environment defaults
ENV SPRING_PROFILES_ACTIVE=prod \
    JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+UseG1GC -Djava.security.egd=file:/dev/./urandom"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
