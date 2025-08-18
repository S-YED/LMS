# Technology Stack & Build System

## Core Technologies
- **Java**: Version 17 (LTS)
- **Spring Boot**: 3.2.0 with Spring Framework ecosystem
- **Database**: MySQL 8.0 with JPA/Hibernate
- **Build Tool**: Maven 3.6+
- **API Documentation**: Swagger/OpenAPI 3 (SpringDoc)

## Key Dependencies
- **Spring Boot Starters**: Web, Data JPA, Validation, Mail, Actuator
- **Database**: MySQL Connector J 8.0.33
- **Testing**: JUnit 5, Mockito, TestContainers, H2 (test scope)
- **Documentation**: SpringDoc OpenAPI UI 2.2.0

## Project Structure
```
src/
├── main/java/com/company/leavemanagementsystem/
│   ├── controller/     # REST endpoints (@RestController)
│   ├── service/        # Business logic (@Service, @Transactional)
│   ├── repository/     # Data access (@Repository, JPA)
│   ├── entity/         # JPA entities (@Entity)
│   ├── dto/           # Request/Response objects
│   └── exception/     # Custom exceptions & global handler
└── test/              # Unit and integration tests
```

## Common Commands

### Development
```bash
# Build project
mvn clean compile

# Run tests
mvn test

# Start application (local profile)
mvn spring-boot:run -Dspring-boot.run.profiles=local

# Start with Windows batch script
run-local.bat
```

### Profiles
- `local`: Development with MySQL/H2
- `test`: Testing with H2 in-memory
- `production`: Production deployment

### Database
- **Schema**: Flyway-style migrations in `src/main/resources/db/migration/`
- **Sample Data**: V2__insert_sample_data.sql provides comprehensive test data
- **Connection**: MySQL with connection pooling via HikariCP

## Application URLs
- **API Base**: `http://localhost:8080/api`
- **Swagger UI**: `http://localhost:8080/swagger-ui.html`
- **Health Check**: `http://localhost:8080/actuator/health`

## Code Conventions
- Package structure follows domain-driven design
- Controllers handle HTTP concerns only
- Services contain business logic with @Transactional
- Repositories use Spring Data JPA conventions
- DTOs separate internal entities from API contracts
- Global exception handling via @ControllerAdvice