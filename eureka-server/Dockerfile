FROM eclipse-temurin:21-jdk-alpine

# Install wget for healthcheck
RUN apk add --no-cache wget

WORKDIR /app

COPY target/*.jar app.jar

EXPOSE 8761

ENTRYPOINT ["java", "-jar", "app.jar"] 