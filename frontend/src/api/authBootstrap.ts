import { isAuthenticated, setStoredTokens } from './client';
import { defaultAppPath } from '../utils/appRoutes';

/** 백엔드 OAuth2LoginSuccessHandler 와 동일한 경로 */
export const OAUTH_CALLBACK_PATH = '/oauth2/redirect';

function readOAuthAccessTokenFromUrl(): string | null {
    const params = new URLSearchParams(window.location.search);
    return params.get('accessToken') ?? params.get('token');
}

/**
 * OAuth 리다이렉트 URL의 token을 useEffect 전에 동기 처리.
 * refreshToken은 httpOnly 쿠키로 관리 (URL query는 레거시 호환만 무시).
 */
export function bootstrapAuthFromCallback(): boolean {
    const isCallback =
        window.location.pathname === OAUTH_CALLBACK_PATH ||
        window.location.pathname.endsWith(OAUTH_CALLBACK_PATH);

    if (!isCallback) {
        return isAuthenticated();
    }

    const accessToken = readOAuthAccessTokenFromUrl();
    if (accessToken) {
        setStoredTokens(accessToken);
        window.history.replaceState({}, '', defaultAppPath());
        return true;
    }

    return isAuthenticated();
}
