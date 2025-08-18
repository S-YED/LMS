-- Mini Leave Management System Sample Data
-- Version: 2.0
-- Description: Insert sample data for testing and development

-- Disable foreign key checks temporarily for easier insertion
SET FOREIGN_KEY_CHECKS = 0;

-- Insert sample employees with manager relationships
-- Note: Inserting in order to handle manager relationships properly

-- CEO (no manager)
INSERT INTO employees (employee_id, name, email, department, joining_date, manager_id, created_at, updated_at) VALUES
('CEO001', 'Sarah Johnson', 'sarah.johnson@company.com', 'Executive', '2020-01-15', NULL, '2020-01-15 09:00:00', '2020-01-15 09:00:00');

-- Department Heads (report to CEO)
INSERT INTO employees (employee_id, name, email, department, joining_date, manager_id, created_at, updated_at) VALUES
('HR001', 'Michael Chen', 'michael.chen@company.com', 'Human Resources', '2020-02-01', 1, '2020-02-01 09:00:00', '2020-02-01 09:00:00'),
('ENG001', 'David Rodriguez', 'david.rodriguez@company.com', 'Engineering', '2020-02-15', 1, '2020-02-15 09:00:00', '2020-02-15 09:00:00'),
('MKT001', 'Lisa Wang', 'lisa.wang@company.com', 'Marketing', '2020-03-01', 1, '2020-03-01 09:00:00', '2020-03-01 09:00:00'),
('FIN001', 'Robert Smith', 'robert.smith@company.com', 'Finance', '2020-03-15', 1, '2020-03-15 09:00:00', '2020-03-15 09:00:00');

-- Team Leads (report to department heads)
INSERT INTO employees (employee_id, name, email, department, joining_date, manager_id, created_at, updated_at) VALUES
('ENG002', 'Jennifer Lee', 'jennifer.lee@company.com', 'Engineering', '2021-01-10', 3, '2021-01-10 09:00:00', '2021-01-10 09:00:00'),
('ENG003', 'Alex Thompson', 'alex.thompson@company.com', 'Engineering', '2021-02-01', 3, '2021-02-01 09:00:00', '2021-02-01 09:00:00'),
('MKT002', 'Emily Davis', 'emily.davis@company.com', 'Marketing', '2021-03-01', 4, '2021-03-01 09:00:00', '2021-03-01 09:00:00'),
('HR002', 'James Wilson', 'james.wilson@company.com', 'Human Resources', '2021-04-01', 2, '2021-04-01 09:00:00', '2021-04-01 09:00:00');

-- Senior Engineers and Specialists
INSERT INTO employees (employee_id, name, email, department, joining_date, manager_id, created_at, updated_at) VALUES
('ENG004', 'Maria Garcia', 'maria.garcia@company.com', 'Engineering', '2022-01-15', 6, '2022-01-15 09:00:00', '2022-01-15 09:00:00'),
('ENG005', 'Kevin Park', 'kevin.park@company.com', 'Engineering', '2022-02-01', 6, '2022-02-01 09:00:00', '2022-02-01 09:00:00'),
('ENG006', 'Amanda Brown', 'amanda.brown@company.com', 'Engineering', '2022-03-01', 7, '2022-03-01 09:00:00', '2022-03-01 09:00:00'),
('ENG007', 'Daniel Kim', 'daniel.kim@company.com', 'Engineering', '2022-04-01', 7, '2022-04-01 09:00:00', '2022-04-01 09:00:00'),
('MKT003', 'Rachel Green', 'rachel.green@company.com', 'Marketing', '2022-05-01', 8, '2022-05-01 09:00:00', '2022-05-01 09:00:00'),
('MKT004', 'Thomas Anderson', 'thomas.anderson@company.com', 'Marketing', '2022-06-01', 8, '2022-06-01 09:00:00', '2022-06-01 09:00:00'),
('FIN002', 'Jessica Taylor', 'jessica.taylor@company.com', 'Finance', '2022-07-01', 5, '2022-07-01 09:00:00', '2022-07-01 09:00:00'),
('HR003', 'Christopher Moore', 'christopher.moore@company.com', 'Human Resources', '2022-08-01', 9, '2022-08-01 09:00:00', '2022-08-01 09:00:00');

