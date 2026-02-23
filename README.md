# ACM Project — Code Review & Improvement Report

## 🐛 Bugs Fixed

### 1. `AuthController` — Login Endpoint Used Wrong DTO

**Original:**
```java
@PostMapping("/login")
public ResponseEntity<AuthRes> login(@RequestBody RegisterReq req) { ... }
```
**Fixed:**

```java
@PostMapping("/login")
public ResponseEntity<AuthRes> login(@Valid @RequestBody LoginReq req) { ... }
```
- `RegisterReq` requires `firstName`, `lastName`, `nationalId` — fields irrelevant for login
- `LoginReq` (new) only requires `username` and `password`

---

### 2. `AuthService.loginCustomer` — Redundant UserDetails Construction

**Original:**

```java
UserDetails userDetails = org.springframework.security.core.userdetails.User
.withUsername(customer.getUsername())
.password(customer.getPassword())
.roles(customer.getRole().name())
.build();
String token = jwtUtil.generateToken(userDetails);
```
**Fixed:**

```java
String token = jwtUtil.generateToken(customer); // Customer already implements UserDetails
```
`Customer` already implements `UserDetails`, so there is no need to reconstruct a new object.

---

### 3. `AuditLog` — No Package Consistency

The original used `ACM.AuditLog` (capital L) as a package name, which violates Java package naming conventions.

**Fixed:** renamed to `com.example.ACM.auditlog` (lowercase).

---

### 4. `AuthenticationManager.java` — Empty Class

The class was empty with no purpose. It has been removed. Spring's `AuthenticationManager` is provided via `SecurityConfig`.

---

## 🏗️ Structure Improvements

**Before:**


ACM/
  AuditLog/          ← capital letter (non-standard)
  auth/
  account/
  customer/
  enums/             ← mixed with account enums
  ...

**After:**


ACM/
  account/
dto/
enums/           ← AccountStatus, AccountType
  auth/
dto/             ← LoginReq, RegisterReq, AuthRes
  auditlog/          ← lowercase (Java standard)
  config/            ← SecurityConfig
  customer/
  enums/             ← global enums: CustomerRole, KycLevel, etc.
  exception/
  repository/
  security/          ← JwtUtil, JwtAuthFilter, CustomerUserDetailsService

---

## ✨ Code Improvements

### Validation Added

All controller inputs now use `@Valid` with constraint annotations:

- `@NotBlank`, `@Size`, `@Pattern` on `RegisterReq`
- `LoginReq` properly created with `@NotBlank`

### `ErrorResponse` Improved

Changed to a record with `@JsonInclude(NON_NULL)` to suppress null `errors` field in simple error responses.

### `AuthRes` Improved

Added `tokenType` field defaulting to `"Bearer"` for standard JWT response format.

### `Customer` Entity Improved

- Implements `UserDetails` properly — `isAccountNonLocked()` checks `BLOCKED` status
- Added `@EnableJpaAuditing` to `AcmApplication` + `@EntityListeners` to entities for auto `createdAt`/`updatedAt`
- Added `kycLevel` field with default `BASIC`
- Added `getFullName()` helper

### `AuditLogService` Improved

- Made `log()` method `@Async` — audit logging no longer blocks request thread
- Added `try/catch` to ensure audit failures don't break business flows

### Repositories Improved

- `CustomerRepo` adds `existsByUsername` and `existsByNationalId` — avoids loading full entity just to check existence
- `AccountRepo` adds `findByCustomerIdAndStatus` for filtered queries

### `SecurityConfig` Added

Replaces the empty `AuthenticationManager` stub:

- Stateless JWT-based filter chain
- Public routes: `/api/v1/auth/**` and `/actuator/health`
- All other routes require authentication

### `JwtAuthFilter` Added

`OncePerRequestFilter` that validates JWT on every request before it reaches controllers.

### `CustomerUserDetailsService` Added

Proper `UserDetailsService` implementation that loads from `CustomerRepo`.

---

## 📄 `application.properties`

- Externalized secrets via environment variables (`${JWT_SECRET}`, `${DB_USER}`, `${DB_PASS}`)
- Sensible defaults for local development
- PostgreSQL dialect configured
- Actuator health endpoint exposed

---

## 🔍 Summary

### 🐛 Bugs Fixed

1. **Login endpoint using wrong DTO** — `AuthController.login()` accepted `RegisterReq` instead of a simple `LoginReq`. A proper `LoginReq` record was created with just `username` and `password`
2. **Redundant UserDetails construction in `loginCustomer()`** — The service was rebuilding a `UserDetails` object from scratch, even though `Customer` already implements `UserDetails`. Now it passes `customer` directly to `jwtUtil.generateToken()`
3. **Empty `AuthenticationManager.java`** — Was a useless stub. Removed and replaced with a proper `SecurityConfig` + `JwtAuthFilter` implementation
4. **Package naming convention violation** — `ACM.AuditLog` (capital L) is not valid Java convention. Renamed to `auditlog` (lowercase)

### 🏗️ Structure Improvements

- Added `config/` package with `SecurityConfig`
- Added `security/` package with `JwtUtil`, `JwtAuthFilter`, `CustomerUserDetailsService`
- Moved DTOs into `auth/dto/` sub-package
- Account-specific enums live in `account/enums/`, global enums stay in `enums/`

### ✨ Other Improvements

- `@Valid` + Bean Validation constraints added to all request DTOs
- `AuditLogService.log()` made `@Async` so audit writes don't slow down requests
- `Customer` implements `UserDetails` properly (blocked status locks account)
- `@EnableJpaAuditing` added so `createdAt`/`updatedAt` are auto-populated
- `application.properties` uses environment variables for secrets (`JWT_SECRET`, `DB_USER`, `DB_PASS`)
- `AuthRes` now includes `tokenType: "Bearer"` for standard JWT response format
