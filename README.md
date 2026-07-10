Backend project ke root me sabse pehle **`README.md`** rakho. Isme project ka overview, tech stack, setup aur run steps honge.

````markdown
# BizCart Backend

Backend API for BizCart – Small Business Commerce Platform.

## Tech Stack

- Java 21
- Spring Boot 3.5.x
- Spring Data JPA
- Spring Security
- MySQL
- Flyway
- Swagger/OpenAPI
- Docker
- Maven

## Project Structure

```text
src/main/java/com/bizcart/
├── auth/
├── user/
├── category/
├── product/
├── inventory/
├── cart/
├── order/
├── common/
└── config/
````

## Environments

* `local` – Local development
* `dev` – Hosted development environment

## Run Locally

```bash
SPRING_PROFILES_ACTIVE=local ./mvnw spring-boot:run
```

## Run with Dev Profile

```bash
SPRING_PROFILES_ACTIVE=dev ./mvnw spring-boot:run
```

## Build Project

```bash
./mvnw clean install
```

## Local URLs

```text
Backend: http://localhost:8080
Swagger: http://localhost:8080/swagger-ui/index.html
```

## Branch Strategy

```text
main
develop
feature/*
fix/*
```

## Documentation

Detailed documentation is maintained in the separate repository:

```text
bizcart-project-vault
```

It contains:

* Project requirements
* Technical approach
* ER diagram
* API documentation
* QA checklists
* Environment setup
* Release notes

````

Backend root me initially ye files enough hain:

```text
bizcart-backend/
├── README.md
├── .gitignore
├── pom.xml
├── mvnw
├── mvnw.cmd
├── .mvn/
└── src/
````

Detailed requirements, approach aur QA files backend repository me duplicate mat karo; unhe `bizcart-project-vault` me manage karo.
