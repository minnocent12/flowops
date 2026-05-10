# FlowOps

A microservices-based order fulfillment system built with Spring Boot, PostgreSQL, and Kafka.

## Services

| Service | Port | Responsibility |
|---|---|---|
| `order-service` | 8081 | Accept or reject orders based on inventory; publishes accepted orders to Kafka |
| `inventory-service` | 8082 | Track warehouse stock levels per SKU |
| `warehouse-service` | 8083 | Consumes accepted orders; routes to best warehouse; publishes route assignments |
| `shipment-service` | 8084 | Consumes route assignments; creates shipments with carrier and tracking ID |
| `postgres` | 5432 | Shared PostgreSQL database |
| `kafka` | 9092 | Event bus between services |

## How it works

Orders flow asynchronously through Kafka after the initial HTTP inventory check:

```
POST /api/orders
      │
      ▼
 order-service ──HTTP──▶ inventory-service
      │                   GET /api/inventory/check
      │◀── true/false ───┘
      │
      │  (if ACCEPTED)
      ▼
   Kafka: order.accepted
      │
      ▼
 warehouse-service ──HTTP──▶ inventory-service
      │                       GET /api/inventory/{sku}
      │◀── stock per warehouse ┘
      │  (picks warehouse with most stock)
      ▼
   Kafka: route.assigned
      │
      ▼
 shipment-service
      │
  creates shipment with carrier + tracking ID
```

All Kafka consumers implement idempotency — re-delivering the same event will not create duplicate shipments.

## Project Structure

```
flowops/
├── docker-compose.yml
├── resources/
│   └── db/
│       ├── order-schema.sql
│       ├── inventory-schema.sql      # includes warehouse seed data
│       └── shipment-schema.sql
├── order-service/
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/main/java/com/flowops/order/
│       ├── OrderServiceApplication.java
│       ├── config/AppConfig.java
│       ├── messaging/OrderEventPublisher.java
│       ├── controller/OrderController.java
│       ├── service/OrderService.java
│       ├── repository/OrderRepository.java
│       ├── model/Order.java
│       ├── dto/OrderRequest.java
│       └── dto/OrderResponse.java
├── inventory-service/
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/main/java/com/flowops/inventory/
│       ├── InventoryServiceApplication.java
│       ├── controller/InventoryController.java
│       ├── service/InventoryService.java
│       ├── repository/InventoryRepository.java
│       └── model/Inventory.java
├── warehouse-service/
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/main/java/com/flowops/warehouse/
│       ├── WarehouseServiceApplication.java
│       ├── config/KafkaConfig.java
│       ├── consumer/OrderEventConsumer.java
│       ├── service/RoutingService.java
│       ├── client/InventoryClient.java
│       ├── model/OrderEvent.java
│       └── model/WarehouseStock.java
└── shipment-service/
    ├── Dockerfile
    ├── pom.xml
    └── src/main/java/com/flowops/shipment/
        ├── ShipmentServiceApplication.java
        ├── config/KafkaConfig.java
        ├── consumer/RouteAssignedConsumer.java
        ├── service/ShipmentService.java
        ├── repository/ShipmentRepository.java
        └── model/Shipment.java
```

## Running locally

**Prerequisites:** Docker Desktop

First run (or after schema changes):
```bash
docker-compose down -v
docker-compose up --build
```

Subsequent runs (no schema changes):
```bash
docker-compose up --build
```

Zookeeper and Kafka start first, then Postgres. All four services wait for both to pass their healthchecks before starting.

## Swagger UI

| Service | URL |
|---|---|
| Order Service | http://localhost:8081/swagger-ui.html |
| Inventory Service | http://localhost:8082/swagger-ui.html |
| Warehouse Service | http://localhost:8083/swagger-ui.html |
| Shipment Service | http://localhost:8084/swagger-ui.html |

## API Endpoints

### Order Service

| Method | Path | Description |
|---|---|---|
| `POST` | `/api/orders` | Place a new order |
| `GET` | `/api/orders` | List all orders |
| `GET` | `/api/orders/{id}` | Get order by ID |

**Place an order:**
```json
POST /api/orders
{
  "customerId": "CUST-001",
  "sku": "SKU-001",
  "quantity": 10
}
```

### Inventory Service

| Method | Path | Description |
|---|---|---|
| `GET` | `/api/inventory` | List all inventory records |
| `GET` | `/api/inventory/{sku}` | Get stock per warehouse for a SKU |
| `GET` | `/api/inventory/check?sku=&quantity=` | Check if enough stock exists |

## Kafka Topics

| Topic | Producer | Consumer | Payload |
|---|---|---|---|
| `order.accepted` | order-service | warehouse-service | `{ orderId, sku, quantity }` |
| `route.assigned` | warehouse-service | shipment-service | `{ orderId, sku, quantity, warehouseId }` |

## Warehouse Routing Algorithm

The warehouse-service picks the fulfillment location with the **highest available stock** that can cover the full requested quantity. Tie-break: reverse alphabetical warehouse ID for consistent routing.

## Seed Data

The database is pre-loaded with stock across three warehouses:

| Warehouse | SKU-001 | SKU-002 |
|---|---|---|
| WH-ATLANTA | 150 | 80 |
| WH-DALLAS | 200 | 0 |
| WH-CHICAGO | 50 | 120 |

## Tech Stack

- Java 17
- Spring Boot 3.2.5
- Spring Data JPA / Hibernate
- PostgreSQL 15
- Apache Kafka (Confluent Platform 7.6.0)
- springdoc-openapi (Swagger UI)
- Docker / Docker Compose
