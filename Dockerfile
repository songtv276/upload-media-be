# Use a specific OpenJDK version for reproducibility
FROM openjdk:17.0.12-jdk-alpine

LABEL authors="Admin"

WORKDIR /app

# Copy the built jar file
COPY target/MinIOTest-0.0.1-SNAPSHOT.jar app.jar

# Healthcheck to ensure the app is running
HEALTHCHECK --interval=30s --timeout=10s --start-period=30s --retries=3 \
  CMD wget --spider -q http://localhost:8080/actuator/health || exit 1

CMD ["java", "-jar", "app.jar"]