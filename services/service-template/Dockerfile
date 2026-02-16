# Multi-stage Docker build with GraalVM for optimal performance
# Stage 1: Build stage
FROM maven:3.9-eclipse-temurin-21 AS builder

# Set working directory
WORKDIR /build

# Copy parent POM and resolve dependencies
COPY ../../pom.xml /build/parent-pom.xml
COPY ../../shared-libraries /build/shared-libraries
COPY pom.xml .

# Download dependencies (cached layer)
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the application
RUN mvn clean package -DskipTests -B

# Stage 2: Extract layers for better caching
FROM eclipse-temurin:21-jre AS layers

WORKDIR /extract

COPY --from=builder /build/target/*.jar app.jar

# Extract layers using Spring Boot's layertools
RUN java -Djarmode=layertools -jar app.jar extract

# Stage 3: Runtime stage with distroless base
FROM eclipse-temurin:21-jre-alpine

# Install required packages
RUN apk --no-cache add curl

# Create non-root user
RUN addgroup -g 1001 -S appgroup && \
    adduser -u 1001 -S appuser -G appgroup

# Set working directory
WORKDIR /app

# Copy layers from extract stage (order matters for caching)
COPY --from=layers --chown=appuser:appgroup /extract/dependencies/ ./
COPY --from=layers --chown=appuser:appgroup /extract/spring-boot-loader/ ./
COPY --from=layers --chown=appuser:appgroup /extract/snapshot-dependencies/ ./
COPY --from=layers --chown=appuser:appgroup /extract/application/ ./

# Configure JVM for containers
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 \
    -XX:+UseG1GC \
    -XX:+UseStringDeduplication \
    -XX:+ExitOnOutOfMemoryError \
    -Djava.security.egd=file:/dev/./urandom \
    -Dspring.backgroundpreinitializer.ignore=true"

# Switch to non-root user
USER appuser

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health/liveness || exit 1

# Entry point
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher"]