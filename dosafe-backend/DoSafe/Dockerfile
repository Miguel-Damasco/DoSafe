# ─── Stage 1: Build ───────────────────────────────────────────────────────────
# Uses a full JDK + Maven image to compile and package the application.
# This image is heavy (~700MB) but is only used during the build — it is NOT
# included in the final image.
FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /app

# Copy the pom.xml first and download dependencies separately.
# Docker caches each layer — if only source code changes (not pom.xml),
# this layer is reused and Maven doesn't re-download dependencies.
COPY pom.xml .
RUN mvn dependency:go-offline -q

# Copy source code and build the JAR, skipping tests (they run in CI).
COPY src ./src
RUN mvn clean package -DskipTests -q

# ─── Stage 2: Runtime ─────────────────────────────────────────────────────────
# Uses a minimal JRE image (~200MB). No Maven, no source code, no JDK —
# only what is needed to run the application.
FROM eclipse-temurin:17-jre

WORKDIR /app

# Copy only the compiled JAR from the builder stage.
COPY --from=builder /app/target/DoSafe-0.0.1-SNAPSHOT.jar app.jar

# Expose the port Spring Boot listens on.
EXPOSE 8080

# Start the application.
ENTRYPOINT ["java", "-jar", "app.jar"]
