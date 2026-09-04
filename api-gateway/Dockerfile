# ---------- Build stage ----------
FROM eclipse-temurin:17-jdk AS builder

WORKDIR /workspace

# Copy Gradle wrapper + build scripts first (better layer caching)
COPY gradlew gradlew.bat ./
COPY gradle ./gradle
COPY build.gradle.kts settings.gradle.kts ./

# Copy the service source
COPY api-gateway ./api-gateway

# Build only this module's bootJar
RUN ./gradlew :api-gateway:bootJar --no-daemon

# ---------- Runtime stage ----------
FROM eclipse-temurin:17-jre

WORKDIR /app
COPY --from=builder /workspace/api-gateway/build/libs/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
