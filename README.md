# Enterprise Commerce Platform

An enterprise-style e-commerce training project built on the current Spring Boot + React codebase.  
It started as a simulated storefront, and is now evolving into a realistic single-repo commerce platform with order lifecycle control, inventory traceability, payment and shipment integration points, customer address book support, refund workflow, and admin audit visibility.

## Documentation

- Requirements specification: [`docs/enterprise-commerce-requirements.md`](./docs/enterprise-commerce-requirements.md)
- Technology usage guide: [`docs/system-technology-usage.md`](./docs/system-technology-usage.md)

## Current Status

- Backend enterprise baseline is implemented and verified locally.
- Swagger / OpenAPI is enabled.
- Flyway database migrations are enabled and currently validated through `V8`.
- The React frontend now supports enterprise-facing checkout and operations flows, including address-driven order placement, customer refund handling, support ticket intake, order tagging, admin order/refund/service-desk review surfaces, and a redesigned admin operations overview.
- Kafka-backed asynchronous order event capture is enabled.
- Prometheus and Grafana are included in the local deployment stack.
- Stripe test-mode payment provider support is implemented for PaymentIntent creation and webhook intake.
- Customer checkout now embeds Stripe Elements against customer-owned orders.
- Kafka consumers retry failed order events and route poison messages into a dead-letter topic.
- GitHub Actions CI is configured for backend verify, frontend lint/build, and Docker image builds.
- Testcontainers integration testing now verifies MySQL, Redis, and Kafka in one end-to-end workflow.
- Local deployment baseline is now included with Dockerfiles, `docker-compose.yml`, environment-variable-driven configuration, and Actuator health probes.

## Core Capabilities

- JWT authentication and role-based authorization
- Customer registration, login, logout, and profile lookup
- Product catalog with pagination, keyword search, category filtering, detail caching, and popularity ranking
- Redis-backed cart
- Order creation from cart with order number generation
- Admin order status transitions with workflow validation
- Inventory adjustment records and order-linked stock movement audit
- Audit log query for operational investigation
- Simulated payment initiation and payment callback processing
- Stripe test-mode PaymentIntent initiation with webhook verification support
- Customer-side Stripe Elements checkout and PaymentIntent reconciliation
- Shipment creation, delivery confirmation, and order completion flow
- Customer address book with default address handling
- Order shipping address snapshot at checkout time
- Admin-only internal notes on orders
- Customer refund request flow and admin refund review
- Frontend checkout address selection with inline address capture
- Frontend order history view with shipment and refund follow-up
- Customer support ticket creation and admin service-desk handling
- Operational order tags for triage and exception handling
- Refund summary metrics for the admin dashboard
- Admin operations summary endpoint for order pressure, support workload, catalog readiness, and low-stock watchlists
- Kafka order lifecycle event publication and asynchronous receipt storage
- Kafka retry policy with dead-letter topic routing for failed order events
- Prometheus metrics endpoint and Grafana operations dashboard
- Frontend admin dashboard with queue metrics, low-stock watchlists, order search filters, order tagging, refund review, and support ticket actions
- Refined React shell with a more production-like storefront, catalog, and admin visual hierarchy
- Swagger-documented admin endpoints for catalog, orders, inventory, payments, shipments, refunds, and audit logs

## Tech Stack

- Java 21
- Spring Boot 4.0.3
- Spring Security
- Spring Data JPA
- Flyway
- MySQL 8.4
- Redis-compatible server  
  Tested locally with Memurai on port `6379`
- Apache Kafka 4.2
- Prometheus
- Grafana
- Stripe Java SDK for test-mode payment provider integration
- springdoc OpenAPI / Swagger UI
- React 19 + Vite 7 + TypeScript
- Docker / Docker Compose ready configuration
- GitHub Actions
- Testcontainers 2.0

## Local Environment

Tested local defaults:

- Backend: `http://127.0.0.1:8080`
- MySQL: `127.0.0.1:3306`
- Database: `ecom_enterprise`
- Redis: `127.0.0.1:6379`
- Kafka (Docker): `127.0.0.1:29092`
- Kafka UI (Docker): `http://127.0.0.1:8081`
- Prometheus (Docker): `http://127.0.0.1:9090`
- Grafana (Docker): `http://127.0.0.1:3000`

Key config files:

- [`src/main/resources/application.properties`](./src/main/resources/application.properties)
- [`src/main/resources/application-dev.properties`](./src/main/resources/application-dev.properties)
- [`src/main/resources/application-docker.properties`](./src/main/resources/application-docker.properties)
- [`docker-compose.yml`](./docker-compose.yml)
- [`frontend/nginx.conf`](./frontend/nginx.conf)

