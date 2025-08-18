# Database Migration Scripts

This directory contains the database schema and migration scripts for the Mini Leave Management System.

## Files Overview

### V1__create_schema.sql
- **Purpose**: Creates the complete database schema with tables, constraints, indexes, views, triggers, and stored procedures
- **Tables Created**:
  - `employees`: Employee information with hierarchical manager relationships
  - `leave_requests`: Leave request tracking with comprehensive workflow support
  - `leave_balances`: Leave balance management by employee, type, and year
- **Features**:
  - Proper foreign key relationships and constraints
  - Performance-optimized indexes
  - Data validation through check constraints
  - Automated triggers for data consistency
  - Stored procedures for common operations
  - Views for simplified querying

### V2__insert_sample_data.sql
- **Purpose**: Inserts comprehensive sample data for testing and development
- **Sample Data Includes**:
  - 27 employees across different departments and hierarchy levels
  - Manager-subordinate relationships
  - Leave balances for 2024 (and some 2023 historical data)
  - 20+ leave requests covering various scenarios
  - Edge cases and test scenarios

## Database Schema Details

### Tables

#### employees
- Stores employee information with hierarchical manager relationships
- Unique constraints on `employee_id` and `email`
- Self-referencing foreign key for manager relationships
- Audit fields (`created_at`, `updated_at`)

#### leave_requests
- Comprehensive leave request tracking
- Support for emergency leave, backdated requests
- Status workflow (PENDING → APPROVED/REJECTED/AUTO_APPROVED)
- Working day calculations excluding weekends
- Approval tracking with timestamps

#### leave_balances
- Year-based leave balance tracking
- Support for fractional days (0.5 for half-day)
- Automatic calculation of available days
- Unique constraint per employee-leave_type-year combination

### Key Features

#### Constraints
- **Data Integrity**: Foreign key constraints ensure referential integrity
- **Business Rules**: Check constraints enforce business logic
- **Uniqueness**: Unique constraints prevent duplicate data
- **Validation**: Format validation for emails, employee IDs, etc.

#### Indexes
- **Performance**: Optimized indexes for common query patterns
- **Composite Indexes**: Multi-column indexes for complex queries
- **Covering Indexes**: Include frequently accessed columns

#### Views
- **employee_hierarchy**: Employee details with manager information
- **leave_request_summary**: Leave requests with employee details
- **leave_balance_summary**: Balance information with utilization metrics

#### Stored Procedures
- **sp_initialize_employee_leave_balance**: Set up default leave balances for new employees
- **sp_update_leave_balance_on_approval**: Update balances when leave is approved
- **sp_restore_leave_balance**: Restore balance when leave is cancelled/rejected

#### Functions
- **fn_check_overlapping_leaves**: Check for conflicting leave requests

#### Triggers
- **Automatic Calculations**: Auto-calculate available days in leave balances
- **Status Management**: Auto-set approval timestamps and backdated flags
- **Data Consistency**: Ensure data integrity across related tables

## Sample Data Scenarios

### Employee Hierarchy
```
CEO (Sarah Johnson)
├── HR Head (Michael Chen)
│   ├── HR Specialist (James Wilson)
│   └── HR Assistant (Christopher Moore, Ethan Davis)
├── Engineering Head (David Rodriguez)
│   ├── Team Lead 1 (Jennifer Lee)
│   │   ├── Senior Engineer (Maria Garcia)
│   │   └── Junior Engineers (Sophie Miller, Ryan Johnson)
│   └── Team Lead 2 (Alex Thompson)
│       ├── Senior Engineer (Amanda Brown)
│       └── Junior Engineers (Olivia Wilson, Grace Liu, Lucas Brown)
├── Marketing Head (Lisa Wang)
│   └── Marketing Lead (Emily Davis)
│       └── Marketing Specialists (Rachel Green, Thomas Anderson, Nathan Clark, Zoe Thompson)
└── Finance Head (Robert Smith)
    └── Finance Specialists (Jessica Taylor, Isabella Martinez)
```

