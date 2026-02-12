const API_BASE = '';

const authToken = localStorage.getItem('authToken');
if (!authToken) {
    window.location.href = '/login.html';
}

const username = localStorage.getItem('username');
if (username) {
    document.addEventListener('DOMContentLoaded', () => {
        const userDisplay = document.getElementById('user-display');
        if (userDisplay) {
            userDisplay.textContent = username;
        }
    });
}

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

    if (response.status === 401) {
        const errorText = await response.text();
        console.error('Unauthorized request:', url, 'Response:', errorText);
        localStorage.removeItem('authToken');
        localStorage.removeItem('username');
        window.location.href = '/login.html';
        throw new Error('Session expired. Please login again.');
    }

    return response;
}

document.querySelectorAll('.tab-btn').forEach(btn => {
    btn.addEventListener('click', () => {
        const targetTab = btn.dataset.tab;

        document.querySelectorAll('.tab-btn').forEach(b => b.classList.remove('active'));
        btn.classList.add('active');

        document.querySelectorAll('.tab-content').forEach(content => {
            content.classList.remove('active');
        });
        document.getElementById(targetTab).classList.add('active');

        if (targetTab === 'user-mapping') {
            loadUsersForMapping();
        } else if (targetTab === 'create-contract') {
            loadClientsForContract();
        } else if (targetTab === 'approval-queue') {
            loadApprovalQueue();
        } else if (targetTab === 'view-contracts') {
            loadMyContracts();
        } else if (targetTab === 'all-contracts') {
            loadAllActiveContracts();
        }
    });
});

function showToast(message, type = 'success') {
    const toast = document.getElementById('toast');
    toast.textContent = message;
    toast.className = `toast show ${type}`;

    setTimeout(() => {
        toast.classList.remove('show');
    }, 3000);
}

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