-- Junior employees
INSERT INTO employees (employee_id, name, email, department, joining_date, manager_id, created_at, updated_at) VALUES
('ENG008', 'Sophie Miller', 'sophie.miller@company.com', 'Engineering', '2023-01-15', 10, '2023-01-15 09:00:00', '2023-01-15 09:00:00'),
('ENG009', 'Ryan Johnson', 'ryan.johnson@company.com', 'Engineering', '2023-02-01', 10, '2023-02-01 09:00:00', '2023-02-01 09:00:00'),
('ENG010', 'Olivia Wilson', 'olivia.wilson@company.com', 'Engineering', '2023-03-01', 11, '2023-03-01 09:00:00', '2023-03-01 09:00:00'),
('MKT005', 'Nathan Clark', 'nathan.clark@company.com', 'Marketing', '2023-04-01', 14, '2023-04-01 09:00:00', '2023-04-01 09:00:00'),
('FIN003', 'Isabella Martinez', 'isabella.martinez@company.com', 'Finance', '2023-05-01', 16, '2023-05-01 09:00:00', '2023-05-01 09:00:00'),
('HR004', 'Ethan Davis', 'ethan.davis@company.com', 'Human Resources', '2023-06-01', 17, '2023-06-01 09:00:00', '2023-06-01 09:00:00');

-- Recent hires (2024)
INSERT INTO employees (employee_id, name, email, department, joining_date, manager_id, created_at, updated_at) VALUES
('ENG011', 'Grace Liu', 'grace.liu@company.com', 'Engineering', '2024-01-15', 12, '2024-01-15 09:00:00', '2024-01-15 09:00:00'),
('ENG012', 'Lucas Brown', 'lucas.brown@company.com', 'Engineering', '2024-02-01', 13, '2024-02-01 09:00:00', '2024-02-01 09:00:00'),
('MKT006', 'Zoe Thompson', 'zoe.thompson@company.com', 'Marketing', '2024-03-01', 15, '2024-03-01 09:00:00', '2024-03-01 09:00:00');

-- Initialize leave balances for all employees for 2024
-- Using the stored procedure we created in the schema

-- CEO
CALL sp_initialize_employee_leave_balance(1, 2024);

-- Department Heads
CALL sp_initialize_employee_leave_balance(2, 2024);
CALL sp_initialize_employee_leave_balance(3, 2024);
CALL sp_initialize_employee_leave_balance(4, 2024);
CALL sp_initialize_employee_leave_balance(5, 2024);

-- Team Leads
CALL sp_initialize_employee_leave_balance(6, 2024);
CALL sp_initialize_employee_leave_balance(7, 2024);
CALL sp_initialize_employee_leave_balance(8, 2024);
CALL sp_initialize_employee_leave_balance(9, 2024);

-- Senior employees
CALL sp_initialize_employee_leave_balance(10, 2024);
CALL sp_initialize_employee_leave_balance(11, 2024);
CALL sp_initialize_employee_leave_balance(12, 2024);
CALL sp_initialize_employee_leave_balance(13, 2024);
CALL sp_initialize_employee_leave_balance(14, 2024);
CALL sp_initialize_employee_leave_balance(15, 2024);
CALL sp_initialize_employee_leave_balance(16, 2024);
CALL sp_initialize_employee_leave_balance(17, 2024);

-- Junior employees
CALL sp_initialize_employee_leave_balance(18, 2024);
CALL sp_initialize_employee_leave_balance(19, 2024);
CALL sp_initialize_employee_leave_balance(20, 2024);
CALL sp_initialize_employee_leave_balance(21, 2024);
CALL sp_initialize_employee_leave_balance(22, 2024);
CALL sp_initialize_employee_leave_balance(23, 2024);

-- Recent hires
CALL sp_initialize_employee_leave_balance(24, 2024);
CALL sp_initialize_employee_leave_balance(25, 2024);
CALL sp_initialize_employee_leave_balance(26, 2024);

