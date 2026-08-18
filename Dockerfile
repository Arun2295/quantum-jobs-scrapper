# ==========================================
# Stage 1: Build the Spring Boot application & download browser
# ==========================================
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /app
COPY pom.xml .
COPY src ./src
# Build the JAR
RUN mvn clean package -DskipTests
# Run the Playwright CLI tool using Maven to download and cache Chromium inside the builder container
RUN mvn exec:java -Dexec.mainClass="com.microsoft.playwright.CLI" -Dexec.args="install chromium"

# ==========================================
# Stage 2: Create the production JRE image
# ==========================================
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Copy the built JAR from the builder stage
COPY --from=builder /app/target/*.jar app.jar

# Copy the pre-downloaded Playwright browser cache from the builder stage
COPY --from=builder /root/.cache/ms-playwright /root/.cache/ms-playwright

# Install Chrome and Playwright Linux shared library dependencies (WAF-compatible Ubuntu libraries)
RUN apt-get update && apt-get install -y \
    libnss3 \
    libnspr4 \
    libatk1.0-0 \
    libatk-bridge2.0-0 \
    libcups2 \
    libdrm2 \
    libxkbcommon0 \
    libxcomposite1 \
    libxdamage1 \
    libxrandr2 \
    libgbm1 \
    libasound2 \
    libpango-1.0-0 \
    libcairo2 \
    ca-certificates \
    fonts-liberation \
    --no-install-recommends && \
    rm -rf /var/lib/apt/lists/*

# Expose Railway's dynamic port
ENV PORT=8081
EXPOSE 8081

# Run the Spring Boot application, dynamically overriding the server port with Railway's PORT env var
ENTRYPOINT ["java", "-jar", "-Dserver.port=${PORT}", "app.jar"]
