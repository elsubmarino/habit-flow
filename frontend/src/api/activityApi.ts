import { apiClient } from './client';
import { dedupeInFlight } from './inFlight';
import { buildScrollParams, ACTIVITY_LOG_PAGE_SIZE } from './pagination';
import type { ActivityLogDto, ScrollResponse } from './types';

export async function fetchActivityLogs(
    lastActivityLogId?: number,
    size = ACTIVITY_LOG_PAGE_SIZE,
): Promise<ScrollResponse<ActivityLogDto>> {
    if (lastActivityLogId == null) {
        return dedupeInFlight(`activity-logs:first:${size}`, async () => {
            const { data } = await apiClient.get<ScrollResponse<ActivityLogDto>>('/api/activity-logs', {
                params: buildScrollParams(undefined, 'lastActivityLogId', size),
            });
            return data;
        });
    }

    return dedupeInFlight(`activity-logs:${lastActivityLogId}:${size}`, async () => {
        const { data } = await apiClient.get<ScrollResponse<ActivityLogDto>>('/api/activity-logs', {
            params: buildScrollParams(lastActivityLogId, 'lastActivityLogId', size),
        });
        return data;
    });
}