-- Insert some historical leave balances for 2023 (with some usage)
INSERT INTO leave_balances (employee_id, leave_type, total_days, used_days, available_days, year, created_at, updated_at) VALUES
-- CEO (minimal usage)
(1, 'VACATION', 20.0, 5.0, 15.0, 2023, '2023-01-01 00:00:00', '2023-12-31 23:59:59'),
(1, 'SICK', 10.0, 1.0, 9.0, 2023, '2023-01-01 00:00:00', '2023-12-31 23:59:59'),
(1, 'PERSONAL', 5.0, 0.0, 5.0, 2023, '2023-01-01 00:00:00', '2023-12-31 23:59:59'),
(1, 'EMERGENCY', 2.0, 0.0, 2.0, 2023, '2023-01-01 00:00:00', '2023-12-31 23:59:59'),

-- Engineering Head (moderate usage)
(3, 'VACATION', 20.0, 12.0, 8.0, 2023, '2023-01-01 00:00:00', '2023-12-31 23:59:59'),
(3, 'SICK', 10.0, 3.0, 7.0, 2023, '2023-01-01 00:00:00', '2023-12-31 23:59:59'),
(3, 'PERSONAL', 5.0, 2.0, 3.0, 2023, '2023-01-01 00:00:00', '2023-12-31 23:59:59'),
(3, 'EMERGENCY', 2.0, 1.0, 1.0, 2023, '2023-01-01 00:00:00', '2023-12-31 23:59:59'),

-- Senior Engineer (high usage)
(10, 'VACATION', 20.0, 18.0, 2.0, 2023, '2023-01-01 00:00:00', '2023-12-31 23:59:59'),
(10, 'SICK', 10.0, 4.0, 6.0, 2023, '2023-01-01 00:00:00', '2023-12-31 23:59:59'),
(10, 'PERSONAL', 5.0, 3.0, 2.0, 2023, '2023-01-01 00:00:00', '2023-12-31 23:59:59'),
(10, 'EMERGENCY', 2.0, 2.0, 0.0, 2023, '2023-01-01 00:00:00', '2023-12-31 23:59:59');

-- Insert sample leave requests with various scenarios
INSERT INTO leave_requests (request_id, employee_id, leave_type, start_date, end_date, duration, total_days, reason, comments, status, is_emergency_leave, is_backdated, approved_by, approved_at, rejection_reason, created_at, updated_at) VALUES

-- Approved vacation requests
('LR-12345678', 10, 'VACATION', '2024-03-15', '2024-03-19', 'FULL_DAY', 5.0, 'Family vacation to Hawaii', 'Pre-planned family trip', 'APPROVED', FALSE, FALSE, 6, '2024-02-15 10:30:00', NULL, '2024-02-10 14:20:00', '2024-02-15 10:30:00'),

('LR-23456789', 18, 'VACATION', '2024-04-22', '2024-04-26', 'FULL_DAY', 5.0, 'Wedding anniversary celebration', 'Celebrating 5th anniversary', 'APPROVED', FALSE, FALSE, 10, '2024-03-20 09:15:00', NULL, '2024-03-15 11:45:00', '2024-03-20 09:15:00'),

('LR-34567890', 14, 'VACATION', '2024-05-10', '2024-05-10', 'HALF_DAY', 0.5, 'Doctor appointment', 'Annual health checkup', 'APPROVED', FALSE, FALSE, 8, '2024-05-08 16:20:00', NULL, '2024-05-07 13:30:00', '2024-05-08 16:20:00'),

-- Pending requests
('LR-45678901', 19, 'VACATION', '2024-06-15', '2024-06-21', 'FULL_DAY', 5.0, 'Summer vacation with family', 'Beach vacation in California', 'PENDING', FALSE, FALSE, NULL, NULL, NULL, '2024-05-20 10:00:00', '2024-05-20 10:00:00'),

('LR-56789012', 21, 'PERSONAL', '2024-06-10', '2024-06-11', 'FULL_DAY', 2.0, 'Moving to new apartment', 'Need time to relocate', 'PENDING', FALSE, FALSE, NULL, NULL, NULL, '2024-05-25 14:15:00', '2024-05-25 14:15:00'),

-- Emergency leave (auto-approved)
('LR-67890123', 12, 'EMERGENCY', '2024-05-15', '2024-05-16', 'FULL_DAY', 2.0, 'Family medical emergency', 'Father hospitalized', 'AUTO_APPROVED', TRUE, FALSE, 7, '2024-05-15 08:30:00', NULL, '2024-05-15 08:30:00', '2024-05-15 08:30:00'),

