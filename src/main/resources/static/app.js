// API Base URL
const API_BASE = '/api';

// Global state
let employees = [];
let leaveRequests = [];
let balances = [];

// Initialize app
document.addEventListener('DOMContentLoaded', function() {
    loadDashboard();
    setupEventListeners();
});

// Setup event listeners
function setupEventListeners() {
    // Form submissions
    document.getElementById('addEmployeeForm').addEventListener('submit', handleAddEmployee);
    document.getElementById('addLeaveForm').addEventListener('submit', handleAddLeave);
    document.getElementById('initializeBalanceForm').addEventListener('submit', handleInitializeBalance);
}

// Tab Management
function showTab(tabName) {
    // Hide all tabs
    document.querySelectorAll('.tab-content').forEach(tab => {
        tab.classList.remove('active');
    });
    
    // Remove active class from all buttons
    document.querySelectorAll('.tab-btn').forEach(btn => {
        btn.classList.remove('active');
    });
    
    // Show selected tab
    document.getElementById(tabName).classList.add('active');
    
    // Add active class to clicked button
    event.target.classList.add('active');
    
    // Load data for the selected tab
    switch(tabName) {
        case 'dashboard':
            loadDashboard();
            break;
        case 'employees':
            loadEmployees();
            break;
        case 'leave-requests':
            loadLeaveRequests();
            break;
        case 'approvals':
            loadPendingApprovals();
            break;
        case 'balances':
            loadBalances();
            break;
    }
}

// Modal Management
function showAddEmployeeModal() {
    document.getElementById('addEmployeeModal').style.display = 'block';
}

function showAddLeaveModal() {
    loadEmployeeOptions('leaveEmployeeSelect');
    document.getElementById('addLeaveModal').style.display = 'block';
}

function showInitializeBalanceModal() {
    loadEmployeeOptions('balanceEmployeeSelect');
    document.getElementById('initializeBalanceModal').style.display = 'block';
}

function closeModal(modalId) {
    document.getElementById(modalId).style.display = 'none';
}

function showApiDocs() {
    window.open('/swagger-ui/index.html', '_blank');
}

// Loading Management
function showLoading() {
    document.getElementById('loading').style.display = 'flex';
}

function hideLoading() {
    document.getElementById('loading').style.display = 'none';
}

// API Calls
async function apiCall(endpoint, options = {}) {
    showLoading();
    try {
        const response = await fetch(API_BASE + endpoint, {
            headers: {
                'Content-Type': 'application/json',
                ...options.headers
            },
            ...options
        });
        
        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }
        
        const data = await response.json();
        return data;
    } catch (error) {
        console.error('API call failed:', error);
        alert('Operation failed: ' + error.message);
        throw error;
    } finally {
        hideLoading();
    }
}

// Dashboard Functions
async function loadDashboard() {
    try {
        // Load all data
        const [employeesData, requestsData] = await Promise.all([
            apiCall('/employees/all'),
            apiCall('/leave-requests?size=1000')
        ]);
        
        employees = employeesData;
        leaveRequests = requestsData.content || requestsData;
        
        // Update statistics
        document.getElementById('totalEmployees').textContent = employees.length;
        
        const pendingCount = leaveRequests.filter(req => req.status === 'PENDING').length;
        const approvedCount = leaveRequests.filter(req => req.status === 'APPROVED').length;
        const rejectedCount = leaveRequests.filter(req => req.status === 'REJECTED').length;
        
        document.getElementById('pendingRequests').textContent = pendingCount;
        document.getElementById('approvedRequests').textContent = approvedCount;
        document.getElementById('rejectedRequests').textContent = rejectedCount;
        
        // Load recent requests
        loadRecentRequests();
        
    } catch (error) {
        console.error('Failed to load dashboard:', error);
    }
}

