-- Mini Leave Management System Database Schema (H2 Compatible)
-- Version: 1.0
-- Description: Initial schema creation with H2 compatibility

-- Create employees table
CREATE TABLE employees (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_id VARCHAR(20) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    department VARCHAR(50) NOT NULL,
    position VARCHAR(100),
    joining_date DATE NOT NULL,
    manager_id BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    
    -- Foreign key constraint for manager relationship
    CONSTRAINT fk_employee_manager FOREIGN KEY (manager_id) REFERENCES employees(id) ON DELETE SET NULL
);

-- Create leave_requests table
CREATE TABLE leave_requests (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    request_id VARCHAR(50) NOT NULL UNIQUE,
    employee_id BIGINT NOT NULL,
    leave_type VARCHAR(20) NOT NULL CHECK (leave_type IN ('VACATION', 'SICK', 'PERSONAL', 'EMERGENCY', 'MATERNITY', 'PATERNITY', 'BEREAVEMENT', 'COMPENSATORY', 'UNPAID')),
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    duration VARCHAR(10) NOT NULL DEFAULT 'FULL_DAY' CHECK (duration IN ('FULL_DAY', 'HALF_DAY')),
    total_days DECIMAL(4,1) NOT NULL,
    reason VARCHAR(500),
    comments VARCHAR(1000),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'AUTO_APPROVED', 'CANCELLED', 'WITHDRAWN')),
    is_emergency_leave BOOLEAN NOT NULL DEFAULT FALSE,
    is_backdated BOOLEAN NOT NULL DEFAULT FALSE,
    approved_by BIGINT,
    approved_at TIMESTAMP NULL,
    rejection_reason VARCHAR(500),
    application_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    
    -- Foreign key constraints
    CONSTRAINT fk_leave_request_employee FOREIGN KEY (employee_id) REFERENCES employees(id) ON DELETE CASCADE,
    CONSTRAINT fk_leave_request_approver FOREIGN KEY (approved_by) REFERENCES employees(id) ON DELETE SET NULL,
    
    -- Check constraints
    CONSTRAINT chk_leave_dates CHECK (end_date >= start_date),
    CONSTRAINT chk_total_days_positive CHECK (total_days > 0),
    CONSTRAINT chk_total_days_reasonable CHECK (total_days <= 365)
);

-- Create leave_balances table
CREATE TABLE leave_balances (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    leave_type VARCHAR(20) NOT NULL CHECK (leave_type IN ('VACATION', 'SICK', 'PERSONAL', 'EMERGENCY', 'MATERNITY', 'PATERNITY', 'BEREAVEMENT', 'COMPENSATORY', 'UNPAID')),
    total_days DECIMAL(5,1) NOT NULL,
    used_days DECIMAL(5,1) NOT NULL DEFAULT 0.0,
    available_days DECIMAL(5,1) NOT NULL,
    year INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    
    -- Foreign key constraint
    CONSTRAINT fk_leave_balance_employee FOREIGN KEY (employee_id) REFERENCES employees(id) ON DELETE CASCADE,
    
    -- Unique constraint for employee, leave type, and year combination
    CONSTRAINT uk_employee_leave_type_year UNIQUE (employee_id, leave_type, year),
    
    -- Check constraints
    CONSTRAINT chk_balance_days_positive CHECK (total_days >= 0 AND used_days >= 0 AND available_days >= 0),
    CONSTRAINT chk_used_not_exceed_total CHECK (used_days <= total_days),
    CONSTRAINT chk_balance_year CHECK (year >= 2020 AND year <= 2050),
    CONSTRAINT chk_reasonable_balance CHECK (total_days <= 500)
);

-- Performance optimization indexes for employees table
CREATE INDEX idx_employee_id ON employees(employee_id);
CREATE INDEX idx_employee_manager ON employees(manager_id);
CREATE INDEX idx_employee_email ON employees(email);
CREATE INDEX idx_employee_department ON employees(department);
CREATE INDEX idx_employee_joining_date ON employees(joining_date);

-- Performance optimization indexes for leave_requests table
CREATE INDEX idx_leave_employee_date ON leave_requests(employee_id, start_date, end_date);
CREATE INDEX idx_leave_status_date ON leave_requests(status, created_at);
CREATE INDEX idx_leave_request_id ON leave_requests(request_id);
CREATE INDEX idx_leave_approved_by ON leave_requests(approved_by);
CREATE INDEX idx_leave_type_status ON leave_requests(leave_type, status);
CREATE INDEX idx_leave_start_date ON leave_requests(start_date);
CREATE INDEX idx_leave_end_date ON leave_requests(end_date);
CREATE INDEX idx_leave_emergency ON leave_requests(is_emergency_leave, status);

-- Performance optimization indexes for leave_balances table
CREATE INDEX idx_balance_employee_type ON leave_balances(employee_id, leave_type, year);
CREATE INDEX idx_balance_year ON leave_balances(year);
CREATE INDEX idx_balance_type ON leave_balances(leave_type);
CREATE INDEX idx_balance_available ON leave_balances(available_days);

-- Additional composite indexes for common query patterns
CREATE INDEX idx_employee_manager_dept ON employees(manager_id, department);
CREATE INDEX idx_leave_employee_status ON leave_requests(employee_id, status);
CREATE INDEX idx_leave_date_range ON leave_requests(start_date, end_date, status);
CREATE INDEX idx_balance_employee_year ON leave_balances(employee_id, year);