# Simulated E-commerce Shopping Website

A full-stack training project that simulates the core workflow of an online shopping platform.

The backend is implemented with Spring Boot and currently covers authentication, product browsing, Redis-backed cart management, order creation, admin product management, and Swagger/OpenAPI documentation.

## Current Status

- Backend is implemented and verified locally.
- Swagger UI is enabled.
- A React + Vite frontend scaffold exists in [`frontend/`](./frontend), but the production UI has not been built yet.

## Features

- User registration and login with JWT authentication
- View current user profile
- Product listing with pagination, keyword search, category filtering, and detail view
- Redis product caching
- Redis shopping cart storage using Hash
- Redis popular product ranking using Sorted Set
- Create orders from the current cart
- View order history and order details
- Admin product create, update, delete
- Admin order listing and detail view

## Tech Stack

- Java 21
- Spring Boot 4.0.3
- Spring Security
- Spring Data JPA
- MySQL
- Redis-compatible cache server
- springdoc OpenAPI / Swagger UI
- React + Vite (frontend scaffold)

## Prerequisites

- Java 21
- MySQL 8.0+  
  Tested locally with MySQL 8.4
- Redis 7 compatible server  
  Tested locally with Memurai on port `6379`
- Node.js 20.19+ or 22.12+ if you want to run the current Vite 7 frontend scaffold

## Local Backend Configuration

The backend reads its settings from [`src/main/resources/application.properties`](./src/main/resources/application.properties).

Current local defaults:

- Server: `http://127.0.0.1:8080`
- MySQL:
  - Host: `127.0.0.1`
  - Port: `3306`
  - Database: `ecom`
- Redis:
  - Host: `127.0.0.1`
  - Port: `6379`

The database will be created automatically if it does not exist.

## Run The Backend

Start MySQL and Redis first, then run:

```powershell
.\mvnw.cmd spring-boot:run
```

Build the jar:

```powershell
.\mvnw.cmd clean package
```

Run the packaged jar:

```powershell
java -jar target\ecom-0.0.1-SNAPSHOT.jar
```

## Swagger UI

After the backend starts, open:

- Swagger UI: `http://127.0.0.1:8080/swagger-ui.html`
- OpenAPI JSON: `http://127.0.0.1:8080/v3/api-docs`

Protected endpoints use Bearer token authentication.

In Swagger UI:

1. Call `POST /api/auth/login`
2. Copy the returned token
3. Click `Authorize`
4. Paste:

```text
Bearer <your-jwt-token>
```

## Seed Data

On a fresh database, the application seeds:

- 100 sample products
- 1 admin account
- 1 customer account

Fresh database default users:

- Admin
  - Username: `admin`
  - Email: `admin@ecom.local`
  - Password: `Admin123!`
- Customer
  - Username: `demo`
  - Email: `demo@ecom.local`
  - Password: `Demo123!`

Note: if your local database already contains users, those records are not overwritten automatically.

## API Overview

Authentication:

- `POST /api/auth/register`
- `POST /api/auth/login`
- `POST /api/auth/logout`

User:

- `GET /api/users/me`

Products:

- `GET /api/products`
- `GET /api/products/{productId}`
- `GET /api/products/categories`
- `GET /api/products/popular`

Cart:

- `GET /api/cart`
- `POST /api/cart/items`
- `PUT /api/cart/items/{productId}`
- `DELETE /api/cart/items/{productId}`

Orders:

- `POST /api/orders`
- `GET /api/orders`
- `GET /api/orders/{orderId}`

Admin:

- `POST /api/admin/products`
- `PUT /api/admin/products/{productId}`
- `DELETE /api/admin/products/{productId}`
- `GET /api/admin/orders`
- `GET /api/admin/orders/{orderId}`

## Order Flow

`POST /api/orders` does not accept a custom order body.

The current flow is:

1. Login
2. Add items to the cart
3. Call `POST /api/orders`
4. The server reads the current cart, validates stock, creates the order, writes order items, decreases stock, and clears the cart

## Project Structure

```text
ecom/
├─ src/main/java/com/eason/ecom
│  ├─ config
│  ├─ controller
│  ├─ dto
│  ├─ entity
│  ├─ exception
│  ├─ repository
│  ├─ security
│  └─ service
├─ src/main/resources
├─ frontend/
├─ pom.xml
└─ README.md
```

## Verification

The backend has been verified locally with:

```powershell
.\mvnw.cmd clean test
```

Verified flows:

- product listing
- login
- cart operations
- order creation
- admin product CRUD
- Swagger/OpenAPI access

## Notes

- The backend uses Spring Boot 4, so some JSON classes come from the Spring Boot 4 dependency set rather than older Boot 3 conventions.
- The frontend scaffold is present, but the final React application is still pending.
