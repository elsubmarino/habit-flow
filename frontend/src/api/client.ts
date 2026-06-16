import axios, { type AxiosError, type InternalAxiosRequestConfig } from 'axios';

const ACCESS_TOKEN_KEY = 'habitflow.accessToken';
const REFRESH_TOKEN_KEY = 'habitflow.refreshToken';
const AUTH_FLAG_KEY = 'habitflow.auth';
const LEGACY_TOKEN_KEY = 'habitflow.token';

const AUTH_LOGOUT_EVENT = 'habitflow:logout';
const ACCESS_TOKEN_SKEW_MS = 30_000;

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
let inFlightRefresh: Promise<string | null> | null = null;

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

function isPublicAuthRequest(url: string, method?: string): boolean {
    const path = url.split('?')[0];
    if (path.includes('/api/members/login')) return true;
    if (path.includes('/api/members/reissue')) return true;
    if (method?.toUpperCase() === 'POST' && /\/api\/members\/?$/.test(path)) return true;
    return false;
}

function isAccessTokenExpired(token: string, skewMs = ACCESS_TOKEN_SKEW_MS): boolean {
    try {
        const payloadPart = token.split('.')[1];
        if (!payloadPart) return true;
        const normalized = payloadPart.replace(/-/g, '+').replace(/_/g, '/');
        const payload = JSON.parse(atob(normalized)) as { exp?: number };
        if (typeof payload.exp !== 'number') return false;
        return payload.exp * 1000 <= Date.now() + skewMs;
    } catch {
        return true;
    }
}

/** 인터셉터 없이 호출 — refresh 루프 방지 */
async function reissueTokensRequest(
    refreshToken: string,
): Promise<{ accessToken: string; refreshToken: string }> {
    const response = await fetch('/api/members/reissue', {
        method: 'POST',
        credentials: 'include',
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
    if (!data.refreshToken && import.meta.env.DEV) {
        console.warn(
            '[auth] reissue 응답에 refreshToken이 없습니다. RTR 적용 시 두 번째 재발급부터 실패할 수 있습니다.',
        );
    }

    return {
        accessToken: data.accessToken,
        refreshToken: data.refreshToken ?? refreshToken,
    };
}

async function refreshAccessToken(): Promise<string | null> {
    const refreshToken = getStoredRefreshToken();
    if (!refreshToken) {
        clearStoredTokens();
        return null;
    }

    if (!inFlightRefresh) {
        inFlightRefresh = reissueTokensRequest(refreshToken)
            .then(tokens => {
                setStoredTokens(tokens.accessToken, tokens.refreshToken);
                return tokens.accessToken;
            })
            .catch(error => {
                clearStoredTokens();
                throw error;
            })
            .finally(() => {
                inFlightRefresh = null;
            });
    }

    try {
        return await inFlightRefresh;
    } catch {
        return null;
    }
}

/** access 만료 시 refreshToken으로 선(reissue) 갱신 */
export async function ensureAccessToken(): Promise<string | null> {
    const accessToken = getStoredAccessToken();
    if (accessToken && !isAccessTokenExpired(accessToken)) {
        return accessToken;
    }
    return refreshAccessToken();
}

apiClient.interceptors.request.use(async config => {
    const url = config.url ?? '';
    if (isPublicAuthRequest(url, config.method)) {
        delete config.headers.Authorization;
        return config;
    }

    const token = await ensureAccessToken();
    if (token) {
        config.headers.Authorization = `Bearer ${token}`;
    } else {
        delete config.headers.Authorization;
    }
    return config;
});

apiClient.interceptors.response.use(
    response => response,
    async (error: AxiosError) => {
        const originalRequest = error.config as InternalAxiosRequestConfig & { _retry?: boolean };
        const status = error.response?.status;

        if (
            (status !== 401 && status !== 403) ||
            !originalRequest ||
            originalRequest._retry ||
            isPublicAuthRequest(originalRequest.url ?? '', originalRequest.method) ||
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
            const accessToken = await refreshAccessToken();
            if (!accessToken) {
                throw new Error('토큰 재발급에 실패했습니다.');
            }
            flushRefreshQueue(null, accessToken);
            originalRequest.headers.Authorization = `Bearer ${accessToken}`;
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
