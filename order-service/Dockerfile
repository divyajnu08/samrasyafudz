# ---------- Build stage ----------
FROM eclipse-temurin:17-jdk AS builder

WORKDIR /workspace

# Copy Gradle wrapper + build scripts first (better layer caching)
COPY gradlew gradlew.bat ./
COPY gradle ./gradle
COPY build.gradle.kts settings.gradle.kts ./
COPY common-security ./common-security

# Copy the service source
COPY order-service ./order-service

# Build only this module's bootJar (resolves the common-security dependency too)
RUN ./gradlew :order-service:bootJar --no-daemon

# ---------- Runtime stage ----------
FROM eclipse-temurin:17-jre

WORKDIR /app
COPY --from=builder /workspace/order-service/build/libs/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
