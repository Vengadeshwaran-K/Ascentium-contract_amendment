// API Base URL
const API_BASE = '';

// Authentication Check
const authToken = localStorage.getItem('authToken');
if (!authToken) {
    if (window.location.pathname !== '/login.html') {
        window.location.href = '/login.html';
    }
}

// Display username and role in header
const username = localStorage.getItem('username');
const userRole = localStorage.getItem('role');

document.addEventListener('DOMContentLoaded', () => {
    // Set user display
    const userDisplay = document.getElementById('user-display');
    if (userDisplay && username) {
        userDisplay.textContent = `${username} (${userRole})`;
    }

    // Initialize UI
    applyRoleBasedAccess();
    loadDashboardStats();

    // Tab Navigation Logic
    const tabButtons = document.querySelectorAll('.tab-btn');
    const tabSections = document.querySelectorAll('.tab-section');

    tabButtons.forEach(btn => {
        btn.addEventListener('click', () => {
            const targetId = btn.getAttribute('data-tab');

            // Toggle Buttons
            tabButtons.forEach(b => b.classList.remove('active'));
            btn.classList.add('active');

            // Toggle Sections
            tabSections.forEach(s => s.classList.remove('active'));
            const targetSection = document.getElementById(targetId);
            if (targetSection) targetSection.classList.add('active');

            // Data Loading based on tab
            switch (targetId) {
                case 'dashboard-overview': loadDashboardStats(); break;
                case 'all-contracts': loadAllActiveContracts(); break;
                case 'audit-log': loadAuditLogs(); break;
                case 'user-mapping': loadUsersForMapping(); break;
                case 'create-contract': loadClientsForContract(); break;
                case 'approval-queue': loadApprovalQueue(); break;
                case 'view-contracts': loadMyContracts(); break;
            }
        });
    });

    // Logout
    const logoutBtn = document.getElementById('logout-btn');
    if (logoutBtn) {
        logoutBtn.addEventListener('click', () => {
            localStorage.clear();
            window.location.href = '/login.html';
        });
    }
});

// Helper for authenticated fetch
async function authenticatedFetch(url, options = {}) {
    const token = localStorage.getItem('authToken');
    const headers = {
        'Content-Type': 'application/json',
        ...options.headers,
    };

    if (token) headers['Authorization'] = `Bearer ${token}`;

    const response = await fetch(url, { ...options, headers });

    if (response.status === 401) {
        localStorage.clear();
        window.location.href = '/login.html';
        throw new Error('Session expired');
    }

    if (response.status === 403) {
        showToast('Access denied for this operation', 'error');
        throw new Error('Forbidden');
    }

    return response;
}

// Toast System
function showToast(message, type = 'success') {
    const toast = document.getElementById('toast');
    if (!toast) return;

    toast.textContent = message;
    toast.className = `toast-msg show ${type}`;

    setTimeout(() => {
        toast.classList.remove('show');
    }, 4000);
}

// UI State Management
function applyRoleBasedAccess() {
    const role = localStorage.getItem('role');
    const adminNav = document.getElementById('admin-nav');

    // Admin only sections
    if (role === 'SUPER_ADMIN') {
        if (adminNav) adminNav.style.display = 'block';
        show('tab-all-contracts');
        show('tab-audit-log');
    } else if (role === 'LEGAL_USER') {
        show('tab-create-contract');
        show('tab-view-contracts');
    } else if (role === 'FINANCE_REVIEWER') {
        show('tab-approval-queue');
        show('tab-view-contracts');
    } else if (role === 'CLIENT') {
        show('tab-approval-queue');
        show('tab-all-contracts');
    }
}

function show(id) {
    const el = document.getElementById(id);
    if (el) el.style.display = 'flex';
}

// Data Loaders
async function loadDashboardStats() {
    try {
        const response = await authenticatedFetch(`${API_BASE}/contracts/stats`);
        if (!response.ok) return;
        const data = await response.json();

        const container = document.getElementById('dashboard-stats');
        if (!container) return;

        container.innerHTML = Object.entries(data.counters).map(([label, value]) => `
            <div class="stat-card">
                <span class="stat-label">${label}</span>
                <span class="stat-value" id="val-${label.replace(/\s/g, '')}">${value}</span>
            </div>
        `).join('');

        // Count up animation
        Object.entries(data.counters).forEach(([label, value]) => {
            animateValue(`val-${label.replace(/\s/g, '')}`, 0, value, 800);
        });
    } catch (err) { console.error('Stats error:', err); }
}