async function loadUsersForMapping() {
    try {
        const response = await authenticatedFetch(`${API_BASE}/admin/users`);
        if (!response.ok) throw new Error('Failed to fetch users');

        const users = await response.json();

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

    select.innerHTML = '<option value="">Select ' + roleFilter.replace('_', ' ') + '</option>';

    const filteredUsers = users.filter(user => user.role.name === roleFilter);

    filteredUsers.forEach(user => {
        const option = document.createElement('option');
        option.value = user.id;
        option.textContent = `${user.username} (${user.email})`;
        select.appendChild(option);
    });

    if (currentValue) {
        select.value = currentValue;
    }
}

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

async function loadClientsForContract() {
    try {
        const response = await authenticatedFetch(`${API_BASE}/admin/users`);
        if (!response.ok) throw new Error('Failed to fetch users');

        const users = await response.json();

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

document.addEventListener('DOMContentLoaded', () => {
    console.log('Contract Management Dashboard loaded');
    applyRoleBasedAccess();
});

function applyRoleBasedAccess() {
    const role = localStorage.getItem('role');
    console.log('Applying access for role:', role);

    const allTabs = ['tab-create-user', 'tab-user-mapping', 'tab-create-contract', 'tab-approval-queue', 'tab-view-contracts'];
    const allSections = ['create-user', 'user-mapping', 'create-contract', 'approval-queue', 'view-contracts'];

    allTabs.forEach(id => {
        const el = document.getElementById(id);
        if (el) el.style.display = 'none';
    });

    let defaultTab = '';
    if (role === 'SUPER_ADMIN') {
        show('tab-create-user');
        show('tab-user-mapping');
        show('tab-all-contracts');
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

    if (defaultTab) {
        document.getElementById(defaultTab).click();
    }
}

function show(id) {
    const el = document.getElementById(id);
    if (el) el.style.display = 'flex';
}

async function loadApprovalQueue() {
    const listBody = document.getElementById('approval-queue-body');
    if (!listBody) return;
    listBody.innerHTML = '<tr><td colspan="6" class="text-center">Loading...</td></tr>';

    try {
        const response = await authenticatedFetch(`${API_BASE}/contracts/approval-queue`);
        if (!response.ok) throw new Error('Failed to fetch approval queue');

        const versions = await response.json();
        listBody.innerHTML = '';

        if (versions.length === 0) {
            listBody.innerHTML = '<tr><td colspan="6" class="text-center">No contracts pending approval.</td></tr>';
            return;
        }

        versions.forEach(v => {
            const row = document.createElement('tr');
            row.innerHTML = `
                <td>${v.contract.contractName}</td>
                <td>V${v.versionNumber}</td>
                <td><span class="status-badge ${v.status.toLowerCase()}">${v.status.replace(/_/g, ' ')}</span></td>
                <td>${v.creator.username}</td>
                <td>${v.remarks || '-'}</td>
                <td class="actions">
                    <button class="btn btn-sm btn-approve" onclick="approveContract(${v.contract.id})">Approve</button>
                    <button class="btn btn-sm btn-reject" onclick="promptReject(${v.contract.id})">Reject</button>
                </td>
            `;
            listBody.appendChild(row);
        });
    } catch (error) {
        showToast(`Error: ${error.message}`, 'error');
    }
}

async function loadMyContracts() {
    const listBody = document.getElementById('contracts-list-body');
    if (!listBody) return;
    listBody.innerHTML = '<tr><td colspan="5" class="text-center">Loading...</td></tr>';

    try {
        const response = await authenticatedFetch(`${API_BASE}/contracts/my-contracts`);
        if (!response.ok) throw new Error('Failed to fetch contracts');

        const versions = await response.json();
        listBody.innerHTML = '';

        if (versions.length === 0) {
            listBody.innerHTML = '<tr><td colspan="5" class="text-center">No contracts found.</td></tr>';
            return;
        }

        versions.forEach(v => {
            const row = document.createElement('tr');
            const canSubmit = (v.status === 'DRAFT' || v.status.startsWith('REJECTED'));
            row.innerHTML = `
                <td>${v.contract.contractName}</td>
                <td>V${v.versionNumber}</td>
                <td><span class="status-badge ${v.status.toLowerCase()}">${v.status.replace(/_/g, ' ')}</span></td>
                <td>${v.remarks || '-'}</td>
                <td class="actions">
                    ${canSubmit ? `
                        <button class="btn btn-sm btn-edit" onclick="editContract(${v.contract.id}, '${v.contract.contractName}', ${v.contract.contractAmount}, '${v.contract.effectiveDate}')">Edit</button>
                        <button class="btn btn-sm btn-submit" onclick="submitContract(${v.contract.id})">Submit</button>
                    ` : '-'}
                </td>
            `;
            listBody.appendChild(row);
        });
    } catch (error) {
        showToast(`Error: ${error.message}`, 'error');
    }
}

async function submitContract(id) {
    try {
        const response = await authenticatedFetch(`${API_BASE}/contracts/${id}/submit`, { method: 'POST' });
        if (response.ok) {
            showToast('Contract submitted successfully!', 'success');
            loadMyContracts();
        } else {
            showToast('Failed to submit contract', 'error');
        }
    } catch (error) {
        showToast(`Error: ${error.message}`, 'error');
    }
}

async function approveContract(id) {
    const result = await showModal('Approve Contract', [
        { id: 'remarks', label: 'Approval Remarks (optional)', type: 'text', placeholder: 'Enter remarks...' }
    ]);

    if (!result) return;
    const { remarks } = result;

    try {
        const url = `${API_BASE}/contracts/${id}/approve${remarks ? `?remarks=${encodeURIComponent(remarks)}` : ''}`;
        const response = await authenticatedFetch(url, { method: 'POST' });
        if (response.ok) {
            showToast('Contract approved!', 'success');
            loadApprovalQueue();
        } else {
            showToast('Failed to approve contract', 'error');
        }
    } catch (error) {
        showToast(`Error: ${error.message}`, 'error');
    }
}

async function promptReject(id) {
    const result = await showModal('Reject Contract', [
        { id: 'remarks', label: 'Rejection Remarks (required)', type: 'text', placeholder: 'Reason for rejection...', required: true }
    ]);

    if (result && result.remarks) {
        rejectContract(id, result.remarks);
    } else if (result) {
        showToast('Remarks are required for rejection', 'error');
    }
}

async function rejectContract(id, remarks) {
    try {
        const url = `${API_BASE}/contracts/${id}/reject?remarks=${encodeURIComponent(remarks)}`;
        const response = await authenticatedFetch(url, { method: 'POST' });
        if (response.ok) {
            showToast('Contract rejected', 'warning');
            loadApprovalQueue();
        } else {
            showToast('Failed to reject contract', 'error');
        }
    } catch (error) {
        showToast(`Error: ${error.message}`, 'error');
    }
}

async function editContract(id, currentName, currentAmount, currentDate) {
    const result = await showModal('Edit Contract', [
        { id: 'name', label: 'Contract Name', type: 'text', value: currentName, required: true },
        { id: 'amount', label: 'Contract Amount', type: 'number', value: currentAmount, required: true },
        { id: 'date', label: 'Effective Date', type: 'date', value: currentDate, required: true }
    ]);

    if (!result) return;
    const { name, amount, date } = result;

    const updateData = {
        contractName: name,
        contractAmount: parseFloat(amount),
        effectiveDate: date,
        clientId: 0
    };

    try {
        const response = await authenticatedFetch(`${API_BASE}/contracts/${id}`, {
            method: 'PUT',
            body: JSON.stringify(updateData)
        });

        if (response.ok) {
            showToast('Contract updated successfully!', 'success');
            loadMyContracts();
        } else {
            const error = await response.text();
            showToast(`Error: ${error}`, 'error');
        }
    } catch (error) {
        showToast(`Network error: ${error.message}`, 'error');
    }
}

function showModal(title, inputs) {
    return new Promise((resolve) => {
        const modal = document.getElementById('custom-modal');
        const modalTitle = document.getElementById('modal-title');
        const container = document.getElementById('modal-inputs-container');
        const btnOk = document.getElementById('modal-ok');
        const btnCancel = document.getElementById('modal-cancel');
        const btnClose = document.getElementById('modal-close');

        modalTitle.textContent = title;
        container.innerHTML = '';

        inputs.forEach(input => {
            const group = document.createElement('div');
            group.className = 'modal-input-group';

            const label = document.createElement('label');
            label.textContent = input.label;
            group.appendChild(label);

            const el = document.createElement(input.type === 'textarea' ? 'textarea' : 'input');
            el.id = `modal-field-${input.id}`;
            if (input.type !== 'textarea') el.type = input.type;
            el.placeholder = input.placeholder || '';
            el.value = input.value || '';
            if (input.required) el.required = true;

            group.appendChild(el);
            container.appendChild(group);
        });

        const closeModal = (result) => {
            modal.classList.remove('show');
            btnOk.onclick = null;
            btnCancel.onclick = null;
            btnClose.onclick = null;
            resolve(result);
        };

        btnOk.onclick = () => {
            const data = {};
            let valid = true;
            inputs.forEach(input => {
                const el = document.getElementById(`modal-field-${input.id}`);
                if (input.required && !el.value) {
                    el.style.borderColor = 'var(--danger)';
                    valid = false;
                } else {
                    el.style.borderColor = 'var(--border-color)';
                    data[input.id] = el.value;
                }
            });
            if (valid) closeModal(data);
        };

        btnCancel.onclick = () => closeModal(null);
        btnClose.onclick = () => closeModal(null);

        modal.classList.add('show');
    });
}

async function loadAllActiveContracts() {
    const listBody = document.getElementById('all-contracts-list-body');
    if (!listBody) return;
    listBody.innerHTML = '<tr><td colspan="6" class="text-center">Loading...</td></tr>';

    try {
        const response = await authenticatedFetch(`${API_BASE}/contracts/all-active`);
        if (!response.ok) throw new Error('Failed to fetch contracts');

        const versions = await response.json();
        listBody.innerHTML = '';

        if (versions.length === 0) {
            listBody.innerHTML = '<tr><td colspan="6" class="text-center">No active contracts found.</td></tr>';
            return;
        }

        versions.forEach(v => {
            const row = document.createElement('tr');
            row.innerHTML = `
                <td>${v.contract.contractName}</td>
                <td>V${v.versionNumber}</td>
                <td>${v.contract.client.username}</td>
                <td>$${v.contract.contractAmount.toLocaleString()}</td>
                <td>${v.contract.effectiveDate}</td>
                <td><span class="status-badge active">ACTIVE</span></td>
            `;
            listBody.appendChild(row);
        });
    } catch (error) {
        showToast(`Error: ${error.message}`, 'error');
    }
}
