# SPMS Centralized Microservices Configuration Repository (`spms-config-repo`)

This directory serves as the centralized configuration repository for all microservices in the Smart Parking Management System (SPMS), loaded by the Spring Cloud **Config Server** (Port: 8888).

## Microservice Configuration Files

| Service | Configuration File | Key Settings Provided |
| :--- | :--- | :--- |
| **API Gateway** | [`api-gateway.yml`](./api-gateway.yml) | Reactive dynamic routing, CORS policy, public auth whitelists |
| **Eureka Server** | [`eureka-server.yml`](./eureka-server.yml) | Service discovery registry settings, eviction timeouts |
| **User Service** | [`user-service.yml`](./user-service.yml) | PostgreSQL DB credentials, RSA JWT Keystore paths, JPA settings |
| **Vehicle Service** | [`vehicle-service.yml`](./vehicle-service.yml) | PostgreSQL DB connection, NestJS port configuration |
| **Parking Service** | [`parking-service.yml`](./parking-service.yml) | PostGIS spatial DB connection, Redis cache TTL, RabbitMQ exchange |
| **Payment Service** | [`payment-service.yml`](./payment-service.yml) | PostgreSQL DB connection, RabbitMQ payment events |
| **Notification Service** | [`notification-service.yml`](./notification-service.yml) | SMTP Mail credentials, RabbitMQ notification queues |

## How It Works

1. **On Startup:** The Spring Cloud Config Server automatically reads these YAML files from `spms-config-repo/`.
2. **Dynamic Distribution:** When other microservices boot up (e.g. `user-service`, `parking-service`), they fetch their specific environment configuration from `config-server:8888` on startup via `spring.config.import=optional:configserver:http://localhost:8888`.
