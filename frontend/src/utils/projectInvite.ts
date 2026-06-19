const PENDING_INVITE_TOKEN_KEY = 'habitflow.pendingInviteToken';

export const PROJECT_INVITE_PATH = '/projects/invite';

export function isProjectInvitePath(pathname: string): boolean {
    return pathname === PROJECT_INVITE_PATH || pathname.endsWith(PROJECT_INVITE_PATH);
}

/** 메일 링크 쿼리: token 또는 tokens (백엔드 메일 템플릿 호환) */
export function getProjectInviteTokenFromLocation(search = window.location.search): string | null {
    const params = new URLSearchParams(search);
    return params.get('token');
}

export function savePendingInviteToken(token: string) {
    sessionStorage.setItem(PENDING_INVITE_TOKEN_KEY, token);
}

export function peekPendingInviteToken(): string | null {
    return sessionStorage.getItem(PENDING_INVITE_TOKEN_KEY);
}

export function consumePendingInviteToken(): string | null {
    const token = sessionStorage.getItem(PENDING_INVITE_TOKEN_KEY);
    if (token) {
        sessionStorage.removeItem(PENDING_INVITE_TOKEN_KEY);
    }
    return token;
}

export function clearPendingInviteToken() {
    sessionStorage.removeItem(PENDING_INVITE_TOKEN_KEY);
}
