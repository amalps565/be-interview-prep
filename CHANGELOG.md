# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project
adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.0.5] - 2026-10-07

### Added

- **Auth: Users can register and log in, and passwords are stored only as BCrypt hashes.**
  `POST /api/auth/register` creates a `USER` account (emails are case-insensitive, a taken email
  returns 409, a password must be 8 to 72 characters) and `POST /api/auth/login` returns a bearer
  token. A wrong password and an unknown email get the same 401 message, and an unknown email still
  runs a password check, so neither the message nor the response time reveals which accounts exist.
  Registration ignores any `role` in the body, so nobody can make themselves an admin. A password
  longer than 72 bytes once UTF-8 encoded, which BCrypt cannot hash, is rejected with 400 instead
  of failing with 500.
  [#4](https://github.com/amalps565/be-interview-prep/issues/4)
- **Auth: A login lasts exactly 15 minutes and no server-side session is kept.** The token is an
  HS256-signed JWT carrying the email, user id and role, so web and mobile clients send it on every
  request and any instance can verify it. The usual 60-second leeway on expiry is turned off, so a
  token stops working at 15 minutes rather than 16.
  [#4](https://github.com/amalps565/be-interview-prep/issues/4)
- **Auth: Every API endpoint now needs a login, and only an admin can list all users.**
  `GET /api/users/me` returns the caller's own profile and `GET /api/admin/users` lists everyone for
  the `ADMIN` role. A request without a valid token gets 401 and a logged-in user without the role
  gets 403, both as the shared JSON error rather than an HTML page. Registration, login, short-link
  redirects and Swagger UI stay public, and the task and URL endpoints from Q1 and Q2 now require a
  token. [#4](https://github.com/amalps565/be-interview-prep/issues/4)
- **Auth: No secret is written in the code.** The signing key comes from `JWT_SECRET`, and the app
  refuses to start when it is missing or shorter than 32 bytes. An admin account is created on
  startup only when `ADMIN_EMAIL` and `ADMIN_PASSWORD` are set. Tests generate a random key and
  admin password on every run. Swagger UI has an Authorize button for the bearer token.
  [#4](https://github.com/amalps565/be-interview-prep/issues/4)
- **Auth: Login and registration are rate-limited per client address.** More than 10 requests to
  `/api/auth/**` from one address within a minute get 429 with a `Retry-After` header and the
  shared JSON error, which slows password guessing and stops a flood of expensive BCrypt hashing.
  Both numbers are set by `app.auth.rate-limit.requests` and `app.auth.rate-limit.window`.
  [#4](https://github.com/amalps565/be-interview-prep/issues/4)

## [0.0.4] - 2026-10-07

### Changed

- **Repo: The code no longer carries a company name.** Every class moved from
  `com.edstem.interviewprep` to `com.interviewprep`, so the source folders are now
  `src/main/java/com/interviewprep` and `src/test/java/com/interviewprep`, and the Maven `groupId` is
  `com.interviewprep`. Endpoints, behaviour and tests are unchanged.
  [#9](https://github.com/amalps565/be-interview-prep/pull/9)

## [0.0.3] - 2026-10-07

### Added

- **URL Shortener: A long URL can be turned into a short link that redirects to it.**
  `POST /api/urls` takes a `url` and an optional future `expiresAt` and returns 201 with a 7-character
  `code` and the full `shortUrl`. `GET /{code}` answers 302 to the original URL with
  `Cache-Control: no-store`, because a cached 301 would let browsers skip the server and the visit
  would never be counted. Codes are random letters and digits from `SecureRandom`, so they cannot be
  guessed by counting, and a unique constraint on the code guarantees no two links share one.
  [#3](https://github.com/amalps565/be-interview-prep/issues/3)
- **URL Shortener: Shortening the same URL twice returns the link that already exists.** When an
  existing link has the same URL and the same expiry, the API returns it with 200 instead of creating
  another, so repeated submissions do not fill the table with duplicates and its visit count stays
  in one place. A different expiry gets its own link, because its lifetime differs. Each link stores
  a SHA-256 key of its URL and expiry under a unique constraint, so even simultaneous requests for
  the same URL end up sharing one link.
  [#3](https://github.com/amalps565/be-interview-prep/issues/3)
- **URL Shortener: Every visit is counted, even when many people open a link at once.**
  `GET /api/urls/{code}/stats` shows the original URL, the visit count and the created and expiry
  dates. The count goes up in one `UPDATE … SET visit_count = visit_count + 1` statement, so the
  database serialises concurrent visits and none are lost; a test fires 200 visits from 32 threads
  and counts exactly 200. [#3](https://github.com/amalps565/be-interview-prep/issues/3)
- **URL Shortener: Bad input and dead links get clear errors.** A URL that is not `http` or `https`
  with a host, or an expiry in the past, returns 400 naming the field. An unknown code returns 404,
  and an expired code returns 410 Gone without counting the visit.
  [#3](https://github.com/amalps565/be-interview-prep/issues/3)
- **URL Shortener: Users can choose their own short code.** An optional `customCode` of 3 to 8
  letters, digits, `-` or `_` is used as the code; one that is taken, or reserved such as `api`,
  returns 409. When several requests claim the same code at once, the database's unique constraint
  picks one winner and the rest get 409. [#3](https://github.com/amalps565/be-interview-prep/issues/3)

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
  filter also returns 400 for that field and lists the allowed values. An overdue task can still be
  updated, for example marked `DONE`, as long as its due date is not moved to another past date.
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