('LR-78901234', 20, 'EMERGENCY', '2024-05-20', '2024-05-20', 'FULL_DAY', 1.0, 'Child sick', 'Daughter has high fever', 'AUTO_APPROVED', TRUE, FALSE, 11, '2024-05-20 07:45:00', NULL, '2024-05-20 07:45:00', '2024-05-20 07:45:00'),

-- Sick leave requests
('LR-89012345', 15, 'SICK', '2024-05-01', '2024-05-03', 'FULL_DAY', 3.0, 'Flu symptoms', 'Doctor advised rest', 'APPROVED', FALSE, FALSE, 4, '2024-05-01 09:00:00', NULL, '2024-05-01 08:15:00', '2024-05-01 09:00:00'),

('LR-90123456', 22, 'SICK', '2024-05-22', '2024-05-22', 'HALF_DAY', 0.5, 'Medical appointment', 'Specialist consultation', 'APPROVED', FALSE, FALSE, 16, '2024-05-21 15:30:00', NULL, '2024-05-21 12:00:00', '2024-05-21 15:30:00'),

-- Rejected requests
('LR-01234567', 11, 'VACATION', '2024-07-01', '2024-07-05', 'FULL_DAY', 5.0, 'Summer vacation', 'Want to visit Europe', 'REJECTED', FALSE, FALSE, 6, '2024-05-28 11:00:00', 'Peak project delivery period, cannot approve extended leave', '2024-05-25 16:30:00', '2024-05-28 11:00:00'),

-- Backdated requests (approved with justification)
('LR-11223344', 13, 'SICK', '2024-05-18', '2024-05-19', 'FULL_DAY', 2.0, 'Food poisoning', 'Was too sick to apply on time', 'APPROVED', FALSE, TRUE, 7, '2024-05-21 10:15:00', NULL, '2024-05-21 09:30:00', '2024-05-21 10:15:00'),

-- Maternity leave (long duration)
('LR-22334455', 16, 'MATERNITY', '2024-08-01', '2024-11-01', 'FULL_DAY', 66.0, 'Maternity leave', 'Expected delivery date: August 15', 'APPROVED', FALSE, FALSE, 5, '2024-06-01 14:00:00', NULL, '2024-05-15 10:00:00', '2024-06-01 14:00:00'),

-- Compensatory off
('LR-33445566', 24, 'COMPENSATORY', '2024-06-03', '2024-06-03', 'FULL_DAY', 1.0, 'Comp off for weekend work', 'Worked on critical deployment last weekend', 'APPROVED', FALSE, FALSE, 12, '2024-05-30 16:45:00', NULL, '2024-05-29 11:20:00', '2024-05-30 16:45:00'),

-- Multiple day personal leave
('LR-44556677', 25, 'PERSONAL', '2024-07-15', '2024-07-17', 'FULL_DAY', 3.0, 'Personal matters', 'Need to handle legal documentation', 'PENDING', FALSE, FALSE, NULL, NULL, NULL, '2024-06-01 09:00:00', '2024-06-01 09:00:00'),

-- Half-day requests
('LR-55667788', 26, 'PERSONAL', '2024-06-05', '2024-06-05', 'HALF_DAY', 0.5, 'Bank work', 'Loan documentation', 'APPROVED', FALSE, FALSE, 15, '2024-06-04 13:20:00', NULL, '2024-06-03 15:45:00', '2024-06-04 13:20:00');

-- Update leave balances to reflect approved leave usage
-- This would normally be done by the application, but for sample data we'll do it manually

-- Update Maria Garcia's balance (approved 5 days vacation)
UPDATE leave_balances SET used_days = 5.0, available_days = 15.0 WHERE employee_id = 10 AND leave_type = 'VACATION' AND year = 2024;

-- Update Sophie Miller's balance (approved 5 days vacation)
UPDATE leave_balances SET used_days = 5.0, available_days = 15.0 WHERE employee_id = 18 AND leave_type = 'VACATION' AND year = 2024;

