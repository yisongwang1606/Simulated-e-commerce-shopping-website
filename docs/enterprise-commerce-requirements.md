# Enterprise Commerce Platform Requirements Specification

## 1. Document Control

| Item | Value |
| --- | --- |
| Document Name | Enterprise Commerce Platform Requirements Specification |
| Project Code | ECOM-ENT-001 |
| Version | v2.1 |
| Status | Updated to match the implemented system |
| Last Updated | March 13, 2026 |
| Business Domain | B2C retail e-commerce |
| Target Market | Canada |
| Primary Language | English |
| Currency | CAD |
| Reference Repository | `Simulated-e-commerce-shopping-website` |
| Source of Truth | Current Spring Boot + React implementation and verified runtime behavior |

## 2. Executive Summary

This project has evolved from a simulated shopping demo into a realistic enterprise-style commerce platform baseline.

The current system is implemented as a modular monolith with:

- a customer-facing storefront
- a separated admin operations portal
- governed order lifecycle handling
- Redis-backed cart and token state
- MySQL-based business persistence
- payment, shipment, refund, support, and audit workflows
- Kafka-backed asynchronous order event processing
- Prometheus and Grafana observability baseline
- GitHub Actions CI baseline
- Testcontainers integration testing baseline
- Swagger/OpenAPI documentation
- Docker-based local deployment
- Actuator health endpoints for operational readiness

The purpose of the current phase is not to imitate a full production marketplace with external PSP and carrier integrations, but to deliver a codebase and workflow model that closely matches real enterprise development work.

## 3. Business Objectives

### 3.1 Primary Objectives

- Provide a realistic e-commerce system structure rather than a classroom CRUD sample.
- Separate customer operations and admin operations in both UI and access control.
- Keep a maintainable single-repository delivery model for the current phase.
- Support realistic review, demo, training, and system extension scenarios.

### 3.2 Measurable Objectives

- Persist and serve at least 100 catalog products.
- Support a full customer flow from login to order placement and after-sales requests.
- Support a full admin flow from order search to payment, shipment, refund review, and support handling.
- Maintain documented and testable APIs through Swagger/OpenAPI.
- Support local full-stack startup through Docker Compose.

## 4. Current Product Scope

### 4.1 Customer Portal Scope

The customer portal currently includes:

- customer login
- product catalog browsing
- keyword search
- category filtering
- product detail display
- cart management
- address book management
- address selection during checkout
- order history
- shipment visibility
- refund request submission
- support ticket creation for orders

The customer portal must not expose internal admin operations, infrastructure details, or internal workflow controls.

### 4.2 Admin Portal Scope

The admin portal currently includes:

- dedicated admin login entry
- operations dashboard summary
- order search and filtering
- order status updates
- order note management
- order tag assignment
- refund review queue
- support ticket handling
- product maintenance
- inventory adjustment visibility and updates
- low-stock watchlist visibility
- audit log query

The admin portal must not act as a customer shopping interface.

### 4.3 Shared Platform Scope

The shared backend platform currently includes:

- JWT authentication and logout
- role-based access control
- MySQL persistence
- Redis-backed token state, cart state, and product ranking state
- Flyway schema migrations through `V8`
- Kafka order lifecycle topic and asynchronous receipt persistence
- Prometheus metrics exposure and Grafana dashboard provisioning
- Swagger/OpenAPI exposure
- Actuator health checks
- Docker Compose deployment baseline

## 5. Roles and Access Model

| Role | Description | Current Access |
| --- | --- | --- |
| `CUSTOMER` | End user purchasing products and requesting after-sales service | Customer portal and customer APIs |
| `ADMIN` | Operations user handling catalog, orders, refunds, inventory, and support | Admin portal and admin APIs |

### 5.1 Current Access Rules

- Customer APIs require authentication where ownership and personal data are involved.
- Admin APIs require authenticated `ADMIN` role.
- Customer and admin portals use separate route trees and separate login entries.
- Admin login success redirects to `/admin`.
- Customer login success redirects to `/`.

## 6. Functional Requirements

### 6.1 Authentication and Session Management

- Users can log in using either username or email.
- JWT is issued on successful login.
- Token validity is tracked through Redis.
- Logout invalidates the current token.
- Default seeded accounts:
  - `admin@ecom.local / Admin123!`
  - `demo@ecom.local / Demo123!`

### 6.2 Product Catalog

Each product currently supports the following core fields:

