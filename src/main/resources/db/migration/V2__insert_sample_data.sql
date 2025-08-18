-- Mini Leave Management System Sample Data (H2 Compatible)
-- Version: 2.0
-- Description: Insert sample data for testing and development

-- Insert sample employees with manager relationships
-- Note: Inserting in order to handle manager relationships properly

-- CEO (no manager)
INSERT INTO employees (employee_id, name, email, department, position, joining_date, manager_id) VALUES
('CEO001', 'Sarah Johnson', 'sarah.johnson@company.com', 'Executive', 'Chief Executive Officer', '2020-01-15', NULL);

-- Department Heads (report to CEO)
INSERT INTO employees (employee_id, name, email, department, position, joining_date, manager_id) VALUES
('HR001', 'Michael Chen', 'michael.chen@company.com', 'Human Resources', 'HR Director', '2020-02-01', 1),
('ENG001', 'David Rodriguez', 'david.rodriguez@company.com', 'Engineering', 'Engineering Director', '2020-02-15', 1),
('MKT001', 'Lisa Wang', 'lisa.wang@company.com', 'Marketing', 'Marketing Director', '2020-03-01', 1),
('FIN001', 'Robert Smith', 'robert.smith@company.com', 'Finance', 'Finance Director', '2020-03-15', 1);

-- Team Leads (report to department heads)
INSERT INTO employees (employee_id, name, email, department, position, joining_date, manager_id) VALUES
('ENG002', 'Jennifer Lee', 'jennifer.lee@company.com', 'Engineering', 'Senior Engineering Manager', '2021-01-10', 3),
('ENG003', 'Alex Thompson', 'alex.thompson@company.com', 'Engineering', 'Engineering Manager', '2021-02-01', 3),
('MKT002', 'Emily Davis', 'emily.davis@company.com', 'Marketing', 'Marketing Manager', '2021-03-01', 4),
('HR002', 'James Wilson', 'james.wilson@company.com', 'Human Resources', 'HR Manager', '2021-04-01', 2);

-- Senior Engineers and Specialists
INSERT INTO employees (employee_id, name, email, department, position, joining_date, manager_id) VALUES
('ENG004', 'Maria Garcia', 'maria.garcia@company.com', 'Engineering', 'Senior Software Engineer', '2022-01-15', 6),
('ENG005', 'Kevin Park', 'kevin.park@company.com', 'Engineering', 'Senior Software Engineer', '2022-02-01', 6),
('ENG006', 'Amanda Brown', 'amanda.brown@company.com', 'Engineering', 'Senior Software Engineer', '2022-03-01', 7),
('ENG007', 'Daniel Kim', 'daniel.kim@company.com', 'Engineering', 'Senior Software Engineer', '2022-04-01', 7),
('MKT003', 'Rachel Green', 'rachel.green@company.com', 'Marketing', 'Marketing Specialist', '2022-05-01', 8),
('MKT004', 'Thomas Anderson', 'thomas.anderson@company.com', 'Marketing', 'Digital Marketing Specialist', '2022-06-01', 8),
('FIN002', 'Jessica Taylor', 'jessica.taylor@company.com', 'Finance', 'Financial Analyst', '2022-07-01', 5),
('HR003', 'Christopher Moore', 'christopher.moore@company.com', 'Human Resources', 'HR Specialist', '2022-08-01', 9);

-- Junior employees
INSERT INTO employees (employee_id, name, email, department, position, joining_date, manager_id) VALUES
('ENG008', 'Sophie Miller', 'sophie.miller@company.com', 'Engineering', 'Software Engineer', '2023-01-15', 10),
('ENG009', 'Ryan Johnson', 'ryan.johnson@company.com', 'Engineering', 'Software Engineer', '2023-02-01', 10),
('ENG010', 'Olivia Wilson', 'olivia.wilson@company.com', 'Engineering', 'Software Engineer', '2023-03-01', 11),
('MKT005', 'Nathan Clark', 'nathan.clark@company.com', 'Marketing', 'Marketing Coordinator', '2023-04-01', 14),
('FIN003', 'Isabella Martinez', 'isabella.martinez@company.com', 'Finance', 'Junior Financial Analyst', '2023-05-01', 16),
('HR004', 'Ethan Davis', 'ethan.davis@company.com', 'Human Resources', 'HR Coordinator', '2023-06-01', 17);

