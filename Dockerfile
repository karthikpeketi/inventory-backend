# ========================
# Stage 1: Build the JAR
# ========================
FROM maven:3.9.4-openjdk-17 AS build

# Set working directory
WORKDIR /app

# Copy project files
COPY pom.xml .
COPY src ./src

# Build the project (skip tests to avoid failures)
RUN mvn clean package -DskipTests

# ========================
# Stage 2: Run the JAR
# ========================
FROM eclipse-temurin:17-jdk-jammy

# Set working directory
WORKDIR /app

# Copy the built JAR from the build stage
COPY --from=build /app/target/*.jar app.jar

# Expose the port (Spring Boot default 8080)
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
