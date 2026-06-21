import { apiClient } from './client';
import { dedupeInFlight } from './inFlight';
import type { EntityId, NotificationDto } from './types';

export async function fetchNotifications(): Promise<NotificationDto[]> {
    return dedupeInFlight('notifications', async () => {
        const { data } = await apiClient.get<NotificationDto[]>('/api/notifications');
        return data;
    });
}

export async function confirmNotification(notificationId: EntityId): Promise<NotificationDto> {
    const { data } = await apiClient.put<NotificationDto>(`/api/notifications/${notificationId}/confirm`, {
        isConfirmed: true,
        confirmed: true,
    });
    return data;
}
