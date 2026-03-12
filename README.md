# Enterprise Commerce Platform

An enterprise-style e-commerce training project built on the current Spring Boot + React codebase.  
It started as a simulated storefront, and is now evolving into a realistic single-repo commerce platform with order lifecycle control, inventory traceability, payment and shipment integration points, customer address book support, refund workflow, and admin audit visibility.

## Documentation

- Requirements specification: [`docs/enterprise-commerce-requirements.md`](./docs/enterprise-commerce-requirements.md)
- Technology usage guide: [`docs/system-technology-usage.md`](./docs/system-technology-usage.md)

## Current Status

- Backend enterprise baseline is implemented and verified locally.
- Swagger / OpenAPI is enabled.
- Flyway database migrations are enabled and currently validated through `V6`.
- The React frontend now supports enterprise-facing checkout and operations flows, including address-driven order placement, customer refund handling, support ticket intake, order tagging, admin order/refund/service-desk review surfaces, and a redesigned admin operations overview.
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
- springdoc OpenAPI / Swagger UI
- React 19 + Vite 7 + TypeScript
- Docker / Docker Compose ready configuration

## Local Environment

Tested local defaults:

- Backend: `http://127.0.0.1:8080`
- MySQL: `127.0.0.1:3306`
- Database: `ecom_enterprise`
- Redis: `127.0.0.1:6379`

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
- `GET /api/orders/{orderId}/shipments`
- `POST /api/orders/{orderId}/refund-requests`
- `GET /api/orders/{orderId}/refund-requests`

Payments:

- `POST /api/payments/callback`

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

- 35 backend tests passed
- Flyway migrations applied through `V6`
- Actuator readiness endpoint returned `UP`
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

## Notes

- Payment, shipment, and refund integrations are still simulated integration points, not production third-party gateway connections.
- This project is currently implemented as a modular monolith, which is intentional for this phase.
- Dockerfiles and Compose configuration are included, but Docker CLI was not available on this machine during this verification pass, so the YAML and container build flow were prepared but not executed here.
- The frontend is now aligned with the core enterprise APIs, but deeper production concerns such as CI/CD, external gateway integration, and full observability stacks still remain for later phases.
