# IMS

Inventory Management System MVP backend assignment.

## Stack

- Java 17
- Spring Boot 3
- Spring Web
- Spring Validation
- Spring Data JPA
- PostgreSQL

## Build

This project is configured with Gradle.

```powershell
.\gradlew.bat test
.\gradlew.bat bootRun
```

## Database

The PostgreSQL DDL is available at [docs/schema.sql](docs/schema.sql).

## API

Create a product when it does not exist and increase stock:

```http
POST /api/products/inbound
Content-Type: application/json

{
  "name": "Product A",
  "quantity": 10
}
```

Increase stock for an existing product:

```http
POST /api/products/{productId}/inbound
Content-Type: application/json

{
  "quantity": 3
}
```

Decrease stock for an existing product:

```http
POST /api/products/{productId}/outbound
Content-Type: application/json

{
  "quantity": 2
}
```

Check current stock:

```http
GET /api/products/{productId}/stock
```

List products:

```http
GET /api/products
```