function loadRecentRequests() {
    const recentContainer = document.getElementById('recentRequests');
    
    if (leaveRequests.length === 0) {
        recentContainer.innerHTML = `
            <div class="empty-state">
                <i class="fas fa-calendar-times"></i>
                <h3>No Leave Requests</h3>
                <p>No leave requests have been submitted yet.</p>
            </div>
        `;
        return;
    }
    
    // Sort by application date (most recent first) and take first 5
    const recentRequests = leaveRequests
        .sort((a, b) => new Date(b.applicationDate) - new Date(a.applicationDate))
        .slice(0, 5);
    
    recentContainer.innerHTML = recentRequests.map(request => {
        const employee = employees.find(emp => emp.employeeId === request.employeeId);
        const employeeName = employee ? employee.name : 'Unknown';
        
        return `
            <div class="activity-item">
                <div class="activity-info">
                    <h4>${employeeName} - ${request.leaveType}</h4>
                    <p>${formatDate(request.startDate)} to ${formatDate(request.endDate)} • ${request.reason}</p>
                </div>
                <span class="status-badge status-${request.status.toLowerCase()}">${request.status}</span>
            </div>
        `;
    }).join('');
}

// Employee Functions
async function loadEmployees() {
    try {
        employees = await apiCall('/employees/all');
        renderEmployees();
    } catch (error) {
        console.error('Failed to load employees:', error);
    }
}

function renderEmployees() {
    const container = document.getElementById('employeesList');
    
    if (employees.length === 0) {
        container.innerHTML = `
            <div class="empty-state">
                <i class="fas fa-users"></i>
                <h3>No Employees</h3>
                <p>No employees have been added yet.</p>
            </div>
        `;
        return;
    }
    
    container.innerHTML = `
        <table class="table">
            <thead>
                <tr>
                    <th>ID</th>
                    <th>Name</th>
                    <th>Email</th>
                    <th>Department</th>
                    <th>Position</th>
                    <th>Manager</th>
                    <th>Actions</th>
                </tr>
            </thead>
            <tbody>
                ${employees.map(employee => {
                    const manager = employees.find(emp => emp.employeeId === employee.managerId);
                    const managerName = manager ? manager.name : 'None';
                    
                    return `
                        <tr>
                            <td>${employee.employeeId}</td>
                            <td>${employee.name}</td>
                            <td>${employee.email}</td>
                            <td>${employee.department}</td>
                            <td>${employee.position}</td>
                            <td>${managerName}</td>
                            <td>
                                <div class="action-buttons">
                                    <button class="btn btn-sm btn-secondary" onclick="editEmployee('${employee.employeeId}')">
                                        <i class="fas fa-edit"></i>
                                    </button>
                                    <button class="btn btn-sm btn-danger" onclick="deleteEmployee('${employee.employeeId}')">
                                        <i class="fas fa-trash"></i>
                                    </button>
                                </div>
                            </td>
                        </tr>
                    `;
                }).join('')}
            </tbody>
        </table>
    `;
}

async function handleAddEmployee(event) {
    event.preventDefault();
    
    const formData = new FormData(event.target);
    const employeeData = {
        firstName: formData.get('firstName'),
        lastName: formData.get('lastName'),
        email: formData.get('email'),
        department: formData.get('department'),
        position: formData.get('position'),
        managerId: formData.get('managerId') ? parseInt(formData.get('managerId')) : null
    };
    
    try {
        await apiCall('/employees', {
            method: 'POST',
            body: JSON.stringify(employeeData)
        });
        
        closeModal('addEmployeeModal');
        event.target.reset();
        loadEmployees();
        alert('Employee added successfully!');
    } catch (error) {
        console.error('Failed to add employee:', error);
    }
}

async function deleteEmployee(employeeId) {
    if (!confirm('Are you sure you want to delete this employee?')) {
        return;
    }
    
    try {
        await apiCall(`/employees/${employeeId}`, {
            method: 'DELETE'
        });
        
        loadEmployees();
        alert('Employee deleted successfully!');
    } catch (error) {
        console.error('Failed to delete employee:', error);
    }
}

function editEmployee(employeeId) {
    alert('Edit functionality would be implemented here. For now, you can delete and re-add the employee.');
}

// Leave Request Functions
async function loadLeaveRequests() {
    try {
        const data = await apiCall('/leave-requests?size=1000');
        leaveRequests = data.content || data;
        renderLeaveRequests();
    } catch (error) {
        console.error('Failed to load leave requests:', error);
    }
}

