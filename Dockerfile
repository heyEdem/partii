# Runtime image using pre-built JAR
# Build locally with: ./mvnw clean package -DskipTests
FROM public.ecr.aws/docker/library/eclipse-temurin:21-jre-alpine

# Install curl for health checks
RUN apk add --no-cache curl

# Add metadata
LABEL maintainer="edem"
LABEL application="partii"

# Create a non-root user for security
RUN addgroup -S spring && adduser -S spring -G spring

# Set working directory
WORKDIR /app

# Copy the pre-built JAR
COPY target/partii-*.jar app.jar

# Change ownership to non-root user
RUN chown -R spring:spring /app

# Switch to non-root user
USER spring

# Expose port
EXPOSE 8080

# Health check (optional but recommended for production)
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:8080/.well-known/jwks.json || exit 1

# JVM tuning for containers
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
