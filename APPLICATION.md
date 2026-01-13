# Digital Wallet Application Documentation

## Overview

**Digital Wallet** is a Spring Boot 4.0.1 RESTful API application built with Java 25 and Maven. It provides a comprehensive wallet management system for customers, enabling them to create wallets, manage funds through deposits and withdrawals, and track financial transactions with built-in approval workflows for large transactions.

## Purpose & Use Cases

The application serves as a backend service for a digital wallet platform where:

- **Customers** can create and maintain multiple wallets in different currencies
- **Financial Operations** include deposits, withdrawals, and transaction tracking
- **Approval Workflows** enforce business rules for large transactions requiring manual approval
- **Balance Management** ensures proper tracking of usable and total balances with transaction state management
- **Transaction History** provides complete audit trails of all wallet operations

## Key Features

### 1. Wallet Management
- **Create Wallets**: Customers can create new wallets with specific currencies
- **Wallet Activation**: Control wallet capabilities with shopping and withdrawal toggles
- **Multi-Currency Support**: Wallets support different currency types
- **Balance Tracking**: Maintains both total and usable balances

### 2. Financial Transactions
- **Deposits**: Add funds to wallets
  - Small deposits complete immediately
  - Large deposits (exceeding `AMOUNT_LIMIT`) require approval
- **Withdrawals**: Remove funds from wallets
  - Validates customer has sufficient balance and withdrawal is enabled
  - Large withdrawals may require approval based on amount
- **Transaction Types**: Tracks DEPOSIT and WITHDRAW operations

### 3. Transaction Approval Workflow
- **Pending Transactions**: Transactions exceeding the threshold are created as PENDING
- **Approval Management**: Approve or deny pending transactions
- **Status Tracking**: Transactions have states: PENDING, APPROVED, DENIED, COMPLETED
- **Balance Reversion**: Denials revert pending transactions and restore previous balances

### 4. Data Management
- **Customer Tracking**: Maintains customer information and wallet relationships
- **Transaction History**: Complete records of all financial operations
- **Audit Trail**: Comprehensive logging for compliance and troubleshooting

## System Architecture

### Layered Design

```
┌─────────────────────────────────────┐
│     REST API Controllers            │  (HTTP Endpoints)
├─────────────────────────────────────┤
│     Service Layer                   │  (Business Logic)
├─────────────────────────────────────┤
│     Repository Layer                │  (Data Access - JPA)
├─────────────────────────────────────┤
│     Database (H2)                   │  (In-Memory Storage)
└─────────────────────────────────────┘
```

### Core Components

#### Controllers
- **WalletController**: Endpoints for wallet operations (create, list, deposit, withdraw)
- **TransactionController**: Endpoints for transaction management (list, approve, deny)
- **IndexController**: Root API endpoint

#### Services
- **WalletService**: Orchestrates wallet creation, deposits, and withdrawals
- **TransactionService**: Manages transaction lifecycle (create, approve, deny, complete)

#### Data Models
- **Customer**: Represents a user; has one-to-many relationship with Wallet
- **Wallet**: Stores balance information, currency, and activation flags
- **Transaction**: Tracks financial operations with status and type information

#### Supporting Components
- **Repositories**: Spring Data JPA interfaces for database operations
- **DTOs**: Request and response objects for API contracts
- **Mappers**: MapStruct-based converters between entities and DTOs
- **Validators**: Custom validation including enum value validation
- **Exception Handlers**: Global exception handling with custom business exceptions

## API Endpoints

### Wallet Endpoints

| Method | Endpoint | Purpose |
|--------|----------|---------|
| POST | `/api/wallet/create` | Create a new wallet for a customer |
| GET | `/api/wallet/list` | List all wallets for a customer |
| POST | `/api/wallet/deposit` | Deposit funds into a wallet |
| POST | `/api/wallet/withdraw` | Withdraw funds from a wallet |

### Transaction Endpoints

| Method | Endpoint | Purpose |
|--------|----------|---------|
| GET | `/api/transaction/list` | List transactions for a wallet |
| POST | `/api/transaction/approve` | Approve a pending transaction |
| POST | `/api/transaction/deny` | Deny a pending transaction |

## Technology Stack

