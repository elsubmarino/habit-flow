import { apiClient } from './client';
import { dedupeInFlight } from './inFlight';
import { ACTIVITY_LOG_PAGE_SIZE } from './pagination';
import type { EntityId, ActivityLogDto } from './types';
import { parseSlicePage, type PaginatedResult, type SpringSlice } from './slice';
import { normalizeActivityLog, type ActivityLogEntry } from '../utils/activityLogMessages';
import {
    activityLogSearchKey,
    buildActivityLogSearchQuery,
    type ActivityLogSearchParams,
} from './activityLogSearch';

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
    search?: ActivityLogSearchParams,
): Promise<PaginatedResult<ActivityLogEntry>> {
    const searchQuery = buildActivityLogSearchQuery(search);
    const cacheKey = [
        'activity-logs',
        lastActivityLogId ?? 'first',
        size,
        activityLogSearchKey(search),
    ].join(':');

    return dedupeInFlight(cacheKey, async () => {
        const params: Record<string, string | number | string[]> = {
            size,
            ...searchQuery,
        };
        if (lastActivityLogId != null) {
            params.lastActivityLogId = lastActivityLogId;
        }

        const { data } = await apiClient.get<SpringSlice<ActivityLogDto>>('/api/activity-logs', {
            params,
            paramsSerializer: {
                indexes: null,
            },
        });
        return mapActivityLogPage(data);
    });
}

export type { ActivityLogSearchParams };
