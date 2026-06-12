import { apiClient } from './client';
import { buildScrollParams, ACTIVITY_LOG_PAGE_SIZE } from './pagination';
import type { ActivityLogDto, ScrollResponse } from './types';

let firstPageInflight: Promise<ScrollResponse<ActivityLogDto>> | null = null;

export async function fetchActivityLogs(
    lastActivityLogId?: number,
    size = ACTIVITY_LOG_PAGE_SIZE,
): Promise<ScrollResponse<ActivityLogDto>> {
    if (lastActivityLogId == null) {
        if (!firstPageInflight) {
            firstPageInflight = apiClient
                .get<ScrollResponse<ActivityLogDto>>('/api/activity-logs', {
                    params: buildScrollParams(undefined, 'lastActivityLogId', size),
                })
                .then(res => res.data)
                .finally(() => {
                    firstPageInflight = null;
                });
        }
        return firstPageInflight;
    }

    const { data } = await apiClient.get<ScrollResponse<ActivityLogDto>>('/api/activity-logs', {
        params: buildScrollParams(lastActivityLogId, 'lastActivityLogId', size),
    });
    return data;
}
