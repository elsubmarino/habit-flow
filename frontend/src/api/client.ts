import axios from 'axios';

const TOKEN_KEY = 'habitflow.token';

export const apiClient = axios.create({
    baseURL: '',
    headers: { Accept: 'application/json' },
});

apiClient.interceptors.request.use(config => {
    const token = localStorage.getItem(TOKEN_KEY);
    if (token) {
        config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
});

apiClient.interceptors.response.use(
    response => response,
    error => {
        // OAuth 콜백 직후에는 401로 토큰을 지우지 않음 (루프 방지)
        const onOAuthCallback = window.location.pathname.includes('/oauth2/redirect');
        if (error.response?.status === 401 && !onOAuthCallback) {
            localStorage.removeItem(TOKEN_KEY);
            localStorage.removeItem('habitflow.auth');
        }
        return Promise.reject(error);
    },
);

export function getStoredToken(): string | null {
    return localStorage.getItem(TOKEN_KEY);
}

export function setStoredToken(token: string) {
    localStorage.setItem(TOKEN_KEY, token);
    localStorage.setItem('habitflow.auth', 'true');
}

export function clearStoredToken() {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem('habitflow.auth');
}

export function isAuthenticated(): boolean {
    return !!getStoredToken();
}
