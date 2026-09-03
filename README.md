# Samrasya FUDZ

A full-stack dry fruits & nuts e-commerce platform, built as a reference project for
learning Spring Boot microservices and JWT authentication.

**Live site:** [www.samrasyafudz.in](https://www.samrasyafudz.in)

---

## Architecture

```
                        ┌──────────────────────┐
   Browser  ───────────▶│  React (Vite + TS)    │  Firebase Hosting
                        └──────────┬────────────┘
                                   │ HTTPS
                        ┌──────────▼────────────┐
                        │     api-gateway        │  Spring Cloud Gateway
                        └──────────┬────────────┘
                    ┌──────────┬───┴────┬──────────┐
                    │          │        │          │
             ┌──────▼─────┐ ┌──▼───────┐┌──────▼──────┐┌─────────────┐
             │user-service│ │product-s.││order-service││payment-svc *│
             │  (auth)    │ │(catalog) ││(cart/orders)││(Razorpay)   │
             └──────┬─────┘ └──┬───────┘└──────┬──────┘└──────┬──────┘
                    │          │        │          │          │
               ┌────▼────┐ ┌───▼───┐ ┌──▼─────┐ ┌─▼────────┐
               │ usersdb │ │prodDB │ │ordersdb│ │paymentsdb│
               └─────────┘ └───────┘ └────────┘ └──────────┘
                    (each service owns its own PostgreSQL database)
                        * payment-service is currently on hold
```

Each microservice is independently deployable, owns its own database, and validates
JWTs issued by `user-service` using a shared signing secret. Services never share a
database or call each other's internal code directly — all cross-service communication
happens over HTTP.

---

## Tech Stack

**Backend**

- Java 17, Spring Boot 3.4.0
- Spring Cloud Gateway (routing, CORS)
- Spring Security + JWT (mobile OTP-based auth, no passwords)
- Spring Data JPA + PostgreSQL, one database per service
- Flyway for schema migrations

**Frontend**

- React + TypeScript + Vite
- React Router
- Google Places Autocomplete (address entry)

**Infrastructure**

- Google Cloud Run (backend services, scale-to-zero)
- Cloud SQL (PostgreSQL)
- Firebase Hosting (frontend)
- Artifact Registry + Secret Manager

---

## Services

| Service           | Port (local) | Responsibility                                     |
|-------------------|--------------|----------------------------------------------------|
| `api-gateway`     | 8080         | Single entry point, request routing, CORS          |
| `user-service`    | 8081         | OTP login, JWT issuance, profiles, saved addresses |
| `product-service` | 8082         | Product catalog, categories, weight-based variants |
| `order-service`   | 8083         | Cart, checkout, order lifecycle                    |

> **Note:** `payment-service` (Razorpay UPI payments) is currently **on hold** and is not
> yet part of the codebase. It will be added in a future iteration.

---

## Quick Start

### Prerequisites

- Java 17+
- Node.js 18+
- PostgreSQL

### Setup

```bash
git clone https://github.com/divyajnu08/samrasyafudz.git
cd samrasyafudz
```

Create the databases:

```bash
psql -U postgres -c "CREATE DATABASE usersdb;"
psql -U postgres -c "CREATE DATABASE productdb;"
psql -U postgres -c "CREATE DATABASE ordersdb;"
```

Copy the example configs and fill in your own values (see [CONTRIBUTING.md](CONTRIBUTING.md)
for the full list and important notes on the shared JWT secret):

```bash
cp user-service/src/main/resources/application-local.properties.example \
   user-service/src/main/resources/application-local.properties
# repeat for product-service, order-service, api-gateway
```

Create the frontend env file at `samrasyafudz-frontend/.env.development` (it is gitignored,
so there is no committed example). It needs at minimum:

```bash
VITE_API_URL=http://localhost:8080
VITE_GOOGLE_MAPS_API_KEY=
```

Run each service in its own terminal:

```bash
./gradlew :user-service:bootRun --args='--spring.profiles.active=local'
./gradlew :product-service:bootRun --args='--spring.profiles.active=local'
./gradlew :order-service:bootRun --args='--spring.profiles.active=local'
./gradlew :api-gateway:bootRun --args='--spring.profiles.active=local'
```

Run the frontend:

```bash
cd samrasyafudz-frontend
npm install
npm run dev
```

Verify everything is connected:

```bash
curl http://localhost:8080/api/categories
```

Full setup details and a troubleshooting table for common first-run issues are in
[CONTRIBUTING.md](CONTRIBUTING.md).

## Contributing

Contributions are welcome! See [CONTRIBUTING.md](CONTRIBUTING.md) for setup
instructions, code conventions, and a list of areas that need help.

## License

MIT — see [LICENSE](LICENSE).