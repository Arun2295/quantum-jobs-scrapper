# ==========================================
# Stage 1: Build the Spring Boot application
# ==========================================
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# ==========================================
# Stage 2: Create the production JRE image
# ==========================================
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Copy the built JAR from the builder stage
COPY --from=builder /app/target/*.jar app.jar

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

# Pre-download and install the Playwright browser binaries into the image during build.
# This prevents downloading them at runtime, making container startups and first requests instant.
RUN java -cp app.jar com.microsoft.playwright.CLI install chromium

# Expose Railway's dynamic port
ENV PORT=8081
EXPOSE 8081

# Run the Spring Boot application, dynamically overriding the server port with Railway's PORT env var
ENTRYPOINT ["java", "-jar", "-Dserver.port=${PORT}", "app.jar"]