### Leave Request Scenarios
- **Approved Requests**: Various leave types with proper approval workflow
- **Pending Requests**: Awaiting manager approval
- **Emergency Leave**: Auto-approved same-day requests
- **Backdated Requests**: Historical requests with justification
- **Rejected Requests**: With detailed rejection reasons
- **Overlapping Requests**: Conflict detection and prevention
- **Long Duration**: Maternity leave spanning multiple months
- **Half-Day Requests**: Fractional day calculations

### Test Cases Covered
1. **Manager Hierarchy**: Multi-level approval chains
2. **Emergency Scenarios**: Auto-approval for urgent requests
3. **Date Validations**: Weekend exclusions, past dates
4. **Balance Management**: Sufficient balance checks, utilization tracking
5. **Conflict Detection**: Overlapping leave prevention
6. **Edge Cases**: Employees without managers, weekend spanning requests

## Usage Instructions

### Running Migrations
1. Ensure MySQL 8.0+ is installed and running
2. Create database: `CREATE DATABASE leave_management_db;`
3. Run schema creation: Execute `V1__create_schema.sql`
4. Insert sample data: Execute `V2__insert_sample_data.sql`

### Verification Queries
```sql
-- Check employee count and hierarchy
SELECT COUNT(*) as total_employees FROM employees;
SELECT * FROM employee_hierarchy LIMIT 10;

-- Check leave balances
SELECT * FROM leave_balance_summary WHERE year = 2024 LIMIT 10;

-- Check leave requests
SELECT * FROM leave_request_summary ORDER BY created_at DESC LIMIT 10;

-- Verify constraints
SELECT COUNT(*) as pending_requests FROM leave_requests WHERE status = 'PENDING';
SELECT COUNT(*) as approved_requests FROM leave_requests WHERE status IN ('APPROVED', 'AUTO_APPROVED');
```

### Common Operations
```sql
-- Initialize leave balance for new employee
CALL sp_initialize_employee_leave_balance(employee_id, 2024);

-- Check for overlapping leaves
SELECT fn_check_overlapping_leaves(employee_id, '2024-06-01', '2024-06-05', NULL);

-- Get employee's current leave balance
SELECT * FROM leave_balance_summary WHERE employee_id = 'EMP001' AND year = 2024;
```

## Performance Considerations

### Indexing Strategy
- Primary keys on all tables for fast lookups
- Foreign key indexes for join performance
- Composite indexes for common query patterns
- Covering indexes to avoid table lookups

### Query Optimization
- Views pre-join frequently accessed data
- Stored procedures reduce network round trips
- Proper data types minimize storage overhead
- Partitioning ready for large datasets (commented out)

### Scalability
- Schema supports horizontal scaling
- Indexes optimized for read-heavy workloads
- Triggers minimize application logic complexity
- Views abstract complex queries

## Maintenance

### Regular Tasks
1. **Index Maintenance**: Monitor and rebuild fragmented indexes
2. **Statistics Update**: Keep query optimizer statistics current
3. **Backup Strategy**: Regular backups of schema and data
4. **Performance Monitoring**: Track slow queries and optimize

### Data Archival
- Consider archiving old leave requests (>2 years)
- Maintain leave balance history for audit purposes
- Implement data retention policies as needed

## Security Considerations

### Access Control
- Use dedicated database user with minimal privileges
- Implement row-level security if needed
- Audit sensitive operations (leave approvals, balance changes)

### Data Protection
- Encrypt sensitive data at rest
- Use SSL/TLS for database connections
- Implement backup encryption
- Regular security audits

## Troubleshooting

### Common Issues
1. **Foreign Key Violations**: Check parent record existence
2. **Constraint Violations**: Verify data meets business rules
3. **Performance Issues**: Check index usage and query plans
4. **Data Inconsistency**: Run integrity checks and fix triggers

### Diagnostic Queries
```sql
-- Check constraint violations
SHOW ENGINE INNODB STATUS;

-- Analyze table statistics
ANALYZE TABLE employees, leave_requests, leave_balances;

-- Check index usage
SHOW INDEX FROM leave_requests;
```

This database schema provides a solid foundation for the Mini Leave Management System with proper data integrity, performance optimization, and comprehensive test data for development and testing purposes.