function renderLeaveRequests() {
    const container = document.getElementById('leaveRequestsList');
    
    if (leaveRequests.length === 0) {
        container.innerHTML = `
            <div class="empty-state">
                <i class="fas fa-paper-plane"></i>
                <h3>No Leave Requests</h3>
                <p>No leave requests have been submitted yet.</p>
            </div>
        `;
        return;
    }
    
    container.innerHTML = `
        <table class="table">
            <thead>
                <tr>
                    <th>ID</th>
                    <th>Employee</th>
                    <th>Type</th>
                    <th>Start Date</th>
                    <th>End Date</th>
                    <th>Days</th>
                    <th>Status</th>
                    <th>Reason</th>
                </tr>
            </thead>
            <tbody>
                ${leaveRequests.map(request => {
                    const employee = employees.find(emp => emp.employeeId === request.employeeId);
                    const employeeName = employee ? employee.name : 'Unknown';
                    
                    return `
                        <tr>
                            <td>${request.id}</td>
                            <td>${employeeName}</td>
                            <td><span class="leave-type-${request.leaveType.toLowerCase()}">${request.leaveType}</span></td>
                            <td>${formatDate(request.startDate)}</td>
                            <td>${formatDate(request.endDate)}</td>
                            <td>${request.totalDays}</td>
                            <td><span class="status-badge status-${request.status.toLowerCase()}">${request.status}</span></td>
                            <td>${request.reason}</td>
                        </tr>
                    `;
                }).join('')}
            </tbody>
        </table>
    `;
}

async function handleAddLeave(event) {
    event.preventDefault();
    
    const formData = new FormData(event.target);
    const leaveData = {
        employeeId: parseInt(formData.get('employeeId')),
        leaveType: formData.get('leaveType'),
        startDate: formData.get('startDate'),
        endDate: formData.get('endDate'),
        halfDay: formData.get('halfDay') === 'on',
        reason: formData.get('reason')
    };
    
    try {
        await apiCall('/leave-requests', {
            method: 'POST',
            body: JSON.stringify(leaveData)
        });
        
        closeModal('addLeaveModal');
        event.target.reset();
        loadLeaveRequests();
        alert('Leave request submitted successfully!');
    } catch (error) {
        console.error('Failed to submit leave request:', error);
    }
}

// Approval Functions
async function loadPendingApprovals() {
    try {
        const data = await apiCall('/leave-requests?size=1000');
        leaveRequests = data.content || data;
        renderPendingApprovals();
    } catch (error) {
        console.error('Failed to load pending approvals:', error);
    }
}

function renderPendingApprovals() {
    const container = document.getElementById('pendingApprovals');
    const pendingRequests = leaveRequests.filter(request => request.status === 'PENDING');
    
    if (pendingRequests.length === 0) {
        container.innerHTML = `
            <div class="empty-state">
                <i class="fas fa-check-circle"></i>
                <h3>No Pending Approvals</h3>
                <p>All leave requests have been processed.</p>
            </div>
        `;
        return;
    }
    
    container.innerHTML = `
        <table class="table">
            <thead>
                <tr>
                    <th>ID</th>
                    <th>Employee</th>
                    <th>Type</th>
                    <th>Start Date</th>
                    <th>End Date</th>
                    <th>Days</th>
                    <th>Reason</th>
                    <th>Actions</th>
                </tr>
            </thead>
            <tbody>
                ${pendingRequests.map(request => {
                    const employee = employees.find(emp => emp.employeeId === request.employeeId);
                    const employeeName = employee ? employee.name : 'Unknown';
                    
                    return `
                        <tr>
                            <td>${request.id}</td>
                            <td>${employeeName}</td>
                            <td><span class="leave-type-${request.leaveType.toLowerCase()}">${request.leaveType}</span></td>
                            <td>${formatDate(request.startDate)}</td>
                            <td>${formatDate(request.endDate)}</td>
                            <td>${request.totalDays}</td>
                            <td>${request.reason}</td>
                            <td>
                                <div class="action-buttons">
                                    <button class="btn btn-sm btn-success" onclick="approveRequest(${request.id})">
                                        <i class="fas fa-check"></i> Approve
                                    </button>
                                    <button class="btn btn-sm btn-danger" onclick="rejectRequest(${request.id})">
                                        <i class="fas fa-times"></i> Reject
                                    </button>
                                </div>
                            </td>
                        </tr>
                    `;
                }).join('')}
            </tbody>
        </table>
    `;
}

