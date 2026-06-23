import type { ActivityType, EntityId } from './types';

export interface ActivityLogSearchParams {
    targetType?: 'PROJECT' | 'TASK' | 'COMMENT';
    targetIds?: EntityId[];
    memberIds?: EntityId[];
    activityType?: ActivityType[];
    fromDate?: string | null;
    toDate?: string | null;
}

export function buildActivityLogSearchQuery(
    search?: ActivityLogSearchParams,
): Record<string, string | string[]> {
    if (!search) return {};

    const params: Record<string, string | string[]> = {};

    if (search.targetType) {
        params.targetType = search.targetType;
    }
    if (search.targetIds?.length) {
        params.targetIds = search.targetIds.map(String);
    }
    if (search.memberIds?.length) {
        params.memberIds = search.memberIds.map(String);
    }
    if (search.activityType?.length) {
        params.activityType = search.activityType;
    }
    if (search.fromDate) {
        params.fromDate = search.fromDate;
    }
    if (search.toDate) {
        params.toDate = search.toDate;
    }

    return params;
}

export function activityLogSearchKey(search?: ActivityLogSearchParams): string {
    return JSON.stringify(search ?? {});
}