function animateValue(id, start, end, duration) {
    const obj = document.getElementById(id);
    if (!obj || isNaN(end)) return;
    let startTimestamp = null;
    const step = (timestamp) => {
        if (!startTimestamp) startTimestamp = timestamp;
        const progress = Math.min((timestamp - startTimestamp) / duration, 1);
        obj.innerHTML = Math.floor(progress * (end - start) + start);
        if (progress < 1) window.requestAnimationFrame(step);
    };
    window.requestAnimationFrame(step);
}

// User Management Actions
async function loadUsersForMapping() {
    try {
        const res = await authenticatedFetch(`${API_BASE}/admin/users`);
        if (!res.ok) return;
        const users = await res.json();

        populateDropdown('legal-user', users, 'LEGAL_USER');
        populateDropdown('finance-user', users, 'FINANCE_REVIEWER');
        populateDropdown('client-user', users, 'CLIENT');
    } catch (err) { showToast('Failed to load users', 'error'); }
}

function populateDropdown(id, users, role) {
    const select = document.getElementById(id);
    if (!select) return;
    const filtered = users.filter(u => u.role.name === role);
    select.innerHTML = `<option value="">Select ${role.replace('_', ' ')}</option>` +
        filtered.map(u => `<option value="${u.id}">${u.displayName || u.username} (${u.email})</option>`).join('');
}

// Mapping Submission
document.getElementById('user-mapping-form')?.addEventListener('submit', async (e) => {
    e.preventDefault();
    const fd = new FormData(e.target);
    const data = {
        legalUserId: parseInt(fd.get('legalUserId')),
        financeUserId: parseInt(fd.get('financeUserId')),
        clientUserId: parseInt(fd.get('clientUserId'))
    };

    try {
        const res = await authenticatedFetch(`${API_BASE}/admin/approval-mappings`, {
            method: 'POST',
            body: JSON.stringify(data)
        });
        if (res.ok) {
            showToast('Workflow mapping established');
            e.target.reset();
        } else {
            const err = await res.text();
            showToast(err, 'error');
        }
    } catch (err) { showToast('Network failure', 'error'); }
});

// Create User Submission
document.getElementById('create-user-form')?.addEventListener('submit', async (e) => {
    e.preventDefault();
    const fd = new FormData(e.target);
    const data = Object.fromEntries(fd.entries());

    try {
        const res = await authenticatedFetch(`${API_BASE}/admin/users/register`, {
            method: 'POST',
            body: JSON.stringify(data)
        });
        if (res.ok) {
            showToast('Identity created successfully');
            e.target.reset();
            loadDashboardStats();
        } else {
            const err = await res.text();
            showToast(err, 'error');
        }
    } catch (err) { showToast('Service unavailable', 'error'); }
});

// Contract Creation
async function loadClientsForContract() {
    try {
        const res = await authenticatedFetch(`${API_BASE}/contracts/mapped-clients`);
        const clients = await res.json();
        const select = document.getElementById('client-id');
        if (select) {
            select.innerHTML = '<option value="">Select mapped client...</option>' +
                clients.map(c => `<option value="${c.id}">${c.displayName || c.username}</option>`).join('');
        }
    } catch (err) { console.error(err); }
}

document.getElementById('create-contract-form')?.addEventListener('submit', async (e) => {
    e.preventDefault();
    const fd = new FormData(e.target);
    const amount = parseFloat(fd.get('contractAmount'));

    if (amount < 0) return showToast('Value cannot be negative', 'error');

    const data = {
        contractName: fd.get('contractName'),
        clientId: parseInt(fd.get('clientId')),
        effectiveDate: fd.get('effectiveDate'),
        contractAmount: amount
    };

    try {
        const res = await authenticatedFetch(`${API_BASE}/contracts`, {
            method: 'POST',
            body: JSON.stringify(data)
        });
        if (res.ok) {
            showToast('Contract draft initialized');
            e.target.reset();
            loadDashboardStats();
        } else {
            const err = await res.text();
            showToast(err, 'error');
        }
    } catch (err) { showToast('Submission failed', 'error'); }
});

