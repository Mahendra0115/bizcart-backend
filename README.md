# BizCart Backend

BizCart Backend is a Spring Boot modular monolith for the BizCart small-business commerce platform.

The current backend includes the foundation for the Authentication module, shared user/RBAC entities, repository interfaces, DTO validation, JWT utility support, and Spring Security configuration.

## Tech Stack

- Java 21
- Spring Boot 3.5.x
- Spring Web
- Spring Data JPA
- Spring Security
- Jakarta Validation
- Flyway
- MySQL
- Maven

## Prerequisites

- JDK 21
- MySQL 8.x
- Maven Wrapper from this repository

## Environment Profiles

- `local`: local development profile
- `dev`: hosted development profile

The application reads JWT settings from the `bizcart.auth` configuration namespace. Set a 256-bit minimum JWT secret before running the application.

Example:

```bash
export BIZCART_AUTH_JWT_SECRET="replace-with-a-strong-32-byte-minimum-secret"
```

## Local Setup

1. Create a local MySQL database for BizCart.
2. Configure datasource values through the active profile configuration or environment variables.
3. Start the application with the local profile:

```bash
SPRING_PROFILES_ACTIVE=local ./mvnw spring-boot:run
```

Default local URL:

```text
http://localhost:8080
```

## Build And Test

Compile:

```bash
./mvnw -q -DskipTests compile
```

Run tests:

```bash
./mvnw -q test
```

Run all Maven verification steps:

```bash
./mvnw clean verify
```

## Project Structure

```text
src/main/java/com/mahendra/bizcart_backend/
├── BizcartBackendApplication.java
├── authentication/
│   ├── config/
│   ├── dto/
│   ├── entity/
│   ├── repository/
│   └── security/
├── common/
│   ├── constants/
│   └── entity/
└── user/
    ├── entity/
    ├── enums/
    └── repository/
```

```text
src/main/resources/
├── application.yml
├── application-local.yml
├── application-dev.yml
└── db/migration/
```

## Documentation

Detailed product requirements and technical approach documents are maintained separately in the BizCart project vault:

```text
bizcart-project-vault
```