-- Update Rachel Green's balance (approved 0.5 days vacation)
UPDATE leave_balances SET used_days = 0.5, available_days = 19.5 WHERE employee_id = 14 AND leave_type = 'VACATION' AND year = 2024;

-- Update Amanda Brown's balance (approved 2 days emergency)
UPDATE leave_balances SET used_days = 2.0, available_days = 0.0 WHERE employee_id = 12 AND leave_type = 'EMERGENCY' AND year = 2024;

-- Update Olivia Wilson's balance (approved 1 day emergency)
UPDATE leave_balances SET used_days = 1.0, available_days = 1.0 WHERE employee_id = 20 AND leave_type = 'EMERGENCY' AND year = 2024;

-- Update Thomas Anderson's balance (approved 3 days sick)
UPDATE leave_balances SET used_days = 3.0, available_days = 7.0 WHERE employee_id = 15 AND leave_type = 'SICK' AND year = 2024;

-- Update Isabella Martinez's balance (approved 0.5 days sick)
UPDATE leave_balances SET used_days = 0.5, available_days = 9.5 WHERE employee_id = 22 AND leave_type = 'SICK' AND year = 2024;

-- Update Daniel Kim's balance (approved 2 days sick - backdated)
UPDATE leave_balances SET used_days = 2.0, available_days = 8.0 WHERE employee_id = 13 AND leave_type = 'SICK' AND year = 2024;

-- Update Jessica Taylor's balance (approved 66 days maternity)
UPDATE leave_balances SET used_days = 66.0, available_days = 0.0 WHERE employee_id = 16 AND leave_type = 'MATERNITY' AND year = 2024;
-- Add maternity leave balance if not exists
INSERT IGNORE INTO leave_balances (employee_id, leave_type, total_days, used_days, available_days, year) 
VALUES (16, 'MATERNITY', 90.0, 66.0, 24.0, 2024);

-- Update Grace Liu's balance (approved 1 day compensatory)
UPDATE leave_balances SET used_days = 1.0, available_days = 1.0 WHERE employee_id = 24 AND leave_type = 'COMPENSATORY' AND year = 2024;
-- Add compensatory leave balance if not exists
INSERT IGNORE INTO leave_balances (employee_id, leave_type, total_days, used_days, available_days, year) 
VALUES (24, 'COMPENSATORY', 5.0, 1.0, 4.0, 2024);

-- Update Zoe Thompson's balance (approved 0.5 days personal)
UPDATE leave_balances SET used_days = 0.5, available_days = 4.5 WHERE employee_id = 26 AND leave_type = 'PERSONAL' AND year = 2024;

-- Re-enable foreign key checks
SET FOREIGN_KEY_CHECKS = 1;

-- Insert some additional test scenarios for edge cases

-- Employee with no manager (for testing delegation)
INSERT INTO employees (employee_id, name, email, department, joining_date, manager_id, created_at, updated_at) VALUES
('TEST001', 'John Orphan', 'john.orphan@company.com', 'Testing', '2024-01-01', NULL, '2024-01-01 09:00:00', '2024-01-01 09:00:00');

-- Initialize leave balance for test employee
CALL sp_initialize_employee_leave_balance(27, 2024);

-- Leave request from employee with no manager
INSERT INTO leave_requests (request_id, employee_id, leave_type, start_date, end_date, duration, total_days, reason, comments, status, is_emergency_leave, is_backdated, approved_by, approved_at, rejection_reason, created_at, updated_at) VALUES
('LR-99887766', 27, 'VACATION', '2024-07-01', '2024-07-03', 'FULL_DAY', 3.0, 'Test vacation', 'Testing approval delegation', 'PENDING', FALSE, FALSE, NULL, NULL, NULL, '2024-06-01 10:00:00', '2024-06-01 10:00:00');

-- Add some weekend dates in leave requests to test working day calculation
INSERT INTO leave_requests (request_id, employee_id, leave_type, start_date, end_date, duration, total_days, reason, comments, status, is_emergency_leave, is_backdated, approved_by, approved_at, rejection_reason, created_at, updated_at) VALUES
('LR-88776655', 19, 'VACATION', '2024-06-28', '2024-07-01', 'FULL_DAY', 2.0, 'Long weekend vacation', 'Friday and Monday off (weekend excluded)', 'PENDING', FALSE, FALSE, NULL, NULL, NULL, '2024-06-15 14:00:00', '2024-06-15 14:00:00');

