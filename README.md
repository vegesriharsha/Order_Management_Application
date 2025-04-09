# Event-Driven Order Management System

An event-driven microservice for order management built with Spring Boot, Java 21, and RabbitMQ.

## Technologies

- Java 21
- Spring Boot 3.2.3
- RabbitMQ
- Gradle
- JUnit 5
- H2 Database
- Spring Data JPA

## Project Structure

- `src/main/java/com/example/eventdriven/` - Main application code
    - `config/` - Configuration classes
    - `controller/` - REST controllers
    - `filter/` - Request filters
    - `health/` - Health check implementations
    - `listener/` - RabbitMQ event listeners
    - `logging/` - Centralized logging components
    - `model/` - Data models
    - `service/` - Business logic services
    - `repository/` - Data access layer

## Key Features

### Event-Driven Communication

The application uses RabbitMQ for message-based communication with the following exchange types:

- **Command Exchange** (`service.command`): For direct commands
- **Event Exchange** (`service.event`): For publishing domain events
- **Broadcast Exchange** (`service.broadcast`): For system-wide notifications
- **Dead Letter Exchange** (`service.deadletter`): For failed messages
- **Logging Exchange** (`service.logging`): For centralized logging

### Centralized Logging

All application logs are published to a dedicated RabbitMQ exchange, which can be processed by an ELK stack (Elasticsearch, Logstash, Kibana) for storage and visualization.

### Message Retry and Recovery

Failed message processing is handled with:

- **Exponential Backoff**: Increasing delays between retries
- **Dead Letter Queue**: Storage for messages that fail after max retries
- **Manual Acknowledgment**: Ensures messages are properly processed

### Graceful Shutdown

The application implements a graceful shutdown mechanism to complete in-flight message processing during application shutdown.

## API Endpoints

### Order Management

- `POST /api/orders` - Create a new order
- `GET /api/orders/{orderId}` - Get order by ID
- `GET /api/orders/customer/{customerId}` - Get all orders for a customer
- `PUT /api/orders/{orderId}/status` - Update order status
- `POST /api/orders/{orderId}/cancel` - Cancel an order

## Setup and Running

### Prerequisites

- Java 21
- Gradle
- RabbitMQ (running locally or in Docker)

### Running RabbitMQ with Docker

```bash
docker run -d --name rabbitmq -p 5672:5672 -p 15672:15672 rabbitmq:3.9-management
```

### Building and Running the Application

```bash
./gradlew clean build
./gradlew bootRun
```

### Running Tests

```bash
./gradlew test
```

### Checking Test Coverage

```bash
./gradlew jacocoTestReport
```

The test coverage report will be available at `build/reports/jacoco/test/html/index.html`

## Monitoring

The application exposes Spring Boot Actuator endpoints for monitoring:

- Health: `http://localhost:8080/actuator/health`
- Info: `http://localhost:8080/actuator/info`
- Prometheus metrics: `http://localhost:8080/actuator/prometheus`
