import { apiClient } from './client';
import type { NotificationDto } from './types';

export async function fetchNotifications(): Promise<NotificationDto[]> {
    const { data } = await apiClient.get<NotificationDto[]>('/api/notifications');
    return data;
}

export async function confirmNotification(notificationId: number): Promise<void> {
    await apiClient.put(`/api/notifications/${notificationId}/confirm`, {
        isConfirmed: true,
    });
}