Important local defaults:

- MySQL user: `root`
- MySQL password: `ok`
- Redis password: `ok`
- Payment callback token: `local-payment-callback-token`
- Stripe provider default currency: `cad`
- Stripe test payment method fallback: `pm_card_visa`
- Stripe publishable key for local frontend wiring: `pk_test_51TAIrr6sJR5QEaTkaEPrdFKRofjAufmkFvVQxd7PXHIS7rKtAT1IdZv5M1dfHKL81eIxyZWqIHe0dDyF7PIHP3N900oGGDsLtz`
- Vite dev proxy target: `http://127.0.0.1:8080`

## Run The Backend

Start MySQL and Redis first, then run:

```powershell
.\mvnw.cmd spring-boot:run
```

Health endpoints after startup:

- `http://127.0.0.1:8080/actuator/health`
- `http://127.0.0.1:8080/actuator/health/liveness`
- `http://127.0.0.1:8080/actuator/health/readiness`

Build:

```powershell
.\mvnw.cmd clean package
```

Run the packaged jar:

```powershell
java -jar target\ecom-0.0.1-SNAPSHOT.jar
```

## Run The Frontend

```powershell
cd frontend
npm install
npm run dev
```

Frontend local default:

- Web UI: `http://127.0.0.1:5173`
- API base URL: same-origin in containers, Vite proxy in local dev

Frontend build verification:

```powershell
cd frontend
npm run build
```

## Run With Docker Compose

> [!WARNING]
> This project now follows a clean-rebuild Docker workflow by default.
> Before running Docker-based verification, clear the old Docker environment and rebuild the latest stack:
>
> ```powershell
> .\scripts\rebuild-docker-env.ps1
> ```
>
> This script will:
> - stop the current project containers
> - remove attached volumes and orphan containers
> - prune old Docker images, build cache, networks, and volumes
> - rebuild and start only the latest version of this project
>
> Local Docker WSL storage has also been capped through [`C:\Users\yisongwang\.wslconfig`](C:\Users\yisongwang\.wslconfig) with `defaultVhdSize=10GB` to prevent `C:` drive exhaustion during repeated test cycles.

Copy the root environment template first if you want custom secrets:

```powershell
Copy-Item .env.example .env
```

Then start the full stack:

```powershell
docker compose up --build
```

Expected service entry points:

- Frontend: `http://127.0.0.1:4173`
- Backend API: `http://127.0.0.1:8080`
- Swagger: `http://127.0.0.1:8080/swagger-ui.html`
- Health: `http://127.0.0.1:8080/actuator/health/readiness`
- Kafka UI: `http://127.0.0.1:8081`
- Prometheus: `http://127.0.0.1:9090`
- Grafana: `http://127.0.0.1:3000`

## CI And Integration Testing

- GitHub Actions workflow: [`.github/workflows/ci.yml`](./.github/workflows/ci.yml)
- Backend CI runs `./mvnw -B verify`
- Frontend CI runs `npm ci`, `npm run lint`, and `npm run build`
- Docker CI validates `docker compose build backend frontend`
- Testcontainers integration coverage lives in [`EnterpriseWorkflowIT.java`](./src/test/java/com/eason/ecom/integration/EnterpriseWorkflowIT.java)

The integration test boots:

- MySQL 8.4
- Redis 7.4
- Kafka

and verifies login, cart, order creation, readiness health, Kafka receipt persistence, and Kafka dead-letter routing for malformed messages.

## Swagger

After the backend starts:

- Swagger UI: `http://127.0.0.1:8080/swagger-ui.html`
- OpenAPI JSON: `http://127.0.0.1:8080/v3/api-docs`

For protected endpoints:

1. Call `POST /api/auth/login`
2. Copy the returned token
3. Click `Authorize`
4. Paste:

```text
Bearer <your-jwt-token>
```

## Seed Data

On a fresh database, the application seeds:

- 100 product master records
- 1 admin account
- 1 customer account

Default demo accounts:

- Admin
  - Username: `admin`
  - Email: `admin@ecom.local`
  - Password: `Admin123!`
- Customer
  - Username: `demo`
  - Email: `demo@ecom.local`
  - Password: `Demo123!`

## API Overview

Authentication:

- `POST /api/auth/register`
- `POST /api/auth/login`
- `POST /api/auth/logout`

User and Address Book:

- `GET /api/users/me`
- `GET /api/addresses`
- `POST /api/addresses`
- `PUT /api/addresses/{addressId}`
- `PUT /api/addresses/{addressId}/default`
- `DELETE /api/addresses/{addressId}`

