import axios from 'axios';

const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080';

const api = axios.create({
    baseURL: API_BASE_URL,
    withCredentials: true,
});

// Request Interceptor (Adds the token to every request)
api.interceptors.request.use((config) => {
    const token = localStorage.getItem('golf_token');
    if (token) {
        config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
});

// Response Interceptor (The "Security Guard")
api.interceptors.response.use(
    (response) => response, // If the request is successful, do nothing
    (error) => {
        // If the server returns 401 (Unauthorized) or 403 (Forbidden)
        if (error.response && (error.response.status === 401 || error.response.status === 403)) {
            console.warn("Session expired or unauthorized. Logging out...");

            localStorage.removeItem('golf_token');

            // Force a reload to the login page
            window.location.href = '/login';
        }
        return Promise.reject(error);
    }
);

export default api;