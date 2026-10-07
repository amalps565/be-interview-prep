# be-interview-prep

Five Spring Boot features built for the backend interview prep assignment. Each feature is delivered on its own branch and merged into `main` through its own pull request.

## Stack

- Java 21
- Spring Boot 3.5 (Web, Validation, Data JPA, Security with OAuth2 resource server for JWT, Cache with Caffeine), springdoc-openapi for Swagger UI
- H2 in-memory database
- Maven (wrapper included), JUnit 5, Spotless with google-java-format

## Run the app

The app signs login tokens with a key from the `JWT_SECRET` environment variable (at least 32 bytes) and will not start without it. `ADMIN_EMAIL` and `ADMIN_PASSWORD` are optional; when both are set, an admin account is created on startup.

```bash
export JWT_SECRET="$(openssl rand -base64 48)"
export ADMIN_EMAIL=admin@example.com ADMIN_PASSWORD='choose-a-password'
./mvnw spring-boot:run
```

On Windows PowerShell set them with `$env:JWT_SECRET = "..."` and run `mvnw.cmd spring-boot:run`. The app starts on `http://localhost:8080`.

## Run the tests

```bash
./mvnw test
```

The tests run with the `test` profile, which generates a random signing key and admin password on every run, so no credential is stored in the repository.

Format the code before committing:

```bash
./mvnw spotless:apply
```

See [WIKI.md](WIKI.md) for the full assignment brief.

## API

Interactive documentation: `http://localhost:8080/swagger-ui.html`. Every error returns the same JSON shape:

```json
{"timestamp": "...", "status": 400, "error": "Bad Request", "message": "Validation failed",
 "path": "/api/tasks", "fieldErrors": [{"field": "title", "message": "is required"}]}
```

### Authentication (Q3)

Every `/api/**` endpoint except register and login needs `Authorization: Bearer <token>`. Short-link redirects (`/{code}`) and Swagger UI stay public. A missing, expired or invalid token returns 401 and a missing role returns 403, both in the JSON error format.

| Method | Path | Who | Result |
|---|---|---|---|
| `POST` | `/api/auth/register` | anyone | 201 with the new `USER`; 409 if the email exists |
| `POST` | `/api/auth/login` | anyone | 200 with `accessToken` valid for 15 minutes; 401 for bad credentials |
| `GET` | `/api/users/me` | any logged-in user | 200 with their own profile |
| `GET` | `/api/admin/users` | `ADMIN` only | 200 with all users; 403 for `USER` |

```bash
curl -X POST localhost:8080/api/auth/register -H "Content-Type: application/json"   -d '{"email":"me@example.com","password":"a-long-password"}'
TOKEN=$(curl -s -X POST localhost:8080/api/auth/login -H "Content-Type: application/json"   -d '{"email":"me@example.com","password":"a-long-password"}' | jq -r .accessToken)
curl -H "Authorization: Bearer $TOKEN" localhost:8080/api/users/me
```

### Tasks (Q1)

| Method | Path | Result |
|---|---|---|
| `POST` | `/api/tasks` | 201 with the task; 400 with field errors |
| `GET` | `/api/tasks?status=TODO` | 200 with the tasks, optionally filtered by status |
| `GET` | `/api/tasks/{id}` | 200, or 404 for an unknown task |
| `PUT` | `/api/tasks/{id}` | 200 with the updated task |
| `DELETE` | `/api/tasks/{id}` | 204 |

```bash
curl -X POST localhost:8080/api/tasks -H "Content-Type: application/json" \
  -d '{"title":"Write tests","status":"IN_PROGRESS","dueDate":"2030-01-01"}'
```

### URL shortener (Q2)

| Method | Path | Result |
|---|---|---|
| `POST` | `/api/urls` | 201 with `code` and `shortUrl`; 200 with the existing link for the same URL and expiry; 400 for an invalid URL; 409 for a taken custom code |
| `GET` | `/{code}` | 302 to the original URL; 404 unknown; 410 expired |
| `GET` | `/api/urls/{code}/stats` | 200 with the original URL, visit count and created date |

```bash
curl -X POST localhost:8080/api/urls -H "Content-Type: application/json" \
  -d '{"url":"https://example.com/a/long/path","expiresAt":"2030-01-01T00:00:00Z"}'
curl -i localhost:8080/<code>
curl localhost:8080/api/urls/<code>/stats
```

### Products (Q4)

100 products are seeded on startup. Any logged-in user can read the catalog; creating, updating and deleting need the `ADMIN` role (a `USER` gets 403).

