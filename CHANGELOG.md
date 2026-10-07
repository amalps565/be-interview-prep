# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project
adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.0.2] - 2026-10-07

### Added

- **Task API: Clients can create, list, fetch, update and delete tasks, and filter the list by
  status.** `POST /api/tasks` returns 201 with a `Location` header, `GET /api/tasks?status=` lists
  tasks oldest first, optionally only `TODO`, `IN_PROGRESS` or `DONE`, and `GET`, `PUT` and
  `DELETE /api/tasks/{id}` work on one task. A task has a title, a description, a status that
  defaults to `TODO`, an optional due date and a created date the server sets.
  [#2](https://github.com/amalps565/be-interview-prep/issues/2)
- **Task API: Invalid input is rejected with a message for each field.** A missing title, a title
  over 100 characters, a description over 2000 characters or a due date in the past returns 400,
  and `fieldErrors` names every invalid field. An unknown status in the body or in the `status`
  filter also returns 400 for that field and lists the allowed values.
  [#2](https://github.com/amalps565/be-interview-prep/issues/2)
- **Repo: Every error uses one JSON format.** Validation errors, unknown tasks (404), unsupported
  methods and anything unexpected all return `{timestamp, status, error, message, path,
  fieldErrors}` with the matching HTTP status. An unexpected error returns 500 with a generic
  message and is logged, so internal details never reach the client.
  [#2](https://github.com/amalps565/be-interview-prep/issues/2)
- **Docs: Interactive API documentation is served by the app.** Swagger UI is at
  `/swagger-ui.html` and the OpenAPI document at `/v3/api-docs`.
  [#2](https://github.com/amalps565/be-interview-prep/issues/2)

## [0.0.1] - 2026-10-07

### Added

- **Repo: A runnable Spring Boot base project is in place for the five assignment questions.** The
  app runs on Spring Boot 3.5 and Java 21 with Web, Validation and Data JPA over an in-memory H2
  database, so `./mvnw spring-boot:run` and `./mvnw test` work with no setup. Spring Boot 3.x is
  pinned by hand because the assignment asks for 3.x and Spring Initializr now offers only 4.x. JPA
  open-in-view is off, so database access stays inside the service layer.
  [#1](https://github.com/amalps565/be-interview-prep/pull/1)
- **Repo: Code is formatted the same way on every branch.** Spotless with google-java-format runs
  through `./mvnw spotless:apply`, and the Maven wrapper pins the Maven version, so no local Maven
  install is needed. [#1](https://github.com/amalps565/be-interview-prep/pull/1)
- **Repo: Every pull request opens with the assignment's description template.** The template asks
  for the problem, the approach, the decisions and trade-offs, and how to test.
  [#1](https://github.com/amalps565/be-interview-prep/pull/1)
- **Docs: The README explains how to run the app and the tests, and tracks each question's PR.** The
  question table has one row per question for its PR link, and the video link goes at the end.
  [#1](https://github.com/amalps565/be-interview-prep/pull/1)
- **Docs: The full assignment brief lives in `WIKI.md`.** It covers the submission workflow, branch
  names, the PR template, the video guidelines, the AI usage policy, every question's requirements
  and acceptance criteria, the optional extras and the submission checklist.
  [#1](https://github.com/amalps565/be-interview-prep/pull/1)
- **Repo: Every change is recorded in this one changelog.** Each PR adds one version heading here
  and bumps the `pom.xml` version to match. [#1](https://github.com/amalps565/be-interview-prep/pull/1)
