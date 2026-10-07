# API collections

`be-interview-prep/` is a [Bruno](https://www.usebruno.com) collection with a request and a test for
every endpoint: authentication (Q3), tasks (Q1), the URL shortener (Q2), the product catalog (Q4)
and orders (Q5), including the error cases (400, 401, 403, 404, 409, 422). Every request shows its
real inputs in Bruno's Body, Headers, Params and Vars tabs, and its Docs tab says what it does,
what to try changing and which status to expect.

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
   `yourAdminPassword`, with the `ADMIN_EMAIL` and `ADMIN_PASSWORD` the app was started with.
   Bruno keeps secret values on your machine only, so they never end up in the `.bru` files.
4. Open the collection's `...` menu and choose **Run** to run every request top to bottom, or check
   them by hand as described below.

## Check the APIs by hand

1. Open the collection, pick the **local** environment and fill in `adminEmail` and
   `yourAdminPassword` as above.
2. Send the requests one by one in folder order: **Auth**, **Tasks**, **URL Shortener**,
   **Products**, **Orders**, and inside each folder from top to bottom. Later requests reuse ids and
   tokens saved by earlier ones, so start with **Auth / Register user** and **Login user**.
3. Before each send, read the request's **Docs** tab for what it does and the expected status, then
   look at the response and the **Tests** result.
4. Change the inputs and send again to see other results:
   - **Environment:** `userEmail` and `yourPassword` are the account that **Register user** creates
     and **Login user** logs in with. Change them to try a different user; a password under 8
     characters gets 400.
   - **Body tab:** edit any field, for example a blank task `title`, a negative product `price` or
     an order `quantity` above the stock.
   - **Params tab:** query parameters such as `status`, `category`, `minPrice`, `sort` and `size`
     can be edited or unticked; path parameters such as `id` and `code` show which record is used.
   - **Vars tab:** `longUrl` on **Shorten URL** and `customCode` on **Shorten with custom code**.
   - **Headers tab:** the `Idempotency-Key` on **Place order** is a new UUID per send. Replace it
     with a fixed string such as `order-001`, send twice, and the second send returns 200 with
     `Idempotent-Replayed: true` and the same order.

## Run order

The requests share variables, so run them in this order (the runner does this by itself):

| Folder | Requests | Sets |
|---|---|---|
| Auth | Register user (201, or 409 if the user exists), Login user, My profile, List users as USER (403), Login admin, List users as admin, Request without token (401) | `token`, `adminToken` |
| Tasks | Create task, List tasks, Filter by status, Get task, Update task, Delete task, Get deleted task (404), Create invalid task (400) | `taskId` |
| URL Shortener | Shorten URL (201, or 200 if shortened before), Shorten same URL again (200, same code), Shorten with custom code (201, or 409 if the code is taken), Follow short link (302), Stats, Unknown code (404), Invalid URL (400) | `code`, `shortenedUrl` |
| Products | List products, List with combined filters, Page size capped (500 becomes 100), Unknown sort field (400), Get product, Get product again, Create product as USER (403), Create product as admin, Get new product, Update product as admin, Get updated product, Delete product as admin, Get deleted product (404) | `firstProductId`, `productId`, `newProductId` |
| Orders | Create product A as admin (stock 2), Create product B as admin (stock 5), Place order, Retry same order (200, `Idempotent-Replayed: true`), Reuse key with different body (422), Insufficient stock (409), Product stock after order, Get order, Cancel order, Product stock after cancel, Cancel again (409), Missing Idempotency-Key (400) | `productAId`, `productBId`, `idempotencyKey`, `orderId`, `shortStockKey` |

- The collection can be run again against the same app. **Register user** accepts 409 when the
  account already exists, **Shorten URL** accepts 200 for a URL shortened before, **Shorten with
  custom code** accepts 409 when `my-link` is taken, and **Stats** expects at least one visit.
- **Login user** stores the access token in `token`, which the collection sends as
  `Authorization: Bearer {{token}}`. **List users as admin** and the admin product requests override
  that with `{{adminToken}}`, so Auth must run before the other folders.
- **Get new product** reads the new product once so it is cached; **Get updated product** then
  proves the cache returns the new price, not the old one.
- **Orders** creates its own two products so stock numbers are known. **Place order** makes a new
  UUID `Idempotency-Key` every send and buys all of product A and one of B. **Insufficient stock**
  asks for one more A alongside one B, gets 409, and **Product stock after order** shows B at 4,
  so the failed order reserved nothing. Cancelling returns A to 2.
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
  --env-var yourAdminPassword="${ADMIN_PASSWORD}"
```

The run prints every request with its tests and ends with a summary; it exits non-zero if any test
fails. Add `--reporter-html results.html` for a report you can open in a browser.
