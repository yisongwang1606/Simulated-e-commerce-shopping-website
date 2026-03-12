# System Technology and Usage Guide

## 1. Purpose

This document explains the technologies currently used by the Enterprise Commerce Platform and describes how each technology is applied in the actual implementation.

It is aligned to the current repository, Docker runtime, and verified system behavior as of March 12, 2026.

## 2. Solution Overview

The system is implemented as a single-repository modular monolith with two frontend portals:

- customer storefront
- admin operations portal

The solution includes:

- Java backend API
- React frontend
- MySQL relational persistence
- Redis operational state storage
- Docker-based local deployment

## 3. Backend Technologies and Usage

### 3.1 Java 21

Used for:

- backend runtime
- language-level baseline for the current Spring Boot generation
- consistent local and containerized execution

Applied in this project:

- Maven build uses Java 21
- local developer environment is aligned to Java 21
- Docker backend image runs on Java 21

### 3.2 Spring Boot 4

Used for:

- application bootstrap
- dependency wiring
- REST API hosting
- environment profile handling
- production-style health exposure

Applied in this project:

- all backend modules are bootstrapped through Spring Boot
- MVC controllers expose business APIs
- Actuator health endpoints are enabled
- local and Docker profiles are managed through property files

### 3.3 Spring MVC

Used for:

- REST endpoint mapping
- request validation
- JSON request/response processing

Applied in this project:

- customer and admin APIs are implemented with controller classes
- path variables, query parameters, and request bodies are validated and mapped to DTOs
- Swagger/OpenAPI descriptions are generated from the same API layer

### 3.4 Spring Security

Used for:

- authentication enforcement
- route protection
- role-based authorization

Applied in this project:

- protected APIs require authenticated requests
- admin endpoints require the `ADMIN` role
- public endpoints such as Swagger and health checks are selectively allowed
- customer and admin route responsibilities are consistent with backend role checks

### 3.5 JWT

Used for:

- stateless session transport between frontend and backend

Applied in this project:

- login returns a JWT and expiry metadata
- frontend stores the token and sends it in the `Authorization` header
- backend filter extracts and validates the token for protected requests
- logout invalidates active token usage through Redis

### 3.6 Spring Data JPA and Hibernate

Used for:

- relational object mapping
- repository access
- transactional business persistence

Applied in this project:

- users, products, orders, order items, addresses, payments, shipments, refunds, support tickets, notes, tags, inventory adjustments, and audit logs are mapped as entities
- repositories and service-layer transactions handle state changes such as order placement, payment settlement, shipment delivery, and refund review
- Hibernate manages lazy/eager loading, relational joins, and transactional persistence behavior

### 3.7 MySQL 8.4

Used for:

- system-of-record persistence

Applied in this project:

- all durable business data is stored in MySQL
- Flyway migrations initialize and evolve the schema
- transactional workflows rely on MySQL consistency for order, payment, shipment, refund, and support data

### 3.8 Flyway

Used for:

- schema versioning
- repeatable environment setup
- controlled database evolution

Applied in this project:

- migration scripts are stored under `src/main/resources/db/migration`
- schema evolves through versioned migrations `V1` to `V6`
- local and Docker startup both run Flyway automatically

Current migration themes:

- `V1` enterprise baseline schema
- `V2` order, inventory, and audit controls
- `V3` payment and shipment placeholders
- `V4` address book and order notes
- `V5` refunds and search indexes
- `V6` order tags and support tickets

### 3.9 Redis

Used for:

- token activity state
- cart state
- cache storage
- popularity ranking

Applied in this project:

- cart contents are stored in Redis hash structures per user
- token validity and logout invalidation use Redis keys
- product detail cache uses Redis entries with time-based expiration
- product popularity is ranked through a Redis sorted set

Important implementation note:

- Redis is used through Redis operations, not through Spring Data Redis repository abstractions
- the application explicitly disables Spring Data Redis repository scanning because Redis is infrastructure state here, not a domain entity repository model

### 3.10 springdoc OpenAPI and Swagger UI

Used for:

- API review
- manual testing
- integration debugging

Applied in this project:

- controllers and DTOs are annotated for API documentation
- Swagger UI is exposed at runtime
- authenticated APIs can be tested by pasting a Bearer token into the Swagger authorization modal

### 3.11 Spring Boot Actuator

Used for:

- operational health visibility
- deployment readiness checks

Applied in this project:

- `health`
- `health/liveness`
- `health/readiness`

are available for runtime validation

These endpoints are used for:

- local smoke checks
- Docker runtime verification
- future deployment readiness validation

## 4. Frontend Technologies and Usage

### 4.1 React 19

Used for:

- single-page application rendering
- customer and admin UI composition
- stateful interactive workflows

Applied in this project:

- storefront pages and admin pages are built as React components
- customer and admin experiences are separated into different shell layouts
- page composition supports cart, address, order, refund, and support workflows

### 4.2 Vite 7

Used for:

- frontend development server
- fast rebuilds
- production bundle generation

Applied in this project:

- local development runs through Vite
- production build generates static assets for nginx
- local development uses a proxy to backend APIs and Swagger endpoints

### 4.3 TypeScript 5.9

Used for:

- typed API contracts
- safer page and component state
- stronger maintainability for admin and customer flows

Applied in this project:

- frontend API modules use typed request and response contracts
- page state, shared UI components, and session data are typed
- backend response structures are mirrored in frontend contract definitions

### 4.4 React Router

Used for:

- client-side route handling
- portal separation
- protected route management

Applied in this project:

- customer routes use storefront-specific route structure
- admin routes use admin-specific route structure
- `/login` and `/admin/login` are separate entry points
- admin route guard checks role-based access on the client side

