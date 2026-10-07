# be-interview-prep

Five Spring Boot features built for the backend interview prep assignment. Each feature is delivered on its own branch and merged into `main` through its own pull request.

## Stack

- Java 21
- Spring Boot 3.5 (Web, Validation, Data JPA), springdoc-openapi for Swagger UI
- H2 in-memory database
- Maven (wrapper included), JUnit 5, Spotless with google-java-format

## Run the app

```bash
./mvnw spring-boot:run
```

On Windows use `mvnw.cmd spring-boot:run`. The app starts on `http://localhost:8080`.

## Run the tests

```bash
./mvnw test
```

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

## Questions

| # | Question | PR link |
|---|----------|---------|
| 1 | Task Manager API | [#7](https://github.com/amalps565/be-interview-prep/pull/7) |
| 2 | URL Shortener | |
| 3 | Authentication & Roles | |
| 4 | Product Catalog | |
| 5 | Order Service | |

**Video:**
