# Use official OpenJDK image as the base image
FROM eclipse-temurin:17-jdk-alpine

# Set working directory
WORKDIR /app

# Copy the JAR file from the local target directory
COPY target/tracking-number-generator-1.0-SNAPSHOT.jar tracking-number-generator.jar

# Expose port 8080
EXPOSE 8080

# Start the application
ENTRYPOINT ["java", "-jar", "tracking-number-generator.jar"]