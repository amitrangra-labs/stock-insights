# syntax=docker/dockerfile:1

# ---- Build stage: compile and package the fat jar ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Cache dependencies first (only re-downloads when pom.xml changes).
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

# Build the application.
COPY src ./src
RUN mvn -B -q clean package -DskipTests

# ---- Runtime stage: small JRE image with just the jar ----
FROM eclipse-temurin:21-jre
WORKDIR /app

# Non-root user for safety.
RUN groupadd --system app && useradd --system --gid app app

COPY --from=build /app/target/stock-insights-*.jar app.jar

# H2 writes here; mount a volume to persist the cache across restarts.
RUN mkdir -p /app/data && chown -R app:app /app
USER app

EXPOSE 8080
ENV JAVA_OPTS=""

# Shell form so JAVA_OPTS is expanded; exec so the JVM is PID 1 and receives signals.
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