Catalog and Cart:

- `GET /api/products`
- `GET /api/products/{productId}`
- `GET /api/products/categories`
- `GET /api/products/popular`
- `GET /api/cart`
- `POST /api/cart/items`
- `PUT /api/cart/items/{productId}`
- `DELETE /api/cart/items/{productId}`

Orders and Customer Order Services:

- `POST /api/orders`
- `GET /api/orders`
- `GET /api/orders/{orderId}`
- `GET /api/orders/{orderId}/payments`
- `POST /api/orders/{orderId}/payments/stripe-intent`
- `POST /api/orders/{orderId}/payments/stripe-reconcile`
- `GET /api/orders/{orderId}/shipments`
- `POST /api/orders/{orderId}/refund-requests`
- `GET /api/orders/{orderId}/refund-requests`

Payments:

- `POST /api/payments/callback`
- `POST /api/payments/stripe/webhook`

Admin:

- `GET /api/admin/dashboard/summary`
- `POST /api/admin/products`
- `PUT /api/admin/products/{productId}`
- `DELETE /api/admin/products/{productId}`
- `GET /api/admin/orders`
- `GET /api/admin/orders/search`
- `GET /api/admin/orders/{orderId}`
- `GET /api/admin/order-tags`
- `GET /api/admin/orders/{orderId}/tags`
- `POST /api/admin/orders/{orderId}/tags`
- `DELETE /api/admin/orders/{orderId}/tags/{orderTagId}`
- `PUT /api/admin/orders/{orderId}/status`
- `POST /api/admin/orders/{orderId}/payments`
- `GET /api/admin/orders/{orderId}/payments`
- `POST /api/admin/orders/{orderId}/shipments`
- `GET /api/admin/orders/{orderId}/shipments`
- `PUT /api/admin/shipments/{shipmentId}/deliver`
- `POST /api/admin/orders/{orderId}/notes`
- `GET /api/admin/orders/{orderId}/notes`
- `POST /api/admin/products/{productId}/inventory-adjustments`
- `GET /api/admin/products/{productId}/inventory-adjustments`
- `GET /api/admin/refund-requests`
- `GET /api/admin/refund-requests/summary`
- `PUT /api/admin/refund-requests/{refundRequestId}/review`
- `GET /api/admin/support-tickets`
- `PUT /api/admin/support-tickets/{ticketId}`
- `GET /api/admin/audit-logs`

## Enterprise Workflow Coverage

Implemented and verified:

- Order lifecycle:
  - `CREATED -> PAYMENT_PENDING -> PAID -> ALLOCATED -> SHIPPED -> COMPLETED`
  - `SHIPPED/COMPLETED -> REFUND_PENDING -> REFUNDED`
  - cancellation and inventory release rules
- Inventory:
  - manual adjustments
  - order reservation and release tracking
  - product cache eviction after stock-impacting changes
- Payment:
  - placeholder payment transactions
  - callback token validation
  - idempotent repeated callback handling
  - Stripe test-mode PaymentIntent creation through `providerCode=STRIPE`
  - Stripe webhook verification through `Stripe-Signature`
- Shipment:
  - shipment number generation
  - tracking data capture
  - delivered state transition
- Refund:
  - customer refund request
  - admin review
  - settlement mark on refund callback
- Support operations:
  - customer support ticket creation from order history
  - admin assignment, escalation, and resolution notes
- Order triage:
  - reusable operational tag catalog
  - tag assignment and removal on admin order results
  - refund summary metrics for live dashboard use
- Operations dashboard:
  - aggregate admin summary for order flow, queue pressure, and low-stock alerts
  - frontend control-tower cards backed by live metrics instead of static placeholders
- Customer data:
  - default address book
  - explicit address selection during order creation
  - shipping address snapshot stored on the order
- Operations:
  - internal order notes
  - admin order search
  - audit trail lookup
- Frontend delivery:
  - shipping address selection during checkout
  - inline customer address creation
  - customer shipment and refund visibility
  - admin refund review queue
  - admin order filtering by status, customer keyword, and date range

## Repository Structure

```text
ecom/
  src/main/java/com/eason/ecom
  src/main/resources
  src/test/java/com/eason/ecom
  frontend/
  pom.xml
  README.md
```

## Verification

Latest backend verification:

```powershell
.\mvnw.cmd test
```

Latest verified results:

