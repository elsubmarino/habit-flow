import type { NavItem } from '../components/Sidebar';
import { OAUTH_CALLBACK_PATH } from '../api/authBootstrap';

/** 브라우저 주소창용 SPA 경로 (백엔드 API와 분리) */
export const APP_ROUTES = {
    inbox: '/inbox',
    today: '/today',
    upcoming: '/upcoming',
    labels: '/labels',
    projects: '/projects',
    report: '/report',
    notifications: '/notifications',
} as const;

/** 예전 SPA/API 미러 경로 (북마크 호환) */
const LEGACY_TASK_LIST_ROUTES: Record<string, NavItem> = {
    '/api/task-instances/inbox': 'inbox',
    '/api/task-instances/today': 'today',
    '/api/task-instances/upcoming': 'upcoming',
    '/api/tasks/inbox': 'inbox',
    '/api/tasks/today': 'today',
    '/api/tasks/upcoming': 'upcoming',
};

const LEGACY_LABELS_BROWSE = new Set(['/api/labels']);
const LEGACY_PROJECTS_BROWSE = new Set(['/api/projects']);
const LEGACY_REPORT = new Set(['/api/activity-logs']);
const LEGACY_NOTIFICATIONS = new Set(['/api/notifications']);

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

function parseLabelId(pathname: string): number | null {
    const match = pathname.match(/^\/labels\/(\d+)$/);
    if (match) return Number(match[1]);
    const legacy = pathname.match(/^\/api\/labels\/(\d+)$/);
    if (legacy) return Number(legacy[1]);
    return null;
}

function parseProjectId(pathname: string): number | null {
    const match = pathname.match(/^\/projects\/(\d+)$/);
    if (match) return Number(match[1]);
    const legacy = pathname.match(/^\/api\/projects\/(\d+)\/tasks$/);
    if (legacy) return Number(legacy[1]);
    return null;
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

    if (pathname === APP_ROUTES.report || LEGACY_REPORT.has(pathname)) {
        return { kind: 'nav', nav: 'report' };
    }
    if (pathname === APP_ROUTES.notifications || LEGACY_NOTIFICATIONS.has(pathname)) {
        return { kind: 'notifications' };
    }
    if (pathname === APP_ROUTES.labels || LEGACY_LABELS_BROWSE.has(pathname)) {
        return { kind: 'labelsBrowse' };
    }
    if (pathname === APP_ROUTES.projects || LEGACY_PROJECTS_BROWSE.has(pathname)) {
        return { kind: 'projectsBrowse' };
    }

    const labelId = parseLabelId(pathname);
    if (labelId != null) {
        return { kind: 'label', labelId };
    }

    const projectId = parseProjectId(pathname);
    if (projectId != null) {
        return { kind: 'project', projectId };
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
        return `/projects/${params.selectedProjectId}`;
    }
    if (params.selectedLabelId != null) {
        return `/labels/${params.selectedLabelId}`;
    }
    if (params.showProjectsBrowse) {
        return APP_ROUTES.projects;
    }
    if (params.activeNav === 'filters') {
        return APP_ROUTES.labels;
    }
    if (params.activeNav === 'report') {
        return APP_ROUTES.report;
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
