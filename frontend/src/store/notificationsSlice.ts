import { createSlice, createAsyncThunk } from '@reduxjs/toolkit';
import * as notificationApi from '../api/notificationApi';
import type { ActivityType } from '../api/types';

export interface AppNotification {
    id: number;
    actor: string;
    action: 'completed' | 'assigned' | 'comment' | 'added' | 'moved' | 'deleted';
    projectId: number | null;
    projectName: string | null;
    taskId: number;
    taskName: string;
    createdAt: string;
    read: boolean;
}

interface NotificationsState {
    items: AppNotification[];
    status: 'idle' | 'loading' | 'failed';
}

const initialState: NotificationsState = { items: [], status: 'idle' };

function mapActivityType(type: ActivityType): AppNotification['action'] {
    switch (type) {
        case 'COMPLETED':
            return 'completed';
        case 'ADDED':
            return 'added';
        case 'MOVED':
            return 'moved';
        case 'DELETED':
            return 'deleted';
        case 'UPDATED':
        default:
            return 'comment';
    }
}

function mapNotification(dto: {
    taskId: number;
    activityType: ActivityType;
    isConfirmed?: boolean;
    confirmed?: boolean;
}, index: number): AppNotification {
    const read = dto.confirmed ?? dto.isConfirmed ?? false;
    return {
        id: index + 1,
        actor: '사용자',
        action: mapActivityType(dto.activityType),
        projectId: null,
        projectName: null,
        taskId: dto.taskId,
        taskName: `작업 #${dto.taskId}`,
        createdAt: new Date().toISOString(),
        read,
    };
}

export const fetchNotifications = createAsyncThunk('notifications/fetch', async () => {
    const items = await notificationApi.fetchNotifications();
    return items.map((item, index) => mapNotification(item, index));
});

/** 백엔드 NotificationListResponse에 id가 없어 confirm API를 안전하게 호출할 수 없음 → 로컬 읽음 처리 */
export const markAllNotificationsRead = createAsyncThunk(
    'notifications/markAllRead',
    async (_, { getState }) => {
        const state = getState() as { notifications: NotificationsState };
        return state.notifications.items.map(item => ({ ...item, read: true }));
    },
);

export const markNotificationRead = createAsyncThunk(
    'notifications/markOneRead',
    async (id: number, { getState }) => {
        const state = getState() as { notifications: NotificationsState };
        return state.notifications.items.map(item =>
            item.id === id ? { ...item, read: true } : item,
        );
    },
);

export function selectUnreadCount(items: AppNotification[]): number {
    return items.filter(n => !n.read).length;
}

export function formatRelativeTime(iso: string): string {
    const diff = Date.now() - new Date(iso).getTime();
    const min = Math.floor(diff / 60000);
    if (min < 1) return '방금 전';
    if (min < 60) return `${min}분 전`;
    const hr = Math.floor(min / 60);
    if (hr < 24) return `${hr}시간 전`;
    const day = Math.floor(hr / 24);
    if (day < 7) return `${day}일 전`;
    return `${Math.floor(day / 7)}주 전`;
}

const notificationsSlice = createSlice({
    name: 'notifications',
    initialState,
    reducers: {},
    extraReducers: builder => {
        builder
            .addCase(fetchNotifications.pending, state => {
                state.status = 'loading';
            })
            .addCase(fetchNotifications.fulfilled, (state, action) => {
                state.status = 'idle';
                state.items = action.payload;
            })
            .addCase(fetchNotifications.rejected, state => {
                state.status = 'failed';
                state.items = [];
            })
            .addCase(markAllNotificationsRead.fulfilled, (state, action) => {
                state.items = action.payload;
            })
            .addCase(markNotificationRead.fulfilled, (state, action) => {
                state.items = action.payload;
            });
    },
});

export default notificationsSlice.reducer;
