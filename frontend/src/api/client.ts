import axios, { type AxiosError, type InternalAxiosRequestConfig } from 'axios';

const ACCESS_TOKEN_KEY = 'habitflow.accessToken';
const REFRESH_TOKEN_KEY = 'habitflow.refreshToken';
const AUTH_FLAG_KEY = 'habitflow.auth';
const LEGACY_TOKEN_KEY = 'habitflow.token';

const AUTH_LOGOUT_EVENT = 'habitflow:logout';

function migrateLegacyToken() {
    const legacy = localStorage.getItem(LEGACY_TOKEN_KEY);
    if (legacy && !localStorage.getItem(ACCESS_TOKEN_KEY)) {
        localStorage.setItem(ACCESS_TOKEN_KEY, legacy);
        localStorage.removeItem(LEGACY_TOKEN_KEY);
    }
}

migrateLegacyToken();

export const apiClient = axios.create({
    baseURL: '',
    headers: { Accept: 'application/json' },
});

type RefreshQueueEntry = {
    resolve: (token: string) => void;
    reject: (error: unknown) => void;
};

let isRefreshing = false;
let refreshQueue: RefreshQueueEntry[] = [];

function flushRefreshQueue(error: unknown | null, token: string | null) {
    refreshQueue.forEach(entry => {
        if (error) entry.reject(error);
        else if (token) entry.resolve(token);
    });
    refreshQueue = [];
}

function isOAuthCallbackPath(): boolean {
    return window.location.pathname.includes('/oauth2/redirect');
}

function shouldSkipTokenRefresh(url?: string): boolean {
    if (!url) return false;
    return (
        url.includes('/api/members/login') ||
        url.includes('/api/members/reissue') ||
        url.includes('/api/members/logout')
    );
}

apiClient.interceptors.request.use(config => {
    const token = getStoredAccessToken();
    if (token) {
        config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
});

apiClient.interceptors.response.use(
    response => response,
    async (error: AxiosError) => {
        const originalRequest = error.config as InternalAxiosRequestConfig & { _retry?: boolean };

        if (
            error.response?.status !== 401 ||
            !originalRequest ||
            originalRequest._retry ||
            shouldSkipTokenRefresh(originalRequest.url) ||
            isOAuthCallbackPath()
        ) {
            return Promise.reject(error);
        }

        const refreshToken = getStoredRefreshToken();
        if (!refreshToken) {
            clearStoredTokens();
            return Promise.reject(error);
        }

        if (isRefreshing) {
            return new Promise<string>((resolve, reject) => {
                refreshQueue.push({ resolve, reject });
            }).then(token => {
                originalRequest.headers.Authorization = `Bearer ${token}`;
                return apiClient(originalRequest);
            });
        }

        originalRequest._retry = true;
        isRefreshing = true;

        try {
            const tokens = await reissueTokensRequest(refreshToken);
            setStoredTokens(tokens.accessToken, tokens.refreshToken);
            flushRefreshQueue(null, tokens.accessToken);
            originalRequest.headers.Authorization = `Bearer ${tokens.accessToken}`;
            return apiClient(originalRequest);
        } catch (refreshError) {
            flushRefreshQueue(refreshError, null);
            clearStoredTokens();
            return Promise.reject(refreshError);
        } finally {
            isRefreshing = false;
        }
    },
);

/** 인터셉터 없이 호출 — refresh 루프 방지 */
async function reissueTokensRequest(
    refreshToken: string,
): Promise<{ accessToken: string; refreshToken: string }> {
    const response = await fetch('/api/members/reissue', {
        method: 'POST',
        headers: {
            Accept: 'application/json',
            'X-Refresh-Token': refreshToken,
        },
    });

    if (!response.ok) {
        throw new Error(`토큰 재발급 실패 (${response.status})`);
    }

    const data = (await response.json()) as {
        accessToken?: string;
        refreshToken?: string;
    };

    if (!data.accessToken) {
        throw new Error('재발급 응답에 accessToken이 없습니다.');
    }

    return {
        accessToken: data.accessToken,
        refreshToken: data.refreshToken ?? refreshToken,
    };
}

export function getStoredAccessToken(): string | null {
    return localStorage.getItem(ACCESS_TOKEN_KEY);
}

export function getStoredRefreshToken(): string | null {
    return localStorage.getItem(REFRESH_TOKEN_KEY);
}

/** @deprecated getStoredAccessToken 사용 */
export function getStoredToken(): string | null {
    return getStoredAccessToken();
}

export function setStoredTokens(accessToken: string, refreshToken?: string | null) {
    localStorage.setItem(ACCESS_TOKEN_KEY, accessToken);
    if (refreshToken) {
        localStorage.setItem(REFRESH_TOKEN_KEY, refreshToken);
    }
    localStorage.setItem(AUTH_FLAG_KEY, 'true');
    localStorage.removeItem(LEGACY_TOKEN_KEY);
}

/** accessToken만 있는 경우 (OAuth 레거시 콜백 등) */
export function setStoredToken(token: string) {
    setStoredTokens(token);
}

export function clearStoredTokens() {
    localStorage.removeItem(ACCESS_TOKEN_KEY);
    localStorage.removeItem(REFRESH_TOKEN_KEY);
    localStorage.removeItem(AUTH_FLAG_KEY);
    localStorage.removeItem(LEGACY_TOKEN_KEY);
    window.dispatchEvent(new CustomEvent(AUTH_LOGOUT_EVENT));
}

/** @deprecated clearStoredTokens 사용 */
export function clearStoredToken() {
    clearStoredTokens();
}

export function isAuthenticated(): boolean {
    return !!(getStoredAccessToken() || getStoredRefreshToken());
}

export function onAuthLogout(listener: () => void): () => void {
    window.addEventListener(AUTH_LOGOUT_EVENT, listener);
    return () => window.removeEventListener(AUTH_LOGOUT_EVENT, listener);
}
