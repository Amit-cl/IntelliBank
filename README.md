# 🏦 IntelliBank — Intelligent Digital Banking & AI Financial Advisory Platform

[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.2.5-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Java 17](https://img.shields.io/badge/Java-17-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://openjdk.org/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15+-4169E1?style=for-the-badge&logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![Groq AI](https://img.shields.io/badge/Groq_LPU-Llama_3.3_70B-f59e0b?style=for-the-badge&logo=meta&logoColor=white)](https://groq.com/)
[![License](https://img.shields.io/badge/License-MIT-blue.svg?style=for-the-badge)](LICENSE)

An enterprise-grade, full-stack digital banking platform engineered with **strict transactional integrity (ACID)**, **deterministic database deadlock prevention**, **immutable audit logging**, and a **Lightweight SQL-based RAG pipeline** powered by **Groq LPU (`llama-3.3-70b-versatile`)**.

---

## 📑 Table of Contents
- [Key Features](#-key-features)
- [System Architecture](#-system-architecture)
- [Lightweight RAG AI Engine](#-lightweight-rag-ai-engine)
- [Concurrency & Deadlock Prevention](#-concurrency--deadlock-prevention)
- [Tech Stack](#-tech-stack)
- [API Endpoints](#-api-endpoints)
- [Getting Started](#-getting-started)
- [Interview Quick Reference](#-interview-quick-reference-for-freshers)

---

## 🌟 Key Features

1. **Production-Grade Core Banking**:
   - Customer onboarding with automated unique 10-digit account generation.
   - Account lifecycle management (`PENDING`, `ACTIVE`, `BLOCKED`, `CLOSED`).
   - Deposit, withdrawal, and peer-to-peer instant transfers.
2. **Deterministic Concurrency & ACID Guarantees**:
   - Pessimistic write locks (`PESSIMISTIC_WRITE`) on bank accounts during balance modifications.
   - Deterministic alphabetical lock ordering on transfer pairs to mathematically eliminate database deadlocks.
3. **Stateless Security & RBAC**:
   - Modern Spring Security 6 with OAuth2 Resource Server & Nimbus JWT Encoder/Decoder.
   - Role-Based Access Control (`CUSTOMER`, `EMPLOYEE`, `ADMIN`) via `@PreAuthorize`.
4. **Immutable Audit Trail**:
   - Full compliance logging tracking `LOGIN`, `ACCOUNT_CREATED`, `DEPOSIT`, `WITHDRAWAL`, `TRANSFER`, and `ACCOUNT_STATUS_CHANGE`.
   - Separate access control for customer personal history vs admin forensic oversight.
5. **AI Financial Advisor (Groq LPU + RAG)**:
   - Ultra-low latency (~200ms) conversational assistant powered by Meta's `llama-3.3-70b-versatile` running on Groq LPUs.
   - Real-time spending insights, net cash-flow calculations, top-expense categorization, and proactive budgeting advice.
6. **Modern FinTech Web UI & Swagger**:
   - Dark-mode glassmorphic single-page web portal at `http://localhost:8081`.
   - Full OpenAPI 3.0 interactive documentation at `http://localhost:8081/swagger-ui.html`.

---

## 🏗️ System Architecture

```
                                      ┌─────────────────────────────────────────┐
                                      │      IntelliBank Frontend Portal        │
                                      │  (Vanilla JS, HTML5, CSS Glassmorphism) │
                                      └────────────────────┬────────────────────┘
                                                           │ HTTP / REST (JWT Bearer)
                                                           ▼
┌───────────────────────────────────────────────────────────────────────────────────────────────────────────────┐
│                                            Spring Boot Backend (Port: 8081)                                   │
│                                                                                                               │
│   ┌─────────────────────┐   ┌─────────────────────┐   ┌─────────────────────┐   ┌─────────────────────────┐   │
│   │   AuthController    │   │  AccountController  │   │TransactionController│   │      AiController       │   │
│   └──────────┬──────────┘   └──────────┬──────────┘   └──────────┬──────────┘   └────────────┬────────────┘   │
│              │                         │                         │                           │                │
│              ▼                         ▼                         ▼                           ▼                │
│   ┌─────────────────────┐   ┌─────────────────────┐   ┌─────────────────────┐   ┌─────────────────────────┐   │
│   │     UserService     │   │   AccountService    │   │ TransactionService  │   │        AiService        │   │
│   └──────────┬──────────┘   └──────────┬──────────┘   └──────────┬──────────┘   └────────────┬────────────┘   │
│              │                         │                         │                           │                │
│              │                         ▼                         │                           ▼                │
│              │              ┌─────────────────────┐              │              ┌─────────────────────────┐   │
│              └─────────────►│    AuditService     │◄─────────────┘              │      GroqApiClient      │   │
│                             └──────────┬──────────┘                             └────────────┬────────────┘   │
│                                        │                                                     │                │
│                                        ▼                                                     ▼                │
│                             ┌─────────────────────┐                             ┌─────────────────────────┐   │
│                             │   Spring Data JPA   │                             │  Groq API (LPU Inference│   │
│                             │     Repositories    │                             │   Llama 3.3 70B Model)  │   │
│                             └──────────┬──────────┘                             └─────────────────────────┘   │
└────────────────────────────────────────┼──────────────────────────────────────────────────────────────────────┘
                                         ▼
                             ┌───────────────────────┐
                             │  PostgreSQL Database  │
                             │  (Tables: users,      │
                             │   customers, accounts,│
                             │   transactions, audit)│
                             └───────────────────────┘
```

---

## 🤖 Lightweight RAG AI Engine

Unlike traditional RAG that requires approximate vector databases (e.g. Pinecone) for unstructured documents, **IntelliBank uses a Lightweight SQL-based RAG pipeline**:

```
[ Customer Query: "How much did I spend on dining this month?" ]
                         │
                         ▼
        [ AiService Data Context Aggregator ]
        ├── 1. Fetches authenticated customer's active bank accounts
        ├── 2. Queries PostgreSQL for recent transactions & computes debits/credits
        ├── 3. Scans recent audit events for account security reassurance
        └── 4. Builds structured prompt injected with factual ledger data
                         │
                         ▼
             [ Groq LPU API Inference ]
                         │
                         ▼
[ "You spent ₹4,200 on dining (Swiggy/Zomato). Your remaining balance is ₹45,800." ]
```

---

## ⚡ Concurrency & Deadlock Prevention

When transferring money between Account A and Account B simultaneously:
- **Thread 1**: Transfers A ➔ B (Locks A, then attempts to lock B).
- **Thread 2**: Transfers B ➔ A (Locks B, then attempts to lock A).
- **Deadlock Risk**: Thread 1 waits for B while Thread 2 waits for A.

### IntelliBank Solution: Deterministic Lock Ordering
```java
// TransactionService.java
String accA = request.getFromAccountNumber();
String accB = request.getToAccountNumber();

// Always lock in lexicographical order, regardless of transfer direction:
if (accA.compareTo(accB) < 0) {
    firstLocked = bankAccountRepository.findByAccountNumberWithLock(accA);
    secondLocked = bankAccountRepository.findByAccountNumberWithLock(accB);
} else {
    firstLocked = bankAccountRepository.findByAccountNumberWithLock(accB);
    secondLocked = bankAccountRepository.findByAccountNumberWithLock(accA);
}
```
This guarantees that all concurrent transactions acquire locks in the exact same sequence, **mathematically eliminating deadlocks**.

---

## 🛠️ Tech Stack

| Layer | Technologies |
|---|---|
| **Backend Framework** | Spring Boot 3.2.5, Java 17 |
| **Security & Auth** | Spring Security 6, OAuth2 Resource Server, Nimbus JWT, BCrypt |
| **Persistence & ORM**| Spring Data JPA, Hibernate ORM 6, PostgreSQL |
| **AI Inference & RAG**| Groq Cloud API, Meta Llama 3.3 70B, Spring `RestClient` |
| **Documentation** | Springdoc OpenAPI 3.0, Swagger UI |
| **Testing** | JUnit 5, Mockito, Spring Boot MockMvc, H2 DB (43 passing tests) |
| **Frontend** | Vanilla JavaScript (ES6+), HTML5, CSS3 Glassmorphism |

---

## 📡 API Endpoints

### 1. Authentication (`/api/auth`)
| Method | Endpoint | Description | Access |
|---|---|---|---|
| `POST` | `/api/auth/register` | Register new user & customer profile | Public |
| `POST` | `/api/auth/login` | Authenticate & issue JWT Bearer token | Public |

### 2. Bank Accounts (`/api/accounts`)
| Method | Endpoint | Description | Access |
|---|---|---|---|
| `POST` | `/api/accounts` | Create new Savings/Current account | Customer |
| `GET` | `/api/accounts/my-accounts` | List customer's accounts | Customer |
| `GET` | `/api/accounts/{accountNumber}` | Get specific account details | Customer |
| `PUT` | `/api/admin/accounts/{accountNumber}/status`| Update status (ACTIVE/BLOCKED/CLOSED) | Admin / Employee |

### 3. Transactions (`/api/transactions`)
| Method | Endpoint | Description | Access |
|---|---|---|---|
| `POST` | `/api/transactions/deposit` | Deposit funds | Customer |
| `POST` | `/api/transactions/withdraw` | Withdraw funds | Customer |
| `POST` | `/api/transactions/transfer` | P2P Fund transfer (with deadlock lock) | Customer |
| `GET` | `/api/transactions/history/{accountNumber}` | Paginated transaction history | Customer |

### 4. Audit Logs (`/api/audit-logs`)
| Method | Endpoint | Description | Access |
|---|---|---|---|
| `GET` | `/api/audit-logs/my-logs` | Customer's activity timeline | Customer |
| `GET` | `/api/admin/audit-logs` | System-wide audit inspection | Admin / Employee |

### 5. AI Financial Advisor (`/api/ai`)
| Method | Endpoint | Description | Access |
|---|---|---|---|
| `POST` | `/api/ai/chat` | Conversational financial assistant | Customer |
| `GET` | `/api/ai/insights/{accountNumber}` | Real-time spending analysis & tips | Customer |

---

## 🚀 Getting Started

### Prerequisites
- JDK 17+ installed
- PostgreSQL installed and running (default: port `5432`, db: `intellibank`, user: `postgres`, password: `password`)
- Maven 3.8+

### Setup Database & Environment
```sql
CREATE DATABASE intellibank;
```

```bash
# Copy environment configuration template
cp .env.example .env

# Edit .env with your credentials (PostgreSQL password, Groq API key)
```

### Run Locally
```bash
# Clone the repository
git clone https://github.com/Amit-cl/IntelliBank.git
cd IntelliBank


# Run full test suite (43 automated tests)
mvn test

# Start the Spring Boot application
mvn spring-boot:run
```

Once started:
- **Web Portal**: Visit `http://localhost:8081` in your browser.
- **Swagger UI**: Visit `http://localhost:8081/swagger-ui.html`.

---

## 🎓 Interview Quick Reference (For Freshers)

When discussing this project in interviews:
1. **Concurrency Control:** Explain how you combined `@Lock(LockModeType.PESSIMISTIC_WRITE)` with deterministic account number ordering to avoid race conditions and deadlocks.
2. **AI Integration:** Explain **Lightweight RAG** — why relational banking data needs SQL retrieval rather than vector databases for 100% numerical accuracy.
3. **Security:** Explain stateless JWT architecture with Spring Security 6's OAuth2 Resource Server and RBAC.
4. **Auditability:** Explain how the `audit_logs` table provides an immutable compliance trail for banking security events.
