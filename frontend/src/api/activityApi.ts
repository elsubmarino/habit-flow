import { apiClient } from './client';
import { buildScrollParams, ACTIVITY_LOG_PAGE_SIZE } from './pagination';
import type { ActivityLogDto, ScrollResponse } from './types';

export async function fetchActivityLogs(
    lastActivityLogId?: number,
    size = ACTIVITY_LOG_PAGE_SIZE,
): Promise<ScrollResponse<ActivityLogDto>> {
    const { data } = await apiClient.get<ScrollResponse<ActivityLogDto>>('/api/activity-logs', {
        params: buildScrollParams(lastActivityLogId, 'lastActivityLogId', size),
    });
    return data;
}