// Approval Management
async function loadApprovalQueue() {
    const body = document.getElementById('approval-queue-body');
    if (!body) return;
    body.innerHTML = '<tr><td colspan="6" style="text-align:center">Fetching queue...</td></tr>';

    try {
        const res = await authenticatedFetch(`${API_BASE}/contracts/approval-queue`);
        const data = await res.json();

        if (data.length === 0) {
            body.innerHTML = '<tr><td colspan="6" style="text-align:center; padding: 4rem; color: var(--text-low)">No items pending your action.</td></tr>';
            return;
        }

        body.innerHTML = data.map(v => `
            <tr>
                <td>${v.contract.contractName}</td>
                <td>V${v.versionNumber}</td>
                <td><span class="badge badge-${v.status.toLowerCase()}">${v.status.replace(/_/g, ' ')}</span></td>
                <td>${v.creator.username}</td>
                <td style="font-size: 0.85rem">${v.remarks || '—'}</td>
                <td class="actions">
                    <button class="btn-action btn-submit" onclick="approveContract(${v.contract.id})">Approve</button>
                    <button class="btn-action btn-reject" onclick="promptReject(${v.contract.id})">Reject</button>
                </td>
            </tr>
        `).join('');
    } catch (err) { showToast('Queue failed to load', 'error'); }
}

async function approveContract(id) {
    const res = await showModal('Review Remarks', [{ id: 'remarks', label: 'Comments (Optional)', type: 'text' }]);
    if (!res) return;

    try {
        const url = `${API_BASE}/contracts/${id}/approve${res.remarks ? `?remarks=${encodeURIComponent(res.remarks)}` : ''}`;
        const resp = await authenticatedFetch(url, { method: 'POST' });
        if (resp.ok) {
            showToast('Agreement authorized');
            loadApprovalQueue();
            loadDashboardStats();
        } else {
            const err = await resp.text();
            showToast(err, 'error');
        }
    } catch (err) { showToast('Sync error', 'error'); }
}

async function promptReject(id) {
    const res = await showModal('Rejection Payload', [{ id: 'remarks', label: 'Reason for rejection', type: 'text', required: true }]);
    if (!res || !res.remarks) return;

    try {
        const url = `${API_BASE}/contracts/${id}/reject?remarks=${encodeURIComponent(res.remarks)}`;
        const resp = await authenticatedFetch(url, { method: 'POST' });
        if (resp.ok) {
            showToast('Document returned', 'warning');
            loadApprovalQueue();
            loadDashboardStats();
        } else {
            const err = await resp.text();
            showToast(err, 'error');
        }
    } catch (err) { showToast('Sync error', 'error'); }
}

// My Contracts Management
async function loadMyContracts() {
    const body = document.getElementById('contracts-list-body');
    if (!body) return;

    try {
        const res = await authenticatedFetch(`${API_BASE}/contracts/my-contracts`);
        const data = await res.json();

        if (data.length === 0) {
            body.innerHTML = '<tr><td colspan="5" style="text-align:center; padding: 4rem; color: var(--text-low)">Workflow is empty.</td></tr>';
            return;
        }

        body.innerHTML = data.map(v => `
            <tr>
                <td>${v.contract.contractName}</td>
                <td>V${v.versionNumber}</td>
                <td><span class="badge badge-${v.status.toLowerCase()}">${v.status.replace(/_/g, ' ')}</span></td>
                <td style="font-size: 0.85rem">${v.remarks || '—'}</td>
                <td class="actions">
                    <button class="btn-action btn-edit" onclick="editContract(${v.contract.id}, '${v.contract.contractName}', ${v.contract.contractAmount}, '${v.contract.effectiveDate}')">Modify</button>
                    <button class="btn-action btn-submit" onclick="submitContract(${v.contract.id})">Release</button>
                </td>
            </tr>
        `).join('');
    } catch (err) { showToast('My contracts failed to load', 'error'); }
}

