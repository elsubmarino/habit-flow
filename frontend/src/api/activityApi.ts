import { apiClient } from './client';
import type { ActivityLogDto } from './types';

export async function fetchActivityLogs(): Promise<ActivityLogDto[]> {
    const { data } = await apiClient.get<ActivityLogDto[]>('/api/activity-logs');
    return data;
}
