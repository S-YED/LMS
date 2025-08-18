package com.company.leavemanagementsystem.controller;

import com.company.leavemanagementsystem.dto.InitializeBalanceRequest;
import com.company.leavemanagementsystem.dto.LeaveBalanceResponse;
import com.company.leavemanagementsystem.dto.LeaveBalanceSummaryResponse;
import com.company.leavemanagementsystem.entity.LeaveType;
import com.company.leavemanagementsystem.service.LeaveBalanceService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Comprehensive test suite for LeaveBalanceController.
 * Tests all REST endpoints, request validation, error responses, and balance management scenarios.
 */
@WebMvcTest(LeaveBalanceController.class)
class LeaveBalanceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LeaveBalanceService leaveBalanceService;

    @Autowired
    private ObjectMapper objectMapper;

    private LeaveBalanceResponse vacationBalance;
    private LeaveBalanceResponse sickBalance;
    private LeaveBalanceSummaryResponse balanceSummary;
    private InitializeBalanceRequest initializeRequest;

    @BeforeEach
    void setUp() {
        // Create test balance responses
        vacationBalance = new LeaveBalanceResponse();
        vacationBalance.setId(1L);
        vacationBalance.setEmployeeId("EMP001");
        vacationBalance.setEmployeeName("Alice Smith");
        vacationBalance.setLeaveType(LeaveType.VACATION);
        vacationBalance.setTotalDays(20.0);
        vacationBalance.setUsedDays(5.0);
        vacationBalance.setAvailableDays(15.0);
        vacationBalance.setYear(2024);
        vacationBalance.setUtilizationPercentage(25.0);
        vacationBalance.setIsRunningLow(false);
        vacationBalance.setCreatedAt(LocalDateTime.now());
        vacationBalance.setUpdatedAt(LocalDateTime.now());

        sickBalance = new LeaveBalanceResponse();
        sickBalance.setId(2L);
        sickBalance.setEmployeeId("EMP001");
        sickBalance.setEmployeeName("Alice Smith");
        sickBalance.setLeaveType(LeaveType.SICK);
        sickBalance.setTotalDays(10.0);
        sickBalance.setUsedDays(2.0);
        sickBalance.setAvailableDays(8.0);
        sickBalance.setYear(2024);
        sickBalance.setUtilizationPercentage(20.0);
        sickBalance.setIsRunningLow(false);
        sickBalance.setCreatedAt(LocalDateTime.now());
        sickBalance.setUpdatedAt(LocalDateTime.now());

        // Create balance summary
        List<LeaveBalanceResponse> allBalances = Arrays.asList(vacationBalance, sickBalance);
        List<LeaveBalanceResponse> lowBalances = Collections.emptyList();

        balanceSummary = new LeaveBalanceSummaryResponse();
        balanceSummary.setEmployeeId("EMP001");
        balanceSummary.setEmployeeName("Alice Smith");
        balanceSummary.setYear(2024);
        balanceSummary.setTotalAllocated(30.0);
        balanceSummary.setTotalUsed(7.0);
        balanceSummary.setTotalAvailable(23.0);
        balanceSummary.setLeaveBalances(allBalances);
        balanceSummary.setLowBalances(lowBalances);

        // Create initialize request
        initializeRequest = new InitializeBalanceRequest();
        initializeRequest.setEmployeeId("EMP001");
        initializeRequest.setYear(2024);
        initializeRequest.setOverwriteExisting(false);
        
        Map<LeaveType, Double> allocations = new HashMap<>();
        allocations.put(LeaveType.VACATION, 20.0);
        allocations.put(LeaveType.SICK, 10.0);
        allocations.put(LeaveType.PERSONAL, 5.0);
        initializeRequest.setLeaveAllocations(allocations);
    }

    @Test
    void testGetEmployeeLeaveBalance_WhenEmployeeExists_ShouldReturnBalances() throws Exception {
        // Given
        List<LeaveBalanceResponse> balances = Arrays.asList(vacationBalance, sickBalance);
        when(leaveBalanceService.getEmployeeLeaveBalance("EMP001")).thenReturn(balances);

        // When & Then
        mockMvc.perform(get("/api/leave-balances/employee/EMP001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].employeeId").value("EMP001"))
                .andExpect(jsonPath("$[0].leaveType").value("VACATION"))
                .andExpect(jsonPath("$[0].totalDays").value(20.0))
                .andExpect(jsonPath("$[0].availableDays").value(15.0))
                .andExpect(jsonPath("$[1].leaveType").value("SICK"))
                .andExpect(jsonPath("$[1].totalDays").value(10.0))
                .andExpect(jsonPath("$[1].availableDays").value(8.0));

        verify(leaveBalanceService).getEmployeeLeaveBalance("EMP001");
    }

    @Test
    void testGetEmployeeLeaveBalance_WithYear_ShouldReturnBalancesForYear() throws Exception {
        // Given
        List<LeaveBalanceResponse> balances = Arrays.asList(vacationBalance, sickBalance);
        when(leaveBalanceService.getEmployeeLeaveBalance("EMP001", 2024)).thenReturn(balances);

        // When & Then
        mockMvc.perform(get("/api/leave-balances/employee/EMP001")
                .param("year", "2024"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].year").value(2024))
                .andExpect(jsonPath("$[1].year").value(2024));

        verify(leaveBalanceService).getEmployeeLeaveBalance("EMP001", 2024);
    }

    @Test
    void testGetEmployeeLeaveBalance_WhenEmployeeNotFound_ShouldReturnBadRequest() throws Exception {
        // Given
        when(leaveBalanceService.getEmployeeLeaveBalance("NONEXISTENT"))
            .thenThrow(new IllegalArgumentException("Employee not found: NONEXISTENT"));

        // When & Then
        mockMvc.perform(get("/api/leave-balances/employee/NONEXISTENT"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Employee not found: NONEXISTENT"));

        verify(leaveBalanceService).getEmployeeLeaveBalance("NONEXISTENT");
    }

    @Test
    void testGetEmployeeLeaveBalance_WhenNoBalances_ShouldReturnEmptyList() throws Exception {
        // Given
        when(leaveBalanceService.getEmployeeLeaveBalance("EMP001")).thenReturn(Collections.emptyList());

        // When & Then
        mockMvc.perform(get("/api/leave-balances/employee/EMP001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        verify(leaveBalanceService).getEmployeeLeaveBalance("EMP001");
    }

    @Test
    void testGetLeaveBalanceSummary_WhenEmployeeExists_ShouldReturnSummary() throws Exception {
        // Given
        when(leaveBalanceService.getLeaveBalanceSummary("EMP001")).thenReturn(balanceSummary);

        // When & Then
        mockMvc.perform(get("/api/leave-balances/employee/EMP001/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.employeeId").value("EMP001"))
                .andExpect(jsonPath("$.employeeName").value("Alice Smith"))
                .andExpect(jsonPath("$.year").value(2024))
                .andExpect(jsonPath("$.totalAllocated").value(30.0))
                .andExpect(jsonPath("$.totalUsed").value(7.0))
                .andExpect(jsonPath("$.totalAvailable").value(23.0))
                .andExpect(jsonPath("$.leaveBalances", hasSize(2)))
                .andExpect(jsonPath("$.lowBalances", hasSize(0)));

        verify(leaveBalanceService).getLeaveBalanceSummary("EMP001");
    }

    @Test
    void testGetLeaveBalanceSummary_WithYear_ShouldReturnSummaryForYear() throws Exception {
        // Given
        when(leaveBalanceService.getLeaveBalanceSummary("EMP001", 2023)).thenReturn(balanceSummary);

        // When & Then
        mockMvc.perform(get("/api/leave-balances/employee/EMP001/summary")
                .param("year", "2023"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.employeeId").value("EMP001"))
                .andExpect(jsonPath("$.year").value(2024));

        verify(leaveBalanceService).getLeaveBalanceSummary("EMP001", 2023);
    }

    @Test
    void testGetLeaveBalanceSummary_WithLowBalances_ShouldIncludeLowBalanceWarnings() throws Exception {
        // Given - Create summary with low balances
        LeaveBalanceResponse lowBalance = new LeaveBalanceResponse();
        lowBalance.setLeaveType(LeaveType.PERSONAL);
        lowBalance.setAvailableDays(2.0);
        lowBalance.setIsRunningLow(true);

        LeaveBalanceSummaryResponse summaryWithLowBalances = new LeaveBalanceSummaryResponse();
        summaryWithLowBalances.setEmployeeId("EMP001");
        summaryWithLowBalances.setLowBalances(Arrays.asList(lowBalance));

        when(leaveBalanceService.getLeaveBalanceSummary("EMP001")).thenReturn(summaryWithLowBalances);

        // When & Then
        mockMvc.perform(get("/api/leave-balances/employee/EMP001/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lowBalances", hasSize(1)))
                .andExpect(jsonPath("$.lowBalances[0].leaveType").value("PERSONAL"))
                .andExpect(jsonPath("$.lowBalances[0].availableDays").value(2.0))
                .andExpect(jsonPath("$.lowBalances[0].isRunningLow").value(true));

        verify(leaveBalanceService).getLeaveBalanceSummary("EMP001");
    }

    @Test
    void testInitializeLeaveBalance_WhenValidRequest_ShouldReturnInitializedBalances() throws Exception {
        // Given
        List<LeaveBalanceResponse> initializedBalances = Arrays.asList(vacationBalance, sickBalance);
        when(leaveBalanceService.initializeLeaveBalance(eq("EMP001"), any(InitializeBalanceRequest.class)))
            .thenReturn(initializedBalances);

        // When & Then
        mockMvc.perform(post("/api/leave-balances/employee/EMP001/initialize")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(initializeRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].employeeId").value("EMP001"))
                .andExpect(jsonPath("$[0].leaveType").value("VACATION"))
                .andExpect(jsonPath("$[0].totalDays").value(20.0))
                .andExpect(jsonPath("$[1].leaveType").value("SICK"))
                .andExpect(jsonPath("$[1].totalDays").value(10.0));

        verify(leaveBalanceService).initializeLeaveBalance(eq("EMP001"), any(InitializeBalanceRequest.class));
    }

    @Test
    void testInitializeLeaveBalance_WhenInvalidRequest_ShouldReturnBadRequest() throws Exception {
        // Given - Invalid request with missing employee ID
        InitializeBalanceRequest invalidRequest = new InitializeBalanceRequest();
        invalidRequest.setEmployeeId(""); // Empty employee ID
        invalidRequest.setYear(null); // Null year

        // When & Then
        mockMvc.perform(post("/api/leave-balances/employee/EMP001/initialize")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(leaveBalanceService, never()).initializeLeaveBalance(anyString(), any(InitializeBalanceRequest.class));
    }

    @Test
    void testInitializeLeaveBalance_WhenEmployeeNotFound_ShouldReturnBadRequest() throws Exception {
        // Given
        when(leaveBalanceService.initializeLeaveBalance(eq("EMP001"), any(InitializeBalanceRequest.class)))
            .thenThrow(new IllegalArgumentException("Employee not found: EMP001"));

        // When & Then
        mockMvc.perform(post("/api/leave-balances/employee/EMP001/initialize")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(initializeRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Employee not found: EMP001"));

        verify(leaveBalanceService).initializeLeaveBalance(eq("EMP001"), any(InitializeBalanceRequest.class));
    }

    @Test
    void testInitializeLeaveBalance_WithOverwriteExisting_ShouldOverwriteBalances() throws Exception {
        // Given
        initializeRequest.setOverwriteExisting(true);
        List<LeaveBalanceResponse> overwrittenBalances = Arrays.asList(vacationBalance);
        when(leaveBalanceService.initializeLeaveBalance(eq("EMP001"), any(InitializeBalanceRequest.class)))
            .thenReturn(overwrittenBalances);

        // When & Then
        mockMvc.perform(post("/api/leave-balances/employee/EMP001/initialize")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(initializeRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].totalDays").value(20.0));

        verify(leaveBalanceService).initializeLeaveBalance(eq("EMP001"), any(InitializeBalanceRequest.class));
    }

    @Test
    void testGetEmployeesWithLowBalance_ShouldReturnLowBalanceEmployees() throws Exception {
        // Given
        LeaveBalanceResponse lowBalance = new LeaveBalanceResponse();
        lowBalance.setEmployeeId("EMP002");
        lowBalance.setLeaveType(LeaveType.VACATION);
        lowBalance.setAvailableDays(3.0);
        lowBalance.setIsRunningLow(true);

        List<LeaveBalanceResponse> lowBalances = Arrays.asList(lowBalance);
        when(leaveBalanceService.getEmployeesWithLowBalance(5.0, 2024)).thenReturn(lowBalances);

        // When & Then
        mockMvc.perform(get("/api/leave-balances/low-balance")
                .param("threshold", "5.0")
                .param("year", "2024"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].employeeId").value("EMP002"))
                .andExpect(jsonPath("$[0].availableDays").value(3.0))
                .andExpect(jsonPath("$[0].isRunningLow").value(true));

        verify(leaveBalanceService).getEmployeesWithLowBalance(5.0, 2024);
    }

    @Test
    void testGetEmployeesWithLowBalance_WithDefaultThreshold_ShouldUseDefaultValue() throws Exception {
        // Given
        when(leaveBalanceService.getEmployeesWithLowBalance(null, 2024)).thenReturn(Collections.emptyList());

        // When & Then
        mockMvc.perform(get("/api/leave-balances/low-balance")
                .param("year", "2024"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        verify(leaveBalanceService).getEmployeesWithLowBalance(null, 2024);
    }

    @Test
    void testGetEmployeesWithHighUtilization_ShouldReturnHighUtilizationEmployees() throws Exception {
        // Given
        LeaveBalanceResponse highUtilizationBalance = new LeaveBalanceResponse();
        highUtilizationBalance.setEmployeeId("EMP003");
        highUtilizationBalance.setLeaveType(LeaveType.VACATION);
        highUtilizationBalance.setUtilizationPercentage(85.0);

        List<LeaveBalanceResponse> highUtilizationBalances = Arrays.asList(highUtilizationBalance);
        when(leaveBalanceService.getEmployeesWithHighUtilization(80.0, 2024)).thenReturn(highUtilizationBalances);

        // When & Then
        mockMvc.perform(get("/api/leave-balances/high-utilization")
                .param("utilizationThreshold", "80.0")
                .param("year", "2024"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].employeeId").value("EMP003"))
                .andExpect(jsonPath("$[0].utilizationPercentage").value(85.0));

        verify(leaveBalanceService).getEmployeesWithHighUtilization(80.0, 2024);
    }

    @Test
    void testGetDepartmentLeaveBalance_ShouldReturnDepartmentBalances() throws Exception {
        // Given
        List<LeaveBalanceResponse> departmentBalances = Arrays.asList(vacationBalance, sickBalance);
        when(leaveBalanceService.getDepartmentLeaveBalance("Engineering", 2024)).thenReturn(departmentBalances);

        // When & Then
        mockMvc.perform(get("/api/leave-balances/department/Engineering")
                .param("year", "2024"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].employeeId").value("EMP001"))
                .andExpect(jsonPath("$[1].employeeId").value("EMP001"));

        verify(leaveBalanceService).getDepartmentLeaveBalance("Engineering", 2024);
    }

    @Test
    void testGetDepartmentLeaveBalance_WithCurrentYear_ShouldUseCurrentYear() throws Exception {
        // Given
        when(leaveBalanceService.getDepartmentLeaveBalance(eq("Engineering"), any(Integer.class)))
            .thenReturn(Collections.emptyList());

        // When & Then
        mockMvc.perform(get("/api/leave-balances/department/Engineering"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        verify(leaveBalanceService).getDepartmentLeaveBalance(eq("Engineering"), any(Integer.class));
    }

    @Test
    void testCalculateDepartmentUtilization_ShouldReturnUtilizationPercentage() throws Exception {
        // Given
        when(leaveBalanceService.calculateDepartmentUtilization("Engineering", 2024)).thenReturn(65.5);

        // When & Then
        mockMvc.perform(get("/api/leave-balances/department/Engineering/utilization")
                .param("year", "2024"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.department").value("Engineering"))
                .andExpect(jsonPath("$.year").value(2024))
                .andExpect(jsonPath("$.utilizationPercentage").value(65.5));

        verify(leaveBalanceService).calculateDepartmentUtilization("Engineering", 2024);
    }

    @Test
    void testCalculateDepartmentUtilization_WhenNoDepartmentData_ShouldReturnNull() throws Exception {
        // Given
        when(leaveBalanceService.calculateDepartmentUtilization("NonExistentDept", 2024)).thenReturn(null);

        // When & Then
        mockMvc.perform(get("/api/leave-balances/department/NonExistentDept/utilization")
                .param("year", "2024"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.department").value("NonExistentDept"))
                .andExpect(jsonPath("$.year").value(2024))
                .andExpect(jsonPath("$.utilizationPercentage").isEmpty());

        verify(leaveBalanceService).calculateDepartmentUtilization("NonExistentDept", 2024);
    }

    @Test
    void testGetLeaveBalanceStatistics_ShouldReturnStatistics() throws Exception {
        // Given
        Map<String, Object> statistics = new HashMap<>();
        statistics.put("totalEmployees", 50L);
        statistics.put("employeesWithLowBalance", 5);
        statistics.put("employeesWithZeroBalance", 2);
        statistics.put("employeesWithHighUtilization", 8);
        
        Map<LeaveType, Double> avgUtilizationByType = new HashMap<>();
        avgUtilizationByType.put(LeaveType.VACATION, 45.5);
        avgUtilizationByType.put(LeaveType.SICK, 25.3);
        statistics.put("averageUtilizationByType", avgUtilizationByType);

        when(leaveBalanceService.getLeaveBalanceStatistics(2024)).thenReturn(statistics);

        // When & Then
        mockMvc.perform(get("/api/leave-balances/statistics")
                .param("year", "2024"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalEmployees").value(50))
                .andExpect(jsonPath("$.employeesWithLowBalance").value(5))
                .andExpect(jsonPath("$.employeesWithZeroBalance").value(2))
                .andExpect(jsonPath("$.employeesWithHighUtilization").value(8))
                .andExpect(jsonPath("$.averageUtilizationByType.VACATION").value(45.5))
                .andExpect(jsonPath("$.averageUtilizationByType.SICK").value(25.3));

        verify(leaveBalanceService).getLeaveBalanceStatistics(2024);
    }

    @Test
    void testGetEmployeesWithoutBalance_ShouldReturnEmployeeIds() throws Exception {
        // Given
        List<String> employeesWithoutBalance = Arrays.asList("EMP004", "EMP005", "EMP006");
        when(leaveBalanceService.getEmployeesWithoutBalance(2024)).thenReturn(employeesWithoutBalance);

        // When & Then
        mockMvc.perform(get("/api/leave-balances/employees-without-balance")
                .param("year", "2024"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$", containsInAnyOrder("EMP004", "EMP005", "EMP006")));

        verify(leaveBalanceService).getEmployeesWithoutBalance(2024);
    }

    @Test
    void testBulkInitializeLeaveBalance_ShouldReturnProcessedCount() throws Exception {
        // Given
        List<String> employeeIds = Arrays.asList("EMP001", "EMP002", "EMP003");
        when(leaveBalanceService.bulkInitializeLeaveBalance(employeeIds, 2024)).thenReturn(3);

        // When & Then
        mockMvc.perform(post("/api/leave-balances/bulk-initialize")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "employeeIds", employeeIds,
                    "year", 2024
                ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.processedCount").value(3))
                .andExpect(jsonPath("$.totalRequested").value(3))
                .andExpect(jsonPath("$.year").value(2024));

        verify(leaveBalanceService).bulkInitializeLeaveBalance(employeeIds, 2024);
    }

    @Test
    void testProcessYearEndRenewal_ShouldReturnProcessedCount() throws Exception {
        // Given
        when(leaveBalanceService.processYearEndRenewal(2023, 2024)).thenReturn(45);

        // When & Then
        mockMvc.perform(post("/api/leave-balances/year-end-renewal")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "currentYear", 2023,
                    "newYear", 2024
                ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.processedEmployees").value(45))
                .andExpect(jsonPath("$.currentYear").value(2023))
                .andExpect(jsonPath("$.newYear").value(2024));

        verify(leaveBalanceService).processYearEndRenewal(2023, 2024);
    }

    // Edge case and validation tests
    @Test
    void testGetEmployeeLeaveBalance_WithInvalidYear_ShouldReturnBadRequest() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/leave-balances/employee/EMP001")
                .param("year", "invalid"))
                .andExpect(status().isBadRequest());

        verify(leaveBalanceService, never()).getEmployeeLeaveBalance(anyString(), any(Integer.class));
    }

    @Test
    void testInitializeLeaveBalance_WithMismatchedEmployeeId_ShouldReturnBadRequest() throws Exception {
        // Given - Request body has different employee ID than path parameter
        initializeRequest.setEmployeeId("EMP002");

        // When & Then
        mockMvc.perform(post("/api/leave-balances/employee/EMP001/initialize")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(initializeRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Employee ID in path and request body must match"));

        verify(leaveBalanceService, never()).initializeLeaveBalance(anyString(), any(InitializeBalanceRequest.class));
    }

    @Test
    void testGetEmployeesWithLowBalance_WithInvalidThreshold_ShouldReturnBadRequest() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/leave-balances/low-balance")
                .param("threshold", "-1")
                .param("year", "2024"))
                .andExpect(status().isBadRequest());

        verify(leaveBalanceService, never()).getEmployeesWithLowBalance(any(Double.class), any(Integer.class));
    }

    @Test
    void testGetEmployeesWithHighUtilization_WithInvalidThreshold_ShouldReturnBadRequest() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/leave-balances/high-utilization")
                .param("utilizationThreshold", "150") // > 100%
                .param("year", "2024"))
                .andExpect(status().isBadRequest());

        verify(leaveBalanceService, never()).getEmployeesWithHighUtilization(any(Double.class), any(Integer.class));
    }

    @Test
    void testBulkInitializeLeaveBalance_WithEmptyEmployeeList_ShouldReturnBadRequest() throws Exception {
        // Given
        List<String> emptyList = Collections.emptyList();

        // When & Then
        mockMvc.perform(post("/api/leave-balances/bulk-initialize")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "employeeIds", emptyList,
                    "year", 2024
                ))))
                .andExpect(status().isBadRequest());

        verify(leaveBalanceService, never()).bulkInitializeLeaveBalance(any(List.class), any(Integer.class));
    }

    @Test
    void testProcessYearEndRenewal_WithInvalidYears_ShouldReturnBadRequest() throws Exception {
        // When & Then - New year before current year
        mockMvc.perform(post("/api/leave-balances/year-end-renewal")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "currentYear", 2024,
                    "newYear", 2023
                ))))
                .andExpect(status().isBadRequest());

        verify(leaveBalanceService, never()).processYearEndRenewal(any(Integer.class), any(Integer.class));
    }

    // Content type and format tests
    @Test
    void testInitializeLeaveBalance_WithInvalidContentType_ShouldReturnUnsupportedMediaType() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/leave-balances/employee/EMP001/initialize")
                .contentType(MediaType.TEXT_PLAIN)
                .content("invalid content"))
                .andExpect(status().isUnsupportedMediaType());

        verify(leaveBalanceService, never()).initializeLeaveBalance(anyString(), any(InitializeBalanceRequest.class));
    }

    @Test
    void testInitializeLeaveBalance_WithMalformedJson_ShouldReturnBadRequest() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/leave-balances/employee/EMP001/initialize")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{invalid json"))
                .andExpect(status().isBadRequest());

        verify(leaveBalanceService, never()).initializeLeaveBalance(anyString(), any(InitializeBalanceRequest.class));
    }
}