async function submitContract(id) {
    try {
        const res = await authenticatedFetch(`${API_BASE}/contracts/${id}/submit`, { method: 'POST' });
        if (res.ok) {
            showToast('Dispatched to next node');
            loadMyContracts();
            loadDashboardStats();
        } else {
            const err = await res.text();
            showToast(err, 'error');
        }
    } catch (err) { showToast('Sync error', 'error'); }
}

async function editContract(id, name, amount, date) {
    const res = await showModal('Update Agreement', [
        { id: 'name', label: 'Contract Title', type: 'text', value: name, required: true },
        { id: 'amount', label: 'Value ($)', type: 'number', value: amount, required: true },
        { id: 'date', label: 'Effective Date', type: 'date', value: date, required: true }
    ]);

    if (!res) return;
    if (parseFloat(res.amount) < 0) return showToast('Value error', 'error');

    const data = { contractName: res.name, contractAmount: parseFloat(res.amount), effectiveDate: res.date, clientId: 0 };

    try {
        const resp = await authenticatedFetch(`${API_BASE}/contracts/${id}`, {
            method: 'PUT',
            body: JSON.stringify(data)
        });
        if (resp.ok) {
            showToast('Changes synchronized');
            loadMyContracts();
            loadDashboardStats();
        } else {
            const err = await resp.text();
            showToast(err, 'error');
        }
    } catch (err) { showToast('Network error', 'error'); }
}

// Global Repository
async function loadAllActiveContracts() {
    const body = document.getElementById('all-contracts-list-body');
    if (!body) return;

    try {
        const res = await authenticatedFetch(`${API_BASE}/contracts/all-active`);
        const data = await res.json();

        body.innerHTML = data.map(v => `
            <tr>
                <td>${v.contract.contractName}</td>
                <td>V${v.versionNumber}</td>
                <td>${v.contract.client?.username || 'N/A'}</td>
                <td>$${v.contract.contractAmount.toLocaleString()}</td>
                <td>${v.contract.effectiveDate}</td>
                <td><span class="badge badge-active">Active</span></td>
            </tr>
        `).join('');
    } catch (err) { showToast('Repository sync failed', 'error'); }
}

// Audit Matrix
async function loadAuditLogs() {
    try {
        const res = await authenticatedFetch(`${API_BASE}/api/admin/audit/logs`);
        if (!res.ok) return;
        const logs = await res.json();
        const body = document.getElementById('audit-log-list-body');
        if (body) {
            body.innerHTML = logs.slice().reverse().map(log => `
                <tr>
                    <td>${new Date(log.timestamp).toLocaleString()}</td>
                    <td><span class="badge" style="background: hsla(250, 95%, 65%, 0.1); color: var(--primary)">${log.action}</span></td>
                    <td style="font-weight: 600;">${log.actor}</td>
                    <td><small style="color: var(--text-low)">${log.actorRole}</small></td>
                    <td style="font-size: 0.85rem">${log.remarks}</td>
                </tr>
            `).join('');
        }
    } catch (err) { console.error('Audit sync error', err); }
}

// Modal Engine
function showModal(title, inputs) {
    return new Promise((resolve) => {
        const modal = document.getElementById('custom-modal');
        const container = document.getElementById('modal-inputs-container');
        const ok = document.getElementById('modal-ok');
        const cancel = document.getElementById('modal-cancel');
        const close = document.getElementById('modal-close');

        document.getElementById('modal-title').textContent = title;
        container.innerHTML = inputs.map(i => `
            <div class="input-block">
                <label>${i.label}</label>
                <input id="mod-${i.id}" type="${i.type}" value="${i.value || ''}" placeholder="${i.id}..." ${i.required ? 'required' : ''}>
            </div>
        `).join('');

        const end = (val) => {
            modal.classList.remove('show');
            ok.onclick = null; cancel.onclick = null; close.onclick = null;
            resolve(val);
        };

        ok.onclick = () => {
            const data = {};
            let valid = true;
            inputs.forEach(i => {
                const el = document.getElementById(`mod-${i.id}`);
                if (i.required && !el.value) { el.style.borderColor = 'var(--danger)'; valid = false; }
                else data[i.id] = el.value;
            });
            if (valid) end(data);
        };
        cancel.onclick = () => end(null);
        close.onclick = () => end(null);
        modal.classList.add('show');
    });
}