async function approveRequest(requestId) {
    if (!confirm('Are you sure you want to approve this leave request?')) {
        return;
    }
    
    try {
        await apiCall(`/leave-requests/${requestId}/approve`, {
            method: 'PUT'
        });
        
        loadPendingApprovals();
        alert('Leave request approved successfully!');
    } catch (error) {
        console.error('Failed to approve request:', error);
    }
}

async function rejectRequest(requestId) {
    const reason = prompt('Please provide a reason for rejection:');
    if (!reason) {
        return;
    }
    
    try {
        await apiCall(`/leave-requests/${requestId}/reject`, {
            method: 'PUT',
            body: JSON.stringify({ reason: reason })
        });
        
        loadPendingApprovals();
        alert('Leave request rejected successfully!');
    } catch (error) {
        console.error('Failed to reject request:', error);
    }
}

// Balance Functions
async function loadBalances() {
    try {
        // Since there's no general balances endpoint, we'll show a message
        // In a real app, you'd either add an endpoint or load balances per employee
        balances = [];
        renderBalances();
    } catch (error) {
        console.error('Failed to load balances:', error);
    }
}

function renderBalances() {
    const container = document.getElementById('balancesList');
    
    if (balances.length === 0) {
        container.innerHTML = `
            <div class="empty-state">
                <i class="fas fa-chart-bar"></i>
                <h3>No Leave Balances</h3>
                <p>No leave balances have been initialized yet.</p>
            </div>
        `;
        return;
    }
    
    container.innerHTML = `
        <table class="table">
            <thead>
                <tr>
                    <th>Employee</th>
                    <th>Leave Type</th>
                    <th>Year</th>
                    <th>Total Days</th>
                    <th>Used Days</th>
                    <th>Remaining Days</th>
                </tr>
            </thead>
            <tbody>
                ${balances.map(balance => {
                    const employee = employees.find(emp => emp.employeeId === balance.employeeId);
                    const employeeName = employee ? employee.name : 'Unknown';
                    
                    return `
                        <tr>
                            <td>${employeeName}</td>
                            <td><span class="leave-type-${balance.leaveType.toLowerCase()}">${balance.leaveType}</span></td>
                            <td>${balance.year}</td>
                            <td>${balance.totalDays}</td>
                            <td>${balance.usedDays}</td>
                            <td>${balance.remainingDays}</td>
                        </tr>
                    `;
                }).join('')}
            </tbody>
        </table>
    `;
}

async function handleInitializeBalance(event) {
    event.preventDefault();
    
    const formData = new FormData(event.target);
    const balanceData = {
        employeeId: parseInt(formData.get('employeeId')),
        leaveType: formData.get('leaveType'),
        year: parseInt(formData.get('year')),
        totalDays: parseFloat(formData.get('totalDays'))
    };
    
    try {
        await apiCall('/leave-balances/initialize', {
            method: 'POST',
            body: JSON.stringify(balanceData)
        });
        
        closeModal('initializeBalanceModal');
        event.target.reset();
        loadBalances();
        alert('Leave balance initialized successfully!');
    } catch (error) {
        console.error('Failed to initialize balance:', error);
    }
}

// Utility Functions
async function loadEmployeeOptions(selectId) {
    if (employees.length === 0) {
        employees = await apiCall('/employees/all');
    }
    
    const select = document.getElementById(selectId);
    select.innerHTML = '<option value="">Select Employee</option>' +
        employees.map(emp => 
            `<option value="${emp.employeeId}">${emp.name}</option>`
        ).join('');
}

function formatDate(dateString) {
    const date = new Date(dateString);
    return date.toLocaleDateString('en-US', {
        year: 'numeric',
        month: 'short',
        day: 'numeric'
    });
}

// Close modals when clicking outside
window.onclick = function(event) {
    const modals = document.querySelectorAll('.modal');
    modals.forEach(modal => {
        if (event.target === modal) {
            modal.style.display = 'none';
        }
    });
}