package com.company.leavemanagementsystem.exception;

import java.time.LocalDate;

/**
 * Exception thrown when invalid date ranges are provided for leave requests.
 * This includes cases where end date is before start date, or other date validation failures.
 */
public class InvalidDateRangeException extends RuntimeException {
    
    private final LocalDate startDate;
    private final LocalDate endDate;
    
    public InvalidDateRangeException(String message) {
        super(message);
        this.startDate = null;
        this.endDate = null;
    }
    
    public InvalidDateRangeException(LocalDate startDate, LocalDate endDate) {
        super(String.format("Invalid date range: start date %s must be before or equal to end date %s", 
                          startDate, endDate));
        this.startDate = startDate;
        this.endDate = endDate;
    }
    
    public InvalidDateRangeException(String message, Throwable cause) {
        super(message, cause);
        this.startDate = null;
        this.endDate = null;
    }
    
    /**
     * Creates an exception for end date before start date
     */
    public static InvalidDateRangeException endDateBeforeStartDate(LocalDate startDate, LocalDate endDate) {
        return new InvalidDateRangeException(startDate, endDate);
    }
    
    /**
     * Creates an exception for invalid date format or parsing
     */
    public static InvalidDateRangeException invalidDateFormat(String dateString) {
        return new InvalidDateRangeException("Invalid date format: " + dateString);
    }
    
    /**
     * Creates an exception for dates too far in the future
     */
    public static InvalidDateRangeException dateTooFarInFuture(LocalDate date, int maxDaysInFuture) {
        return new InvalidDateRangeException(
            String.format("Date %s is too far in the future. Maximum allowed: %d days from today", 
                         date, maxDaysInFuture));
    }
    
    public LocalDate getStartDate() {
        return startDate;
    }
    
    public LocalDate getEndDate() {
        return endDate;
    }
}