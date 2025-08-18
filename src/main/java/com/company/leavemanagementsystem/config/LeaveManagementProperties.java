package com.company.leavemanagementsystem.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Map;

/**
 * Configuration properties for the Leave Management System.
 * Maps application-specific properties from application.yml files.
 */
@Component
@ConfigurationProperties(prefix = "app")
public class LeaveManagementProperties {

    private Leave leave = new Leave();
    private Notifications notifications = new Notifications();

    public Leave getLeave() {
        return leave;
    }

    public void setLeave(Leave leave) {
        this.leave = leave;
    }

    public Notifications getNotifications() {
        return notifications;
    }

    public void setNotifications(Notifications notifications) {
        this.notifications = notifications;
    }

    /**
     * Leave-related configuration properties
     */
    public static class Leave {
        private Map<String, Integer> defaultAllocations;
        private Integer maxEmergencyDays = 2;
        private Integer maxBackdatedDays = 30;
        private Integer lowBalanceThreshold = 5;
        private List<DayOfWeek> workingDays;

        public Map<String, Integer> getDefaultAllocations() {
            return defaultAllocations;
        }

        public void setDefaultAllocations(Map<String, Integer> defaultAllocations) {
            this.defaultAllocations = defaultAllocations;
        }

        public Integer getMaxEmergencyDays() {
            return maxEmergencyDays;
        }

        public void setMaxEmergencyDays(Integer maxEmergencyDays) {
            this.maxEmergencyDays = maxEmergencyDays;
        }

        public Integer getMaxBackdatedDays() {
            return maxBackdatedDays;
        }

        public void setMaxBackdatedDays(Integer maxBackdatedDays) {
            this.maxBackdatedDays = maxBackdatedDays;
        }

        public Integer getLowBalanceThreshold() {
            return lowBalanceThreshold;
        }

        public void setLowBalanceThreshold(Integer lowBalanceThreshold) {
            this.lowBalanceThreshold = lowBalanceThreshold;
        }

        public List<DayOfWeek> getWorkingDays() {
            return workingDays;
        }

        public void setWorkingDays(List<DayOfWeek> workingDays) {
            this.workingDays = workingDays;
        }

        /**
         * Get default allocation for a specific leave type
         * @param leaveType the leave type (vacation, sick, personal, etc.)
         * @return the default allocation in days
         */
        public Integer getDefaultAllocation(String leaveType) {
            return defaultAllocations != null ? defaultAllocations.get(leaveType.toLowerCase()) : 0;
        }

        /**
         * Check if a day is a working day
         * @param dayOfWeek the day to check
         * @return true if it's a working day
         */
        public boolean isWorkingDay(DayOfWeek dayOfWeek) {
            return workingDays != null && workingDays.contains(dayOfWeek);
        }
    }

    /**
     * Notification-related configuration properties
     */
    public static class Notifications {
        private Boolean enabled = true;
        private String fromEmail = "noreply@company.com";
        private String fromName = "Leave Management System";
        private Integer retryAttempts = 3;
        private Long retryDelay = 5000L;
        private Templates templates = new Templates();

        public Boolean getEnabled() {
            return enabled;
        }

        public void setEnabled(Boolean enabled) {
            this.enabled = enabled;
        }

        public String getFromEmail() {
            return fromEmail;
        }

        public void setFromEmail(String fromEmail) {
            this.fromEmail = fromEmail;
        }

        public String getFromName() {
            return fromName;
        }

        public void setFromName(String fromName) {
            this.fromName = fromName;
        }

        public Integer getRetryAttempts() {
            return retryAttempts;
        }

        public void setRetryAttempts(Integer retryAttempts) {
            this.retryAttempts = retryAttempts;
        }

        public Long getRetryDelay() {
            return retryDelay;
        }

        public void setRetryDelay(Long retryDelay) {
            this.retryDelay = retryDelay;
        }

        public Templates getTemplates() {
            return templates;
        }

        public void setTemplates(Templates templates) {
            this.templates = templates;
        }

        /**
         * Email template configuration
         */
        public static class Templates {
            private String leaveApplied = "Leave application submitted";
            private String leaveApproved = "Leave request approved";
            private String leaveRejected = "Leave request rejected";
            private String lowBalanceWarning = "Low leave balance warning";

            public String getLeaveApplied() {
                return leaveApplied;
            }

            public void setLeaveApplied(String leaveApplied) {
                this.leaveApplied = leaveApplied;
            }

            public String getLeaveApproved() {
                return leaveApproved;
            }

            public void setLeaveApproved(String leaveApproved) {
                this.leaveApproved = leaveApproved;
            }

            public String getLeaveRejected() {
                return leaveRejected;
            }

            public void setLeaveRejected(String leaveRejected) {
                this.leaveRejected = leaveRejected;
            }

            public String getLowBalanceWarning() {
                return lowBalanceWarning;
            }

            public void setLowBalanceWarning(String lowBalanceWarning) {
                this.lowBalanceWarning = lowBalanceWarning;
            }
        }
    }
}