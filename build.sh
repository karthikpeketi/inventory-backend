#!/bin/bash
echo "Building Spring Boot application..."
./mvnw clean package -DskipTests
echo "Build completed!"