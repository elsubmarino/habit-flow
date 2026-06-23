import { ensureAccessToken, isAuthenticated, setAccessToken } from './client';
import { defaultAppPath } from '../utils/appRoutes';

/** 백엔드 OAuth2LoginSuccessHandler 와 동일한 경로 */
export const OAUTH_CALLBACK_PATH = '/oauth2/redirect';

function readOAuthAccessTokenFromUrl(): string | null {
    const params = new URLSearchParams(window.location.search);
    return params.get('accessToken') ?? params.get('token');
}

function isOAuthCallbackPath(): boolean {
    return (
        window.location.pathname === OAUTH_CALLBACK_PATH ||
        window.location.pathname.endsWith(OAUTH_CALLBACK_PATH)
    );
}

/**
 * OAuth 리다이렉트 URL의 token을 useEffect 전에 동기 처리.
 * refreshToken은 httpOnly 쿠키, accessToken은 메모리에만 저장.
 */
export function bootstrapAuthFromCallback(): boolean {
    if (!isOAuthCallbackPath()) {
        return isAuthenticated();
    }

    const accessToken = readOAuthAccessTokenFromUrl();
    if (accessToken) {
        setAccessToken(accessToken);
        window.history.replaceState({}, '', defaultAppPath());
        return true;
    }

    return isAuthenticated();
}

/**
 * 앱 기동 시 Silent Refresh — 메모리에 accessToken이 없거나 만료되면
 * httpOnly refreshToken 쿠키로 /api/auth/reissue 호출.
 */
export async function restoreAuthSession(): Promise<boolean> {
    const accessToken = await ensureAccessToken();
    return !!accessToken;
}
