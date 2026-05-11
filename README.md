# FlowOps — Fulfillment Order Orchestrator

A production-grade distributed system simulating how large retailers like Home Depot route orders across fulfillment centers. Built as an internship preparation project for the Home Depot Supply Chain Technology Group.

## Architecture

```
Customer → Order Service → Kafka (order.accepted)
                                    ↓
                          Warehouse Service (routing algorithm)
                                    ↓ HTTP
                          Inventory Service (Redis cached)
                                    ↓
                          Kafka (route.assigned)
                                    ↓
                          Shipment Service → PostgreSQL
```

## Services

| Service | Port | Responsibility |
|---|---|---|
| order-service | 8081 | Receives orders, validates inventory, publishes Kafka events |
| inventory-service | 8082 | Stock levels per warehouse, Redis cached |
| warehouse-service | 8083 | "Ship from best location" routing algorithm |
| shipment-service | 8084 | Creates shipments, assigns carrier and tracking ID |
| dashboard | 3000 | React live pipeline view |

## Tech Stack

- **Backend** — Java 17, Spring Boot 3, Spring Kafka
- **Messaging** — Apache Kafka + Zookeeper
- **Database** — PostgreSQL 15
- **Caching** — Redis 7
- **Resilience** — Resilience4j circuit breaker
- **API Docs** — OpenAPI / Swagger UI
- **Testing** — JUnit 5, Mockito, AssertJ
- **CI** — GitHub Actions
- **Infra** — Docker Compose

## Quick Start

```bash
# Clone and start everything with one command
git clone https://github.com/your-username/flowops.git
cd flowops
docker-compose up --build
```

**Start the dashboard (separate terminal):**

```bash
cd dashboard
npm install
npm run dev
# Open http://localhost:3000
```

**Verify all services are running:**

| URL | Description |
|---|---|
| http://localhost:8081/swagger-ui.html | Order Service API |
| http://localhost:8082/swagger-ui.html | Inventory Service API |
| http://localhost:8083/swagger-ui.html | Warehouse Service API |
| http://localhost:8084/swagger-ui.html | Shipment Service API |
| http://localhost:3000 | React Dashboard |

## Place a Test Order

```bash
# Accepted order
curl -X POST http://localhost:8081/api/orders \
  -H "Content-Type: application/json" \
  -d '{"customerId":"CUST-001","sku":"SKU-001","quantity":10}'

# Rejected order (out of stock)
curl -X POST http://localhost:8081/api/orders \
  -H "Content-Type: application/json" \
  -d '{"customerId":"CUST-002","sku":"SKU-001","quantity":9999}'

# View metrics
curl http://localhost:8081/api/metrics
curl http://localhost:8084/api/metrics
```

## Seed Data

The database is pre-loaded with stock across three warehouses:

| Warehouse | SKU-001 | SKU-002 |
|---|---|---|
| WH-ATLANTA | 150 | 80 |
| WH-DALLAS | 200 | 0 |
| WH-CHICAGO | 50 | 120 |

Orders requesting more than the available quantity per SKU will be `REJECTED_OUT_OF_STOCK`.

## Kafka Topics

| Topic | Producer | Consumer | Payload |
|---|---|---|---|
| `order.accepted` | order-service | warehouse-service | `{ orderId, sku, quantity }` |
| `route.assigned` | warehouse-service | shipment-service | `{ orderId, sku, quantity, warehouseId }` |

## Routing Algorithm

The "ship from best location" algorithm in `RoutingService.java`:

1. Fetches stock levels across all warehouses for the requested SKU
2. Filters to warehouses that can fulfill the requested quantity
3. Selects the warehouse with the highest stock (maximizes utilization)
4. Tie-breaks alphabetically by warehouse ID for consistent routing

## Key Engineering Concepts Demonstrated

- **Event-driven architecture** — services communicate via Kafka topics, not direct HTTP calls
- **Idempotency** — duplicate Kafka events do not create duplicate shipments
- **Circuit breaker** — order service degrades gracefully when inventory is unavailable
- **Redis caching** — inventory lookups cached with 10s TTL to reduce DB load
- **Structured logging** — correlation IDs trace a single order across all services
- **OpenAPI** — all endpoints documented and testable via Swagger UI
- **CI pipeline** — tests run automatically on every push via GitHub Actions

## Running Tests

```bash
# Run all unit tests across all services
cd order-service     && mvn test
cd warehouse-service && mvn test
cd shipment-service  && mvn test
```

| Service | Tests | What's covered |
|---|---|---|
| order-service | 3 | Accept/reject logic, Kafka event gating |
| warehouse-service | 6 | Routing: best-stock selection, tie-breaking, edge cases |
| shipment-service | 3 | Idempotency, carrier assignment consistency |

## Simulate Failures

```bash
# Simulate inventory service going down
curl -X POST http://localhost:8081/api/simulate/inventory-down

# Place order — circuit breaker activates, order saved as PENDING
curl -X POST http://localhost:8081/api/orders \
  -H "Content-Type: application/json" \
  -d '{"customerId":"CUST-001","sku":"SKU-001","quantity":5}'

# Restore inventory service
curl -X POST http://localhost:8081/api/simulate/inventory-up
```

Circuit breaker config: 5-request sliding window, opens at 50% failure rate, recovers after 10s with 2 probe calls.

## Project Structure

```
flowops/
├── docker-compose.yml
├── order-service/        # Spring Boot — REST API + Kafka producer
├── inventory-service/    # Spring Boot — stock checks + Redis cache
├── warehouse-service/    # Spring Boot — routing algorithm + Kafka consumer
├── shipment-service/     # Spring Boot — shipment creation + Kafka consumer
├── dashboard/            # React — live order pipeline view
└── resources/db/         # PostgreSQL schemas
```
