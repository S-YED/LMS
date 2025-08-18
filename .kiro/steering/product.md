# Product Overview

## Mini Leave Management System

A comprehensive leave management system designed for small to medium-sized organizations to efficiently manage employee leave requests, approvals, and balance tracking.

### Core Features
- **Employee Management**: Hierarchical manager relationships with department organization
- **Leave Application**: Multiple leave types (vacation, sick, personal, emergency, maternity, etc.) with full/half-day support
- **Approval Workflows**: Automated approval with delegation support and emergency leave auto-approval
- **Balance Tracking**: Real-time leave balance management with working day calculations
- **Audit & Reporting**: Comprehensive tracking with email notifications and audit trails
- **API-First**: REST API with Swagger documentation for integration

### Business Rules
- Employees can apply for various leave types with proper balance validation
- Manager approval required for most leave types (except emergency auto-approval)
- Backdated leave requests supported with proper flagging
- Overlapping leave requests prevented through validation
- Working days calculated excluding weekends
- Leave balances tracked annually with fractional day support (0.5 for half-day)

### Target Users
- **Employees**: Apply for leave, view balances, track request status
- **Managers**: Approve/reject subordinate requests, view team leave calendar
- **HR**: Manage employee records, initialize balances, generate reports
- **System Admins**: Configure leave policies, manage system settings