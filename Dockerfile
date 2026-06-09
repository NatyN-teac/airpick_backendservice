# # Use official OpenJDK 17 runtime image as base
# FROM openjdk:17-jdk-slim
#
# # Set working directory inside the container
# WORKDIR /app
#
# # Copy the built jar into the container
# COPY target/projalpha-0.0.1-SNAPSHOT.jar app.jar
#
# # Expose the port your app listens on (default 8080)
# EXPOSE 8080
#
# # Run the jar file
# ENTRYPOINT ["java", "-jar", "app.jar"]

# Stage 1: Build
FROM maven:3.8.6-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Run
FROM eclipse-temurin:17-jdk-jammy
#FROM openjdk:17-jdk-slim
WORKDIR /app
COPY --from=build /app/target/airpick_service-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]