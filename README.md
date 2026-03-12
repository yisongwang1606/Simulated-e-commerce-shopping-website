# Enterprise Commerce Platform

An enterprise-style e-commerce training project built on the current Spring Boot + React codebase.  
It started as a simulated storefront, and is now evolving into a realistic single-repo commerce platform with order lifecycle control, inventory traceability, payment and shipment integration points, customer address book support, refund workflow, and admin audit visibility.

## Current Status

- Backend enterprise baseline is implemented and verified locally.
- Swagger / OpenAPI is enabled.
- Flyway database migrations are enabled and currently validated through `V5`.
- The React frontend now supports enterprise-facing checkout and operations flows, including address-driven order placement, customer refund handling, and admin order/refund review surfaces.

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
- Frontend admin dashboard with order search filters and refund review actions
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

## Local Environment

Tested local defaults:

- Backend: `http://127.0.0.1:8080`
- MySQL: `127.0.0.1:3306`
- Database: `ecom_enterprise`
- Redis: `127.0.0.1:6379`

Key config files:

- [`src/main/resources/application.properties`](./src/main/resources/application.properties)
- [`src/main/resources/application-dev.properties`](./src/main/resources/application-dev.properties)

Important local defaults:

- MySQL user: `root`
- MySQL password: `ok`
- Redis password: `ok`
- Payment callback token: `local-payment-callback-token`

## Run The Backend

Start MySQL and Redis first, then run:

```powershell
.\mvnw.cmd spring-boot:run
```

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

Frontend build verification:

```powershell
cd frontend
npm run build
```

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

- `POST /api/admin/products`
- `PUT /api/admin/products/{productId}`
- `DELETE /api/admin/products/{productId}`
- `GET /api/admin/orders`
- `GET /api/admin/orders/search`
- `GET /api/admin/orders/{orderId}`
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
- `PUT /api/admin/refund-requests/{refundRequestId}/review`
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
.\mvnw.cmd clean test
```

Latest verified results:

- 27 backend tests passed
- Flyway migrations applied through `V5`
- Real end-to-end local verification covered:
  - address selection at order creation
  - admin order search
  - payment initiation and callback
  - shipment creation and delivery
  - refund request, review, and refund settlement callback
  - audit and inventory side effects

Latest frontend verification:

```powershell
cd frontend
npm run lint
npm run build
```

Latest verified results:

- Frontend lint passed
- Frontend production build passed
- Checkout page now creates orders with a selected address snapshot
- Orders page now surfaces shipment placeholders and refund requests
- Admin page now filters orders and reviews refund requests against live APIs

## Notes

- Payment, shipment, and refund integrations are still simulated integration points, not production third-party gateway connections.
- This project is currently implemented as a modular monolith, which is intentional for this phase.
- The frontend is now aligned with the core enterprise APIs, but deeper production concerns such as observability, CI/CD, and external gateway integration still remain for later phases.
