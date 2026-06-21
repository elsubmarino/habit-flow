import type { EntityId } from './types';

export const TASK_PAGE_SIZE = 20;
export const LABEL_PAGE_SIZE = 20;
export const ACTIVITY_LOG_PAGE_SIZE = 20;
export const INTEGRATED_SEARCH_PAGE_SIZE = 5;
export const PROJECT_SEARCH_PAGE_SIZE = 20;

/** offset/page 없이 lastLabelId 커서 + size만 전송 */
export function buildLabelCursorParams(
    lastLabelId?: EntityId,
    size = LABEL_PAGE_SIZE,
): Record<string, string | number> {
    const params: Record<string, string | number> = { size };
    if (lastLabelId != null) {
        params.lastLabelId = lastLabelId;
    }
    return params;
}

/** offset/page 없이 lastActivityLogId 커서 + size만 전송 */
export function buildActivityLogCursorParams(
    lastActivityLogId?: EntityId,
    size = ACTIVITY_LOG_PAGE_SIZE,
): Record<string, string | number> {
    const params: Record<string, string | number> = { size };
    if (lastActivityLogId != null) {
        params.lastActivityLogId = lastActivityLogId;
    }
    return params;
}

/** page/offset 기반 목록 (inbox·today·upcoming·project tasks) */
export function buildPageParams(size: number, page = 0): Record<string, number> {
    return { page, size };
}
