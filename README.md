# IntelliBank

A RESTful banking backend API developed with Spring Boot 3, Java 17, PostgreSQL, and Spring Security. The project simulates core digital banking operations like account creation, fund transfers, and audit logs, along with an AI-assisted spending insights feature integrated with Groq.

---

## 📌 Project Overview

IntelliBank was built to practice developing a secure, transaction-safe backend service using modern Spring Boot practices. It handles core banking workflows such as customer onboarding, role-based access control, deposits, withdrawals, and account-to-account transfers with concurrency safety. Additionally, it integrates a Groq-powered AI assistant that provides basic spending summaries and answers questions using the user's account and transaction context.

---

## ✨ Current Features

### 1. Authentication & Security
- User registration and login using JWT (JSON Web Tokens).
- Password encryption using BCrypt.
- Role-Based Access Control (`ROLE_CUSTOMER`, `ROLE_EMPLOYEE`, `ROLE_ADMIN`).
- Stateless authentication using Spring Security 6 with OAuth2 Resource Server.

### 2. Bank Account Management
- Create Savings or Current accounts linked to a customer profile.
- Automated generation of unique 10-digit account numbers.
- Account status tracking (`ACTIVE`, `BLOCKED`, `CLOSED`).
- Administrative status updates (e.g., block/unblock accounts) restricted to employees and admins.

### 3. Transactions & Concurrency
- Deposit and withdrawal operations with balance checks and input validation.
- Account-to-account money transfers wrapped in `@Transactional`.
- Pessimistic write locks (`PESSIMISTIC_WRITE`) and ordered account locking (by account number) to avoid race conditions and deadlock issues during simultaneous transfers.
- Paginated transaction history for each account.

### 4. Audit Logging
- Immutable logging of security and transaction events (login, account creation, deposits, withdrawals, transfers, and status changes).
- Customers can view their own activity timeline (`/api/audit-logs/my-logs`).
- Employees and admins can view system-wide logs (`/api/admin/audit-logs`).

### 5. AI Financial Assistant
- Integration with Groq Cloud API using the `llama-3.3-70b-versatile` model.
- Chat endpoint (`/api/ai/chat`) that answers user questions using their recent transaction history and current account balance as context.
- Spending insights endpoint (`/api/ai/insights/{accountNumber}`) that calculates debits, credits, and top expense categories.

---

## 🛠️ Tech Stack

- **Language:** Java 17
- **Framework:** Spring Boot 3.2.5
- **Database:** PostgreSQL (H2 for unit/integration tests)
- **Persistence:** Spring Data JPA, Hibernate 6
- **Security:** Spring Security 6, Nimbus JWT
- **AI Integration:** Groq API (Spring `RestClient`)
- **API Documentation:** Springdoc OpenAPI 3.0 (Swagger UI)
- **Testing:** JUnit 5, Mockito, Spring Boot Test (MockMvc)
- **Build Tool:** Maven

---

## 📁 Project Structure

```text
src/main/java/com/intellibank/
├── IntelliBankApplication.java   # Main application entry point
├── config/                        # Security, OpenAPI, and RestClient configs
│   ├── OpenApiConfig.java
│   ├── RestClientConfig.java
│   └── SecurityConfig.java
├── controller/                    # REST controllers
│   ├── AccountController.java
│   ├── AdminAccountController.java
│   ├── AdminAuditLogController.java
│   ├── AiController.java
│   ├── AuditLogController.java
│   ├── AuthController.java
│   └── TransactionController.java
├── dto/                           # Request and response payloads
├── entity/                        # JPA entities (User, Account, Transaction, AuditLog)
├── exception/                     # Custom exceptions and GlobalExceptionHandler
├── repository/                    # Spring Data JPA interfaces
├── security/                      # JWT token provider and security filters
└── service/                       # Business logic and Groq API client
```

---

## 🚀 Setup & Run Instructions

### Prerequisites
- JDK 17 or higher
- Maven 3.8+
- PostgreSQL installed and running locally

### 1. Database Setup
Create a PostgreSQL database named `intellibank`:
```sql
CREATE DATABASE intellibank;
```

### 2. Configure Database & Properties
Check `src/main/resources/application.properties` and verify your PostgreSQL database credentials:
- Set your PostgreSQL password for `spring.datasource.password` (or pass via `DB_PASSWORD` environment variable).
- Set your Groq API key via `GROQ_API_KEY` (optional, for AI assistant features).

### 3. Run Automated Tests
```bash
mvn test
```

### 4. Start the Application
```bash
mvn spring-boot:run
```
The server will start on port `8081`.

---

## 📖 API Documentation

Interactive API documentation is available via Swagger UI once the application is running:

- **Swagger UI:** `http://localhost:8081/swagger-ui.html`
- **OpenAPI JSON:** `http://localhost:8081/v3/api-docs`

### Key Endpoints

| Area | Method | Endpoint | Description | Access |
|---|---|---|---|---|
| **Auth** | `POST` | `/api/auth/register` | Register customer profile | Public |
| **Auth** | `POST` | `/api/auth/login` | Login and receive JWT token | Public |
| **Accounts** | `POST` | `/api/accounts` | Open a new bank account | Customer |
| **Accounts** | `GET` | `/api/accounts/my-accounts` | List customer accounts | Customer |
| **Accounts** | `GET` | `/api/accounts/{accountNumber}` | Get account details | Customer |
| **Admin** | `PUT` | `/api/admin/accounts/{accountNumber}/status` | Update account status | Admin / Employee |
| **Transactions** | `POST` | `/api/transactions/deposit` | Deposit money | Customer |
| **Transactions** | `POST` | `/api/transactions/withdraw` | Withdraw money | Customer |
| **Transactions** | `POST` | `/api/transactions/transfer` | Transfer between accounts | Customer |
| **Transactions** | `GET` | `/api/transactions/history/{accountNumber}` | View transaction history | Customer |
| **Audit Logs** | `GET` | `/api/audit-logs/my-logs` | View own audit events | Customer |
| **Audit Logs** | `GET` | `/api/admin/audit-logs` | View system audit events | Admin / Employee |
| **AI Assistant** | `POST` | `/api/ai/chat` | Chat with AI financial advisor | Customer |
| **AI Assistant** | `GET` | `/api/ai/insights/{accountNumber}` | Spending summary & insights | Customer |

---

## 🔮 Future Improvements

- Add a dedicated frontend interface (React or Vue).
- Implement email or SMS notifications for transfers.
- Add rate limiting for authentication and transaction endpoints.
- Support scheduled / recurring transfers.
- Containerize using Docker and Docker Compose.
