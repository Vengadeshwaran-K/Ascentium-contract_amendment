// API Base URL
const API_BASE = '';

// Check if already logged in
if (localStorage.getItem('authToken')) {
    window.location.href = '/dashboard.html';
}

// Login Form Handler
document.getElementById('login-form').addEventListener('submit', async (e) => {
    e.preventDefault();

    const username = document.getElementById('username').value.trim();
    const password = document.getElementById('password').value;
    const errorBox = document.getElementById('error-message');
    const loginBtn = document.getElementById('login-btn');

    // Clear previous errors
    errorBox.classList.remove('show');

    // Validate inputs
    if (!username || !password) {
        showError('Identity and key required.');
        return;
    }

    // Show loading state
    loginBtn.disabled = true;
    loginBtn.classList.add('btn-loading');

    try {
        const response = await fetch(`${API_BASE}/auth/login`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username, password })
        });

        if (response.ok) {
            const data = await response.json();

            // Secure storage
            localStorage.setItem('authToken', data.token);
            localStorage.setItem('username', username);
            localStorage.setItem('role', data.role);

            // Navigate
            window.location.href = '/dashboard.html';
        } else {
            const errorMsg = await response.text();
            showError(errorMsg || 'Authorization failed. Check credentials.');
            resetButton();
        }
    } catch (error) {
        showError('Authentication node unreachable. Check connectivity.');
        resetButton();
    }
});

function showError(message) {
    const errorBox = document.getElementById('error-message');
    errorBox.textContent = message;
    errorBox.classList.add('show');
}

function resetButton() {
    const loginBtn = document.getElementById('login-btn');
    loginBtn.disabled = false;
    loginBtn.classList.remove('btn-loading');
}

// Enter key support
document.getElementById('password').addEventListener('keypress', (e) => {
    if (e.key === 'Enter') {
        document.getElementById('login-form').dispatchEvent(new Event('submit'));
    }
});
