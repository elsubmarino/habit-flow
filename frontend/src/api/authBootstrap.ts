import { getStoredAccessToken, getStoredRefreshToken, setStoredTokens } from './client';

function hasStoredSession(): boolean {
    return !!(getStoredAccessToken() || getStoredRefreshToken());
}
import { defaultAppPath } from '../utils/appRoutes';

/** 백엔드 OAuth2LoginSuccessHandler 와 동일한 경로 */
export const OAUTH_CALLBACK_PATH = '/oauth2/redirect';

function readOAuthTokensFromUrl(): { accessToken: string; refreshToken?: string } | null {
    const params = new URLSearchParams(window.location.search);

    const accessToken = params.get('accessToken') ?? params.get('token');
    if (!accessToken) return null;

    const refreshToken = params.get('refreshToken') ?? undefined;
    return { accessToken, refreshToken };
}

/**
 * OAuth 리다이렉트 URL의 token을 useEffect 전에 동기 처리.
 * (useEffect만 쓰면 첫 렌더에서 로그인 화면이 잠깐 뜨거나 루프 유발 가능)
 */
export function bootstrapAuthFromCallback(): boolean {
    const isCallback =
        window.location.pathname === OAUTH_CALLBACK_PATH ||
        window.location.pathname.endsWith(OAUTH_CALLBACK_PATH);

    if (!isCallback) {
        return hasStoredSession();
    }

    const tokens = readOAuthTokensFromUrl();
    if (tokens) {
        setStoredTokens(tokens.accessToken, tokens.refreshToken);
        window.history.replaceState({}, '', defaultAppPath());
        return true;
    }

    return hasStoredSession();
}
