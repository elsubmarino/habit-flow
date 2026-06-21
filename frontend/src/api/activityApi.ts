import { apiClient } from './client';
import { dedupeInFlight } from './inFlight';
import { buildActivityLogCursorParams, ACTIVITY_LOG_PAGE_SIZE } from './pagination';
import type { EntityId, ActivityLogDto } from './types';
import { parseSlicePage, type PaginatedResult, type SpringSlice } from './slice';
import { normalizeActivityLog, type ActivityLogEntry } from '../utils/activityLogMessages';

function mapActivityLogPage(
    data: SpringSlice<ActivityLogDto>,
): PaginatedResult<ActivityLogEntry> {
    const page = parseSlicePage(data, log => log.id);
    return {
        ...page,
        content: page.content.map(normalizeActivityLog),
    };
}

export async function fetchActivityLogs(
    lastActivityLogId?: EntityId,
    size = ACTIVITY_LOG_PAGE_SIZE,
): Promise<PaginatedResult<ActivityLogEntry>> {
    if (lastActivityLogId == null) {
        return dedupeInFlight(`activity-logs:first:${size}`, async () => {
            const { data } = await apiClient.get<SpringSlice<ActivityLogDto>>('/api/activity-logs', {
                params: buildActivityLogCursorParams(undefined, size),
            });
            return mapActivityLogPage(data);
        });
    }

    return dedupeInFlight(`activity-logs:${lastActivityLogId}:${size}`, async () => {
        const { data } = await apiClient.get<SpringSlice<ActivityLogDto>>('/api/activity-logs', {
            params: buildActivityLogCursorParams(lastActivityLogId, size),
        });
        return mapActivityLogPage(data);
    });
}
