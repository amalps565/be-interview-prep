# be-interview-prep

Five Spring Boot features built for the backend interview prep assignment. Each feature is delivered on its own branch and merged into `main` through its own pull request.

## Stack

- Java 21
- Spring Boot 3.5 (Web, Validation, Data JPA)
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

## Questions

| # | Question | PR link |
|---|----------|---------|
| 1 | Task Manager API | |
| 2 | URL Shortener | |
| 3 | Authentication & Roles | |
| 4 | Product Catalog | |
| 5 | Order Service | |

**Video:**
