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

# curl is used by the container HEALTHCHECK below.
RUN apt-get update \
 && apt-get install -y --no-install-recommends curl \
 && rm -rf /var/lib/apt/lists/*

# Non-root user for safety.
RUN groupadd --system app && useradd --system --gid app app

COPY --from=build /app/target/stock-insights-*.jar app.jar

# H2 writes here; mount a volume to persist the cache across restarts.
RUN mkdir -p /app/data && chown -R app:app /app
USER app

# Container-appropriate H2 URL: a container is single-process, so drop AUTO_SERVER.
# AUTO_SERVER starts an in-process TCP lock-server and resolves the container hostname
# on startup — both are unnecessary here and can crash-loop the container (e.g. a stale
# .lock.db left by an unclean stop blocks the next boot). Overridable at run time.
ENV SPRING_DATASOURCE_URL="jdbc:h2:file:/app/data/stock-insights"
ENV JAVA_OPTS=""

EXPOSE 8080

# Lets Docker/Compose track readiness (and stop crash-loops from wedging Docker Desktop).
HEALTHCHECK --interval=15s --timeout=5s --start-period=45s --retries=5 \
  CMD curl -fsS http://localhost:8080/health || exit 1

# Shell form so JAVA_OPTS is expanded; exec so the JVM is PID 1 and receives signals.
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
