// API Base URL
const API_BASE = '';

// Authentication Check
const authToken = localStorage.getItem('authToken');
if (!authToken) {
    // No token found, redirect to login
    window.location.href = '/login.html';
}

// Display username in header
const username = localStorage.getItem('username');
if (username) {
    document.addEventListener('DOMContentLoaded', () => {
        const userDisplay = document.getElementById('user-display');
        if (userDisplay) {
            userDisplay.textContent = username;
        }
    });
}

// Logout functionality
document.addEventListener('DOMContentLoaded', () => {
    const logoutBtn = document.getElementById('logout-btn');
    if (logoutBtn) {
        logoutBtn.addEventListener('click', () => {
            localStorage.removeItem('authToken');
            localStorage.removeItem('username');
            window.location.href = '/login.html';
        });
    }
});

// Helper function to make authenticated API calls
async function authenticatedFetch(url, options = {}) {
    const token = localStorage.getItem('authToken');

    const headers = {
        'Content-Type': 'application/json',
        ...options.headers,
    };

    if (token) {
        headers['Authorization'] = `Bearer ${token}`;
    }

    const response = await fetch(url, {
        ...options,
        headers
    });

    // Handle 401 Unauthorized - token expired or invalid
    if (response.status === 401) {
        localStorage.removeItem('authToken');
        localStorage.removeItem('username');
        window.location.href = '/login.html';
        throw new Error('Session expired. Please login again.');
    }

    return response;
}

// Tab Navigation
document.querySelectorAll('.tab-btn').forEach(btn => {
    btn.addEventListener('click', () => {
        const targetTab = btn.dataset.tab;

        // Update active tab button
        document.querySelectorAll('.tab-btn').forEach(b => b.classList.remove('active'));
        btn.classList.add('active');

        // Update active tab content
        document.querySelectorAll('.tab-content').forEach(content => {
            content.classList.remove('active');
        });
        document.getElementById(targetTab).classList.add('active');

        // Load users when switching to mapping or contract tabs
        if (targetTab === 'user-mapping') {
            loadUsersForMapping();
        } else if (targetTab === 'create-contract') {
            loadClientsForContract();
        }
    });
});

// Toast Notification
function showToast(message, type = 'success') {
    const toast = document.getElementById('toast');
    toast.textContent = message;
    toast.className = `toast show ${type}`;

    setTimeout(() => {
        toast.classList.remove('show');
    }, 3000);
}

// Create User Form
document.getElementById('create-user-form').addEventListener('submit', async (e) => {
    e.preventDefault();

    const formData = new FormData(e.target);
    const userData = {
        username: formData.get('username'),
        email: formData.get('email'),
        password: formData.get('password'),
        role: formData.get('role')
    };

    try {
        const response = await authenticatedFetch(`${API_BASE}/admin/users/register`, {
            method: 'POST',
            body: JSON.stringify(userData)
        });

        if (response.ok) {
            const user = await response.json();
            showToast(`User "${user.username}" created successfully!`, 'success');
            e.target.reset();
        } else {
            const error = await response.text();
            showToast(`Error: ${error}`, 'error');
        }
    } catch (error) {
        showToast(`Network error: ${error.message}`, 'error');
    }
});

// Load Users for Mapping
async function loadUsersForMapping() {
    try {
        const response = await fetch(`${API_BASE}/admin/users`);
        if (!response.ok) throw new Error('Failed to fetch users');

        const users = await response.json();

        // Populate dropdowns
        populateUserDropdown('legal-user', users, 'LEGAL_USER');
        populateUserDropdown('finance-user', users, 'FINANCE_REVIEWER');
        populateUserDropdown('client-user', users, 'CLIENT');
    } catch (error) {
        showToast(`Error loading users: ${error.message}`, 'error');
    }
}

function populateUserDropdown(selectId, users, roleFilter) {
    const select = document.getElementById(selectId);
    const currentValue = select.value;

    // Clear existing options except the first one
    select.innerHTML = '<option value="">Select ' + roleFilter.replace('_', ' ') + '</option>';

    // Filter users by role
    const filteredUsers = users.filter(user => user.role.name === roleFilter);

    filteredUsers.forEach(user => {
        const option = document.createElement('option');
        option.value = user.id;
        option.textContent = `${user.username} (${user.email})`;
        select.appendChild(option);
    });

    // Restore previous selection if it still exists
    if (currentValue) {
        select.value = currentValue;
    }
}

