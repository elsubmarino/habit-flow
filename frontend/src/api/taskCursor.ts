import type { PriorityType } from './types';

export type CursorDirection = 'NEXT' | 'PREV';

export interface TaskCursor {
    lastDueDate: string | null;
    lastPriorityType: PriorityType | null;
    lastSortOrder: number | null;
    lastTaskId: number | null;
    direction?: CursorDirection;
}

export interface TaskListSliceDto<T> {
    content?: T[];
    hasNext?: boolean;
    hasPrev?: boolean;
    nextCursor?: TaskCursor | null;
    prevCursor?: TaskCursor | null;
}

export interface TaskPageResult<T> {
    content: T[];
    hasNext: boolean;
    hasPrev: boolean;
    nextCursor: TaskCursor | null;
    prevCursor: TaskCursor | null;
}

export function buildTaskCursorParams(
    cursor: TaskCursor | null | undefined,
    size: number,
    extra?: Record<string, string | number | undefined>,
): Record<string, string | number> {
    const params: Record<string, string | number> = { size, ...extra };

    if (cursor?.lastTaskId == null) {
        return params;
    }

    if (cursor.lastDueDate) {
        params.lastDueDate = cursor.lastDueDate;
    }
    if (cursor.lastPriorityType) {
        params.lastPriorityType = cursor.lastPriorityType;
    }
    if (cursor.lastSortOrder != null) {
        params.lastSortOrder = cursor.lastSortOrder;
    }
    params.lastTaskId = cursor.lastTaskId;
    if (cursor.direction) {
        params.direction = cursor.direction;
    }

    return params;
}
