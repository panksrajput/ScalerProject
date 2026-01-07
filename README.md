# E-Commerce Microservices

A microservices-based e-commerce application built with Spring Boot and Spring Cloud.

## Technologies Used

- **Backend**:
  - Java 17
  - Spring Boot 3.x
  - Spring Cloud
  - Spring Security
  - JWT Authentication
  - Spring Data JPA
  - Kafka (for event-driven communication)
  - MongoDB (for product and cart services)
  - Postgres (for order and payment services)
  - Eureka Service Registry
  - OpenFeign (for service-to-service communication)

- **DevOps**:
  - Docker
  - Docker Compose
  - Maven

## Prerequisites

- Java 17 or later
- Maven 3.8+
- Docker and Docker Compose
- Postgres
- MongoDB
- Kafka
- Elasticsearch
- Redis

## Getting Started

### 1. Infrastructure Setup

1. Navigate to the `config` directory:
   ```bash
   cd config
   ```

2. Start the required services using Docker Compose:
   ```bash
   # Start database services
   docker-compose -f db/docker-compose.yml up -d
   
   # Start Kafka
   docker-compose -f kafka/docker-compose.yml up -d
   
   # Start Elasticsearch (if needed)
   docker-compose -f elasticsearch/docker-compose.yml up -d
   ```

### 2. Running Microservices

Each microservice can be run individually using Maven:

```bash
# Service Registry (run first)
cd service-registry
mvn spring-boot:run

# Auth Service
cd ../auth-service
mvn spring-boot:run

# Product Service
cd ../product-service
mvn spring-boot:run

# Cart Service
cd ../cart-service
mvn spring-boot:run

# Order Service
cd ../order-service
mvn spring-boot:run

# Payment Service
cd ../payment-service
mvn spring-boot:run

# Notification Service
cd ../notification-service
mvn spring-boot:run
```

### 3. Running the Frontend

1. Navigate to the frontend directory:
   ```bash
   cd ecommerce-frontend
   ```

2. Install dependencies and start the development server:
   ```bash
   npm install
   npm start
   ```

## API Documentation

Postman collections are available in each service's `collection` directory for testing the APIs.

## Service Ports

- Service Registry: 8761
- Auth Service: 8081
- Product Service: 8082
- Cart Service: 8083
- Order Service: 8084
- Payment Service: 8085
- Notification Service: 8086

## Testing

1. Import the Postman collection from the respective service's `collection` folder.
2. Ensure all services are running.
3. Start with the auth service to get JWT tokens.
4. Use the tokens to access other protected endpoints.
5. Pre-created admin account: admin/Admin@123

## Project Structure

```
ecommerce-microservices/
├── auth-service/          # Authentication & Authorization
├── cart-service/          # Shopping cart management
├── config/                # Docker configurations
├── ecommerce-frontend/    # Frontend application
├── notification-service/  # Notifications service
├── order-service/         # Order processing
├── payment-service/       # Payment processing
├── product-service/       # Product catalog
└── service-registry/      # Service discovery
```

## Troubleshooting

- Ensure all required services (Postgres, MongoDB, Kafka, Elasticsearch, Redis) are running.
- Check service logs for any errors.
- Verify that the service registry is running before starting other services.
- Make sure all services can connect to the service registry.

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
