# API collections

`be-interview-prep/` is a [Bruno](https://www.usebruno.com) collection with a request and a test for
every endpoint: authentication (Q3), tasks (Q1) and the URL shortener (Q2), including the error
cases (400, 401, 403, 404).

## Start the app

The collection talks to a running app on `http://localhost:8080`. Start it with a signing key and an
admin account (pick your own values; they are never committed):

```bash
export JWT_SECRET="$(openssl rand -base64 48)"
export ADMIN_EMAIL=admin@example.test
export ADMIN_PASSWORD='<choose-a-password>'
./mvnw spring-boot:run
```

## Open it in Bruno

1. In Bruno, choose **Open Collection** and pick the `api-collections/be-interview-prep` folder.
2. Pick the **local** environment in the top-right environment selector.
3. Open the environment settings and fill in the two secret values, `adminEmail` and
   `adminPassword`, with the `ADMIN_EMAIL` and `ADMIN_PASSWORD` the app was started with. Bruno keeps
   secret values on your machine only, so they never end up in the `.bru` files.
4. Open the collection's `...` menu and choose **Run** to run every request top to bottom, or send them
   one by one in the order below.

## Run order

The requests share variables, so run them in this order (the runner does this by itself):

| Folder | Requests | Sets |
|---|---|---|
| Auth | Register user, Login user, My profile, List users as USER (403), Login admin, List users as admin, Request without token (401) | `userEmail`, `userPassword`, `token`, `adminToken` |
| Tasks | Create task, List tasks, Filter by status, Get task, Update task, Delete task, Get deleted task (404), Create invalid task (400) | `taskId` |
| URL Shortener | Shorten URL, Shorten same URL again (200, same code), Shorten with custom code, Follow short link (302), Stats, Unknown code (404), Invalid URL (400) | `longUrl`, `code`, `customCode` |

- **Register user** makes a fresh email and a random password for every run, so the collection can
  be run again against the same app without a 409.
- **Login user** stores the access token in `token`, which the collection sends as
  `Authorization: Bearer {{token}}`. **List users as admin** overrides that with `{{adminToken}}`.
- The register and login request bodies are built in their pre-request scripts so no password is
  written in a `.bru` file.
- **Follow short link** and **Unknown code** turn off redirect following for that request, so the
  `302` and its `Location` header are checked instead of the page it points to.
- A token lasts 15 minutes. If you come back later, send **Login user** again.
- `/api/auth/**` allows 10 requests per minute per address. One run makes 3 of them, so wait a
  minute if you run it more than three times in a row.

## Run it from the command line

From `api-collections/be-interview-prep`, with the app running:

```bash
npx -y @usebruno/cli run --env local \
  --env-var adminEmail="${ADMIN_EMAIL}" \
  --env-var adminPassword="${ADMIN_PASSWORD}"
```

The run prints every request with its tests and ends with a summary; it exits non-zero if any test
fails. Add `--reporter-html results.html` for a report you can open in a browser.
