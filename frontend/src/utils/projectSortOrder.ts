import type { EntityId } from '../api/types';
import type { Project } from '../store/habitSlice';
import { reorderList } from './taskSortOrder';

function fallbackSortOrder(index: number): number {
    return (index + 1) * 1000;
}

export function readProjectSortOrder(project: Project, index: number): number {
    return project.sortOrder ?? fallbackSortOrder(index);
}

export function computeProjectSortOrderAfterMove(items: Project[], toIndex: number): number {
    const prev = items[toIndex - 1];
    const next = items[toIndex + 1];

    if (!prev && !next) {
        return fallbackSortOrder(0);
    }
    if (!prev) {
        const nextOrder = readProjectSortOrder(next!, toIndex + 1);
        return nextOrder - 500;
    }
    if (!next) {
        const prevOrder = readProjectSortOrder(prev, toIndex - 1);
        return prevOrder + 500;
    }

    const prevOrder = readProjectSortOrder(prev, toIndex - 1);
    const nextOrder = readProjectSortOrder(next, toIndex + 1);
    if (nextOrder > prevOrder + 1) {
        return Math.floor((prevOrder + nextOrder) / 2);
    }
    return prevOrder + 1;
}

export function sortProjectsByOrder(projects: Project[]): Project[] {
    return [...projects].sort((a, b) => {
        const orderDiff = (a.sortOrder ?? 0) - (b.sortOrder ?? 0);
        if (orderDiff !== 0) return orderDiff;
        return String(a.id).localeCompare(String(b.id));
    });
}

export interface ReorderProjectRequest {
    projectId: EntityId;
    fromIndex: number;
    toIndex: number;
    sortOrder: number;
    contextList: Project[];
}

export function applyProjectReorder(projects: Project[], request: ReorderProjectRequest): Project[] {
    const reordered = reorderList(request.contextList, request.fromIndex, request.toIndex).map(
        (project, index) =>
            index === request.toIndex ? { ...project, sortOrder: request.sortOrder } : project,
    );
    const visibleIds = new Set(reordered.map(project => project.id));
    const queue = [...reordered];
    const merged = projects.map(project => (visibleIds.has(project.id) ? queue.shift()! : project));
    return sortProjectsByOrder(merged);
}
