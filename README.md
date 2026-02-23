# ACM — Core Banking System

> A production-grade core banking backend built with **Java 21 + Spring Boot 3**, featuring double-entry bookkeeping, JWT authentication, Flyway migrations, and role-based access control.

---

## Table of Contents

- [Overview](#overview)
- [Tech Stack](#tech-stack)
- [Architecture](#architecture)
- [Project Structure](#project-structure)
- [Domain Model](#domain-model)
- [Database Schema](#database-schema)
- [API Reference](#api-reference)
  - [Auth](#auth)
  - [Accounts](#accounts)
  - [Transfers](#transfers)
  - [Payments](#payments)
  - [Admin](#admin)
- [Security](#security)
- [Double-Entry Ledger](#double-entry-ledger)
- [Idempotency](#idempotency)
- [Concurrency & Locking](#concurrency--locking)
- [Audit Logging](#audit-logging)
- [Getting Started](#getting-started)
- [Environment Variables](#environment-variables)
- [Running with Docker](#running-with-docker)
- [Running Tests](#running-tests)

---

## Overview

ACM (Account & Customer Management) is a sample core banking system designed to demonstrate senior-level Spring Boot patterns. It covers the full lifecycle of a banking transaction — from customer onboarding and account creation, through fund transfers with a double-entry ledger, to payment processing with a state machine.

**Key capabilities:**

- Customer registration and JWT-based authentication
- Multi-type bank account management (SAVINGS, CHECKING, CURRENT)
- Fund transfers with double-entry bookkeeping (DEBIT + CREDIT ledger entries)
- Payment lifecycle: `INITIATED → PROCESSING → COMPLETED / FAILED`
- Idempotency keys to prevent duplicate transactions
- Optimistic + pessimistic locking to prevent race conditions
- Async audit logging for every financial operation
- Role-based access control (`USER` / `ADMIN`)
- Flyway-managed database migrations
- OpenAPI / Swagger UI documentation

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Java 21 |
| Framework | Spring Boot 3.x |
| Security | Spring Security 6 + JWT (JJWT) |
| Persistence | Spring Data JPA + Hibernate |
| Database | PostgreSQL 15 |
| Migrations | Flyway |
| Validation | Jakarta Bean Validation |
| Utilities | Lombok |
| API Docs | SpringDoc OpenAPI (Swagger UI) |
| Build | Maven |

---

## Architecture

```
┌─────────────────────────────────────────────────────┐
│                   HTTP Clients                       │
│            (Browser / Postman / Frontend)            │
└────────────────────────┬────────────────────────────┘
                         │
┌────────────────────────▼────────────────────────────┐
│              Spring Security Filter Chain            │
│         JwtAuthFilter → UsernamePasswordFilter       │
└────────────────────────┬────────────────────────────┘
                         │
┌────────────────────────▼────────────────────────────┐
│                  REST Controllers                    │
│   AuthController │ AccountController │               │
│   TransferController │ AdminController               │
└────────────────────────┬────────────────────────────┘
                         │
┌────────────────────────▼────────────────────────────┐
│                   Service Layer                      │
│   AuthService │ AccountService │ TransferService     │
│   PaymentService │ AuditLogService (async)           │
└────────────────────────┬────────────────────────────┘
                         │
┌────────────────────────▼────────────────────────────┐
│                Repository Layer (JPA)                │
│   CustomerRepo │ AccountRepo │ LedgerTransactionRepo │
│   LedgerEntryRepo │ LedgerAccountRepo │ PaymentRepo  │
└────────────────────────┬────────────────────────────┘
                         │
┌────────────────────────▼────────────────────────────┐
│              PostgreSQL 15 Database                  │
│  customers │ accounts │ ledger_accounts              │
│  ledger_transactions │ ledger_entries │ payments     │
│  audit_logs                                          │
└─────────────────────────────────────────────────────┘
```

---

## Project Structure

```
src/main/
├── java/com/example/ACM/
│   ├── AcmApplication.java          # Entry point (@EnableScheduling, @EnableJpaAuditing)
│   │
│   ├── account/                     # Account module
│   │   ├── Account.java             # Entity (@Version for optimistic locking)
│   │   ├── AccountController.java   # REST endpoints /api/v1/accounts
│   │   ├── AccountService.java      # Business logic
│   │   └── dto/
│   │       ├── AccountRes.java      # Response DTO
│   │       └── OpenAccountReq.java  # Request DTO
│   │
│   ├── auth/                        # Authentication module
│   │   ├── AuthController.java      # /api/v1/auth/register, /login
│   │   ├── AuthService.java         # Registration + login logic
│   │   └── dto/
│   │       ├── AuthRes.java         # JWT token response
│   │       ├── LoginReq.java
│   │       └── RegisterReq.java
│   │
│   ├── transfer/                    # Transfer module
│   │   ├── TransferController.java  # POST /api/v1/transfers
│   │   ├── TransferService.java     # Double-entry transfer logic
│   │   ├── TransferReq.java         # Request record
│   │   └── TransferRes.java         # Response record
│   │
│   ├── payment/                     # Payment module
│   │   └── Payment.java             # Payment entity (state machine)
│   │
│   ├── service/
│   │   └── PaymentService.java      # Payment lifecycle + @Scheduled processor
│   │
│   ├── ledger/                      # Double-entry ledger entities
│   │   ├── LedgerAccount.java       # Chart of accounts
│   │   ├── LedgerTransaction.java   # Transaction header
│   │   └── LedgerEntry.java         # Individual DEBIT/CREDIT lines
│   │
│   ├── customer/
│   │   └── Customer.java            # Customer entity (implements UserDetails)
│   │
│   ├── admin/
│   │   └── AdminController.java     # /api/v1/admin/* (ADMIN role only)
│   │
│   ├── auditLog/
│   │   ├── AuditLog.java
│   │   └── AuditLogService.java     # Async audit logging (@Async)
│   │
│   ├── security/
│   │   ├── SecurityConfig.java      # Filter chain, CORS, role rules
│   │   ├── JwtUtil.java             # Token generation & validation
│   │   ├── JwtAuthFilter.java       # Per-request JWT extraction
│   │   ├── JwtProperties.java       # JWT config binding
│   │   └── CustomerUserDetailsService.java
│   │
│   ├── repository/                  # Spring Data JPA interfaces
│   │   ├── AccountRepo.java         # Includes @Lock PESSIMISTIC_WRITE
│   │   ├── CustomerRepo.java
│   │   ├── LedgerTransactionRepo.java
│   │   ├── LedgerEntryRepo.java
│   │   ├── LedgerAccountRepo.java
│   │   ├── PaymentRepo.java
│   │   └── AuditLogRepo.java
│   │
│   ├── enums/
│   │   ├── AccountStatus.java       # ACTIVE, BLOCKED, CLOSED
│   │   ├── AccountType.java         # SAVINGS, CHECKING, CURRENT
│   │   ├── CustomerRole.java        # USER, ADMIN
│   │   ├── CustomerStatus.java      # ACTIVE, BLOCKED, INACTIVE
│   │   ├── EntryType.java           # DEBIT, CREDIT
│   │   ├── KycLevel.java            # BASIC, STANDARD, ENHANCED
│   │   ├── LedgerAccountType.java   # ASSET, LIABILITY, EQUITY, INCOME, EXPENSE
│   │   ├── LedgerTransactionStatus.java # PENDING, COMPLETED, FAILED
│   │   └── PaymentStatus.java       # INITIATED, PROCESSING, COMPLETED, FAILED
│   │
│   ├── exception/
│   │   ├── GlobalException.java     # @RestControllerAdvice
│   │   ├── ErrorResponse.java
│   │   ├── ResourceNotFoundException.java
│   │   └── ResourceAlreadyExistsException.java
│   │
│   └── config/
│       └── OpenApiConfig.java       # Swagger / OpenAPI 3 configuration
│
└── resources/
    ├── application.yaml
    └── db/migration/
        ├── V1__init_customers.sql
        ├── V2__create_accounts.sql
        ├── V3__create_ledger_accounts.sql
        ├── V4__create_ledger_transactions.sql
        ├── V5__create_ledger_entries.sql
        ├── V6__create_payments.sql
        └── V7__create_audit_logs.sql
```

---

## Domain Model

```
Customer (1) ──────────────── (N) Account
   │                                │
   │                         has @Version
   │                         (optimistic lock)
   │
   └── has CustomerRole (USER / ADMIN)
       has KycLevel (BASIC / STANDARD / ENHANCED)
       has CustomerStatus (ACTIVE / BLOCKED)

Account ──── linked to ──── LedgerAccount (1:1 on open)
                                  │
                             LedgerEntry (N)
                                  │
                             LedgerTransaction (N:1)
                             [ reference, status, description ]

Payment ─── triggers ──── TransferService ──── LedgerTransaction
  INITIATED                   doTransfer()
  PROCESSING                  (atomic)
  COMPLETED / FAILED
```

---

## Database Schema

### customers
| Column | Type | Notes |
|--------|------|-------|
| id | BIGSERIAL PK | |
| first_name | VARCHAR(100) | NOT NULL |
| last_name | VARCHAR(100) | NOT NULL |
| username | VARCHAR(100) | NOT NULL UNIQUE |
| password | VARCHAR(255) | BCrypt hashed |
| national_id | VARCHAR(20) | NOT NULL UNIQUE |
| role | VARCHAR(20) | USER / ADMIN |
| kyc_level | VARCHAR(20) | BASIC / STANDARD / ENHANCED |
| status | VARCHAR(20) | ACTIVE / BLOCKED |
| created_at | TIMESTAMP | auto |
| updated_at | TIMESTAMP | auto |

### accounts
| Column | Type | Notes |
|--------|------|-------|
| id | BIGSERIAL PK | |
| account_number | VARCHAR(26) | NOT NULL UNIQUE |
| customer_id | BIGINT FK | → customers.id |
| type | VARCHAR(20) | SAVINGS / CHECKING / CURRENT |
| status | VARCHAR(20) | ACTIVE / BLOCKED / CLOSED |
| balance | NUMERIC(18,2) | DEFAULT 0 |
| version | BIGINT | Optimistic lock counter |
| created_at | TIMESTAMP | |
| updated_at | TIMESTAMP | |

### ledger_accounts
| Column | Type | Notes |
|--------|------|-------|
| id | BIGSERIAL PK | |
| code | VARCHAR(50) | UNIQUE — e.g. `CUSTOMER_1_ACC001` |
| name | VARCHAR(255) | Human-readable name |
| type | VARCHAR(20) | ASSET / LIABILITY / EQUITY |

### ledger_transactions
| Column | Type | Notes |
|--------|------|-------|
| id | BIGSERIAL PK | |
| reference | VARCHAR(100) | UNIQUE — e.g. `TXN-A3F1B2C4` |
| description | VARCHAR(255) | |
| status | VARCHAR(20) | PENDING / COMPLETED / FAILED |
| created_at | TIMESTAMP | |

### ledger_entries
| Column | Type | Notes |
|--------|------|-------|
| id | BIGSERIAL PK | |
| transaction_id | BIGINT FK | → ledger_transactions.id |
| ledger_account_id | BIGINT FK | → ledger_accounts.id |
| entry_type | VARCHAR(10) | DEBIT / CREDIT |
| amount | NUMERIC(18,2) | |

### payments
| Column | Type | Notes |
|--------|------|-------|
| id | BIGSERIAL PK | |
| reference_id | VARCHAR(100) | UNIQUE |
| amount | NUMERIC(18,2) | |
| status | VARCHAR(20) | INITIATED / PROCESSING / COMPLETED / FAILED |
| from_account | VARCHAR(100) | |
| to_account | VARCHAR(100) | |
| ledger_reference | VARCHAR(100) | Set after COMPLETED |
| failure_reason | VARCHAR(500) | Set after FAILED |
| created_at | TIMESTAMP | |

### audit_logs
| Column | Type | Notes |
|--------|------|-------|
| id | BIGSERIAL PK | |
| action | VARCHAR(100) | e.g. TRANSFER, ACCOUNT_OPENED |
| entity_type | VARCHAR(100) | e.g. Account, Payment |
| entity_id | VARCHAR(255) | |
| performed_by | BIGINT | Customer ID (nullable for system actions) |
| details | TEXT | |
| created_at | TIMESTAMP | |

---

## API Reference

Base URL: `http://localhost:8080/api/v1`

> All endpoints except `/auth/**` require a `Bearer` JWT token in the `Authorization` header.

---

### Auth

#### `POST /auth/register`
Register a new customer. Returns a JWT token immediately.

**Request body:**
```json
{
  "firstName": "Ali",
  "lastName": "Ahmadi",
  "username": "ali_ahmadi",
  "password": "secret123",
  "nationalId": "0012345678"
}
```

**Response `201 Created`:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "username": "ali_ahmadi"
}
```

**Error cases:**
- `409 Conflict` — username or nationalId already exists

---

#### `POST /auth/login`
Authenticate an existing customer.

**Request body:**
```json
{
  "username": "ali_ahmadi",
  "password": "secret123"
}
```

**Response `200 OK`:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "username": "ali_ahmadi"
}
```

**Error cases:**
- `401 Unauthorized` — invalid credentials
- `404 Not Found` — username not found

---

### Accounts

#### `POST /accounts`
Open a new bank account for the authenticated customer.

**Headers:** `Authorization: Bearer <token>`

**Request body:**
```json
{
  "type": "SAVINGS"
}
```
> Accepted types: `SAVINGS`, `CHECKING`, `CURRENT`

**Response `201 Created`:**
```json
{
  "id": 1,
  "accountNumber": "ACC17098234561234",
  "type": "SAVINGS",
  "status": "ACTIVE",
  "balance": 0.00,
  "createdAt": "2025-01-15T10:30:00"
}
```

> A corresponding `LedgerAccount` is automatically created when an account is opened.

---

#### `GET /accounts/my`
Get all accounts belonging to the authenticated customer.

**Response `200 OK`:** Array of account objects (same shape as above)

---

#### `GET /accounts/{accountNumber}`
Get a specific account by account number.

**Required roles:** `USER`, `ADMIN`

**Response `200 OK`:** Single account object

**Error cases:**
- `404 Not Found` — account does not exist

---

#### `DELETE /accounts/{accountNumber}`
Close an account. Only allowed if the balance is exactly zero.

**Response `204 No Content`**

**Error cases:**
- `400 Bad Request` — account has non-zero balance
- `404 Not Found` — account does not exist

---

### Transfers

#### `POST /transfers`
Transfer funds between two accounts using double-entry bookkeeping. The operation is fully atomic — both accounts and all ledger entries are updated in a single transaction.

**Headers:**
```
Authorization: Bearer <token>
Idempotency-Key: <uuid>   (optional but recommended)
```

**Request body:**
```json
{
  "fromAccountNumber": "ACC17098234561234",
  "toAccountNumber":   "ACC17098234569876",
  "amount": 500.00,
  "description": "Rent payment for January"
}
```

**Response `201 Created`:**
```json
{
  "reference": "TXN-A3F1B2C4",
  "fromAccountNumber": "ACC17098234561234",
  "toAccountNumber":   "ACC17098234569876",
  "amount": 500.00,
  "status": "COMPLETED",
  "createdAt": "2025-01-15T10:35:00"
}
```

**Error cases:**

| Status | Reason |
|--------|--------|
| `400 Bad Request` | Same source and destination account |
| `400 Bad Request` | Amount is zero or negative |
| `404 Not Found` | Source or destination account not found |
| `404 Not Found` | LedgerAccount not found for either account |
| `422 Unprocessable` | Source account is not ACTIVE |
| `422 Unprocessable` | Destination account is not ACTIVE |
| `422 Unprocessable` | Insufficient balance |
| `409 Conflict` | Concurrent modification — retry the request |

> If the same `Idempotency-Key` is sent again, the original result is returned without executing a new transfer.

---

### Payments

#### `POST /payments`
Initiate a new payment. Creates a payment record in `INITIATED` status without immediately processing it.

**Request body:**
```json
{
  "fromAccountNumber": "ACC17098234561234",
  "toAccountNumber":   "ACC17098234569876",
  "amount": 200.00
}
```

**Response `201 Created`:**
```json
{
  "id": 1,
  "referenceId": "a3f1b2c4-...",
  "amount": 200.00,
  "status": "INITIATED",
  "fromAccountNumber": "ACC17098234561234",
  "toAccountNumber":   "ACC17098234569876",
  "createdAt": "2025-01-15T10:40:00"
}
```

---

#### `POST /payments/{referenceId}/process`
Manually trigger processing of an `INITIATED` payment. Internally calls `TransferService.transfer()`.

**Response `200 OK`:** Updated payment with `COMPLETED` or `FAILED` status

> Payments in `INITIATED` status are also auto-processed every 30 seconds by a `@Scheduled` background job.

**Payment state machine:**
```
INITIATED ──► PROCESSING ──► COMPLETED
                    │
                    └──────► FAILED  (failure_reason is populated)
```

---

### Admin

> All `/admin` endpoints require `ADMIN` role.

#### `GET /admin/customers`
List all customers with pagination.

**Query params:** `?page=0&size=20&sort=createdAt,desc`

**Response `200 OK`:**
```json
{
  "content": [ { "id": 1, "username": "ali_ahmadi", ... } ],
  "totalElements": 42,
  "totalPages": 3,
  "number": 0
}
```

---

#### `PUT /admin/accounts/{accountNumber}/block`
Block a customer account. A blocked account cannot send or receive transfers.

**Response `200 OK`**

---

#### `GET /admin/ledger/transactions`
List all ledger transactions with pagination.

**Query params:** `?page=0&size=20`

**Response `200 OK`:** Paginated list of `LedgerTransaction` objects

---

## Security

### JWT Authentication Flow

```
1. Client sends credentials to POST /auth/login
2. Server validates credentials, generates JWT
3. JWT contains: subject (username), issued-at, expiration
4. Client includes token in every request:
   Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
5. JwtAuthFilter extracts and validates token on each request
6. If valid, sets SecurityContext with authenticated Customer
7. @PreAuthorize annotations enforce role-based access
```

### Token Configuration

| Property | Value |
|----------|-------|
| Algorithm | HS256 |
| Expiration | 3600000 ms (1 hour) — configurable |
| Secret | Min 256-bit key — set via environment variable |

### Password Storage
Passwords are hashed with **BCrypt** before persistence. Plain-text passwords are never stored.

### Endpoint Security Matrix

| Endpoint | Public | USER | ADMIN |
|----------|--------|------|-------|
| `POST /auth/**` | ✅ | ✅ | ✅ |
| `POST /accounts` | — | ✅ | ✅ |
| `GET /accounts/my` | — | ✅ | — |
| `GET /accounts/{n}` | — | ✅ | ✅ |
| `DELETE /accounts/{n}` | — | ✅ | — |
| `POST /transfers` | — | ✅ | — |
| `POST /payments` | — | ✅ | — |
| `GET /admin/**` | — | — | ✅ |
| `PUT /admin/accounts/*/block` | — | — | ✅ |
| `/swagger-ui/**` | ✅ | ✅ | ✅ |
| `/actuator/health` | ✅ | ✅ | ✅ |

---

## Double-Entry Ledger

Every transfer creates a balanced pair of ledger entries. The sum of all DEBITs always equals the sum of all CREDITs — this is the foundation of double-entry bookkeeping used by all financial institutions since the 15th century.

**Example: Transfer of 500 from Account A to Account B**

```
LedgerTransaction:
  reference:   TXN-A3F1B2C4
  description: Rent payment
  status:      COMPLETED

LedgerEntry 1:
  ledger_account: CUSTOMER_1_ACC001  (Account A)
  entry_type:     DEBIT
  amount:         500.00

LedgerEntry 2:
  ledger_account: CUSTOMER_2_ACC002  (Account B)
  entry_type:     CREDIT
  amount:         500.00

DEBIT total = CREDIT total = 500.00  ✓ Balanced
```

**Why this matters:**
- Provides a complete, immutable audit trail of every money movement
- Entries are never deleted or modified — only new entries are created
- Balances can always be recomputed by replaying the ledger
- Enables detecting data corruption: if DEBIT ≠ CREDIT, something is wrong

---

## Idempotency

The `POST /transfers` endpoint accepts an optional `Idempotency-Key` header. If the same key is sent in a subsequent request (e.g., due to a network timeout and client retry), the server returns the original result without executing the transfer again.

**How to use:**
```
POST /api/v1/transfers
Idempotency-Key: 550e8400-e29b-41d4-a716-446655440000
Content-Type: application/json

{ "fromAccountNumber": "...", ... }
```

**Implementation:**
- The `Idempotency-Key` is matched against the `reference` field of existing `LedgerTransaction` records
- If found, the cached result is returned immediately
- If not found, the transfer proceeds and the key is stored as the transaction reference

---

## Concurrency & Locking

### Pessimistic Locking (Transfers)
When a transfer is executed, both the source and destination accounts are locked with `PESSIMISTIC_WRITE` before any balance changes occur:

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT a FROM Account a WHERE a.accountNumber = :accountNumber")
Optional<Account> findByAccountNumberForUpdate(@Param("accountNumber") String accountNumber);
```

This prevents two concurrent transfers from reading the same balance and both succeeding with an incorrect final state.

### Optimistic Locking (Detection)
The `Account` entity has a `@Version` field. If two transactions somehow bypass pessimistic locking and attempt to update the same account simultaneously, Hibernate detects the conflict and throws `OptimisticLockingFailureException`, which `TransferService` catches and converts to a user-friendly error with a retry suggestion.

### Deadlock Prevention
When a transfer involves two accounts, they are always locked in a consistent order (by account number) to prevent the classic deadlock scenario where Transaction A locks Account 1 and waits for Account 2, while Transaction B has locked Account 2 and waits for Account 1.

---

## Audit Logging

Every significant financial operation is recorded asynchronously in the `audit_logs` table. The `@Async` annotation ensures that audit logging never slows down the main transaction.

**Logged events:**

| Action | Triggered By |
|--------|-------------|
| `ACCOUNT_OPENED` | AccountService.openAccount() |
| `ACCOUNT_CLOSED` | AccountService.closeAccount() |
| `TRANSFER` | TransferService.doTransfer() |
| `PAYMENT_INITIATED` | PaymentService.initiatePayment() |
| `PAYMENT_COMPLETED` | PaymentService.processPayment() |
| `PAYMENT_FAILED` | PaymentService.processPayment() |

**Note:** Because audit logs are async, a failed main transaction does NOT prevent audit log failures from being silent — errors in audit logging are caught and logged to the application log instead of propagating.

---

## Getting Started

### Prerequisites

- Java 21+
- Maven 3.9+
- PostgreSQL 15+

### 1. Create the database

```sql
CREATE DATABASE core_banking;
CREATE USER core_user WITH ENCRYPTED PASSWORD 'core_password';
GRANT ALL PRIVILEGES ON DATABASE core_banking TO core_user;
```

### 2. Configure the application

Edit `src/main/resources/application.yaml` or set environment variables (see [Environment Variables](#environment-variables)).

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/core_banking
    username: core_user
    password: core_password

security:
  jwt:
    secret: "CHANGE_THIS_TO_A_LONG_256_BIT_SECRET_KEY"
    expiration: 3600000
```

### 3. Run the application

```bash
mvn spring-boot:run
```

Flyway will automatically run all migrations on startup.

### 4. Explore the API

Open Swagger UI: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)

---

## Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `SPRING_DATASOURCE_URL` | PostgreSQL JDBC URL | `jdbc:postgresql://localhost:5432/core_banking` |
| `SPRING_DATASOURCE_USERNAME` | DB username | `core_user` |
| `SPRING_DATASOURCE_PASSWORD` | DB password | `core_password` |
| `SECURITY_JWT_SECRET` | HS256 signing key (min 32 chars) | ⚠ change before deploying |
| `SECURITY_JWT_EXPIRATION` | Token TTL in milliseconds | `3600000` (1 hour) |
| `SERVER_PORT` | HTTP port | `8080` |

---

## Running with Docker

### docker-compose.yml

```yaml
version: '3.8'

services:
  postgres:
    image: postgres:15-alpine
    environment:
      POSTGRES_DB: core_banking
      POSTGRES_USER: core_user
      POSTGRES_PASSWORD: core_password
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U core_user -d core_banking"]
      interval: 10s
      timeout: 5s
      retries: 5

  app:
    build: .
    ports:
      - "8080:8080"
    depends_on:
      postgres:
        condition: service_healthy
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/core_banking
      SPRING_DATASOURCE_USERNAME: core_user
      SPRING_DATASOURCE_PASSWORD: core_password
      SECURITY_JWT_SECRET: "replace-this-with-a-real-256-bit-secret-key-in-production"

volumes:
  postgres_data:
```

### Dockerfile

```dockerfile
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### Run

```bash
mvn clean package -DskipTests
docker-compose up --build
```

---

## Running Tests

```bash
# Unit tests only
mvn test

# Integration tests (requires Docker for Testcontainers)
mvn verify

# Skip tests
mvn clean package -DskipTests
```

### Integration Test Setup (Testcontainers)

Integration tests spin up a real PostgreSQL container automatically. Add this dependency to `pom.xml`:

```xml
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-testcontainers</artifactId>
  <scope>test</scope>
</dependency>
<dependency>
  <groupId>org.testcontainers</groupId>
  <artifactId>postgresql</artifactId>
  <scope>test</scope>
</dependency>
```

---

## License

This project is a portfolio/resume sample. Feel free to use it as a reference or starting point for your own projects.