| Component | Technology | Version |
|-----------|-----------|---------|
| Framework | Spring Boot | 4.0.1 |
| Language | Java | 25 |
| Build Tool | Maven | 3.x |
| Database | H2 (In-Memory) | Latest |
| ORM | Spring Data JPA / Hibernate | Latest |
| Mapping | MapStruct | 1.6.3 |
| Logging | SLF4J + Log4j 2 | 2.25.3 |
| API Documentation | SpringDoc OpenAPI | Latest |
| Virtual Threads | Enabled | - |

## Request/Response Flow

### Deposit with Approval

```
1. Client POST /api/wallet/deposit
   ↓
2. WalletService.DepositWallet (validates customer, checks amount)
   ↓
3. Amount > Limit?
   ├─ YES: Create PENDING transaction → Return 202 Accepted
   └─ NO: Complete immediately → Return 200 OK
   ↓
4. If pending, client POST /api/transaction/approve
   ↓
5. TransactionService updates status and completes transaction
   ↓
6. Wallet balance updated → Return 200 OK
```

### Withdrawal with Denial

```
1. Client POST /api/wallet/withdraw
   ↓
2. WalletService.WithdrawWallet (validates customer, balance, permissions)
   ↓
3. Amount > Limit?
   ├─ YES: Create PENDING transaction → Return 202 Accepted
   └─ NO: Complete immediately → Return 200 OK
   ↓
4. If pending, client POST /api/transaction/deny
   ↓
5. TransactionService reverts pending transaction
   ↓
6. Balance restored → Return 200 OK
```

## Configuration

### Application Properties (`application.properties`)

```properties
# Virtual Threads
spring.threads.virtual.enabled=true

# H2 Database
spring.datasource.url=jdbc:h2:mem:digitalwalletdb
spring.datasource.driverClassName=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=password

# H2 Console
spring.h2.console.enabled=true
spring.h2.console.path=/h2-console
```

## Building & Running

### Prerequisites
- Java 25 or higher
- Maven 3.6+

### Build & Run Locally
```bash
# Build application
mvn clean install

# Run application
mvn spring-boot:run

# Run tests
mvn test
```

### Docker & Docker Compose

#### Prerequisites
- Docker 20.10+
- Docker Compose 2.0+

#### Run with Docker Compose

The application includes a complete Docker setup with multi-stage build optimization:

```bash
docker-compose up --build
```

This command will:
1. Build the application using Maven and Java 25
2. Package it as a Docker image
3. Start the container with proper port mapping and health checks
4. Expose the application on `http://localhost:8080`

#### Docker Configuration Details

**Multi-Stage Build (Dockerfile)**:
- **Build Stage**: Uses `maven:3.9.12-eclipse-temurin-25` to compile and package the application
  - Maven dependencies are cached as a separate layer for faster rebuilds
  - Source code compiled and packaged as JAR
- **Runtime Stage**: Uses lightweight `eclipse-temurin:25-jre-jammy` (slim JRE)
  - Only includes runtime dependencies, reducing image size
  - Final image is optimized for production

**JVM Performance Tuning**:
- ZGC garbage collector enabled for low-latency performance
- ZGenerational mode for reduced pause times
- Optimized for Java 25 virtual threads

**Docker Compose Configuration**:
- **Container Name**: `digitalwallet`
- **Port Mapping**: `8080:8080` (host:container)
- **Health Check**: Monitors application health every 30 seconds using Spring Actuator endpoint
  - Test: `curl -f http://localhost:8080/actuator/health`
  - Timeout: 10 seconds
  - Retries: 3 before marking unhealthy
- **Restart Policy**: Automatically restarts unless explicitly stopped
- **Environment Variables**: 
  ```
  SPRING_DATASOURCE_URL=jdbc:h2:mem:digitalwalletdb
  SPRING_DATASOURCE_USERNAME=sa
  SPRING_DATASOURCE_PASSWORD=password
  SPRING_H2_CONSOLE_ENABLED=true
  ```

#### Docker Commands

```bash
# Build and start the application
docker-compose up --build

# Start in background (detached mode)
docker-compose up -d --build

# View logs
docker-compose logs -f

# Stop the application
docker-compose down

# Remove images and volumes
docker-compose down --volumes --rmi all

# Check container status
docker-compose ps

# Execute command inside container
docker-compose exec app sh
```

