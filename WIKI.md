# Wiki — Backend Interview Prep Assignment (Java / Spring Boot)

Reference for the assignment this repository implements: what to build, how to deliver it, and how it is assessed.

## Contents

- [Overview](#overview)
- [Submission workflow](#submission-workflow)
- [PR description template](#pr-description-template)
- [README requirements](#readme-requirements)
- [Video guidelines](#video-guidelines)
- [AI usage policy](#ai-usage-policy)
- [Q1 — Task Manager API](#q1--task-manager-api)
- [Q2 — URL Shortener](#q2--url-shortener)
- [Q3 — Authentication & Roles](#q3--authentication--roles)
- [Q4 — Product Catalog](#q4--product-catalog)
- [Q5 — Order Service](#q5--order-service)
- [Optional extras](#optional-extras)
- [Submission checklist](#submission-checklist)

## Overview

Build 5 Spring Boot features in one GitHub repository. Ship each one as its own pull request, then record one 2-minute video explaining all five. AI tools may be used, but every line of submitted code must be explainable in the interview.

| Item | Detail |
|---|---|
| Stack | Java 17+ and Spring Boot 3.x. Build tool, database, libraries and test tools are free choices. |
| Time limit | 2 hours for all 5 questions, including PRs and merges, plus 30 minutes after the cut-off to record and upload the video. |
| What is assessed | Working code, clean structure, tests, Git/PR discipline, and above all understanding of what was built. |

**Suggested time split**

| Question | Time |
|---|---|
| Q1 Task Manager API | 15 min |
| Q2 URL Shortener | 15 min |
| Q3 Authentication & Roles | 25 min |
| Q4 Product Catalog | 25 min |
| Q5 Order Service | 40 min |

Claude Code is expected: use it to write the code fast and spend the saved time understanding it. The questions describe *what* to build, not *how*; the design decisions are yours. Be ready to explain every decision, the alternatives considered, and to make a short live change.

## Submission workflow

Each question is one branch, one PR and one merge, in order from Q1 to Q5.

1. Create a public GitHub repo named `be-interview-prep`. On `main`, add a README and a base Spring Boot project.
2. For each question, create a branch from the latest `main`:

   | Question | Branch |
   |---|---|
   | Q1 | `feature/q1-task-api` |
   | Q2 | `feature/q2-url-shortener` |
   | Q3 | `feature/q3-auth` |
   | Q4 | `feature/q4-product-catalog` |
   | Q5 | `feature/q5-order-service` |

3. Commit in small, meaningful steps, such as `Add create task endpoint` or `Return field errors for invalid input`. Avoid commits like `fix`, `changes` or `final`.
4. Open a PR into `main` using the template below. Review your own diff before merging.
5. Merge the PR (squash or merge commit are both fine), then pull `main` before starting the next branch.
6. After the last merge, record the video, upload it to YouTube and add the link to the README.

## PR description template

```markdown
## Problem
What this PR solves (1–2 lines).

## Approach
Key classes and how a request flows through them.

## Decisions & trade-offs
What you chose, and why over the alternatives.

## How to test
Commands, sample requests, test classes.
```

## README requirements

Keep the README updated as PRs merge, and add the video link at the end:

| # | Question | PR link |
|---|---|---|
| 1 | Task Manager API | |
| 2 | URL Shortener | |
| 3 | Authentication & Roles | |
| 4 | Product Catalog | |
| 5 | Order Service | |

**Video:**

## Video guidelines

After the last merge, record **one screen recording of at most 2 minutes** covering all 5 questions. Upload it to YouTube as **Unlisted** and add the link to the README.

| Time | What to show |
|---|---|
| 0:00–0:10 | Quick tour of the repo and the merged PRs |
| 0:10–1:50 | About 20 seconds per question: what it does, and the most important decision made |
| 1:50–2:00 | One thing to improve with more time |

- Skip line-by-line walkthroughs. Show the one piece of code that matters most and say *why* it was built that way.
- Any recorder works: Loom, OBS, QuickTime or the built-in screen recorder.
- Face camera is optional; keep the voice clear and zoom the IDE font to at least 16px.

## AI usage policy

AI tools (Claude, Copilot, ChatGPT) are allowed. Not understanding your own code is not. The interviewer will open the PRs and ask follow-up questions such as:

- Walk me through what happens from the HTTP request to the database.
- Why did you choose this approach? What alternatives did you consider?
- What breaks if two requests hit this endpoint at the same time?
- What happens if you remove this line?
- Change this behaviour live.

If these cannot be answered about your own code, the question counts as not done.

## Q1 — Task Manager API

Build a REST API to create, view, update, delete and filter tasks.

**Requirements**

- A task has a title (required, max 100 characters), a description, a status (To do / In progress / Done), a due date (cannot be in the past) and a created date.
- Create, list, get one, update and delete tasks. Filter the list by status.
- Reject invalid input with a clear message for each invalid field.
- All errors (invalid input, not found, unexpected) return one consistent JSON format with the right HTTP status.

**Acceptance criteria**

- Invalid input returns 400 with field-level messages. An unknown task returns 404.
- At least one automated test.

## Q2 — URL Shortener

Build a service that turns long URLs into short links.

**Requirements**

- Submit a long URL, optionally with an expiry date, and get back a short code and a short URL.
- Visiting the short URL redirects to the original URL.
- Count every visit. A stats endpoint shows the original URL, the visit count and the created date.
- Short codes are at most 8 characters, unique and URL-safe.
- Reject invalid URLs. Handle unknown and expired codes with an appropriate status.

**Acceptance criteria**

- Shortening the same URL twice behaves the way you decided it should, and you can explain why.
- Visit counts stay accurate when many people open the same link at once.
- At least one automated test.

## Q3 — Authentication & Roles

Secure an API so that only logged-in users can use it, and some endpoints are admin-only.

**Requirements**

- Users can register and log in. Store passwords securely.
- The API is used by web and mobile clients, so authentication must not rely on server-side sessions.
- Login expires after 15 minutes.
- There are two roles, USER and ADMIN. Any logged-in user can view their own profile. Only an ADMIN can list all users.
- A request that isn't logged in returns 401. A logged-in user without the right role gets 403. Both return JSON, not an HTML error page.

**Acceptance criteria**

- A test proves that a USER cannot access the admin endpoint.
- No secrets are hard-coded in the source.

## Q4 — Product Catalog

Build a product listing API that stays fast as the catalog grows.

**Requirements**

- A product has a name, category, price, stock, rating and created date. Seed 100 products on startup.
- List products with pagination and sorting by any field. The response includes the total count and the number of pages.
- Optional filters that can be combined freely: category, price range, in-stock only, and name search.
- The page size is capped at 100.
- Single-product lookups happen far more often than products change. Make repeated lookups fast, but never return stale data after a product is updated or deleted.

**Acceptance criteria**

- Any combination of filters works in a single request.
- Repeated lookups of the same product don't query the database every time. Be able to show how you know.
- At least one automated test.

## Q5 — Order Service

Build an order API that stays correct under heavy, simultaneous use.

**Requirements**

- Products have limited stock. A customer places an order with one or more items.
- An order is all-or-nothing: either every item is reserved, or none is.
- Stock must never go negative or be oversold, even when many customers order the same product at the same moment.
- Clients may retry a request after a network failure. A retried request must not create a duplicate order. Design how a retry is recognised.
- Insufficient stock returns 409 with a clear message.
- Cancelling an order returns its stock.

**Acceptance criteria**

- An automated test fires 50 simultaneous orders for a product with stock 10. Exactly 10 succeed, and the stock ends at 0.
- Retrying the same request creates only one order.

## Optional extras

None of these are required. Finish all 5 core questions first.

| Question | Extra |
|---|---|
| Q1 | Interactive API documentation. |
| Q2 | Let users choose their own custom short code. |
| Q3 | Let users stay logged in beyond 15 minutes without re-entering their password, plus a logout that ends that. |
| Q4 | Make the fast lookups work when several instances of the app are running. |
| Q5 | Implement a second approach to the concurrency problem and write a short comparison; run the tests against a real database. |

## Submission checklist

- [ ] Public repo `be-interview-prep` created, with a README
- [ ] 5 branches, 5 PRs, all merged into `main`
- [ ] Every PR uses the description template
- [ ] The README explains how to run the app and the tests
- [ ] All tests pass
- [ ] Code and PRs done within 2 hours; one video of at most 2 minutes uploaded to YouTube (Unlisted) within 30 minutes after
- [ ] The README has every PR link and the YouTube link
- [ ] You can explain every file in the repo without AI help
