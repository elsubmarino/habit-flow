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
    localStorage.removeItem(REFRESH_TOKEN_KEY);
}

migrateLegacyToken();

export const apiClient = axios.create({
    baseURL: '',
    headers: { Accept: 'application/json' },
    withCredentials: true,
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

function isPublicAuthRequest(url: string): boolean {
    const path = url.split('?')[0];
    return (
        path.includes('/api/auth/login')
        || path.includes('/api/auth/reissue')
        || path.includes('/api/auth/signup')
        || path.includes('/api/auth/email/')
    );
}

function parseAccessTokenPayload(token: string): Record<string, unknown> | null {
    try {
        const payloadPart = token.split('.')[1];
        if (!payloadPart) return null;
        const normalized = payloadPart.replace(/-/g, '+').replace(/_/g, '/');
        return JSON.parse(atob(normalized)) as Record<string, unknown>;
    } catch {
        return null;
    }
}

function isAccessTokenExpired(token: string, skewMs = ACCESS_TOKEN_SKEW_MS): boolean {
    const payload = parseAccessTokenPayload(token);
    if (!payload || typeof payload.exp !== 'number') return true;
    return payload.exp * 1000 <= Date.now() + skewMs;
}

/** 인터셉터 없이 호출 — refresh 루프 방지. refreshToken은 httpOnly 쿠키로 전송 */
async function reissueTokensRequest(): Promise<string> {
    const response = await fetch('/api/auth/reissue', {
        method: 'POST',
        credentials: 'include',
        headers: {
            Accept: 'application/json',
        },
    });

    if (!response.ok) {
        throw new Error(`토큰 재발급 실패 (${response.status})`);
    }

    const data = (await response.json()) as { accessToken?: string };

    if (!data.accessToken) {
        throw new Error('재발급 응답에 accessToken이 없습니다.');
    }

    return data.accessToken;
}

async function refreshAccessToken(): Promise<string | null> {
    if (!inFlightRefresh) {
        inFlightRefresh = reissueTokensRequest()
            .then(accessToken => {
                setStoredTokens(accessToken);
                return accessToken;
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

/** access 만료 시 쿠키 refreshToken으로 선(reissue) 갱신 */
export async function ensureAccessToken(): Promise<string | null> {
    const accessToken = getStoredAccessToken();
    if (accessToken && !isAccessTokenExpired(accessToken)) {
        return accessToken;
    }
    return refreshAccessToken();
}

apiClient.interceptors.request.use(async config => {
    const url = config.url ?? '';
    if (isPublicAuthRequest(url)) {
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
            isPublicAuthRequest(originalRequest.url ?? '') ||
            isOAuthCallbackPath()
        ) {
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

/** accessToken JWT의 memberId 클레임 (회원 API에 id가 없을 때 사용) */
export function getLoggedInMemberId(): import('./types').EntityId | null {
    const token = getStoredAccessToken();
    if (!token) return null;

    const payload = parseAccessTokenPayload(token);
    const memberId = payload?.memberId;
    if (typeof memberId === 'string' && memberId.trim() !== '') {
        return memberId;
    }
    if (typeof memberId === 'number' && Number.isFinite(memberId)) {
        return String(memberId);
    }
    return null;
}

/** @deprecated refreshToken은 httpOnly 쿠키로 관리됩니다 */
export function getStoredRefreshToken(): null {
    return null;
}

/** @deprecated getStoredAccessToken 사용 */
export function getStoredToken(): string | null {
    return getStoredAccessToken();
}

export function setStoredTokens(accessToken: string) {
    localStorage.setItem(ACCESS_TOKEN_KEY, accessToken);
    localStorage.setItem(AUTH_FLAG_KEY, 'true');
    localStorage.removeItem(REFRESH_TOKEN_KEY);
    localStorage.removeItem(LEGACY_TOKEN_KEY);
}

/** accessToken만 있는 경우 (OAuth 콜백 등) */
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
    if (localStorage.getItem(AUTH_FLAG_KEY) === 'true') {
        return true;
    }
    const accessToken = getStoredAccessToken();
    return !!accessToken && !isAccessTokenExpired(accessToken);
}

export function onAuthLogout(listener: () => void): () => void {
    window.addEventListener(AUTH_LOGOUT_EVENT, listener);
    return () => window.removeEventListener(AUTH_LOGOUT_EVENT, listener);
}
