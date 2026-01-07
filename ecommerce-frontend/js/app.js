import { Cart } from './cart.js';
import { Auth } from './auth.js';
import { showMessage } from './utils.js';
import { Products } from './products.js';

document.addEventListener('DOMContentLoaded', async () => {
    console.log('DOM fully loaded');

    // DOM Elements
    const loginForm = document.getElementById('login-form');
    const signupForm = document.getElementById('signup-form');
    const forgotPasswordForm = document.getElementById('forgot-password-form');
    const showSignupLink = document.getElementById('show-signup');
    const showLoginLink = document.getElementById('show-login');
    const forgotPasswordLink = document.getElementById('forgot-password-link');
    const backToLoginLink = document.getElementById('back-to-login');
    const logoutBtn = document.getElementById('logout-btn');
    const resetPasswordForm = document.getElementById('reset-password-form');

    // Initialize forms safely
    if (loginForm) loginForm.addEventListener('submit', handleLogin);
    if (signupForm) signupForm.addEventListener('submit', handleSignup);
    if (forgotPasswordForm) forgotPasswordForm.addEventListener('submit', handleForgotPassword);
    if (showSignupLink) showSignupLink.addEventListener('click', () => showForm('signup-container'));
    if (showLoginLink) showLoginLink.addEventListener('click', () => showForm('login-container'));
    if (forgotPasswordLink) forgotPasswordLink.addEventListener('click', () => showForm('forgot-password-container'));
    if (backToLoginLink) backToLoginLink.addEventListener('click', () => showForm('login-container'));
    if (logoutBtn) logoutBtn.addEventListener('click', Auth.logout);
    if (resetPasswordForm) resetPasswordForm.addEventListener('submit', handleResetPassword);

    const urlParams = new URLSearchParams(window.location.search);
    const resetToken = urlParams.get('token');
    if (resetToken) {
        console.log('Reset token detected:', resetToken);
        //window.resetPasswordToken = resetToken;
        showForm('reset-password-container');
        return;
    }

    // Initialize app
    await initializeApp();

    // If this is cart.html, render cart automatically
    if (window.location.pathname.endsWith('cart.html')) {
        try {
            await Cart.loadCart();
            await Cart.renderCart();
            Cart.updateCartCount();
        } catch (error) {
            console.error('Failed to load cart on cart.html:', error);
            showMessage('Failed to load cart', 'error');
        }
    }
});

// ------------------------ CORE FUNCTIONS ------------------------

async function initializeApp() {
    try {
        // Show dashboard if authenticated, otherwise login form
        if (Auth.isAuthenticated()) {
            console.log('User is already authenticated');
            await showDashboard();
        } else {
            console.log('User not authenticated, showing login form');
            showForm('login-container');
        }

        setupSearch();
        await initCart();
        console.log('App initialization complete');
    } catch (error) {
        console.error('Error initializing app:', error);
    }
}

function showForm(formId) {
    document.querySelectorAll('.form-container, #dashboard').forEach(el => {
        if (el) el.style.display = 'none';
    });

    const target = document.getElementById(formId);
    if (target) {
        target.style.display = 'block';
    } else {
        console.warn(`showForm: Element with id "${formId}" not found.`);
    }
}

// ------------------------ LOGIN/SIGNUP/FORGOT ------------------------

async function handleLogin(e) {
    e.preventDefault();
    const email = document.getElementById('email')?.value;
    const password = document.getElementById('password')?.value;

    if (!email || !password) {
        showMessage('Email and password are required', 'error');
        return;
    }

    try {
        await Auth.login(email, password);
        await showDashboard();
        showMessage('Login successful!', 'success');
    } catch (error) {
        console.error('Login error:', error);
        showMessage(error.message || 'Login failed. Please try again.', 'error');
    }
}

async function handleSignup(e) {
    e.preventDefault();
    const email = document.getElementById('signup-email')?.value;
    const firstName = document.getElementById('signup-firstname')?.value;
    const lastName = document.getElementById('signup-lastname')?.value;
    const username = document.getElementById('signup-username')?.value;
    const password = document.getElementById('signup-password')?.value;
    const confirmPassword = document.getElementById('confirm-password')?.value;

    if (!email || !firstName || !lastName || !username || !password || !confirmPassword) {
        showMessage('All fields are required', 'error');
        return;
    }

    if (password !== confirmPassword) {
        showMessage('Passwords do not match', 'error');
        return;
    }

    try {
        await Auth.signup(email, firstName, lastName, username, password);
        showForm('login-container');
        showMessage('Signup successful! Please login.', 'success');
    } catch (error) {
        console.error('Signup error:', error);
        showMessage(error.message || 'Signup failed. Please try again.', 'error');
    }
}