- `sku`
- `name`
- `brand`
- `category`
- `description`
- `price`
- `costPrice`
- `stock`
- `safetyStock`
- `status`
- `taxClass`
- `weightKg`
- `leadTimeDays`
- `featured`

Current catalog capabilities:

- paginated listing
- keyword search
- category filtering
- detail lookup
- popularity ranking

### 6.3 Cart

- Cart is stored per customer in Redis.
- Adding the same product merges quantity.
- Quantity updates revalidate stock.
- Cart response includes line totals and overall total.
- Cart is cleared after successful order creation.

### 6.4 Address Book

- Customers can create, update, and delete addresses.
- Customers can define one default address.
- Checkout can explicitly choose an address.
- Orders store a shipping address snapshot when created.

### 6.5 Order Management

Order creation currently enforces:

- authenticated customer
- non-empty cart
- valid product references
- stock availability
- valid selected address if provided

Current order lifecycle implemented:

- `CREATED`
- `PAYMENT_PENDING`
- `PAID`
- `ALLOCATED`
- `SHIPPED`
- `COMPLETED`
- `CANCELLED`
- `REFUND_PENDING`
- `REFUNDED`

Current order data retained:

- order number
- customer identity
- item snapshots
- subtotal amount
- tax amount
- shipping amount
- discount amount
- total amount
- shipping address snapshot
- internal notes
- tags
- audit trail references

### 6.6 Payment Workflow

Current phase supports both the existing simulated gateway flow and a Stripe test-mode provider path.

Implemented behavior:

- admin can create a payment transaction
- callback endpoint validates a callback token
- Stripe-backed PaymentIntent creation is available through `providerCode=STRIPE`
- Stripe webhook endpoint validates `Stripe-Signature` and maps supported events into the internal payment workflow
- successful payment callback advances the order payment flow
- refund callback settles the refund flow
- duplicate callbacks are processed idempotently

### 6.7 Shipment Workflow

Current phase uses shipment placeholders rather than carrier APIs.

Implemented behavior:

- admin can create shipment records
- shipment stores carrier code and tracking number
- delivery confirmation can complete the order
- customer can query shipment records from order history

### 6.8 Refund Workflow

Implemented behavior:

- customer can submit refund requests for eligible orders
- admin can approve or reject refund requests
- approved requests move the order into `REFUND_PENDING`
- refund callback settles the request and marks the order `REFUNDED`
- admin dashboard and refund summary APIs aggregate refund pressure

### 6.9 Support Ticket Workflow

Implemented behavior:

- customer can create an order-linked support ticket
- admin can search and update support tickets
- ticket records support assignee, latest note, resolution note, and status

### 6.10 Admin Operations

Implemented admin capabilities:

- operations dashboard summary
- order search and filtering
- order detail lookup
- order tagging
- internal order notes
- refund review
- shipment handling
- payment handling
- inventory adjustment handling
- audit log query
- support ticket queue management

## 7. Data Scope and Structure

### 7.1 Seed Data

The system currently seeds:

- 100 product master records
- 1 admin account
- 1 customer account

The seed product master data is maintained in:

- `Desktop/EcomProject/mock-data/product-master-100.csv`
- repository seed mirror under `src/main/resources/seed-data/product-master-100.csv`

### 7.2 Primary Business Entities

The current implementation includes at least the following entities:

- `User`
- `Product`
- `CustomerOrder`
- `OrderItem`
- `CustomerAddress`
- `PaymentTransaction`
- `Shipment`
- `RefundRequest`
- `SupportTicket`
- `OrderInternalNote`
- `OrderTag`
- `OrderTagAssignment`
- `InventoryAdjustment`
- `AuditLog`

### 7.3 Systems of Record

- MySQL is the system of record for transactional and operational business data.
- Redis is the operational store for cart state, token state, cache entries, and product popularity ranking.

## 8. API Baseline

### 8.1 Authentication

- `POST /api/auth/register`
- `POST /api/auth/login`
- `POST /api/auth/logout`

### 8.2 Customer APIs

