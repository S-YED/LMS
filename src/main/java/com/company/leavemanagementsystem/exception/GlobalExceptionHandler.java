package com.company.leavemanagementsystem.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Global exception handler for the Leave Management System.
 * Provides centralized exception handling with proper HTTP status codes and
 * error messages.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handle employee not found exceptions
     */
    @ExceptionHandler(EmployeeNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleEmployeeNotFoundException(
            EmployeeNotFoundException ex, WebRequest request) {
        logger.warn("Employee not found: {}", ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.NOT_FOUND.value())
                .error("Employee Not Found")
                .message(ex.getMessage())
                .path(request.getDescription(false).replace("uri=", ""))
                .build();

        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }

    /**
     * Handle insufficient leave balance exceptions
     */
    @ExceptionHandler(InsufficientLeaveBalanceException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientLeaveBalanceException(
            InsufficientLeaveBalanceException ex, WebRequest request) {
        logger.warn("Insufficient leave balance: {}", ex.getMessage());

        Map<String, Object> details = new HashMap<>();
        if (ex.getEmployeeId() != null) {
            details.put("employeeId", ex.getEmployeeId());
            details.put("leaveType", ex.getLeaveType());
            details.put("requestedDays", ex.getRequestedDays());
            details.put("availableDays", ex.getAvailableDays());
        }

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Insufficient Leave Balance")
                .message(ex.getMessage())
                .path(request.getDescription(false).replace("uri=", ""))
                .details(details)
                .build();

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handle leave request conflict exceptions
     */
    @ExceptionHandler(LeaveRequestConflictException.class)
    public ResponseEntity<ErrorResponse> handleLeaveRequestConflictException(
            LeaveRequestConflictException ex, WebRequest request) {
        logger.warn("Leave request conflict: {}", ex.getMessage());

        Map<String, Object> details = new HashMap<>();
        if (ex.getEmployeeId() != null) {
            details.put("employeeId", ex.getEmployeeId());
            details.put("startDate", ex.getStartDate());
            details.put("endDate", ex.getEndDate());
            details.put("conflictingRequestId", ex.getConflictingRequestId());
        }

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.CONFLICT.value())
                .error("Leave Request Conflict")
                .message(ex.getMessage())
                .path(request.getDescription(false).replace("uri=", ""))
                .details(details)
                .build();

        return new ResponseEntity<>(errorResponse, HttpStatus.CONFLICT);
    }

    /**
     * Handle invalid date range exceptions
     */
    @ExceptionHandler(InvalidDateRangeException.class)
    public ResponseEntity<ErrorResponse> handleInvalidDateRangeException(
            InvalidDateRangeException ex, WebRequest request) {
        logger.warn("Invalid date range: {}", ex.getMessage());

        Map<String, Object> details = new HashMap<>();
        if (ex.getStartDate() != null && ex.getEndDate() != null) {
            details.put("startDate", ex.getStartDate());
            details.put("endDate", ex.getEndDate());
        }

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Invalid Date Range")
                .message(ex.getMessage())
                .path(request.getDescription(false).replace("uri=", ""))
                .details(details)
                .build();

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handle leave before joining exceptions
     */
    @ExceptionHandler(LeaveBeforeJoiningException.class)
    public ResponseEntity<ErrorResponse> handleLeaveBeforeJoiningException(
            LeaveBeforeJoiningException ex, WebRequest request) {
        logger.warn("Leave before joining date: {}", ex.getMessage());

        Map<String, Object> details = new HashMap<>();
        if (ex.getEmployeeId() != null) {
            details.put("employeeId", ex.getEmployeeId());
            details.put("joiningDate", ex.getJoiningDate());
            details.put("leaveStartDate", ex.getLeaveStartDate());
        }

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Leave Before Joining Date")
                .message(ex.getMessage())
                .path(request.getDescription(false).replace("uri=", ""))
                .details(details)
                .build();

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handle leave request not found exceptions
     */
    @ExceptionHandler(LeaveRequestNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleLeaveRequestNotFoundException(
            LeaveRequestNotFoundException ex, WebRequest request) {
        logger.warn("Leave request not found: {}", ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.NOT_FOUND.value())
                .error("Leave Request Not Found")
                .message(ex.getMessage())
                .path(request.getDescription(false).replace("uri=", ""))
                .build();

        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }

    /**
     * Handle unauthorized approval exceptions
     */
    @ExceptionHandler(UnauthorizedApprovalException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorizedApprovalException(
            UnauthorizedApprovalException ex, WebRequest request) {
        logger.warn("Unauthorized approval attempt: {}", ex.getMessage());

        Map<String, Object> details = new HashMap<>();
        if (ex.getEmployeeId() != null) {
            details.put("employeeId", ex.getEmployeeId());
            details.put("requestId", ex.getRequestId());
            details.put("attemptedBy", ex.getAttemptedBy());
        }

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.FORBIDDEN.value())
                .error("Unauthorized Approval")
                .message(ex.getMessage())
                .path(request.getDescription(false).replace("uri=", ""))
                .details(details)
                .build();

        return new ResponseEntity<>(errorResponse, HttpStatus.FORBIDDEN);
    }

    /**
     * Handle leave balance not found exceptions
     */
    @ExceptionHandler(LeaveBalanceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleLeaveBalanceNotFoundException(
            LeaveBalanceNotFoundException ex, WebRequest request) {
        logger.warn("Leave balance not found: {}", ex.getMessage());

        Map<String, Object> details = new HashMap<>();
        if (ex.getEmployeeId() != null) {
            details.put("employeeId", ex.getEmployeeId());
            details.put("leaveType", ex.getLeaveType());
            details.put("year", ex.getYear());
        }

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.NOT_FOUND.value())
                .error("Leave Balance Not Found")
                .message(ex.getMessage())
                .path(request.getDescription(false).replace("uri=", ""))
                .details(details)
                .build();

        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }

    /**
     * Handle invalid leave status exceptions
     */
    @ExceptionHandler(InvalidLeaveStatusException.class)
    public ResponseEntity<ErrorResponse> handleInvalidLeaveStatusException(
            InvalidLeaveStatusException ex, WebRequest request) {
        logger.warn("Invalid leave status operation: {}", ex.getMessage());

        Map<String, Object> details = new HashMap<>();
        if (ex.getRequestId() != null) {
            details.put("requestId", ex.getRequestId());
            details.put("currentStatus", ex.getCurrentStatus());
            details.put("attemptedStatus", ex.getAttemptedStatus());
        }

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Invalid Leave Status")
                .message(ex.getMessage())
                .path(request.getDescription(false).replace("uri=", ""))
                .details(details)
                .build();

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handle duplicate employee exceptions
     */
    @ExceptionHandler(DuplicateEmployeeException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateEmployeeException(
            DuplicateEmployeeException ex, WebRequest request) {
        logger.warn("Duplicate employee: {}", ex.getMessage());

        Map<String, Object> details = new HashMap<>();
        if (ex.getDuplicateField() != null) {
            details.put("duplicateField", ex.getDuplicateField());
            details.put("employeeId", ex.getEmployeeId());
            details.put("email", ex.getEmail());
        }

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.CONFLICT.value())
                .error("Duplicate Employee")
                .message(ex.getMessage())
                .path(request.getDescription(false).replace("uri=", ""))
                .details(details)
                .build();

        return new ResponseEntity<>(errorResponse, HttpStatus.CONFLICT);
    }

    /**
     * Handle manager not found exceptions
     */
    @ExceptionHandler(ManagerNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleManagerNotFoundException(
            ManagerNotFoundException ex, WebRequest request) {
        logger.warn("Manager not found: {}", ex.getMessage());

        Map<String, Object> details = new HashMap<>();
        if (ex.getManagerId() != null) {
            details.put("managerId", ex.getManagerId());
            details.put("employeeId", ex.getEmployeeId());
        }

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.NOT_FOUND.value())
                .error("Manager Not Found")
                .message(ex.getMessage())
                .path(request.getDescription(false).replace("uri=", ""))
                .details(details)
                .build();

        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }

    /**
     * Handle validation errors from @Valid annotations
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(
            MethodArgumentNotValidException ex, WebRequest request) {
        logger.warn("Validation error: {}", ex.getMessage());

        Map<String, String> validationErrors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            validationErrors.put(fieldName, errorMessage);
        });

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Validation Failed")
                .message("Input validation failed")
                .path(request.getDescription(false).replace("uri=", ""))
                .validationErrors(validationErrors)
                .build();

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handle constraint violation exceptions
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolationException(
            ConstraintViolationException ex, WebRequest request) {
        logger.warn("Constraint violation: {}", ex.getMessage());

        Map<String, String> validationErrors = ex.getConstraintViolations()
                .stream()
                .collect(Collectors.toMap(
                        violation -> violation.getPropertyPath().toString(),
                        ConstraintViolation::getMessage));

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Constraint Violation")
                .message("Constraint validation failed")
                .path(request.getDescription(false).replace("uri=", ""))
                .validationErrors(validationErrors)
                .build();

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handle database constraint violations
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolationException(
            DataIntegrityViolationException ex, WebRequest request) {
        logger.error("Database constraint violation: {}", ex.getMessage());

        String message = "Database constraint violation";
        String rootCauseMessage = ex.getRootCause() != null ? ex.getRootCause().getMessage() : ex.getMessage();

        // Parse common constraint violations
        if (rootCauseMessage != null) {
            if (rootCauseMessage.contains("employee_id")) {
                message = "Employee ID already exists";
            } else if (rootCauseMessage.contains("email")) {
                message = "Email address already exists";
            } else if (rootCauseMessage.contains("foreign key constraint")) {
                message = "Referenced record does not exist";
            }
        }

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.CONFLICT.value())
                .error("Database Constraint Violation")
                .message(message)
                .path(request.getDescription(false).replace("uri=", ""))
                .build();

        return new ResponseEntity<>(errorResponse, HttpStatus.CONFLICT);
    }

    /**
     * Handle method argument type mismatch
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatchException(
            MethodArgumentTypeMismatchException ex, WebRequest request) {
        logger.warn("Method argument type mismatch: {}", ex.getMessage());

        String message = String.format("Invalid value '%s' for parameter '%s'. Expected type: %s",
                ex.getValue(), ex.getName(), ex.getRequiredType().getSimpleName());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Invalid Parameter Type")
                .message(message)
                .path(request.getDescription(false).replace("uri=", ""))
                .build();

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handle malformed JSON requests
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException ex, WebRequest request) {
        logger.warn("Malformed JSON request: {}", ex.getMessage());

        String message = "Malformed JSON request";
        if (ex.getCause() != null && ex.getCause().getMessage() != null) {
            String causeMessage = ex.getCause().getMessage();
            if (causeMessage.contains("Cannot deserialize")) {
                message = "Invalid JSON format or data type";
            } else if (causeMessage.contains("Unexpected character")) {
                message = "Invalid JSON syntax";
            }
        }

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Malformed Request")
                .message(message)
                .path(request.getDescription(false).replace("uri=", ""))
                .build();

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handle all other unexpected exceptions
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex, WebRequest request) {
        logger.error("Unexpected error occurred: ", ex);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error("Internal Server Error")
                .message("An unexpected error occurred. Please try again later.")
                .path(request.getDescription(false).replace("uri=", ""))
                .build();

        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}