# Docker Setup for Authentication Service

This guide explains how to build and run the Authentication Service using Docker.

## Prerequisites

- Docker installed on your machine
- Docker Compose installed on your machine

## Building and Running with Docker Compose

1. Navigate to the authentication-service directory:
   ```
   cd authentication-service
   ```

2. Build and start the containers:
   ```
   docker-compose up -d
   ```
   This will start both the authentication service and a MySQL database container.

3. To view logs:
   ```
   docker-compose logs -f
   ```

4. To stop the containers:
   ```
   docker-compose down
   ```

## Building and Running with Docker (without Compose)

1. Build the Docker image:
   ```
   docker build -t auth-service .
   ```

2. Run the container:
   ```
   docker run -p 8082:8082 \
     -e SPRING_DATASOURCE_URL=jdbc:mysql://host.docker.internal:3307/shopshoe2?useSSL=false \
     -e SPRING_DATASOURCE_USERNAME=root \
     -e SPRING_DATASOURCE_PASSWORD=123456789 \
     -e EUREKA_CLIENT_SERVICEURL_DEFAULTZONE=http://host.docker.internal:8761/eureka \
     --name auth-service \
     auth-service
   ```

## Environment Variables

You can customize the following environment variables:

- `SERVER_PORT`: The port on which the service will run (default: 8082)
- `SPRING_DATASOURCE_URL`: MySQL database URL
- `SPRING_DATASOURCE_USERNAME`: Database username
- `SPRING_DATASOURCE_PASSWORD`: Database password
- `EUREKA_CLIENT_SERVICEURL_DEFAULTZONE`: URL of the Eureka server

## Connecting to the Service

The authentication service will be available at:
- http://localhost:8082

## Integration with Other Services

When running in Docker, update other services to use the container name instead of localhost:
- For services inside the same Docker network, use: `http://auth-service:8082` 