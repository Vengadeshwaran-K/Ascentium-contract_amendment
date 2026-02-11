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
    const errorMessage = document.getElementById('error-message');
    const loginBtn = document.getElementById('login-btn');

    // Clear previous errors
    errorMessage.classList.remove('show');

    // Validate inputs
    if (!username || !password) {
        showError('Please enter both username and password');
        return;
    }

    // Show loading state
    loginBtn.disabled = true;
    loginBtn.classList.add('loading');
    loginBtn.textContent = '';

    try {
        const response = await fetch(`${API_BASE}/auth/login`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({ username, password })
        });

        if (response.ok) {
            const data = await response.json();

            // Store token in localStorage
            localStorage.setItem('authToken', data.token);
            localStorage.setItem('username', username);
            localStorage.setItem('role', data.role);

            // Redirect to dashboard
            window.location.href = '/dashboard.html';
        } else {
            // Handle error responses
            let errorMsg = 'Invalid credentials. Please try again.';

            try {
                const errorData = await response.text();
                if (errorData) {
                    errorMsg = errorData;
                }
            } catch (e) {
                // Use default error message
            }

            showError(errorMsg);
            resetButton();
        }
    } catch (error) {
        showError('Network error. Please check your connection and try again.');
        resetButton();
    }
});

function showError(message) {
    const errorMessage = document.getElementById('error-message');
    errorMessage.textContent = message;
    errorMessage.classList.add('show');
}

function resetButton() {
    const loginBtn = document.getElementById('login-btn');
    loginBtn.disabled = false;
    loginBtn.classList.remove('loading');
    loginBtn.textContent = 'Sign In';
}

// Allow Enter key to submit
document.getElementById('password').addEventListener('keypress', (e) => {
    if (e.key === 'Enter') {
        document.getElementById('login-form').dispatchEvent(new Event('submit'));
    }
});
