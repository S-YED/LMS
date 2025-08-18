-- Mini Leave Management System Database Schema
-- Version: 1.0
-- Description: Initial schema creation with proper constraints and indexes

-- Create employees table with unique constraints and indexes
CREATE TABLE employees (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_id VARCHAR(20) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    department VARCHAR(50) NOT NULL,
    joining_date DATE NOT NULL,
    manager_id BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NOT NULL,
    
    -- Foreign key constraint for manager relationship
    CONSTRAINT fk_employee_manager FOREIGN KEY (manager_id) REFERENCES employees(id) ON DELETE SET NULL,
    
    -- Check constraints
    CONSTRAINT chk_employee_id_format CHECK (employee_id REGEXP '^[A-Z0-9]{3,20}$'),
    CONSTRAINT chk_email_format CHECK (email REGEXP '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$'),
    CONSTRAINT chk_joining_date CHECK (joining_date <= CURDATE())
);

-- Create leave_requests table with foreign key relationships
CREATE TABLE leave_requests (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    request_id VARCHAR(50) NOT NULL UNIQUE,
    employee_id BIGINT NOT NULL,
    leave_type ENUM('VACATION', 'SICK', 'PERSONAL', 'EMERGENCY', 'MATERNITY', 'PATERNITY', 'BEREAVEMENT', 'COMPENSATORY', 'UNPAID') NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    duration ENUM('FULL_DAY', 'HALF_DAY') NOT NULL DEFAULT 'FULL_DAY',
    total_days DECIMAL(4,1) NOT NULL,
    reason VARCHAR(500),
    comments VARCHAR(1000),
    status ENUM('PENDING', 'APPROVED', 'REJECTED', 'AUTO_APPROVED', 'CANCELLED', 'WITHDRAWN') NOT NULL DEFAULT 'PENDING',
    is_emergency_leave BOOLEAN NOT NULL DEFAULT FALSE,
    is_backdated BOOLEAN NOT NULL DEFAULT FALSE,
    approved_by BIGINT,
    approved_at TIMESTAMP NULL,
    rejection_reason VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NOT NULL,
    
    -- Foreign key constraints
    CONSTRAINT fk_leave_request_employee FOREIGN KEY (employee_id) REFERENCES employees(id) ON DELETE CASCADE,
    CONSTRAINT fk_leave_request_approver FOREIGN KEY (approved_by) REFERENCES employees(id) ON DELETE SET NULL,
    
    -- Check constraints
    CONSTRAINT chk_leave_dates CHECK (end_date >= start_date),
    CONSTRAINT chk_total_days_positive CHECK (total_days > 0),
    CONSTRAINT chk_total_days_reasonable CHECK (total_days <= 365),
    CONSTRAINT chk_request_id_format CHECK (request_id REGEXP '^LR-[A-Z0-9]{8}$'),
    CONSTRAINT chk_approved_status CHECK (
        (status IN ('APPROVED', 'AUTO_APPROVED', 'REJECTED') AND approved_by IS NOT NULL AND approved_at IS NOT NULL) OR
        (status NOT IN ('APPROVED', 'AUTO_APPROVED', 'REJECTED'))
    ),
    CONSTRAINT chk_rejection_reason CHECK (
        (status = 'REJECTED' AND rejection_reason IS NOT NULL) OR
        (status != 'REJECTED')
    )
);

