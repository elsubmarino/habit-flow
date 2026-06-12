import type { ScrollResponse, TaskDto } from './types';
import { readMasterId } from './mappers';

/** 백엔드 nextCursor가 비어 있을 때 마지막 항목의 TaskMaster ID로 커서 보정 */
export function resolveTaskScrollCursor<T extends TaskDto>(
    page: Pick<ScrollResponse<T>, 'content' | 'hasNext' | 'nextCursor'>,
): { hasNext: boolean; nextCursor: number | null } {
    let nextCursor = page.nextCursor ?? null;
    const hasNext = page.hasNext;

    if ((nextCursor == null || nextCursor <= 0) && hasNext && page.content.length > 0) {
        const last = page.content[page.content.length - 1];
        const fallback = readMasterId(last);
        if (Number.isFinite(fallback) && fallback > 0) {
            nextCursor = fallback;
        }
    }

    return { hasNext, nextCursor };
}