async function handleForgotPassword(e) {
    e.preventDefault();
    const email = document.getElementById('forgot-email')?.value;
    if (!email) {
        showMessage('Email is required', 'error');
        return;
    }

    try {
        await Auth.forgotPassword(email);
        showMessage('Password reset instructions sent to your email', 'success');
        showForm('login-container');
    } catch (error) {
        console.error('Forgot password error:', error);
        showMessage(error.message || 'Failed to process your request', 'error');
    }
}

async function handleResetPassword(e) {
    e.preventDefault();
    alert('Hi');
    const newPassword = document.getElementById('new-password')?.value;
    const confirmPassword = document.getElementById('confirm-new-password')?.value;
    const urlParams = new URLSearchParams(window.location.search);
    const token = urlParams.get('token');

    if (!token) {
        showMessage('Invalid or expired reset link', 'error');
        return;
    }

    if (!newPassword || !confirmPassword) {
        showMessage('All fields are required', 'error');
        return;
    }

    if (newPassword !== confirmPassword) {
        showMessage('Passwords do not match', 'error');
        return;
    }

    try {
        const url = new URL(API_CONFIG.getFullUrl('AUTH', '/api/auth/reset-password'));
        url.searchParams.append('token', token);
        url.searchParams.append('newPassword', newPassword);

        await fetch(url.toString(), {
            method: 'POST'
        });

        showMessage('Password reset successful. Please login.', 'success');
        showForm('login-container');
        window.history.replaceState({}, document.title, window.location.pathname);
    } catch (error) {
        console.error(error);
        showMessage('Invalid or expired reset link', 'error');
    }
}

// ------------------------ DASHBOARD & PRODUCTS ------------------------

async function showDashboard() {
    showForm('dashboard');
    const user = Auth.getCurrentUser();

    // User menu
    const userMenu = document.getElementById('user-menu');
    if (userMenu) userMenu.style.display = 'block';

    // Update user icon
    const userIcon = document.querySelector('.user-icon span');
    if (user && user.username && userIcon) userIcon.textContent = user.username.charAt(0).toUpperCase();

    // Cart icon click
    const cartIcon = document.getElementById('cart-icon');
    if (cartIcon) {
        cartIcon.addEventListener('click', async (e) => {
            e.preventDefault();
            if (Auth.isAuthenticated()) {
                try {
                    await Cart.loadCart();
                    window.location.href = 'cart.html';
                } catch (error) {
                    console.error('Error loading cart:', error);
                    showMessage('Failed to load cart', 'error');
                }
            } else {
                showMessage('Please login to view your cart', 'info');
                showForm('login-container');
            }
        });
    }

    // Load products
    try {
        const products = await Products.getAll();
        Products.displayProducts(products);
        await Products.initCategoryFilter();
    } catch (error) {
        console.error('Error loading products:', error);
        showMessage(error.message || 'Failed to load products', 'error');
    }

    // Load cart count
    await initCart();
}

// ------------------------ CART ------------------------

async function initCart() {
    if (Auth.isAuthenticated()) {
        try {
            await Cart.loadCart();
            Cart.updateCartCount();
        } catch (error) {
            console.error('Failed to initialize cart:', error);
        }
    }
}

// ------------------------ SEARCH ------------------------

function setupSearch() {
    const searchButton = document.getElementById('search-button');
    const searchInput = document.getElementById('search-input');

    async function performSearch() {
        if (!searchInput) return;
        const query = searchInput.value.trim();
        try {
            let products = [];
            if (!query) {
                products = await Products.getAll();
            } else {
                const results = await Products.searchProducts(query);
                if (Array.isArray(results)) products = results;
                else if (results.content) products = results.content;
                else if (results.hits?.hits) products = results.hits.hits.map(h => h._source);
                else if (results._embedded?.products) products = results._embedded.products;
            }
            Products.displayProducts(products);
        } catch (error) {
            console.error('Search error:', error);
            showMessage(error.message || 'Error performing search', 'error');
        }
    }

    if (searchButton) searchButton.addEventListener('click', performSearch);
    if (searchInput) searchInput.addEventListener('keypress', (e) => {
        if (e.key === 'Enter') performSearch();
    });
}

// ------------------------ EXPORTS ------------------------
export { showForm, showDashboard };