#### Docker Access Points

Once running in Docker (or locally), access the application at:

| Resource | URL | Credentials |
|----------|-----|-------------|
| **API Root** | `http://localhost:8080/` | - |
| **Swagger UI** | `http://localhost:8080/swagger-ui.html` | - |
| **OpenAPI JSON** | `http://localhost:8080/v3/api-docs` | - |
| **H2 Console** | `http://localhost:8080/h2-console` | User: `sa` / Password: `password` |
| **Health Check** | `http://localhost:8080/actuator/health` | - |
| **Metrics** | `http://localhost:8080/actuator/metrics` | - |

**Database Connection Details** (for H2 Console):
- **Database URL**: `jdbc:h2:mem:digitalwalletdb`
- **Username**: `sa`
- **Password**: `password`

## Business Rules & Constraints

### Transaction Amounts
- **Approval Threshold**: Transactions exceeding `WalletConstants.AMOUNT_LIMIT` require approval
- **Pending Status**: Large transactions are created as PENDING and don't affect balance until approved
- **Auto-Completion**: Transactions under the limit are automatically completed

### Wallet Operations
- **Withdrawal Validation**: 
  - Customer must exist
  - Wallet must have sufficient usable balance
  - Wallet must have withdrawal enabled (`isActiveWithdraw=true`)
- **Deposit Validation**:
  - Customer must exist
  - Wallet must exist and be active

### Transaction Statuses
- **PENDING**: Awaiting approval/denial
- **APPROVED**: Approved but not yet completed (balance update deferred)
- **DENIED**: Rejected; pending balance is reverted
- **COMPLETED**: Transaction fully processed; balance updated

## Error Handling

### HTTP Status Codes

| Status | Meaning | Example |
|--------|---------|---------|
| 200 OK | Successful operation | Transaction approved/completed |
| 202 Accepted | Pending approval required | Large deposit/withdrawal |
| 400 Bad Request | Invalid input or business rule violation | Insufficient balance, invalid enum |
| 404 Not Found | Resource not found | Customer/wallet not found |
| 500 Internal Server Error | Server error | Unexpected exception |

### Exception Types
- **WithdrawalDeniedException**: Raised when withdrawal fails validation
- **ValidationException**: Invalid request data (via Jakarta validation)
- **Custom Exceptions**: Domain-specific business logic failures

## Logging & Monitoring

### Request/Response Logging
- **LoggingInterceptor**: Logs all HTTP requests (method, URI) and responses (status code)
- Format: `[METHOD] [URI] → [STATUS_CODE]`

### Service Logging
- **Info Level**: Business operations with entity IDs for traceability
- **Warn Level**: Not-found cases (customer/wallet not found)
- **Backend**: Log4j 2.25.3 via SLF4J facade

## Testing

### Test Coverage
- **WalletTests**: Wallet service and controller functionality
- **TransactionTests**: Transaction service and controller functionality

### Running Tests
```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=WalletServiceTest
```

## Code Organization

```
src/main/java/com/inghubs/digitalwallet/
├── DigitalWalletApplication.java          # Entry point
├── configurations/
│   ├── OpenApiConfig.java                 # Swagger/OpenAPI configuration
│   └── WebConfig.java                     # Web configuration
├── controllers/                           # REST endpoints
│   ├── IndexController.java
│   ├── WalletController.java
│   └── TransactionController.java
├── dtos/
│   ├── requests/                          # Request DTOs
│   └── responses/                         # Response DTOs
├── entities/                              # JPA entities
│   ├── Customer.java
│   ├── Wallet.java
│   └── Transaction.java
├── repositories/                          # Spring Data JPA
│   ├── CustomerRepository.java
│   ├── WalletRepository.java
│   └── TransactionRepository.java
├── services/                              # Business logic
│   ├── WalletService.java
│   └── TransactionService.java
├── interceptors/
│   └── LoggingInterceptor.java
└── utilities/
    ├── constants/                         # Constants (e.g., AMOUNT_LIMIT)
    ├── enums/                             # Enumerations
    ├── exceptions/                        # Custom exceptions
    ├── mappers/                           # MapStruct mappers
    └── validators/                        # Custom validators
```