// User Mapping Form
document.getElementById('user-mapping-form').addEventListener('submit', async (e) => {
    e.preventDefault();

    const formData = new FormData(e.target);
    const mappingData = {
        legalUserId: parseInt(formData.get('legalUserId')),
        financeUserId: parseInt(formData.get('financeUserId')),
        clientUserId: parseInt(formData.get('clientUserId'))
    };

    try {
        const response = await authenticatedFetch(`${API_BASE}/admin/approval-mappings`, {
            method: 'POST',
            body: JSON.stringify(mappingData)
        });

        if (response.ok) {
            showToast('Approval mapping created successfully!', 'success');
            e.target.reset();
        } else {
            const error = await response.text();
            showToast(`Error: ${error}`, 'error');
        }
    } catch (error) {
        showToast(`Network error: ${error.message}`, 'error');
    }
});

// Load Clients for Contract
async function loadClientsForContract() {
    try {
        const response = await fetch(`${API_BASE}/admin/users`);
        if (!response.ok) throw new Error('Failed to fetch users');

        const users = await response.json();

        // Populate client dropdown
        const select = document.getElementById('client-id');
        select.innerHTML = '<option value="">Select Client</option>';

        const clients = users.filter(user => user.role.name === 'CLIENT');

        clients.forEach(client => {
            const option = document.createElement('option');
            option.value = client.id;
            option.textContent = `${client.username} (${client.email})`;
            select.appendChild(option);
        });
    } catch (error) {
        showToast(`Error loading clients: ${error.message}`, 'error');
    }
}

// Create Contract Form
document.getElementById('create-contract-form').addEventListener('submit', async (e) => {
    e.preventDefault();

    const formData = new FormData(e.target);
    const contractData = {
        contractName: formData.get('contractName'),
        clientId: parseInt(formData.get('clientId')),
        effectiveDate: formData.get('effectiveDate'),
        contractAmount: parseFloat(formData.get('contractAmount'))
    };

    try {
        const response = await authenticatedFetch(`${API_BASE}/contracts`, {
            method: 'POST',
            body: JSON.stringify(contractData)
        });

        if (response.ok) {
            const contract = await response.json();
            showToast(`Contract "${contract.contractName}" created successfully!`, 'success');
            e.target.reset();
        } else {
            const error = await response.text();
            showToast(`Error: ${error}`, 'error');
        }
    } catch (error) {
        showToast(`Network error: ${error.message}`, 'error');
    }
});

// Initialize
document.addEventListener('DOMContentLoaded', () => {
    console.log('Contract Management Dashboard loaded');
    applyRoleBasedAccess();
});

function applyRoleBasedAccess() {
    const role = localStorage.getItem('role');
    console.log('Applying access for role:', role);

    // Default: Hide everything
    const allTabs = ['tab-create-user', 'tab-user-mapping', 'tab-create-contract', 'tab-approval-queue', 'tab-view-contracts'];
    const allSections = ['create-user', 'user-mapping', 'create-contract', 'approval-queue', 'view-contracts'];

    allTabs.forEach(id => {
        const el = document.getElementById(id);
        if (el) el.style.display = 'none';
    });

    // Show based on role
    let defaultTab = '';
    if (role === 'SUPER_ADMIN') {
        show('tab-create-user');
        show('tab-user-mapping');
        defaultTab = 'tab-create-user';
    } else if (role === 'LEGAL_USER') {
        show('tab-create-contract');
        show('tab-view-contracts');
        defaultTab = 'tab-create-contract';
    } else if (role === 'FINANCE_REVIEWER' || role === 'CLIENT') {
        show('tab-approval-queue');
        show('tab-view-contracts');
        defaultTab = 'tab-approval-queue';
    }

    // Set default active tab if current one is hidden
    if (defaultTab) {
        document.getElementById(defaultTab).click();
    }
}

function show(id) {
    const el = document.getElementById(id);
    if (el) el.style.display = 'flex';
}
