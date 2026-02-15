# Stage 1: Build
FROM public.ecr.aws/docker/library/eclipse-temurin:21-jdk-alpine AS build

WORKDIR /app

# Copy Maven wrapper and POM first (dependency layer caching)
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

# Copy source and build
COPY src src
RUN ./mvnw package -DskipTests -B

# Stage 2: Runtime
FROM public.ecr.aws/docker/library/eclipse-temurin:21-jre-alpine

# Install curl for health checks
RUN apk add --no-cache curl

LABEL maintainer="edem"
LABEL application="partii"

# Create a non-root user for security
RUN addgroup -S spring && adduser -S spring -G spring

WORKDIR /app

# Copy only the jar from the build stage
COPY --from=build /app/target/partii-*.jar app.jar

RUN chown -R spring:spring /app

USER spring

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:8080/.well-known/jwks.json || exit 1

ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