-- Recent hires (2024)
INSERT INTO employees (employee_id, name, email, department, position, joining_date, manager_id) VALUES
('ENG011', 'Grace Liu', 'grace.liu@company.com', 'Engineering', 'Junior Software Engineer', '2024-01-15', 12),
('ENG012', 'Lucas Brown', 'lucas.brown@company.com', 'Engineering', 'Junior Software Engineer', '2024-02-01', 13),
('MKT006', 'Zoe Adams', 'zoe.adams@company.com', 'Marketing', 'Marketing Assistant', '2024-03-01', 15);

-- Initialize leave balances for all employees for 2024
INSERT INTO leave_balances (employee_id, leave_type, total_days, used_days, available_days, year) VALUES
-- CEO
(1, 'VACATION', 25.0, 0.0, 25.0, 2024),
(1, 'SICK', 15.0, 0.0, 15.0, 2024),
(1, 'PERSONAL', 5.0, 0.0, 5.0, 2024),
(1, 'EMERGENCY', 3.0, 0.0, 3.0, 2024),

-- HR Director
(2, 'VACATION', 22.0, 2.0, 20.0, 2024),
(2, 'SICK', 12.0, 1.0, 11.0, 2024),
(2, 'PERSONAL', 5.0, 0.0, 5.0, 2024),
(2, 'EMERGENCY', 2.0, 0.0, 2.0, 2024),

-- Engineering Director
(3, 'VACATION', 22.0, 3.0, 19.0, 2024),
(3, 'SICK', 12.0, 0.0, 12.0, 2024),
(3, 'PERSONAL', 5.0, 1.0, 4.0, 2024),
(3, 'EMERGENCY', 2.0, 0.0, 2.0, 2024),

-- Marketing Director
(4, 'VACATION', 22.0, 5.0, 17.0, 2024),
(4, 'SICK', 12.0, 2.0, 10.0, 2024),
(4, 'PERSONAL', 5.0, 0.0, 5.0, 2024),
(4, 'EMERGENCY', 2.0, 0.0, 2.0, 2024),

-- Finance Director
(5, 'VACATION', 22.0, 1.0, 21.0, 2024),
(5, 'SICK', 12.0, 0.0, 12.0, 2024),
(5, 'PERSONAL', 5.0, 0.0, 5.0, 2024),
(5, 'EMERGENCY', 2.0, 0.0, 2.0, 2024);

-- Add balances for managers (employees 6-9)
INSERT INTO leave_balances (employee_id, leave_type, total_days, used_days, available_days, year) VALUES
(6, 'VACATION', 20.0, 4.0, 16.0, 2024), (6, 'SICK', 10.0, 1.0, 9.0, 2024), (6, 'PERSONAL', 5.0, 0.0, 5.0, 2024), (6, 'EMERGENCY', 2.0, 0.0, 2.0, 2024),
(7, 'VACATION', 20.0, 2.0, 18.0, 2024), (7, 'SICK', 10.0, 0.0, 10.0, 2024), (7, 'PERSONAL', 5.0, 1.0, 4.0, 2024), (7, 'EMERGENCY', 2.0, 0.0, 2.0, 2024),
(8, 'VACATION', 20.0, 6.0, 14.0, 2024), (8, 'SICK', 10.0, 2.0, 8.0, 2024), (8, 'PERSONAL', 5.0, 0.0, 5.0, 2024), (8, 'EMERGENCY', 2.0, 0.0, 2.0, 2024),
(9, 'VACATION', 20.0, 3.0, 17.0, 2024), (9, 'SICK', 10.0, 1.0, 9.0, 2024), (9, 'PERSONAL', 5.0, 0.0, 5.0, 2024), (9, 'EMERGENCY', 2.0, 0.0, 2.0, 2024);

