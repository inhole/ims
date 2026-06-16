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

## Concurrency Control

Stock changes for existing products use pessimistic row locks through Spring Data JPA.

- `findByIdForUpdate` protects inbound and outbound changes by product id.
- `findByNameForUpdate` protects inbound changes by product name when the product already exists.
- New product inbound requests use a bounded in-memory striped lock by product name so one application instance does not race while creating the same product name.
- The database also enforces `UNIQUE (name)` and `CHECK (quantity >= 0)`.

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

## Error Response

Invalid requests and business rule failures return a consistent JSON response.

```json
{
  "code": "INSUFFICIENT_STOCK",
  "message": "Insufficient stock quantity."
}
```

Main error codes:

- `INVALID_REQUEST`
- `PRODUCT_NOT_FOUND`
- `INSUFFICIENT_STOCK`
- `DUPLICATE_PRODUCT`
- `INTERNAL_SERVER_ERROR`
