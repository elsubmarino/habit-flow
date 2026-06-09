import { apiClient } from './client';
import type { ActivityLogDto, ScrollResponse } from './types';

export async function fetchActivityLogs(
    lastActivityLogId?: number,
): Promise<ScrollResponse<ActivityLogDto>> {
    const { data } = await apiClient.get<ScrollResponse<ActivityLogDto>>('/api/activity-logs', {
        params: lastActivityLogId != null ? { lastActivityLogId } : undefined,
    });
    return data;
}
