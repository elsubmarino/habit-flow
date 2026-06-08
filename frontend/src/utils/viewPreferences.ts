import type { Habit } from '../store/habitSlice';

export type ViewLayout = 'list' | 'board' | 'calendar';
export type ViewGrouping = 'none' | 'date' | 'project' | 'priority' | 'label';
export type ViewSorting = 'smart' | 'date' | 'priority' | 'name';
export type FilterAssignee = 'me-unassigned' | 'all';
export type FilterPriority = 'all' | 1 | 2 | 3 | 4;
export type FilterLabel = 'all' | number;

export interface ViewPreferences {
    layout: ViewLayout;
    showCompleted: boolean;
    grouping: ViewGrouping;
    sorting: ViewSorting;
    filterAssignee: FilterAssignee;
    filterPriority: FilterPriority;
    filterLabel: FilterLabel;
}

const STORAGE_KEY = 'habitflow.viewPreferences.v1';

export const DEFAULT_VIEW_PREFERENCES: ViewPreferences = {
    layout: 'list',
    showCompleted: true,
    grouping: 'none',
    sorting: 'smart',
    filterAssignee: 'me-unassigned',
    filterPriority: 'all',
    filterLabel: 'all',
};

export function loadViewPreferences(): ViewPreferences {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) return DEFAULT_VIEW_PREFERENCES;
    try {
        return { ...DEFAULT_VIEW_PREFERENCES, ...JSON.parse(raw) as Partial<ViewPreferences> };
    } catch {
        return DEFAULT_VIEW_PREFERENCES;
    }
}

export function saveViewPreferences(prefs: ViewPreferences) {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(prefs));
}

function priorityRank(p: number): number {
    return p;
}

function compareHabits(a: Habit, b: Habit, sorting: ViewSorting): number {
    switch (sorting) {
        case 'date': {
            const da = a.dueDate ?? '9999-99-99';
            const db = b.dueDate ?? '9999-99-99';
            return da.localeCompare(db) || a.name.localeCompare(b.name, 'ko');
        }
        case 'priority':
            return priorityRank(a.priority) - priorityRank(b.priority) || a.name.localeCompare(b.name, 'ko');
        case 'name':
            return a.name.localeCompare(b.name, 'ko');
        case 'smart':
        default: {
            if (a.completedToday !== b.completedToday) {
                return a.completedToday ? 1 : -1;
            }
            const pr = priorityRank(a.priority) - priorityRank(b.priority);
            if (pr !== 0) return pr;
            const da = a.dueDate ?? '9999-99-99';
            const db = b.dueDate ?? '9999-99-99';
            return da.localeCompare(db) || a.name.localeCompare(b.name, 'ko');
        }
    }
}

export function filterHabits(habits: Habit[], prefs: ViewPreferences): Habit[] {
    let result = [...habits];
    if (prefs.filterPriority !== 'all') {
        result = result.filter(h => h.priority === prefs.filterPriority);
    }
    if (prefs.filterLabel !== 'all') {
        result = result.filter(h => h.labels.some(l => l.id === prefs.filterLabel));
    }
    return result.sort((a, b) => compareHabits(a, b, prefs.sorting));
}

export function groupHabits(
    habits: Habit[],
    grouping: ViewGrouping,
): { key: string; title: string; habits: Habit[] }[] {
    if (grouping === 'none') {
        return [{ key: 'all', title: '', habits }];
    }

    const buckets = new Map<string, Habit[]>();
    const titles = new Map<string, string>();

    for (const habit of habits) {
        let key: string;
        let title: string;
        switch (grouping) {
            case 'date':
                key = habit.dueDate ?? 'none';
                title = key === 'none' ? '날짜 없음' : key;
                break;
            case 'project':
                key = habit.projectId != null ? String(habit.projectId) : 'none';
                title = habit.projectName ?? '관리함';
                break;
            case 'priority':
                key = String(habit.priority);
                title = `우선순위 P${habit.priority}`;
                break;
            case 'label': {
                if (habit.labels.length === 0) {
                    key = 'none';
                    title = '라벨 없음';
                } else {
                    key = String(habit.labels[0].id);
                    title = habit.labels[0].name;
                }
                break;
            }
            default:
                key = 'all';
                title = '';
        }
        if (!buckets.has(key)) buckets.set(key, []);
        buckets.get(key)!.push(habit);
        titles.set(key, title);
    }

    return [...buckets.entries()]
        .sort(([a], [b]) => a.localeCompare(b))
        .map(([key, groupHabits]) => ({
            key,
            title: titles.get(key) ?? key,
            habits: groupHabits,
        }));
}
