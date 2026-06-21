import type { EntityId } from '../api/types';
import { getUserProfile } from './userProfile';
import { toISODate } from './date';

export type ActivityType =
    | 'added'
    | 'completed'
    | 'uncompleted'
    | 'deleted'
    | 'moved'
    | 'date_changed';

export interface ActivityEntry {
    id: EntityId;
    actor: string;
    actorColor: string;
    type: ActivityType;
    taskId: EntityId;
    taskName: string;
    projectId: EntityId | null;
    projectName: string | null;
    projectColor: string | null;
    createdAt: string;
    meta?: string;
}

const STORAGE_KEY = 'habitflow.activity.v1';
const MAX_ENTRIES = 500;

function readRaw(): ActivityEntry[] {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) return seedActivities();
    try {
        const parsed = JSON.parse(raw) as ActivityEntry[];
        return parsed.length > 0 ? parsed : seedActivities();
    } catch {
        return seedActivities();
    }
}

function writeRaw(items: ActivityEntry[]) {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(items.slice(0, MAX_ENTRIES)));
}

function seedActivities(): ActivityEntry[] {
    const now = Date.now();
    const profile = getUserProfile();
    const items: ActivityEntry[] = [
        {
            id: String(now - 1),
            actor: profile.displayName,
            actorColor: '#4073ff',
            type: 'added',
            taskId: '901',
            taskName: '포인트 환전',
            projectId: '1',
            projectName: 'Money',
            projectColor: '#4073ff',
            createdAt: new Date(now - 37 * 60 * 1000).toISOString(),
        },
        {
            id: String(now - 2),
            actor: profile.displayName,
            actorColor: '#299438',
            type: 'completed',
            taskId: '902',
            taskName: '만보 걷기',
            projectId: null,
            projectName: '관리함',
            projectColor: '#808080',
            createdAt: new Date(now - 3 * 60 * 60 * 1000).toISOString(),
        },
        {
            id: String(now - 3),
            actor: 'minji',
            actorColor: '#db4c3f',
            type: 'moved',
            taskId: '903',
            taskName: 'Fitness Set',
            projectId: '2',
            projectName: 'Study',
            projectColor: '#299438',
            createdAt: new Date(now - 5 * 60 * 60 * 1000).toISOString(),
        },
        {
            id: String(now - 4),
            actor: profile.displayName,
            actorColor: '#4073ff',
            type: 'date_changed',
            taskId: '904',
            taskName: 'Good Morning set',
            projectId: '1',
            projectName: 'Money',
            projectColor: '#4073ff',
            createdAt: new Date(now - 8 * 60 * 60 * 1000).toISOString(),
            meta: '6월 2일',
        },
    ];
    writeRaw(items);
    return items;
}

export function readActivities(): ActivityEntry[] {
    return readRaw().sort(
        (a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime(),
    );
}

export function logActivity(
    entry: Omit<ActivityEntry, 'id' | 'createdAt' | 'actor' | 'actorColor'> & {
        id?: EntityId;
        createdAt?: string;
        actor?: string;
        actorColor?: string;
    },
) {
    const profile = getUserProfile();
    const items = readRaw();
    items.unshift({
        id: entry.id ?? String(Date.now()),
        actor: entry.actor ?? profile.displayName,
        actorColor: entry.actorColor ?? '#4073ff',
        createdAt: entry.createdAt ?? new Date().toISOString(),
        type: entry.type,
        taskId: entry.taskId,
        taskName: entry.taskName,
        projectId: entry.projectId,
        projectName: entry.projectName,
        projectColor: entry.projectColor,
        meta: entry.meta,
    });
    writeRaw(items);
    window.dispatchEvent(new CustomEvent('habitflow:activity'));
}

export function formatActivityRelativeTime(iso: string): string {
    const diff = Date.now() - new Date(iso).getTime();
    const min = Math.floor(diff / 60000);
    if (min < 1) return '방금 전에';
    if (min < 60) return `${min}분 전에`;
    const hr = Math.floor(min / 60);
    if (hr < 24) return `${hr}시간 전에`;
    const day = Math.floor(hr / 24);
    if (day < 7) return `${day}일 전에`;
    return `${Math.floor(day / 7)}주 전에`;
}

export function activityDateKey(iso: string): string {
    return iso.slice(0, 10);
}

export function formatActivityDateHeader(dateKey: string, count: number): string {
    const date = new Date(dateKey + 'T00:00:00');
    const today = toISODate(new Date());
    const datePart = date.toLocaleDateString('ko-KR', { month: 'long', day: 'numeric' });
    const weekday = date.toLocaleDateString('ko-KR', { weekday: 'long' });
    const todayPart = dateKey === today ? ' · 오늘' : '';
    return `${datePart}${todayPart} · ${weekday} · ${count}`;
}

export function activityMessage(entry: ActivityEntry): string {
    const task = entry.taskName;
    switch (entry.type) {
        case 'added':
            return `${entry.actor}님이 ${task}을(를) 추가했습니다`;
        case 'completed':
            return `${entry.actor}님이 ${task}을(를) 완료했습니다`;
        case 'uncompleted':
            return `${entry.actor}님이 ${task} 완료를 취소했습니다`;
        case 'deleted':
            return `${entry.actor}님이 ${task}을(를) 삭제했습니다`;
        case 'moved':
            return `${entry.actor}님이 ${task}을(를) 이동시켰습니다`;
        case 'date_changed':
            return `${entry.actor}님이 ${task}의 날짜를 ${entry.meta ?? ''}(으)로 변경했습니다`;
        default:
            return `${entry.actor}님이 ${task}을(를) 수정했습니다`;
    }
}

export function activityBadge(type: ActivityType): string {
    switch (type) {
        case 'added':
            return '+';
        case 'completed':
            return '✓';
        case 'uncompleted':
            return '○';
        case 'deleted':
            return '−';
        case 'moved':
        case 'date_changed':
        default:
            return '↻';
    }
}
