export const TASK_PAGE_SIZE = 20;
export const LABEL_PAGE_SIZE = 20;
export const ACTIVITY_LOG_PAGE_SIZE = 20;
export const INTEGRATED_SEARCH_PAGE_SIZE = 5;
export const PROJECT_SEARCH_PAGE_SIZE = 20;

export function buildScrollParams(
    cursor: number | undefined,
    cursorKey: 'lastLabelId' | 'lastActivityLogId',
    size: number,
): Record<string, number> {
    const params: Record<string, number> = { size };
    if (cursor != null) {
        params[cursorKey] = cursor;
    }
    return params;
}

export function buildPageParams(size: number, page = 0): Record<string, number> {
    return { page, size };
}