| Method | Path | Who | Result |
|---|---|---|---|
| `GET` | `/api/products` | any logged-in user | 200 with a page of products, `totalElements` and `totalPages` |
| `GET` | `/api/products/{id}` | any logged-in user | 200, or 404 for an unknown product |
| `POST` | `/api/products` | `ADMIN` only | 201 with the product and a `Location` header; 400 with field errors |
| `PUT` | `/api/products/{id}` | `ADMIN` only | 200 with the updated product |
| `DELETE` | `/api/products/{id}` | `ADMIN` only | 204 |

Every list parameter is optional and they combine freely in one request:

| Parameter | Meaning |
|---|---|
| `category` | exact category, ignoring case |
| `minPrice`, `maxPrice` | inclusive price range; `minPrice` above `maxPrice` returns 400 |
| `inStock=true` | only products with stock above 0 |
| `name` | part of the name, ignoring case |
| `page`, `size` | zero-based page and page size; default size 20 |
| `sort` | `field,asc` or `field,desc` on `id`, `name`, `category`, `price`, `stock`, `rating` or `createdAt`; default `id,asc`; any other field returns 400 |

The page size is capped at 100 by `spring.data.web.pageable.max-page-size=100`: a larger `size` is silently clamped to 100, and the response's `size` shows the value used.

```bash
curl -H "Authorization: Bearer $TOKEN"   "localhost:8080/api/products?category=books&minPrice=10&maxPrice=200&inStock=true&name=lamp&sort=price,desc&size=10"
curl -H "Authorization: Bearer $TOKEN" localhost:8080/api/products/1
```

Single-product lookups are cached in memory (Caffeine, up to 10,000 entries, 10-minute expiry). An update replaces the cached entry and a delete removes it, and both happen only after the database transaction commits, so a lookup never returns stale data. How we know repeated lookups skip the database:

- `ProductCacheTest` wraps the repository in a Mockito spy, requests the same product twice and verifies `findById` ran once. It also checks that the next lookup after an update returns the new price and the next lookup after a delete returns 404.
- Run the app with `./mvnw spring-boot:run -Dspring-boot.run.arguments=--spring.jpa.show-sql=true` and request `/api/products/1` repeatedly: the log shows one `select` for the first request and none after.

### Orders (Q5)

Every order request needs an `Idempotency-Key` header (1 to 100 characters, for example a UUID the client creates once per order and reuses on every retry). The key is unique per customer and is stored with a SHA-256 hash of the order lines.

| Method | Path | Result |
|---|---|---|
| `POST` | `/api/orders` | 201 with the order; 200 with `Idempotent-Replayed: true` when the same key and body are sent again; 409 when any item lacks stock (nothing is reserved); 422 when the key was used for a different order; 400 without a key |
| `GET` | `/api/orders/{id}` | 200 for your own order, 404 otherwise |
| `POST` | `/api/orders/{id}/cancel` | 200 with status `CANCELLED` and the stock returned; 409 if already cancelled |

```bash
KEY=$(uuidgen)
curl -X POST localhost:8080/api/orders -H "Authorization: Bearer $TOKEN"   -H "Idempotency-Key: $KEY" -H "Content-Type: application/json"   -d '{"items":[{"productId":1,"quantity":2},{"productId":2,"quantity":1}]}'
```

How it stays correct under load:

- Each item is reserved with one statement, `UPDATE products SET stock = stock - :qty WHERE id = :id AND stock >= :qty`. The database applies it atomically, so two buyers can never both take the last item, and stock can never go negative. If it updates no row, the item is short.
- All items of one order are reserved in one transaction, in product-id order. A short item throws, the transaction rolls back, and every earlier reservation is undone. The fixed order means two orders never lock the same rows in opposite order, so they cannot deadlock.
- A retry is recognised by `(customer, Idempotency-Key)`, which has a unique constraint. A key that already exists returns the stored order. Two copies of one request that race each other both try to insert, the constraint lets exactly one win, and the loser returns the winner's order.
- Cancelling changes the status only `WHERE status = 'PLACED'`, so a double cancel cannot return stock twice.
- Every stock change evicts the product from the Q4 cache after commit, so product reads never show old stock.

## Questions

| # | Question | PR link |
|---|----------|---------|
| 1 | Task Manager API | [#7](https://github.com/amalps565/be-interview-prep/pull/7) |
| 2 | URL Shortener | [#8](https://github.com/amalps565/be-interview-prep/pull/8) |
| 3 | Authentication & Roles | [#10](https://github.com/amalps565/be-interview-prep/pull/10) |
| 4 | Product Catalog | [#11](https://github.com/amalps565/be-interview-prep/pull/11) |
| 5 | Order Service | [#13](https://github.com/amalps565/be-interview-prep/pull/13) |

**Video:**
