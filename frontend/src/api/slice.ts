/** Spring Data Slice JSON (ScrollResponse 대체) */
export interface SpringSlice<T> {
    content?: T[];
    hasNext?: boolean;
    last?: boolean;
}

/** 앱 내부 페이지네이션 결과 (기존 ScrollResponse와 동일한 형태) */
export interface PaginatedResult<T> {
    content: T[];
    hasNext: boolean;
    nextCursor: number | null;
}

export function parseSlicePage<T>(
    slice: SpringSlice<T>,
    getCursorId?: (item: T) => number | null | undefined,
): PaginatedResult<T> {
    const content = Array.isArray(slice.content) ? slice.content : [];
    const hasNext = slice.hasNext ?? (slice.last != null ? !slice.last : false);

    let nextCursor: number | null = null;
    if (hasNext && content.length > 0 && getCursorId) {
        const id = getCursorId(content[content.length - 1]!);
        nextCursor = id ?? null;
    }

    return { content, hasNext, nextCursor };
}
