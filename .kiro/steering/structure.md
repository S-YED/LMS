# Project Structure & Organization

## Root Directory
```
mini-leave-management-system/
├── .kiro/                    # Kiro AI assistant configuration
├── .mvn/                     # Maven wrapper configuration
├── .vscode/                  # VS Code settings
├── src/                      # Source code
├── target/                   # Maven build output
├── pom.xml                   # Maven project configuration
├── README.md                 # Project documentation
├── run-local.bat            # Windows startup script
└── mvnw.cmd                 # Maven wrapper (Windows)
```

## Source Code Organization

### Main Application (`src/main/java/com/company/leavemanagementsystem/`)
```
controller/          # REST API endpoints
├── EmployeeController.java
├── LeaveRequestController.java
└── LeaveBalanceController.java

service/            # Business logic layer
├── EmployeeService.java
├── LeaveService.java
├── LeaveBalanceService.java
├── LeaveValidationService.java
└── ApprovalDelegationService.java

repository/         # Data access layer
├── EmployeeRepository.java
├── LeaveRequestRepository.java
└── LeaveBalanceRepository.java

entity/            # JPA entities (database models)
├── Employee.java
├── LeaveRequest.java
└── LeaveBalance.java

dto/               # Data Transfer Objects
├── CreateEmployeeRequest.java
├── UpdateEmployeeRequest.java
├── EmployeeResponse.java
├── LeaveApplicationRequest.java
├── LeaveRequestResponse.java
├── InitializeBalanceRequest.java
├── LeaveBalanceResponse.java
└── LeaveBalanceSummaryResponse.java

exception/         # Error handling
├── GlobalExceptionHandler.java
└── ErrorResponse.java
```

### Resources (`src/main/resources/`)
```
db/
├── migration/              # Database schema migrations
│   ├── V1__create_schema.sql
│   └── V2__insert_sample_data.sql
└── README.md              # Database documentation

application.yml            # Main configuration
application-local.yml      # Local development config
application-test.yml       # Test environment config
application-production.yml # Production config
```

### Test Structure (`src/test/java/com/company/leavemanagementsystem/`)
```
controller/        # Controller integration tests
├── EmployeeControllerTest.java
├── LeaveRequestControllerTest.java
└── LeaveBalanceControllerTest.java

service/          # Service unit tests
├── EmployeeServiceTest.java
├── LeaveValidationServiceTest.java
└── ApprovalDelegationServiceTest.java

repository/       # Repository tests
├── EmployeeRepositoryTest.java
├── LeaveRequestRepositoryTest.java
└── LeaveBalanceRepositoryTest.java
```

## Architecture Patterns

### Layered Architecture
1. **Controller Layer**: HTTP request handling, validation, response formatting
2. **Service Layer**: Business logic, transaction management, orchestration
3. **Repository Layer**: Data access, query implementation
4. **Entity Layer**: Domain models, JPA mappings

### Key Design Principles
- **Separation of Concerns**: Each layer has distinct responsibilities
- **Dependency Injection**: Spring-managed beans with constructor injection
- **Transaction Management**: Service layer handles @Transactional boundaries
- **DTO Pattern**: Separate API contracts from internal entities
- **Repository Pattern**: Spring Data JPA with custom query methods

### Naming Conventions
- **Entities**: Singular nouns (Employee, LeaveRequest)
- **DTOs**: Purpose-based suffixes (CreateEmployeeRequest, EmployeeResponse)
- **Services**: Domain + "Service" (EmployeeService, LeaveService)
- **Controllers**: Domain + "Controller" (EmployeeController)
- **Repositories**: Entity + "Repository" (EmployeeRepository)

### Database Schema Organization
- **Tables**: Snake_case naming (employees, leave_requests, leave_balances)
- **Columns**: Snake_case with descriptive names
- **Indexes**: Prefixed with idx_ for performance optimization
- **Constraints**: Prefixed with fk_, uk_, chk_ for clarity
- **Views**: Descriptive names for common query patterns
- **Procedures**: Prefixed with sp_ for stored procedures

## Configuration Management
- **Profiles**: Environment-specific configurations (local, test, production)
- **Properties**: Externalized configuration via application.yml files
- **Database**: Connection pooling, JPA settings, migration configuration
- **Logging**: Structured logging with appropriate levels per environment