### 4.5 Zustand

Used for:

- lightweight global session state

Applied in this project:

- current user, token, and token expiry are held in a shared store
- login updates the store
- logout clears the store
- customer/admin UI decisions use the shared session state

### 4.6 Axios

Used for:

- HTTP communication with the backend

Applied in this project:

- centralized API client handles the base URL
- authenticated requests include the Bearer token automatically
- feature-specific modules wrap endpoint calls for catalog, cart, orders, admin operations, addresses, and support actions

### 4.7 CSS Architecture

Used for:

- project-owned visual system without a heavyweight UI framework

Applied in this project:

- `base.css` provides global layout and design tokens
- `components.css` provides reusable block styling
- customer portal and admin portal share common structure while keeping different emphasis
- the customer shell avoids exposing internal technical badges, while the admin shell intentionally retains operational context

## 5. Deployment and Infrastructure Technologies

### 5.1 Docker

Used for:

- containerized packaging
- environment consistency
- reproducible local deployment

Applied in this project:

- backend has a dedicated Docker image
- frontend has a dedicated Docker image
- the frontend image serves static files through nginx

### 5.2 Docker Compose

Used for:

- multi-service local orchestration

Applied in this project:

- Compose starts MySQL, Redis, backend, and frontend together
- service health and dependency ordering support realistic local startup
- full-stack smoke validation is executed against the Compose runtime

### 5.3 nginx

Used for:

- serving the frontend production bundle in containers

Applied in this project:

- built frontend assets are served through nginx
- same-origin API calls are supported inside the containerized setup

### 5.4 Environment Variables and Profiles

Used for:

- environment portability
- secret and endpoint separation

Applied in this project:

- backend properties are overridden through environment variables where appropriate
- Docker runtime uses a dedicated profile
- frontend can consume same-origin APIs in containers and proxy APIs in local Vite development

## 6. How the Technologies Map to Business Features

| Business Feature | Main Technologies Used | Implementation Pattern |
| --- | --- | --- |
| Login and logout | Spring Security, JWT, Redis, React, Zustand | JWT issuance, token storage, Redis-backed invalidation, protected UI state |
| Product browsing | Spring MVC, JPA, MySQL, Redis, React, Axios | paginated queries, cached detail reads, typed frontend catalog views |
| Cart | Redis, Spring services, React | Redis hash per user, real-time cart totals, checkout preparation |
| Address book | Spring MVC, JPA, MySQL, React forms | persistent customer addresses, default selection, order snapshot source |
| Order placement | JPA, MySQL, Redis, service-layer transactions | cart-to-order conversion with stock validation and address snapshot |
| Payments | Spring MVC, JPA, MySQL | simulated payment transaction creation and callback settlement |
| Shipments | Spring MVC, JPA, MySQL | shipment record creation, delivery status updates, customer shipment visibility |
| Refunds | Spring MVC, JPA, MySQL | refund request, admin review, callback-based settlement |
| Support tickets | Spring MVC, JPA, MySQL, React admin views | customer service intake and admin queue handling |
| Admin dashboard | JPA queries, DTO aggregation, React admin UI | operational summary metrics and low-stock watchlist |
| API documentation | springdoc OpenAPI, Swagger UI | runtime contract discovery and manual API testing |
| Runtime readiness | Actuator, Docker Compose | health checks and startup validation |

## 7. Testing and Verification Approach

### 7.1 Backend Testing

Used technologies:

- JUnit
- Mockito
- Spring test support

Current usage:

- service and workflow tests validate order, refund, support, inventory, and security behavior
- current verified backend test count: `35`

### 7.2 Frontend Verification

Used tools:

- ESLint
- Vite production build

Current usage:

- lint validates code consistency and common mistakes
- production build validates bundle integrity and route-level imports

### 7.3 Runtime Smoke Testing

Current practice:

- execute real login, cart, order, support, admin, payment, shipment, and refund flows against the running application
- verify final state through APIs and audit data

Sample verified result from March 12, 2026:

- order number: `ORD-20260312215111221-4696`
- final order status: `REFUNDED`
- support ticket status: `IN_PROGRESS`
- shipment status: `DELIVERED`

## 8. Current Technology Decisions and Rationale

### 8.1 Why a Modular Monolith

Chosen because:

- the current team and project size do not justify microservice overhead
- a monolith keeps delivery faster and testing simpler
- domain boundaries can still be modeled cleanly for future extraction

### 8.2 Why Redis in Addition to MySQL

Chosen because:

- cart and token state benefit from fast in-memory operations
- popularity ranking is a natural fit for sorted sets
- cache reads reduce repeated product-detail database access

### 8.3 Why React Without a Heavy UI Library

Chosen because:

- the project needs control over both customer and admin visual systems
- avoiding a large component framework keeps bundle complexity lower
- the UI can be reshaped more easily as the product evolves

### 8.4 Why Docker Compose

Chosen because:

- it gives a realistic local multi-service workflow
- developers can start the whole stack consistently
- it matches how enterprise systems are often validated before higher-level deployment automation

## 9. Next Recommended Technology Additions

The following technologies are reasonable next steps if the system continues toward production-grade enterprise maturity:

- Testcontainers for stronger integration testing
- Prometheus and Grafana for metrics visualization
- OpenTelemetry for tracing
- message queue integration for asynchronous workflows
- CI/CD pipeline automation
- external payment and carrier sandbox integrations

## 10. Closing Statement

The current technology set is no longer that of a simple demo application.

It now supports realistic enterprise-style development work across:

- domain modeling
- API design
- operational workflows
- portal separation
- local deployment
- documentation
- testing and smoke validation