-- Add some overlapping leave requests for testing conflict detection
INSERT INTO leave_requests (request_id, employee_id, leave_type, start_date, end_date, duration, total_days, reason, comments, status, is_emergency_leave, is_backdated, approved_by, approved_at, rejection_reason, created_at, updated_at) VALUES
('LR-77665544', 18, 'SICK', '2024-04-24', '2024-04-25', 'FULL_DAY', 2.0, 'Flu symptoms', 'Overlaps with approved vacation', 'REJECTED', FALSE, FALSE, 10, '2024-04-23 16:00:00', 'Conflicts with already approved vacation leave', '2024-04-23 14:30:00', '2024-04-23 16:00:00');

-- Create some data for reporting and analytics
-- Add leave requests across different months for trend analysis
INSERT INTO leave_requests (request_id, employee_id, leave_type, start_date, end_date, duration, total_days, reason, comments, status, is_emergency_leave, is_backdated, approved_by, approved_at, rejection_reason, created_at, updated_at) VALUES
-- January requests
('LR-JAN00001', 11, 'VACATION', '2024-01-15', '2024-01-17', 'FULL_DAY', 3.0, 'New Year extended break', 'Post-holiday vacation', 'APPROVED', FALSE, FALSE, 6, '2024-01-10 11:00:00', NULL, '2024-01-08 09:00:00', '2024-01-10 11:00:00'),

-- February requests
('LR-FEB00001', 12, 'SICK', '2024-02-14', '2024-02-15', 'FULL_DAY', 2.0, 'Seasonal flu', 'Doctor recommended rest', 'APPROVED', FALSE, FALSE, 7, '2024-02-14 10:30:00', NULL, '2024-02-14 08:00:00', '2024-02-14 10:30:00'),

-- March requests
('LR-MAR00001', 13, 'PERSONAL', '2024-03-08', '2024-03-08', 'FULL_DAY', 1.0, 'International Women\'s Day', 'Company holiday', 'APPROVED', FALSE, FALSE, 7, '2024-03-07 15:00:00', NULL, '2024-03-05 12:00:00', '2024-03-07 15:00:00');

-- Update balances for these additional approved leaves
UPDATE leave_balances SET used_days = used_days + 3.0, available_days = available_days - 3.0 WHERE employee_id = 11 AND leave_type = 'VACATION' AND year = 2024;
UPDATE leave_balances SET used_days = used_days + 2.0, available_days = available_days - 2.0 WHERE employee_id = 12 AND leave_type = 'SICK' AND year = 2024;
UPDATE leave_balances SET used_days = used_days + 1.0, available_days = available_days - 1.0 WHERE employee_id = 13 AND leave_type = 'PERSONAL' AND year = 2024;

-- Add some comments to document the sample data
-- This helps developers understand the test scenarios

/*
Sample Data Summary:
===================

Employees: 27 total
- 1 CEO (no manager)
- 4 Department heads (report to CEO)
- 4 Team leads (report to department heads)
- 8 Senior employees (report to team leads)
- 6 Junior employees (report to senior employees)
- 3 Recent hires (2024)
- 1 Test employee (no manager for testing)

Leave Requests: 20+ scenarios including:
- Approved vacation, sick, personal, emergency leaves
- Pending requests awaiting approval
- Rejected requests with reasons
- Auto-approved emergency leaves
- Backdated requests
- Maternity leave (long duration)
- Compensatory off
- Half-day requests
- Overlapping requests (rejected)
- Weekend spanning requests
- Requests from employees with no manager

Leave Balances:
- All employees have 2024 balances initialized
- Some employees have 2023 historical data
- Balances reflect approved leave usage
- Various utilization levels (low, medium, high)
- Special leave types (maternity, compensatory) where applicable

Test Scenarios Covered:
- Manager hierarchy and approval delegation
- Emergency leave auto-approval
- Backdated request handling
- Working day calculations (excluding weekends)
- Leave balance validation
- Overlapping leave detection
- Various leave types and durations
- Approval workflow states
- Edge cases (no manager, weekend dates)
*/