-- Add balances for senior employees (employees 10-17)
INSERT INTO leave_balances (employee_id, leave_type, total_days, used_days, available_days, year) VALUES
(10, 'VACATION', 18.0, 5.0, 13.0, 2024), (10, 'SICK', 10.0, 2.0, 8.0, 2024), (10, 'PERSONAL', 5.0, 1.0, 4.0, 2024), (10, 'EMERGENCY', 2.0, 0.0, 2.0, 2024),
(11, 'VACATION', 18.0, 3.0, 15.0, 2024), (11, 'SICK', 10.0, 0.0, 10.0, 2024), (11, 'PERSONAL', 5.0, 0.0, 5.0, 2024), (11, 'EMERGENCY', 2.0, 0.0, 2.0, 2024),
(12, 'VACATION', 18.0, 7.0, 11.0, 2024), (12, 'SICK', 10.0, 1.0, 9.0, 2024), (12, 'PERSONAL', 5.0, 0.0, 5.0, 2024), (12, 'EMERGENCY', 2.0, 1.0, 1.0, 2024),
(13, 'VACATION', 18.0, 4.0, 14.0, 2024), (13, 'SICK', 10.0, 3.0, 7.0, 2024), (13, 'PERSONAL', 5.0, 0.0, 5.0, 2024), (13, 'EMERGENCY', 2.0, 0.0, 2.0, 2024),
(14, 'VACATION', 18.0, 2.0, 16.0, 2024), (14, 'SICK', 10.0, 0.0, 10.0, 2024), (14, 'PERSONAL', 5.0, 1.0, 4.0, 2024), (14, 'EMERGENCY', 2.0, 0.0, 2.0, 2024),
(15, 'VACATION', 18.0, 6.0, 12.0, 2024), (15, 'SICK', 10.0, 1.0, 9.0, 2024), (15, 'PERSONAL', 5.0, 0.0, 5.0, 2024), (15, 'EMERGENCY', 2.0, 0.0, 2.0, 2024),
(16, 'VACATION', 18.0, 1.0, 17.0, 2024), (16, 'SICK', 10.0, 0.0, 10.0, 2024), (16, 'PERSONAL', 5.0, 0.0, 5.0, 2024), (16, 'EMERGENCY', 2.0, 0.0, 2.0, 2024),
(17, 'VACATION', 18.0, 3.0, 15.0, 2024), (17, 'SICK', 10.0, 2.0, 8.0, 2024), (17, 'PERSONAL', 5.0, 0.0, 5.0, 2024), (17, 'EMERGENCY', 2.0, 0.0, 2.0, 2024);

-- Add balances for junior employees (employees 18-23)
INSERT INTO leave_balances (employee_id, leave_type, total_days, used_days, available_days, year) VALUES
(18, 'VACATION', 15.0, 2.0, 13.0, 2024), (18, 'SICK', 10.0, 1.0, 9.0, 2024), (18, 'PERSONAL', 3.0, 0.0, 3.0, 2024), (18, 'EMERGENCY', 2.0, 0.0, 2.0, 2024),
(19, 'VACATION', 15.0, 4.0, 11.0, 2024), (19, 'SICK', 10.0, 0.0, 10.0, 2024), (19, 'PERSONAL', 3.0, 1.0, 2.0, 2024), (19, 'EMERGENCY', 2.0, 0.0, 2.0, 2024),
(20, 'VACATION', 15.0, 1.0, 14.0, 2024), (20, 'SICK', 10.0, 2.0, 8.0, 2024), (20, 'PERSONAL', 3.0, 0.0, 3.0, 2024), (20, 'EMERGENCY', 2.0, 0.0, 2.0, 2024),
(21, 'VACATION', 15.0, 3.0, 12.0, 2024), (21, 'SICK', 10.0, 0.0, 10.0, 2024), (21, 'PERSONAL', 3.0, 0.0, 3.0, 2024), (21, 'EMERGENCY', 2.0, 0.0, 2.0, 2024),
(22, 'VACATION', 15.0, 0.0, 15.0, 2024), (22, 'SICK', 10.0, 1.0, 9.0, 2024), (22, 'PERSONAL', 3.0, 0.0, 3.0, 2024), (22, 'EMERGENCY', 2.0, 0.0, 2.0, 2024),
(23, 'VACATION', 15.0, 2.0, 13.0, 2024), (23, 'SICK', 10.0, 0.0, 10.0, 2024), (23, 'PERSONAL', 3.0, 0.0, 3.0, 2024), (23, 'EMERGENCY', 2.0, 0.0, 2.0, 2024);

