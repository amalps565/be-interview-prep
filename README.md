# be-interview-prep

Five Spring Boot features built for the backend interview prep assignment. Each feature is delivered on its own branch and merged into `main` through its own pull request.

## Stack

- Java 21
- Spring Boot 3.5 (Web, Validation, Data JPA, Security with OAuth2 resource server for JWT), springdoc-openapi for Swagger UI
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

## Questions

| # | Question | PR link |
|---|----------|---------|
| 1 | Task Manager API | [#7](https://github.com/amalps565/be-interview-prep/pull/7) |
| 2 | URL Shortener | [#8](https://github.com/amalps565/be-interview-prep/pull/8) |
| 3 | Authentication & Roles | |
| 4 | Product Catalog | |
| 5 | Order Service | |

**Video:**
