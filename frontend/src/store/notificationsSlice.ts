import { createSlice, createAsyncThunk, type PayloadAction } from '@reduxjs/toolkit';
import * as notificationApi from '../api/notificationApi';
import type { ActivityType, EntityId, NotificationDto, NotificationType } from '../api/types';

export interface AppNotification {
    id: EntityId;
    dedupeKey: string;
    receiverId: EntityId;
    actorId: EntityId;
    actor: string;
    action: 'completed' | 'assigned' | 'comment' | 'added' | 'moved' | 'deleted' | 'invited';
    notificationType: NotificationType;
    targetId: EntityId;
    projectId: EntityId | null;
    taskId: EntityId | null;
    taskName: string;
    customMessage: string | null;
    createdAt: string;
    read: boolean;
}

interface NotificationsState {
    items: AppNotification[];
    status: 'idle' | 'loading' | 'failed';
}

const initialState: NotificationsState = {
    items: [],
    status: 'idle',
};

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
        case 'INVITED':
            return 'invited';
        case 'UPDATED':
        default:
            return 'comment';
    }
}

export function notificationDedupeKey(dto: NotificationDto): string {
    if (dto.id != null) return `id:${dto.id}`;
    return [
        dto.receiverId,
        dto.actorId,
        dto.targetId,
        dto.notificationType,
        dto.activityType,
    ].join(':');
}

function mapNotification(dto: NotificationDto): AppNotification {
    const isProject = dto.notificationType === 'PROJECT';
    const isTask = dto.notificationType === 'TASK';

    return {
        id: dto.id,
        dedupeKey: notificationDedupeKey(dto),
        receiverId: dto.receiverId,
        actorId: dto.actorId,
        actor: dto.actorName,
        action: mapActivityType(dto.activityType),
        notificationType: dto.notificationType,
        targetId: dto.targetId,
        projectId: isProject ? dto.targetId : null,
        taskId: isTask ? dto.targetId : null,
        taskName: isProject
            ? `프로젝트 ${dto.targetId}`
            : isTask
                ? `작업 ${dto.targetId}`
                : '알림',
        customMessage: dto.customMessage?.trim() || null,
        createdAt: dto.createdAt ?? new Date().toISOString(),
        read: dto.isConfirmed,
    };
}

export const fetchNotifications = createAsyncThunk('notifications/fetch', async () => {
    const items = await notificationApi.fetchNotifications();
    return items.map(mapNotification);
});

export const markAllNotificationsRead = createAsyncThunk(
    'notifications/markAllRead',
    async (_, { getState }) => {
        const state = getState() as { notifications: NotificationsState };
        const unread = state.notifications.items.filter(item => !item.read);
        const confirmed = await Promise.all(
            unread.map(item => notificationApi.confirmNotification(item.id).catch(() => undefined)),
        );
        const confirmedIds = new Set(
            confirmed
                .filter((dto): dto is NotificationDto => dto != null && dto.isConfirmed)
                .map(dto => dto.id),
        );
        return state.notifications.items.map(item =>
            confirmedIds.has(item.id) || unread.some(u => u.id === item.id)
                ? { ...item, read: true }
                : item,
        );
    },
);

export const markNotificationRead = createAsyncThunk(
    'notifications/markOneRead',
    async (id: EntityId, { getState }) => {
        const dto = await notificationApi.confirmNotification(id);
        const state = getState() as { notifications: NotificationsState };
        return state.notifications.items.map(item =>
            item.id === id ? { ...item, read: dto.isConfirmed } : item,
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
    reducers: {
        pushNotification(state, action: PayloadAction<NotificationDto>) {
            const key = notificationDedupeKey(action.payload);
            if (state.items.some(item => item.dedupeKey === key)) return;
            state.items.unshift(mapNotification(action.payload));
        },
    },
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

export const { pushNotification } = notificationsSlice.actions;
export default notificationsSlice.reducer;