- 41 backend tests passed
- Flyway migrations applied through `V8`
- Actuator readiness endpoint returned `UP`
- Prometheus target scrape returned `UP`
- Grafana provisioning loaded the enterprise dashboard
- Kafka order lifecycle topic was created and consumed successfully
- Real end-to-end local verification covered:
  - admin dashboard summary endpoint
  - address selection at order creation
  - order tag assignment
  - customer support ticket creation
  - admin support ticket update
  - admin order search
  - refund summary dashboard endpoint
  - payment, shipment, refund, audit, and inventory flows from previous iterations

Latest frontend verification:

```powershell
cd frontend
npm run lint
npm run build
```

Latest verified results:

- Frontend lint passed
- Frontend production build passed
- Home, catalog, and admin pages were restyled into stronger dashboard-style layouts
- Vite local development now proxies `/api`, `/swagger-ui`, `/v3/api-docs`, and `/actuator`
- Checkout page now creates orders with a selected address snapshot
- Orders page now surfaces shipment placeholders, refund requests, and support ticket intake
- Admin page now surfaces live operations metrics, low-stock watchlists, and filters orders while reviewing refunds and support tickets against live APIs

## Monitoring

Primary operations endpoints:

- Prometheus: `http://127.0.0.1:9090`
- Grafana: `http://127.0.0.1:3000`
- Readiness: `http://127.0.0.1:8080/actuator/health/readiness`
- Liveness: `http://127.0.0.1:8080/actuator/health/liveness`

Grafana dashboard file:

- [`ops/grafana/dashboards/ecom-platform-overview.json`](./ops/grafana/dashboards/ecom-platform-overview.json)

Current dashboard coverage includes:

- service availability for backend and Prometheus
- HTTP error rate
- order API mean latency
- Kafka reliability signals
- HTTP request rate
- Kafka throughput
- order status transition rate
- refund workflow activity
- payment callback counters
- dead-letter topic routing through Kafka retry and dead-letter counters

Kafka message inspection:

- Kafka UI: `http://127.0.0.1:8081`
- Cluster name: `ecom-local`
- Main topic: `ecom.order.lifecycle.v1`
- Dead-letter topic: `ecom.order.lifecycle.v1.dlt`

## Stripe Test Mode

To exercise the Stripe-backed payment path, provide these environment variables:

- `ECOM_STRIPE_ENABLED=true`
- `ECOM_STRIPE_SECRET_KEY=sk_test_...`
- `ECOM_STRIPE_WEBHOOK_SECRET=whsec_...` for verified webhook delivery
- `VITE_STRIPE_PUBLISHABLE_KEY=pk_test_51TAIrr6sJR5QEaTkaEPrdFKRofjAufmkFvVQxd7PXHIS7rKtAT1IdZv5M1dfHKL81eIxyZWqIHe0dDyF7PIHP3N900oGGDsLtz`

The publishable key above can live in frontend configuration. The Stripe secret key and webhook secret remain environment-only and are intentionally not committed into the repository.

Example admin payment request:

```json
{
  "paymentMethod": "CARD",
  "amount": 129.99,
  "providerCode": "STRIPE",
  "currency": "cad",
  "providerPaymentMethodToken": "pm_card_visa",
  "confirmImmediately": true,
  "note": "Stripe test mode payment"
}
```

This path will create a Stripe PaymentIntent in test mode. If the request is confirmed immediately with `pm_card_visa`, the platform will mark the payment as settled without waiting for a separate callback. Webhook support remains available for later asynchronous events.

Customer checkout path:

- `POST /api/orders/{orderId}/payments/stripe-intent` creates a Stripe PaymentIntent and returns `clientSecret`
- React mounts Stripe Elements inside the customer orders workspace
- `POST /api/orders/{orderId}/payments/stripe-reconcile` pulls the latest PaymentIntent status back into the order workflow after `stripe.confirmPayment(...)`

Latest verified Stripe sandbox sample:

- Order: `ORD-20260312235017910-5624`
- Transaction ref: `PAY-20260312235018153-3279`
- PaymentIntent: `pi_3TAIzl6sJR5QEaTk02ucuzsY`
- Stripe status: `succeeded`
- Platform order status after payment: `PAID`

## Notes

- Shipment and refund integrations are still platform-managed placeholders, not production carrier or PSP refund connections.
- This project is currently implemented as a modular monolith, which is intentional for this phase.
- Docker Compose has been exercised with MySQL, Redis, Kafka, backend, frontend, Prometheus, and Grafana.
- A live Stripe sandbox call still requires a project-specific `sk_test_...` key and, for webhook verification, a matching `whsec_...` secret. Those secrets are intentionally excluded from version control.
