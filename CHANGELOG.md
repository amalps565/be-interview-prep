# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project
adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

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
