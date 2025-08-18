package com.company.leavemanagementsystem.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Entity representing leave balance for an employee by leave type and year.
 * Supports fractional days and year-based balance tracking.
 */
@Entity
@Table(name = "leave_balances", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"employee_id", "leave_type", "year"}),
       indexes = {
           @Index(name = "idx_balance_employee_type", columnList = "employee_id, leave_type, year"),
           @Index(name = "idx_balance_year", columnList = "year")
       })
public class LeaveBalance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Enumerated(EnumType.STRING)
    @Column(name = "leave_type", nullable = false, length = 20)
    private LeaveType leaveType;

    @Column(name = "total_days", nullable = false)
    private Double totalDays;

    @Column(name = "used_days", nullable = false)
    private Double usedDays = 0.0;

    @Column(name = "available_days", nullable = false)
    private Double availableDays;

    @Column(nullable = false)
    private Integer year;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Default constructor
    public LeaveBalance() {}

    // Constructor with required fields
    public LeaveBalance(Employee employee, LeaveType leaveType, Double totalDays, Integer year) {
        this.employee = employee;
        this.leaveType = leaveType;
        this.totalDays = totalDays;
        this.availableDays = totalDays;
        this.year = year;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Employee getEmployee() {
        return employee;
    }

    public void setEmployee(Employee employee) {
        this.employee = employee;
    }

    public LeaveType getLeaveType() {
        return leaveType;
    }

    public void setLeaveType(LeaveType leaveType) {
        this.leaveType = leaveType;
    }

    public Double getTotalDays() {
        return totalDays;
    }

    public void setTotalDays(Double totalDays) {
        this.totalDays = totalDays;
        // Recalculate available days when total changes
        this.availableDays = totalDays - usedDays;
    }

    public Double getUsedDays() {
        return usedDays;
    }

    public void setUsedDays(Double usedDays) {
        this.usedDays = usedDays;
        // Recalculate available days when used changes
        this.availableDays = totalDays - usedDays;
    }

    public Double getAvailableDays() {
        return availableDays;
    }

    public void setAvailableDays(Double availableDays) {
        this.availableDays = availableDays;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    // Business logic methods
    
    /**
     * Check if sufficient balance is available for the requested days
     */
    public boolean hasSufficientBalance(Double requestedDays) {
        return availableDays >= requestedDays;
    }

    /**
     * Deduct days from available balance
     */
    public void deductDays(Double days) {
        if (!hasSufficientBalance(days)) {
            throw new IllegalArgumentException("Insufficient leave balance. Available: " + availableDays + ", Requested: " + days);
        }
        this.usedDays += days;
        this.availableDays = totalDays - usedDays;
    }

    /**
     * Add days back to available balance (for cancelled/rejected leaves)
     */
    public void addDays(Double days) {
        this.usedDays = Math.max(0, usedDays - days);
        this.availableDays = totalDays - usedDays;
    }

    /**
     * Check if balance is running low (less than 5 days)
     */
    public boolean isRunningLow() {
        return availableDays < 5.0;
    }

    /**
     * Get utilization percentage
     */
    public double getUtilizationPercentage() {
        if (totalDays == 0) return 0.0;
        return (usedDays / totalDays) * 100.0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LeaveBalance that = (LeaveBalance) o;
        return Objects.equals(employee, that.employee) &&
               leaveType == that.leaveType &&
               Objects.equals(year, that.year);
    }

    @Override
    public int hashCode() {
        return Objects.hash(employee, leaveType, year);
    }

    @Override
    public String toString() {
        return "LeaveBalance{" +
                "id=" + id +
                ", employeeId=" + (employee != null ? employee.getEmployeeId() : null) +
                ", leaveType=" + leaveType +
                ", totalDays=" + totalDays +
                ", usedDays=" + usedDays +
                ", availableDays=" + availableDays +
                ", year=" + year +
                '}';
    }
}