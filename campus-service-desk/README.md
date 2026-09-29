# 🏫 Campus Service Desk REST API

[![Java](https://img.shields.io/badge/Java-21%2B-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.4-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Security](https://img.shields.io/badge/Spring%20Security-JWT-blue.svg)](https://spring.io/projects/spring-security)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)

A production-grade, portfolio-ready RESTful backend API for a university service desk management platform. University staff and students can submit support tickets, track status in real time, add comments, and handle role-based ticket assignments.

---

## 🌟 Key Features & Highlights

- **🔐 Authentication & RBAC**: Stateless JWT-based authentication supporting three fine-grained roles (`STUDENT`, `SUPPORT_AGENT`, `ADMIN`).
- **🎫 Ticket Lifecycle Management**: Full CRUD operations for support tickets with priority levels (`LOW`, `MEDIUM`, `HIGH`, `URGENT`) and status tracking (`OPEN`, `IN_PROGRESS`, `RESOLVED`, `CLOSED`).
- **💬 Interactivity**: Ticket comment threads for collaboration between students and support agents.
- **📊 Admin Analytics**: Statistics dashboard endpoint providing real-time metrics on open, resolved, and pending tickets.
- **🗄 Database Migrations**: Version-controlled database schema and initial seed data using **Flyway**.
- **🐳 Containerization**: Ready-to-deploy **Docker Compose** setup for both MySQL database and Spring Boot application.
- **📖 API Documentation**: Auto-generated interactive Swagger / OpenAPI 3 UI.
- **🧪 Comprehensive Test Suite**: Unit tests and slice integration tests using JUnit 5, Mockito, and MockMvc.

---

## 🛠 Tech Stack

| Domain | Technology |
|---|---|
| **Language & Framework** | Java 21+, Spring Boot 3.2.4 |
| **Security & Auth** | Spring Security, JJWT (JSON Web Token) |
| **Persistence** | Spring Data JPA, Hibernate, MySQL 8 |
| **Database Migration** | Flyway Migration Engine |
| **API Documentation** | Springdoc OpenAPI (Swagger UI) |
| **Containerization** | Docker, Docker Compose |
| **Testing** | JUnit 5, Mockito, Spring Security Test |

---

## 📐 Architecture & Role-Based Access Control (RBAC)

```
[ Client / Postman / Swagger UI ]
               │
               ▼  (HTTP + JWT Bearer Token)
[ Spring Security Filter Chain & JwtAuthenticationFilter ]
               │
               ▼
[ REST Controllers: AuthController | TicketController | AdminController ]
               │
               ▼
[ Service Layer: AuthService | TicketService ]
               │
               ▼
[ JPA Repositories → Flyway Schema → MySQL Database ]
```

### Roles & Permissions Matrix

| Endpoint | Method | Role Allowed | Description |
|---|---|---|---|
| `/api/auth/register` | `POST` | Public | Register a new user |
| `/api/auth/login` | `POST` | Public | Authenticate and obtain JWT token |
| `/api/tickets` | `POST` | `STUDENT` | Create a new ticket |
| `/api/tickets/my-tickets` | `GET` | `STUDENT` | View user's own tickets |
| `/api/tickets/{id}` | `GET` | `STUDENT`, `SUPPORT_AGENT`, `ADMIN` | View ticket details |
| `/api/tickets/{id}/comments` | `POST` | `STUDENT`, `SUPPORT_AGENT`, `ADMIN` | Add comment to ticket |
| `/api/tickets/assigned` | `GET` | `SUPPORT_AGENT` | View assigned tickets |
| `/api/tickets/{id}/status` | `PUT` | `SUPPORT_AGENT`, `ADMIN` | Update ticket status |
| `/api/admin/tickets/{id}/assign` | `PUT` | `ADMIN` | Assign ticket to an agent |
| `/api/admin/stats` | `GET` | `ADMIN` | View ticket metrics & breakdown |

---

## 🚀 Quick Start Guide

### Prerequisites
- Java 21+ JDK
- Maven 3.8+ (or use `./mvnw`)
- MySQL 8.0+ or Docker Desktop

### 1. Run with Docker Compose (Recommended)

To spin up MySQL and the Spring Boot application in containers:

```bash
docker-compose up --build -d
```

Access the application at `http://localhost:8080`.

### 2. Run Locally with Maven

1. **Start a local MySQL instance** and create a database named `campus_service_desk`.
2. Update database credentials in `src/main/resources/application.properties` (or set environment variables `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`).
3. **Run the application**:

```bash
./mvnw spring-boot:run
```

---

## 📖 API Documentation & Testing

Interactive Swagger UI is accessible when the server is running:

👉 **http://localhost:8080/swagger-ui.html**

### Pre-seeded Demo Accounts

| Role | Username | Password |
|---|---|---|
| Admin | `admin` | `password123` |
| Support Agent | `agent1` | `password123` |
| Student | `student1` | `password123` |

---

## 🧪 Running Automated Tests

Run the full test suite (unit & integration tests):

```bash
./mvnw test
```

---

## 📝 License

This project is licensed under the MIT License.
