import { createSlice, createAsyncThunk } from '@reduxjs/toolkit';

export interface AppNotification {
    id: number;
    actor: string;
    action: 'completed' | 'assigned' | 'comment';
    projectId: number | null;
    projectName: string | null;
    taskId: number;
    taskName: string;
    createdAt: string;
    read: boolean;
}

interface NotificationsState {
    items: AppNotification[];
}

const initialState: NotificationsState = { items: [] };

const STORAGE_KEY = 'habitflow.notifications.v1';

function readItems(): AppNotification[] {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) return seedNotifications();
    try {
        return JSON.parse(raw) as AppNotification[];
    } catch {
        return seedNotifications();
    }
}

function writeItems(items: AppNotification[]) {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(items));
}

function seedNotifications(): AppNotification[] {
    const now = Date.now();
    return [
        {
            id: 1,
            actor: 'jay',
            action: 'completed',
            projectId: 1,
            projectName: 'Money',
            taskId: 101,
            taskName: '에버랜드',
            createdAt: new Date(now - 12 * 7 * 24 * 60 * 60 * 1000).toISOString(),
            read: false,
        },
        {
            id: 2,
            actor: 'jay',
            action: 'completed',
            projectId: 1,
            projectName: 'Money',
            taskId: 102,
            taskName: '성수동 B5카본 교체',
            createdAt: new Date(now - 20 * 24 * 60 * 60 * 1000).toISOString(),
            read: true,
        },
        {
            id: 3,
            actor: 'minji',
            action: 'completed',
            projectId: 2,
            projectName: 'Study',
            taskId: 103,
            taskName: '주간 리뷰 정리',
            createdAt: new Date(now - 3 * 24 * 60 * 60 * 1000).toISOString(),
            read: false,
        },
    ];
}

function formatRelativeTime(iso: string): string {
    const diff = Date.now() - new Date(iso).getTime();
    const min = Math.floor(diff / 60000);
    if (min < 60) return '방금 전';
    const hr = Math.floor(min / 60);
    if (hr < 24) return `${hr}시간 전`;
    const day = Math.floor(hr / 24);
    if (day < 7) return `${day}일 전`;
    const week = Math.floor(day / 7);
    if (week < 5) return `${week}주 전`;
    return `${Math.floor(day / 30)}개월 전`;
}

export const fetchNotifications = createAsyncThunk('notifications/fetch', async () => readItems());

export const markAllNotificationsRead = createAsyncThunk('notifications/markAllRead', async () => {
    const items = readItems().map(n => ({ ...n, read: true }));
    writeItems(items);
    return items;
});

export const markNotificationRead = createAsyncThunk(
    'notifications/markOneRead',
    async (id: number) => {
        const items = readItems().map(n => (n.id === id ? { ...n, read: true } : n));
        writeItems(items);
        return items;
    },
);

const notificationsSlice = createSlice({
    name: 'notifications',
    initialState,
    reducers: {},
    extraReducers: builder => {
        builder
            .addCase(fetchNotifications.fulfilled, (state, action) => {
                state.items = action.payload;
            })
            .addCase(markAllNotificationsRead.fulfilled, (state, action) => {
                state.items = action.payload;
            })
            .addCase(markNotificationRead.fulfilled, (state, action) => {
                state.items = action.payload;
            });
    },
});

export function selectUnreadCount(items: AppNotification[]): number {
    return items.filter(n => !n.read).length;
}

export { formatRelativeTime };
export default notificationsSlice.reducer;