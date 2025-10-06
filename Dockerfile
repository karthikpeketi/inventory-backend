# Stage 1: Build the JAR
FROM maven:3-eclipse-temurin-17 AS build

# Use a dedicated workdir
WORKDIR /app

# Copy only the Maven descriptor first to leverage layer caching for dependencies
COPY pom.xml ./

# Pre-fetch dependencies (faster incremental builds)
RUN mvn -B -q -DskipTests=true dependency:go-offline

# Copy source and build
COPY src ./src
RUN mvn -B -DskipTests=true clean package

# Stage 2: Runtime image
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Copy the built jar (rename to app.jar). The wildcard handles version changes.
COPY --from=build /app/target/*.jar /app/app.jar

# Render will set PORT; Spring Boot uses server.port=${PORT:8080}
EXPOSE 8080

# Allow custom JVM options via JAVA_OPTS
ENV JAVA_OPTS=""

# Start the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
