# AGENTS.md

## Project Goal

Implement an inventory management MVP backend for a hiring assignment.

The system must provide APIs for:

- Checking the current stock quantity of products
- Processing stock inbound operations
- Processing stock outbound operations
- Preserving data consistency under concurrent requests

Target stack:

- Java
- Spring Boot
- PostgreSQL

## Assignment Requirements

### Common

- Consider concurrency control for simultaneous inbound and outbound requests.
- Return appropriate error responses for invalid requests.
- Product data starts from this basic shape, but can be extended when needed:

```json
{
  "id": 1,
  "name": "Product A",
  "quantity": 1
}
```

### Inbound

- Increase the current stock quantity of a product.
- If the product is not registered, create it first and then process inbound stock.

### Outbound

- Decrease the current stock quantity of a product.
- Product quantity must never become negative.

### Stock

- Return the current stock quantity of a product.

## Engineering Direction

Build a small but production-shaped backend instead of a throwaway sample.

Recommended architecture:

- `controller`: HTTP API layer, request validation, response mapping
- `service`: transaction boundary and business rules
- `repository`: persistence access
- `domain`: JPA entities and domain-level invariants
- `dto`: request and response DTOs
- `exception`: custom exceptions and global error response handling

## Data Model Guidance

Use PostgreSQL DDL that can be submitted with the assignment.

Suggested minimal table:

```sql
CREATE TABLE products (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    quantity INTEGER NOT NULL CHECK (quantity >= 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
```

If stock movement history is added, prefer a separate append-only table:

```sql
CREATE TABLE stock_movements (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL REFERENCES products(id),
    movement_type VARCHAR(20) NOT NULL,
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
```

## Concurrency Strategy

The implementation must explicitly protect stock updates from race conditions.

Preferred options:

- Pessimistic locking with `SELECT ... FOR UPDATE` through Spring Data JPA `@Lock(LockModeType.PESSIMISTIC_WRITE)`
- Atomic conditional SQL update for outbound stock, such as `UPDATE products SET quantity = quantity - :amount WHERE id = :id AND quantity >= :amount`

Use one strategy consistently and cover it with tests.

For this MVP, pessimistic locking is acceptable and easy to explain:

- Inbound and outbound operations run inside a transaction.
- The target product row is locked before changing quantity.
- Outbound validates available stock while the row lock is held.

## API Design Guidance

Keep the API simple and explicit.

Suggested endpoints:

- `POST /api/products/inbound`
- `POST /api/products/{productId}/inbound`
- `POST /api/products/{productId}/outbound`
- `GET /api/products/{productId}/stock`
- `GET /api/products`

For creating an unregistered product during inbound, support a request that can identify the product by name:

```json
{
  "name": "Product A",
  "quantity": 10
}
```

For existing product stock changes:

```json
{
  "quantity": 3
}
```

## Validation Rules

- Product name must not be blank when creating or registering by name.
- Quantity in inbound and outbound requests must be greater than zero.
- Outbound must fail if product does not exist.
- Outbound must fail if available stock is lower than requested quantity.
- Stock lookup must return `404 Not Found` when the product does not exist.

## Error Response Guidance

Use a consistent JSON error shape.

Example:

```json
{
  "code": "INSUFFICIENT_STOCK",
  "message": "Insufficient stock quantity."
}
```

Recommended error codes:

- `INVALID_REQUEST`
- `PRODUCT_NOT_FOUND`
- `INSUFFICIENT_STOCK`
- `DUPLICATE_PRODUCT`
- `INTERNAL_SERVER_ERROR`

## Testing Expectations

Add focused tests that prove the important behavior:

- Inbound increases stock.
- Inbound creates a product when the product name does not exist.
- Outbound decreases stock.
- Outbound cannot make stock negative.
- Invalid quantities are rejected.
- Concurrent inbound/outbound requests keep final stock consistent.

Use integration tests where transaction and locking behavior matters.

## Deliverables

The final GitHub repository should include:

- Spring Boot source code
- PostgreSQL DDL file
- README with setup, run, test, and API examples
- Tests for core business rules and concurrency

## Local Development Notes

Prefer these commands once the project is scaffolded:

```powershell
.\gradlew.bat test
.\gradlew.bat bootRun
```

If Maven is used instead:

```powershell
.\mvnw.cmd test
.\mvnw.cmd spring-boot:run
```

## Coding Standards

- Keep changes scoped to the assignment requirements.
- Prefer clear service methods over premature abstraction.
- Keep transaction boundaries in the service layer.
- Do not allow negative stock at either application or database level.
- Keep API responses stable and documented in README.
- Include DDL in a dedicated SQL file such as `docs/schema.sql` or `src/main/resources/schema.sql`.
