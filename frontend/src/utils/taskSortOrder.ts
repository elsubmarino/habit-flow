import type { EntityId } from '../api/types';
import type { Habit } from '../store/habitSlice';

export function reorderList<T>(items: T[], fromIndex: number, toIndex: number): T[] {
    if (fromIndex === toIndex) return [...items];
    const next = [...items];
    const [moved] = next.splice(fromIndex, 1);
    next.splice(toIndex, 0, moved);
    return next;
}

function fallbackSortOrder(index: number): number {
    return (index + 1) * 1000;
}

export function readHabitSortOrder(habit: Habit, index: number): number {
    return habit.sortOrder ?? fallbackSortOrder(index);
}

/** 드롭 위치 기준 새 sortOrder 계산 */
export function computeSortOrderAfterMove(items: Habit[], toIndex: number): number {
    const prev = items[toIndex - 1];
    const next = items[toIndex + 1];

    if (!prev && !next) {
        return fallbackSortOrder(0);
    }
    if (!prev) {
        const nextOrder = readHabitSortOrder(next!, toIndex + 1);
        return nextOrder - 500;
    }
    if (!next) {
        const prevOrder = readHabitSortOrder(prev, toIndex - 1);
        return prevOrder + 500;
    }

    const prevOrder = readHabitSortOrder(prev, toIndex - 1);
    const nextOrder = readHabitSortOrder(next, toIndex + 1);
    if (nextOrder > prevOrder + 1) {
        return Math.floor((prevOrder + nextOrder) / 2);
    }
    return prevOrder + 1;
}

/** 화면에 보이는 순서만 바꾼 뒤 전체 목록에 반영 */
export function mergeVisibleOrder(fullList: Habit[], visibleOrdered: Habit[]): Habit[] {
    const visibleIds = new Set(visibleOrdered.map(habit => habit.id));
    const queue = [...visibleOrdered];
    return fullList.map(habit => (visibleIds.has(habit.id) ? queue.shift()! : habit));
}

export interface ReorderHabitRequest {
    habitId: EntityId;
    fromIndex: number;
    toIndex: number;
    sortOrder: number;
    contextList: Habit[];
}