- `GET /api/users/me`
- `GET /api/addresses`
- `POST /api/addresses`
- `PUT /api/addresses/{addressId}`
- `PUT /api/addresses/{addressId}/default`
- `DELETE /api/addresses/{addressId}`
- `GET /api/products`
- `GET /api/products/categories`
- `GET /api/products/popular`
- `GET /api/products/{productId}`
- `GET /api/cart`
- `POST /api/cart/items`
- `PUT /api/cart/items/{productId}`
- `DELETE /api/cart/items/{productId}`
- `POST /api/orders`
- `GET /api/orders`
- `GET /api/orders/{orderId}`
- `GET /api/orders/{orderId}/shipments`
- `POST /api/orders/{orderId}/refund-requests`
- `GET /api/orders/{orderId}/refund-requests`
- `POST /api/orders/{orderId}/support-tickets`
- `GET /api/orders/{orderId}/support-tickets`

### 8.3 Shared Integration API

- `POST /api/payments/callback`
- `POST /api/payments/stripe/webhook`

### 8.4 Admin APIs

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
- `GET /api/admin/refund-requests`
- `GET /api/admin/refund-requests/summary`
- `PUT /api/admin/refund-requests/{refundRequestId}/review`
- `GET /api/admin/support-tickets`
- `PUT /api/admin/support-tickets/{ticketId}`
- `POST /api/admin/products/{productId}/inventory-adjustments`
- `GET /api/admin/products/{productId}/inventory-adjustments`
- `GET /api/admin/audit-logs`

## 9. Non-Functional and Operational Requirements

### 9.1 Runtime Baseline

- Backend runtime: Java 21
- Database: MySQL 8.4
- Cache/session/cart state: Redis 7.4 compatible runtime
- Frontend: React 19 + TypeScript + Vite 7

### 9.2 Deployment Baseline

- Local standalone execution is supported.
- Local full-stack container orchestration is supported through Docker Compose.
- Customer storefront and admin portal containers are served independently through nginx.
- Backend exposes readiness and liveness endpoints through Actuator.
- Kafka, Prometheus, and Grafana are included in the containerized local stack.
- GitHub Actions validates backend verify, customer/admin lint-build jobs, and Docker image builds.
- Testcontainers verifies MySQL, Redis, and Kafka through an end-to-end integration test.

### 9.3 Documentation Baseline

- Swagger/OpenAPI must remain enabled for API review and integration testing.
- README and desktop documentation must stay aligned with actual implemented behavior.

### 9.4 Testing Baseline

The current implementation is expected to pass:

- backend automated tests
- customer storefront lint and production build
- admin portal lint and production build
- Docker-based runtime health checks
- end-to-end smoke validation through real API calls

## 10. Validation Evidence

The following validation evidence has already been recorded for the current implementation:

- backend automated tests passed: `41`
- customer-web `npm run lint` passed
- customer-web `npm run build` passed
- admin-web `npm run lint` passed
- admin-web `npm run build` passed
- Docker Compose runtime came up successfully
- readiness endpoint returned `UP`
- Prometheus targets returned `UP`
- Grafana dashboard provisioning completed successfully
- Kafka broker endpoints came up successfully
- Testcontainers integration test passed against MySQL, Redis, and Kafka
- real Stripe sandbox PaymentIntent creation succeeded with platform reconciliation back to `PAID`
- real smoke flow executed successfully on March 12, 2026

### 10.1 Real Smoke Flow Summary

Validated scenario:

- customer login
- address creation
- cart population
- order placement
- support ticket creation
- admin dashboard access
- order search
- internal note creation
- order tag assignment
- payment initiation and success callback
- shipment creation and delivery confirmation
- refund request creation
- refund review and refund settlement callback

Reference validation sample:

- order number: `ORD-20260312215111221-4696`
- final status: `REFUNDED`
- support ticket status: `IN_PROGRESS`
- payment status: `REFUNDED`
- shipment status: `DELIVERED`

Additional Stripe sandbox verification:

- order number: `ORD-20260312235017910-5624`
- transaction ref: `PAY-20260312235018153-3279`
- provider reference: `pi_3TAIzl6sJR5QEaTk02ucuzsY`
- provider status: `succeeded`
- platform order status after payment: `PAID`

## 11. Known Gaps and Next-Phase Recommendations

The current system is a strong enterprise-style baseline, but still has planned expansion areas:

- real carrier and fulfillment integration
- richer permission model beyond `ADMIN` and `CUSTOMER`
- operational reporting export
- full Stripe sandbox credential rollout across environments
- CI/CD pipeline standardization

## 12. Acceptance Statement

This requirements document is now aligned to the implemented system behavior, not to an earlier conceptual demo-only scope.

It should be used as the working reference for:

- ongoing code evolution
- test planning
- demo preparation
- portfolio review
- future enterprise expansion phases
