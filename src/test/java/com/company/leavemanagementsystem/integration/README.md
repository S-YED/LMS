# Integration Tests Documentation

This directory contains comprehensive integration tests for the Leave Management System using TestContainers with MySQL database.

## Overview

The integration tests are designed to test complete workflows, edge cases, and end-to-end scenarios with real database interactions. They use TestContainers to spin up a MySQL database for each test execution, ensuring isolated and reliable testing.

## Test Structure

### Base Test Class
- **BaseIntegrationTest**: Abstract base class that sets up TestContainers with MySQL, configures Spring Boot test environment, and provides common utilities.

### Test Classes

#### 1. LeaveApplicationWorkflowIntegrationTest
Tests complete leave application workflows including:
- **Complete workflow**: Employee creation → Leave application → Manager approval → Balance update
- **Leave rejection workflow**: Application → Manager rejection with reason
- **Emergency leave auto-approval**: Same-day emergency leave processing
- **Overlapping leave validation**: Prevention of conflicting leave requests
- **Insufficient balance validation**: Rejection when exceeding available balance

#### 2. ApprovalProcessIntegrationTest
Tests complex approval scenarios with multiple users:
- **Manager delegation**: Approval by alternate manager when primary is unavailable
- **Multiple pending requests**: Handling multiple requests per employee
- **Concurrent approval attempts**: Thread-safe approval processing
- **Self-approval prevention**: Blocking employees from approving their own requests
- **Backdated leave handling**: Processing of retroactive leave requests

#### 3. EdgeCaseIntegrationTest
Tests edge cases and boundary conditions:
- **Employee without manager**: Emergency leave processing for employees without assigned managers
- **Half-day calculations**: Fractional leave balance handling (0.5 days)
- **Weekend exclusion**: Working day calculations excluding weekends
- **Leave before joining date**: Validation preventing leave before employment start
- **Database constraints**: Handling of unique constraint violations
- **Large dataset pagination**: Performance with multiple employees and requests
- **Invalid date ranges**: Validation of start/end date relationships
- **Employee deletion**: Handling deletion of employees with leave history

#### 4. EndToEndApiTest
Tests complete user journeys from start to finish:
- **Complete employee lifecycle**: Creation → Leave management → Balance tracking
- **Multi-user concurrent scenarios**: Team-wide leave applications and approvals
- **Error recovery**: Data consistency after failed operations
- **Performance testing**: Concurrent operations with multiple users
- **Data consistency**: Verification across multiple operations

#### 5. PerformanceIntegrationTest
Tests system performance under load:
- **Concurrent leave applications**: 20+ employees applying simultaneously
- **Concurrent approval operations**: Multiple managers approving requests
- **Database connection pool stress**: High-volume operations testing connection limits
- **Performance benchmarks**: Response time and throughput measurements

## Key Features Tested

### Business Logic Validation
- Leave balance calculations with fractional days
- Working day calculations (excluding weekends)
- Overlapping leave detection and prevention
- Emergency leave auto-approval (≤2 days)
- Manager hierarchy and delegation
- Self-approval prevention

### Data Integrity
- Database constraint enforcement
- Transaction rollback on errors
- Concurrent operation safety
- Data consistency across operations

### API Functionality
- Complete REST API endpoint coverage
- Request/response validation
- Error handling and status codes
- Pagination and filtering

### Performance Characteristics
- Response times under load
- Database connection pool management
- Concurrent user handling
- Memory and resource usage

## Test Configuration

### TestContainers Setup
```java
@Container
static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
        .withDatabaseName("leave_management_test")
        .withUsername("test")
        .withPassword("test")
        .withReuse(true);
```

### Dynamic Configuration
- Database URL, username, and password configured dynamically
- Mail server disabled for testing
- JPA DDL set to create-drop for clean test environment
- SQL logging enabled for debugging

## Running the Tests

### Prerequisites
- Java 17+
- Maven 3.6+
- Docker (for TestContainers)

### Execution Commands
```bash
# Run all integration tests
./mvnw test -Dtest="**/*IntegrationTest"

# Run specific test class
./mvnw test -Dtest=LeaveApplicationWorkflowIntegrationTest

# Run with specific profile
./mvnw test -Dspring.profiles.active=test -Dtest="**/*IntegrationTest"
```

### Test Execution Time
- Individual test classes: 30-60 seconds
- Complete integration suite: 3-5 minutes
- Performance tests: 1-2 minutes each

## Test Data Management

### Setup Strategy
- Each test creates its own test data
- Helper methods for common setup (employees, managers, balances)
- Isolated test execution with database cleanup

### Sample Data Patterns
- Organizational hierarchies (CEO → Director → Manager → Employee)
- Multiple leave types and durations
- Various approval scenarios
- Edge case data (backdated requests, emergency leaves)

## Assertions and Validations

### Response Validation
- HTTP status codes
- Response body structure and content
- Data type consistency
- Business rule compliance

### Database State Verification
- Leave balance updates
- Request status changes
- Audit trail completeness
- Referential integrity

### Performance Assertions
- Response time thresholds
- Success rate requirements
- Concurrent operation handling
- Resource utilization limits

## Coverage Areas

### Requirements Coverage
All requirements from the specification are covered:
- Employee management (1.1-1.5)
- Leave application (2.1-2.6)
- Approval workflows (3.1-3.6)
- Balance tracking (4.1-4.5)
- Reporting capabilities (5.1-5.5)

### Edge Cases Covered
- Date boundary conditions
- Fractional day calculations
- Concurrent operations
- Error recovery scenarios
- Large dataset handling
- Database constraint violations

### Integration Points
- REST API endpoints
- Database operations
- Business logic services
- Validation mechanisms
- Error handling

## Troubleshooting

### Common Issues
1. **TestContainers startup failures**: Ensure Docker is running
2. **Port conflicts**: TestContainers uses random ports
3. **Memory issues**: Increase JVM heap size for large tests
4. **Timeout errors**: Adjust test timeouts for slower systems

### Debug Configuration
- Enable SQL logging: `spring.jpa.show-sql=true`
- Increase log levels: `logging.level.com.company=DEBUG`
- TestContainers logs: `logging.level.org.testcontainers=DEBUG`

## Future Enhancements

### Potential Additions
- Integration with external services (email, calendar)
- Multi-tenant testing scenarios
- Advanced performance profiling
- Security testing with authentication
- API versioning compatibility tests

### Scalability Testing
- Load testing with JMeter integration
- Database performance under high load
- Memory leak detection
- Connection pool optimization validation

## Best Practices Demonstrated

### Test Design
- Single responsibility per test method
- Clear test naming and documentation
- Proper setup and teardown
- Isolated test execution

### Data Management
- Minimal test data creation
- Realistic test scenarios
- Proper cleanup strategies
- Reusable helper methods

### Assertions
- Comprehensive validation
- Clear failure messages
- Business rule verification
- Performance benchmarks

This integration test suite provides comprehensive coverage of the Leave Management System, ensuring reliability, performance, and correctness across all major use cases and edge conditions.