# Build stage
FROM maven:3.8.6-openjdk-17 AS build
COPY . .
RUN ./mvnw clean package -DskipTests

# Package stage
FROM openjdk:17-jdk-slim
COPY --from=build target/local-inventory-manager-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]