-- Add balances for new hires (employees 24-26)
INSERT INTO leave_balances (employee_id, leave_type, total_days, used_days, available_days, year) VALUES
(24, 'VACATION', 12.0, 0.0, 12.0, 2024), (24, 'SICK', 8.0, 0.0, 8.0, 2024), (24, 'PERSONAL', 2.0, 0.0, 2.0, 2024), (24, 'EMERGENCY', 2.0, 0.0, 2.0, 2024),
(25, 'VACATION', 12.0, 1.0, 11.0, 2024), (25, 'SICK', 8.0, 0.0, 8.0, 2024), (25, 'PERSONAL', 2.0, 0.0, 2.0, 2024), (25, 'EMERGENCY', 2.0, 0.0, 2.0, 2024),
(26, 'VACATION', 10.0, 0.0, 10.0, 2024), (26, 'SICK', 8.0, 0.0, 8.0, 2024), (26, 'PERSONAL', 2.0, 0.0, 2.0, 2024), (26, 'EMERGENCY', 2.0, 0.0, 2.0, 2024);

-- Insert sample leave requests
INSERT INTO leave_requests (request_id, employee_id, leave_type, start_date, end_date, duration, total_days, reason, status, application_date) VALUES
('LR-20240001', 10, 'VACATION', '2024-03-15', '2024-03-19', 'FULL_DAY', 5.0, 'Family vacation to Hawaii', 'APPROVED', '2024-02-15 10:30:00'),
('LR-20240002', 12, 'SICK', '2024-02-20', '2024-02-21', 'FULL_DAY', 2.0, 'Flu symptoms', 'APPROVED', '2024-02-20 08:15:00'),
('LR-20240003', 15, 'PERSONAL', '2024-04-10', '2024-04-10', 'HALF_DAY', 0.5, 'Doctor appointment', 'APPROVED', '2024-04-05 14:20:00'),
('LR-20240004', 18, 'VACATION', '2024-05-20', '2024-05-22', 'FULL_DAY', 3.0, 'Long weekend trip', 'PENDING', '2024-05-10 09:45:00'),
('LR-20240005', 8, 'EMERGENCY', '2024-01-25', '2024-01-25', 'FULL_DAY', 1.0, 'Family emergency', 'AUTO_APPROVED', '2024-01-25 07:30:00'),
('LR-20240006', 19, 'VACATION', '2024-06-15', '2024-06-20', 'FULL_DAY', 4.0, 'Summer vacation', 'PENDING', '2024-06-01 11:00:00'),
('LR-20240007', 13, 'SICK', '2024-03-05', '2024-03-07', 'FULL_DAY', 3.0, 'Food poisoning', 'APPROVED', '2024-03-05 06:45:00'),
('LR-20240008', 21, 'PERSONAL', '2024-07-12', '2024-07-12', 'FULL_DAY', 1.0, 'Moving day', 'PENDING', '2024-07-01 16:30:00');