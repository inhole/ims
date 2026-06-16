# IMS

재고 관리 시스템 MVP 백엔드 과제입니다.

상품의 현재 재고 조회, 입고, 출고 API를 제공하며 동시 요청에서도 재고 수량이 일관되게 유지되도록 구현했습니다.

## 기술 스택

- Java 17
- Spring Boot 3
- Spring Web
- Spring Validation
- Spring Data JPA
- PostgreSQL
- Docker Compose
- Swagger UI
- Gradle

## 실행 방법

PostgreSQL 실행:

```powershell
docker compose up -d
```

테스트 실행:

```powershell
.\gradlew.bat test
```

애플리케이션 실행:

```powershell
.\gradlew.bat bootRun
```

기본 서버 포트는 `8080`입니다.
기본 DB 접속 정보는 `docker-compose.yml`의 PostgreSQL 설정과 동일합니다.

Swagger UI 접속:

```text
http://localhost:8080/swagger-ui.html
```

애플리케이션과 DB 종료:

```powershell
docker compose down
```

DB 데이터를 포함해서 초기화하려면 볼륨까지 삭제합니다.

```powershell
docker compose down -v
```

## 환경 변수

PostgreSQL 연결 정보는 환경 변수로 변경할 수 있습니다.

| 이름 | 기본값 |
| --- | --- |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/ims` |
| `SPRING_DATASOURCE_USERNAME` | `ims` |
| `SPRING_DATASOURCE_PASSWORD` | `ims` |
| `SERVER_PORT` | `8080` |

## 데이터베이스

제출용 전체 PostgreSQL DDL은 [docs/full-ddl.sql](docs/full-ddl.sql)에 있습니다.
Docker 초기화용 DDL은 [docs/schema.sql](docs/schema.sql)에 있습니다.
Docker Compose로 PostgreSQL을 처음 실행하면 `docs/schema.sql`이 `/docker-entrypoint-initdb.d/01-schema.sql`로 마운트되어 자동 실행됩니다.

주의: PostgreSQL Docker 이미지는 데이터 볼륨이 비어 있는 최초 실행 시에만 init SQL을 실행합니다. 스키마를 다시 적용하려면 `docker compose down -v`로 볼륨을 삭제한 뒤 다시 실행해야 합니다.

주요 제약:

- `products.name`은 `UNIQUE`입니다.
- `products.quantity`는 `CHECK (quantity >= 0)`로 음수 재고를 방지합니다.
- `stock_movements.quantity`는 `CHECK (quantity > 0)`입니다.
- `stock_movements.movement_type`은 `INBOUND`, `OUTBOUND`만 허용합니다.

## 동시성 제어 전략

기존 상품의 입고/출고는 Spring Data JPA의 pessimistic row lock을 사용합니다.

- `findByIdForUpdate`: 상품 ID 기준 입고/출고 시 해당 상품 row를 잠급니다.
- `findByNameForUpdate`: 이미 존재하는 상품을 이름으로 입고할 때 해당 상품 row를 잠급니다.
- 신규 상품을 이름으로 동시에 입고하는 경우에는 아직 잠글 row가 없기 때문에, 단일 애플리케이션 인스턴스 안에서 상품명 기반 striped lock을 먼저 잡고 생성 경쟁을 막습니다.
- DB 레벨에서도 `UNIQUE (name)`과 `CHECK (quantity >= 0)` 제약으로 마지막 방어선을 둡니다.

## API

브라우저에서 `http://localhost:8080/swagger-ui.html`에 접속하면 아래 API를 직접 호출해 볼 수 있습니다.

### 신규 또는 이름 기준 입고

등록되지 않은 상품이면 새로 생성하고, 이미 존재하는 상품이면 현재 재고를 증가시킵니다.

```http
POST /api/products/inbound
Content-Type: application/json

{
  "name": "Product A",
  "quantity": 10
}
```

응답:

```json
{
  "id": 1,
  "name": "Product A",
  "quantity": 10
}
```

### 기존 상품 입고

```http
POST /api/products/{productId}/inbound
Content-Type: application/json

{
  "quantity": 3
}
```

### 기존 상품 출고

재고가 부족하면 `INSUFFICIENT_STOCK` 에러를 반환하며 재고는 음수가 되지 않습니다.

```http
POST /api/products/{productId}/outbound
Content-Type: application/json

{
  "quantity": 2
}
```

### 재고 조회

```http
GET /api/products/{productId}/stock
```

응답:

```json
{
  "productId": 1,
  "name": "Product A",
  "quantity": 8
}
```

### 상품 목록 조회

```http
GET /api/products
```

## 에러 응답

잘못된 요청과 비즈니스 규칙 위반은 동일한 JSON 형식으로 응답합니다.

```json
{
  "code": "INSUFFICIENT_STOCK",
  "message": "Insufficient stock quantity."
}
```

주요 에러 코드:

- `INVALID_REQUEST`: 요청 JSON, 경로 변수, 필드 검증 실패
- `PRODUCT_NOT_FOUND`: 상품 없음
- `INSUFFICIENT_STOCK`: 출고 요청 수량보다 현재 재고가 부족함
- `DUPLICATE_PRODUCT`: 상품명 중복
- `INTERNAL_SERVER_ERROR`: 서버 내부 오류

## 테스트

테스트는 H2의 PostgreSQL 호환 모드로 실행됩니다.

검증하는 주요 항목:

- 입고 시 재고 증가
- 미등록 상품 입고 시 신규 상품 생성
- 출고 시 재고 감소
- 출고 후 재고가 음수가 되지 않음
- 잘못된 수량, 잘못된 JSON, 잘못된 path variable 검증
- 없는 상품 조회/출고 에러 응답
- 동시 입고/출고 요청에서 최종 재고 정합성 유지
- 동일 상품명 신규 입고 동시 요청 시 상품 1개만 생성
