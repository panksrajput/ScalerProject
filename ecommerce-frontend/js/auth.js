import { API_CONFIG } from './config.js';

// Authentication functions
class Auth {
    static async login(email, password) {
        try {
            console.log('Login attempt with:', { email });
            const url = API_CONFIG.getFullUrl('AUTH', API_CONFIG.ENDPOINTS.LOGIN);
            console.log('Login URL:', url);

            const response = await fetch(url, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Accept': 'application/json'
                },
                body: JSON.stringify({ usernameOrEmail: email, password })
            });

            console.log('Login response status:', response.status);

            if (!response.ok) {
                let errorMessage = 'Login failed';
                try {
                    const errorData = await response.json();
                    errorMessage = errorData.message || errorMessage;
                    console.error('Login error response:', errorData);
                } catch (e) {
                    errorMessage = `HTTP error! status: ${response.status}`;
                }
                throw new Error(errorMessage);
            }

            const data = await response.json();
            console.log('Login successful, user data:', data);

            if (data.accessToken) {
                localStorage.setItem('authToken', data.accessToken);
                if (data.username) {
                    localStorage.setItem('user', JSON.stringify(data.username));
                }
                return data;
            } else {
                throw new Error('No access token received from server');
            }
        } catch (error) {
            console.error('Login error:', error);
            throw error;
        }
    }

    static async signup(email, firstName, lastName, username, password) {
        try {
            const response = await fetch(API_CONFIG.getFullUrl('AUTH', API_CONFIG.ENDPOINTS.SIGNUP), {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Accept': 'application/json'
                },
                body: JSON.stringify({ email, firstName, lastName, username, password })
            });

            if (!response.ok) {
                const error = await response.json();
                throw new Error(error.message || 'Signup failed');
            }

            return await response.json();
        } catch (error) {
            console.error('Signup error:', error);
            throw error;
        }
    }

    static async forgotPassword(email) {
        try {
            const url = `${API_CONFIG.getFullUrl('AUTH', API_CONFIG.ENDPOINTS.FORGOT_PASSWORD)}?email=${encodeURIComponent(email)}`;

            const response = await fetch(url, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Accept': 'application/json'
                },
                body: JSON.stringify({ email })
            });

            if (!response.ok) {
                const error = await response.json();
                throw new Error(error.message || 'Password reset failed');
            }

            return await response.json();
        } catch (error) {
            console.error('Forgot password error:', error);
            throw error;
        }
    }

    static logout() {
        localStorage.removeItem('authToken');
        localStorage.removeItem('user');
        window.location.href = '/';
    }

    static isAuthenticated() {
        return !!localStorage.getItem('authToken');
    }

    static getCurrentUser() {
        const user = localStorage.getItem('user');
        return user ? JSON.parse(user) : null;
    }
}

// Export for use in other modules
export { Auth };