-- Create leave_balances table with composite unique constraints
CREATE TABLE leave_balances (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    leave_type ENUM('VACATION', 'SICK', 'PERSONAL', 'EMERGENCY', 'MATERNITY', 'PATERNITY', 'BEREAVEMENT', 'COMPENSATORY', 'UNPAID') NOT NULL,
    total_days DECIMAL(5,1) NOT NULL,
    used_days DECIMAL(5,1) NOT NULL DEFAULT 0.0,
    available_days DECIMAL(5,1) NOT NULL,
    year INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NOT NULL,
    
    -- Foreign key constraint
    CONSTRAINT fk_leave_balance_employee FOREIGN KEY (employee_id) REFERENCES employees(id) ON DELETE CASCADE,
    
    -- Unique constraint for employee, leave type, and year combination
    CONSTRAINT uk_employee_leave_type_year UNIQUE (employee_id, leave_type, year),
    
    -- Check constraints
    CONSTRAINT chk_balance_days_positive CHECK (total_days >= 0 AND used_days >= 0 AND available_days >= 0),
    CONSTRAINT chk_balance_consistency CHECK (available_days = total_days - used_days),
    CONSTRAINT chk_used_not_exceed_total CHECK (used_days <= total_days),
    CONSTRAINT chk_balance_year CHECK (year >= 2020 AND year <= 2050),
    CONSTRAINT chk_reasonable_balance CHECK (total_days <= 500) -- Reasonable upper limit
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

-- Partitioning for leave_requests table (for better performance with large datasets)
-- Note: This is commented out as it requires specific MySQL configuration
-- ALTER TABLE leave_requests 
-- PARTITION BY RANGE (YEAR(created_at)) (
--     PARTITION p2023 VALUES LESS THAN (2024),
--     PARTITION p2024 VALUES LESS THAN (2025),
--     PARTITION p2025 VALUES LESS THAN (2026),
--     PARTITION p2026 VALUES LESS THAN (2027),
--     PARTITION p_future VALUES LESS THAN MAXVALUE
-- );

-- Create views for common queries

-- View for employee hierarchy with manager details
CREATE VIEW employee_hierarchy AS
SELECT 
    e.id,
    e.employee_id,
    e.name,
    e.email,
    e.department,
    e.joining_date,
    m.employee_id AS manager_employee_id,
    m.name AS manager_name,
    m.email AS manager_email
FROM employees e
LEFT JOIN employees m ON e.manager_id = m.id;

-- View for leave request summary with employee details
CREATE VIEW leave_request_summary AS
SELECT 
    lr.id,
    lr.request_id,
    lr.employee_id,
    e.employee_id AS employee_code,
    e.name AS employee_name,
    e.department,
    lr.leave_type,
    lr.start_date,
    lr.end_date,
    lr.duration,
    lr.total_days,
    lr.status,
    lr.is_emergency_leave,
    lr.is_backdated,
    a.employee_id AS approver_employee_id,
    a.name AS approver_name,
    lr.approved_at,
    lr.created_at
FROM leave_requests lr
JOIN employees e ON lr.employee_id = e.id
LEFT JOIN employees a ON lr.approved_by = a.id;

-- View for leave balance summary with utilization
CREATE VIEW leave_balance_summary AS
SELECT 
    lb.id,
    lb.employee_id,
    e.employee_id AS employee_code,
    e.name AS employee_name,
    e.department,
    lb.leave_type,
    lb.total_days,
    lb.used_days,
    lb.available_days,
    lb.year,
    ROUND((lb.used_days / lb.total_days) * 100, 2) AS utilization_percentage,
    CASE 
        WHEN lb.available_days < 5 THEN 'LOW'
        WHEN lb.available_days < 10 THEN 'MEDIUM'
        ELSE 'HIGH'
    END AS balance_status
FROM leave_balances lb
JOIN employees e ON lb.employee_id = e.id;

-- Create triggers for data consistency

-- Trigger to automatically calculate available_days in leave_balances
DELIMITER //
CREATE TRIGGER tr_leave_balance_calculate_available
    BEFORE INSERT ON leave_balances
    FOR EACH ROW
BEGIN
    SET NEW.available_days = NEW.total_days - NEW.used_days;
END//

CREATE TRIGGER tr_leave_balance_update_available
    BEFORE UPDATE ON leave_balances
    FOR EACH ROW
BEGIN
    SET NEW.available_days = NEW.total_days - NEW.used_days;
END//

-- Trigger to set is_backdated flag automatically
CREATE TRIGGER tr_leave_request_set_backdated
    BEFORE INSERT ON leave_requests
    FOR EACH ROW
BEGIN
    IF NEW.start_date < CURDATE() THEN
        SET NEW.is_backdated = TRUE;
    END IF;
END//

-- Trigger to set approved_at when status changes to approved/rejected
CREATE TRIGGER tr_leave_request_set_approved_at
    BEFORE UPDATE ON leave_requests
    FOR EACH ROW
BEGIN
    IF NEW.status IN ('APPROVED', 'AUTO_APPROVED', 'REJECTED') AND OLD.status NOT IN ('APPROVED', 'AUTO_APPROVED', 'REJECTED') THEN
        SET NEW.approved_at = CURRENT_TIMESTAMP;
    END IF;
END//

DELIMITER ;

-- Create stored procedures for common operations

-- Procedure to initialize leave balances for a new employee
DELIMITER //
CREATE PROCEDURE sp_initialize_employee_leave_balance(
    IN p_employee_id BIGINT,
    IN p_year INT
)
BEGIN
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;
    
    START TRANSACTION;
    
    -- Insert default leave balances for all leave types
    INSERT INTO leave_balances (employee_id, leave_type, total_days, used_days, available_days, year)
    VALUES 
        (p_employee_id, 'VACATION', 20.0, 0.0, 20.0, p_year),
        (p_employee_id, 'SICK', 10.0, 0.0, 10.0, p_year),
        (p_employee_id, 'PERSONAL', 5.0, 0.0, 5.0, p_year),
        (p_employee_id, 'EMERGENCY', 2.0, 0.0, 2.0, p_year);
    
    COMMIT;
END//

-- Procedure to update leave balance when leave is approved
CREATE PROCEDURE sp_update_leave_balance_on_approval(
    IN p_employee_id BIGINT,
    IN p_leave_type VARCHAR(20),
    IN p_total_days DECIMAL(4,1),
    IN p_year INT
)
BEGIN
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;
    
    START TRANSACTION;
    
    -- Update the leave balance
    UPDATE leave_balances 
    SET used_days = used_days + p_total_days,
        available_days = total_days - (used_days + p_total_days)
    WHERE employee_id = p_employee_id 
      AND leave_type = p_leave_type 
      AND year = p_year;
    
    -- Check if update was successful
    IF ROW_COUNT() = 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Leave balance record not found';
    END IF;
    
    COMMIT;
END//

-- Procedure to restore leave balance when leave is cancelled/rejected
CREATE PROCEDURE sp_restore_leave_balance(
    IN p_employee_id BIGINT,
    IN p_leave_type VARCHAR(20),
    IN p_total_days DECIMAL(4,1),
    IN p_year INT
)
BEGIN
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;
    
    START TRANSACTION;
    
    -- Restore the leave balance
    UPDATE leave_balances 
    SET used_days = GREATEST(0, used_days - p_total_days),
        available_days = total_days - GREATEST(0, used_days - p_total_days)
    WHERE employee_id = p_employee_id 
      AND leave_type = p_leave_type 
      AND year = p_year;
    
    COMMIT;
END//

DELIMITER ;

-- Create function to check for overlapping leave requests
DELIMITER //
CREATE FUNCTION fn_check_overlapping_leaves(
    p_employee_id BIGINT,
    p_start_date DATE,
    p_end_date DATE,
    p_exclude_request_id VARCHAR(50)
) RETURNS INT
READS SQL DATA
DETERMINISTIC
BEGIN
    DECLARE overlap_count INT DEFAULT 0;
    
    SELECT COUNT(*)
    INTO overlap_count
    FROM leave_requests
    WHERE employee_id = p_employee_id
      AND status IN ('APPROVED', 'AUTO_APPROVED', 'PENDING')
      AND (p_exclude_request_id IS NULL OR request_id != p_exclude_request_id)
      AND NOT (end_date < p_start_date OR start_date > p_end_date);
    
    RETURN overlap_count;
END//

DELIMITER ;

-- Add comments to tables for documentation
ALTER TABLE employees COMMENT = 'Stores employee information with hierarchical manager relationships';
ALTER TABLE leave_requests COMMENT = 'Stores all leave requests with comprehensive tracking and approval workflow';
ALTER TABLE leave_balances COMMENT = 'Tracks leave balances by employee, leave type, and year with fractional day support';