# Contributing

## Prerequisites

- Java 17+ (JDK 21 recommended if using Gradle 8.x — see note on Gradle/JDK compatibility below)
- Node.js 18+
- PostgreSQL running locally
- Gradle (via the included wrapper — do not use a globally installed Gradle; see note below)

## Setup

### 1. Fork and clone

```bash
git clone https://github.com/yourusername/samrasyafudz.git
cd samrasyafudz
```

### 2. Create the databases

```bash
psql -U postgres -c "CREATE DATABASE usersdb;"
psql -U postgres -c "CREATE DATABASE productdb;"
psql -U postgres -c "CREATE DATABASE ordersdb;"
```

### 3. Configure each service

Copy the example config and fill in your own local values:

```bash
cp user-service/src/main/resources/application-local.properties.example \
   user-service/src/main/resources/application-local.properties

cp product-service/src/main/resources/application-local.properties.example \
   product-service/src/main/resources/application-local.properties

cp order-service/src/main/resources/application-local.properties.example \
   order-service/src/main/resources/application-local.properties

cp api-gateway/src/main/resources/application-local.properties.example \
   api-gateway/src/main/resources/application-local.properties

Create `samrasyafudz-frontend/.env.development` (gitignored, no committed example) with:

```bash
VITE_API_URL=http://localhost:8080
VITE_GOOGLE_MAPS_API_KEY=
```

**Important:** the `jwt.secret` value must be **identical, character-for-character**, across
`user-service`, `product-service`, `order-service`. A mismatch here is
the single most common setup mistake — it causes JWT signature validation to silently fail,
which shows up as a confusing `403 Forbidden` rather than a clear error (see "Troubleshooting"
below).

### 4. Run each service — in separate terminals, in this order

Order matters here: `user-service` and `product-service` have no dependencies on other
services and can start in any order; `order-service` calls `product-service`;
`api-gateway` should be started last since it's just a router in front of everything else.

```bash
# Terminal 1 — user-service (port 8081)
./gradlew :user-service:bootRun --args='--spring.profiles.active=local'

# Terminal 2 — product-service (port 8082)
./gradlew :product-service:bootRun --args='--spring.profiles.active=local'

# Terminal 3 — order-service (port 8083)
./gradlew :order-service:bootRun --args='--spring.profiles.active=local'

# Terminal 4 — api-gateway (port 8080)
./gradlew :api-gateway:bootRun --args='--spring.profiles.active=local'
```

Alternatively, if you're using IntelliJ: create a Run/Debug Configuration for each service's
`*Application` class, set **Active profiles** to `local` in each configuration (not the
Program Arguments field), and run all four.

### 5. Run the frontend

```bash
cd samrasyafudz-frontend
npm install
npm run dev
```

Opens on `http://localhost:5173`, configured to talk to `api-gateway` on `localhost:8080`.

### 6. Verify everything is connected

```bash
curl http://localhost:8080/actuator/gateway/routes
curl http://localhost:8080/api/categories
```

The second command is the real end-to-end check — if it returns category data, the full
chain (gateway → product-service → Postgres) is working.

---

## Making changes

- Create a branch: `feature/your-feature-name` or `fix/bug-description`
- Follow existing code conventions (see below)
- Test manually against a full local run (all four services + frontend) before opening a PR — this project doesn't yet
  have automated integration tests, so manual verification matters

## Code conventions

- **Layered structure per service**: `entity/ repository/ dto/ service/ controller/ exception/ security/`
- **Each microservice owns its own database** — never add a direct database connection from one service to another's
  tables; if you need data from another service, add a client call (see `ProductServiceClient` in `order-service` as the
  reference pattern)
- **Ownership scoping**: any endpoint returning per-user data must scope its query by the authenticated user's ID (e.g.,
  `findByIdAndUserId`, not just `findById`) — see `AddressController`/`AddressService` in `user-service` as the
  reference example
- **DTOs, never raw entities, in API responses** — controllers should never return `@Entity` classes directly
- **Snapshot fields for financial/order data**: `CartItem` and `OrderItem` copy product name/price at the time of the
  action rather than referencing live data — preserve this pattern for any new order-related fields
- **Service-to-service calls** use Spring's synchronous `RestClient`, not `WebClient`/reactive — mixing servlet and
  reactive stacks in one service causes classpath conflicts (this bit us once with `api-gateway`, documented in git
  history if you want the full story)

## Pull requests

- Keep PRs focused on one change
- Describe what changed and why in the PR description
- Note which services you tested manually and how