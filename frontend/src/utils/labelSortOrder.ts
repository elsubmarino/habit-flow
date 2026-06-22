import type { EntityId } from '../api/types';
import type { Label } from '../store/habitSlice';
import { reorderList } from './taskSortOrder';

function fallbackSortOrder(index: number): number {
    return (index + 1) * 1000;
}

export function readLabelSortOrder(label: Label, index: number): number {
    return label.sortOrder ?? fallbackSortOrder(index);
}

export function computeLabelSortOrderAfterMove(items: Label[], toIndex: number): number {
    const prev = items[toIndex - 1];
    const next = items[toIndex + 1];

    if (!prev && !next) {
        return fallbackSortOrder(0);
    }
    if (!prev) {
        const nextOrder = readLabelSortOrder(next!, toIndex + 1);
        return nextOrder - 500;
    }
    if (!next) {
        const prevOrder = readLabelSortOrder(prev, toIndex - 1);
        return prevOrder + 500;
    }

    const prevOrder = readLabelSortOrder(prev, toIndex - 1);
    const nextOrder = readLabelSortOrder(next, toIndex + 1);
    if (nextOrder > prevOrder + 1) {
        return Math.floor((prevOrder + nextOrder) / 2);
    }
    return prevOrder + 1;
}

export function sortLabelsByOrder(labels: Label[]): Label[] {
    return [...labels].sort((a, b) => {
        const orderDiff = (a.sortOrder ?? 0) - (b.sortOrder ?? 0);
        if (orderDiff !== 0) return orderDiff;
        return String(b.id).localeCompare(String(a.id));
    });
}

export interface ReorderLabelRequest {
    labelId: EntityId;
    fromIndex: number;
    toIndex: number;
    sortOrder: number;
    contextList: Label[];
}

export function applyLabelReorder(labels: Label[], request: ReorderLabelRequest): Label[] {
    const reordered = reorderList(request.contextList, request.fromIndex, request.toIndex).map(
        (label, index) =>
            index === request.toIndex ? { ...label, sortOrder: request.sortOrder } : label,
    );
    const visibleIds = new Set(reordered.map(label => label.id));
    const queue = [...reordered];
    const merged = labels.map(label => (visibleIds.has(label.id) ? queue.shift()! : label));
    return sortLabelsByOrder(merged);
}
