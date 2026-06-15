import { apiClient } from './client';
import { dedupeInFlight } from './inFlight';
import { buildActivityLogCursorParams, ACTIVITY_LOG_PAGE_SIZE } from './pagination';
import type { ActivityLogDto } from './types';
import { parseSlicePage, type PaginatedResult, type SpringSlice } from './slice';

export async function fetchActivityLogs(
    lastActivityLogId?: number,
    size = ACTIVITY_LOG_PAGE_SIZE,
): Promise<PaginatedResult<ActivityLogDto>> {
    if (lastActivityLogId == null) {
        return dedupeInFlight(`activity-logs:first:${size}`, async () => {
            const { data } = await apiClient.get<SpringSlice<ActivityLogDto>>('/api/activity-logs', {
                params: buildActivityLogCursorParams(undefined, size),
            });
            return parseSlicePage(data, log => log.id);
        });
    }

    return dedupeInFlight(`activity-logs:${lastActivityLogId}:${size}`, async () => {
        const { data } = await apiClient.get<SpringSlice<ActivityLogDto>>('/api/activity-logs', {
            params: buildActivityLogCursorParams(lastActivityLogId, size),
        });
        return parseSlicePage(data, log => log.id);
    });
}
