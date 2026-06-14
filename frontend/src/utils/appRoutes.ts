import type { NavItem } from '../components/Sidebar';
import { OAUTH_CALLBACK_PATH } from '../api/authBootstrap';

/** 브라우저 주소창 경로 — 백엔드 Task 목록 API와 동일한 prefix 사용 */
export const APP_ROUTES = {
    inbox: '/api/tasks/inbox',
    today: '/api/tasks/today',
    upcoming: '/api/tasks/upcoming',
    labels: '/api/labels',
    projects: '/api/projects',
    activityLogs: '/api/activity-logs',
    notifications: '/api/notifications',
} as const;

/** 예전 SPA 경로 (북마크 호환용) */
const LEGACY_TASK_LIST_ROUTES: Record<string, NavItem> = {
    '/api/task-instances/inbox': 'inbox',
    '/api/task-instances/today': 'today',
    '/api/task-instances/upcoming': 'upcoming',
};

export type AppLocation =
    | { kind: 'oauth' }
    | { kind: 'nav'; nav: NavItem }
    | { kind: 'labelsBrowse' }
    | { kind: 'label'; labelId: number }
    | { kind: 'projectsBrowse' }
    | { kind: 'project'; projectId: number }
    | { kind: 'notifications' }
    | { kind: 'unknown' };

export function isOAuthCallbackPath(pathname: string): boolean {
    return pathname === OAUTH_CALLBACK_PATH || pathname.endsWith(OAUTH_CALLBACK_PATH);
}

export function parseAppPath(pathname: string): AppLocation {
    if (isOAuthCallbackPath(pathname)) {
        return { kind: 'oauth' };
    }

    if (pathname === APP_ROUTES.inbox) return { kind: 'nav', nav: 'inbox' };
    if (pathname === APP_ROUTES.today) return { kind: 'nav', nav: 'today' };
    if (pathname === APP_ROUTES.upcoming) return { kind: 'nav', nav: 'upcoming' };

    const legacyNav = LEGACY_TASK_LIST_ROUTES[pathname];
    if (legacyNav) return { kind: 'nav', nav: legacyNav };
    if (pathname === APP_ROUTES.activityLogs) return { kind: 'nav', nav: 'report' };
    if (pathname === APP_ROUTES.notifications) return { kind: 'notifications' };
    if (pathname === APP_ROUTES.labels) return { kind: 'labelsBrowse' };
    if (pathname === APP_ROUTES.projects) return { kind: 'projectsBrowse' };

    const labelMatch = pathname.match(/^\/api\/labels\/(\d+)$/);
    if (labelMatch) {
        return { kind: 'label', labelId: Number(labelMatch[1]) };
    }

    const projectMatch = pathname.match(/^\/api\/projects\/(\d+)\/tasks$/);
    if (projectMatch) {
        return { kind: 'project', projectId: Number(projectMatch[1]) };
    }

    return { kind: 'unknown' };
}

export function buildAppPath(params: {
    activeNav: NavItem;
    showProjectsBrowse: boolean;
    selectedProjectId: number | null;
    selectedLabelId: number | null;
    showNotifications: boolean;
}): string {
    if (params.showNotifications) {
        return APP_ROUTES.notifications;
    }
    if (params.selectedProjectId != null) {
        return `/api/projects/${params.selectedProjectId}/tasks`;
    }
    if (params.selectedLabelId != null) {
        return `/api/labels/${params.selectedLabelId}`;
    }
    if (params.showProjectsBrowse) {
        return APP_ROUTES.projects;
    }
    if (params.activeNav === 'filters') {
        return APP_ROUTES.labels;
    }
    if (params.activeNav === 'report') {
        return APP_ROUTES.activityLogs;
    }
    if (params.activeNav === 'inbox') {
        return APP_ROUTES.inbox;
    }
    if (params.activeNav === 'upcoming') {
        return APP_ROUTES.upcoming;
    }
    return APP_ROUTES.today;
}

export function defaultAppPath(): string {
    return APP_ROUTES.today;
}
