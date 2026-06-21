import { createSlice, createAsyncThunk } from '@reduxjs/toolkit';
import type { NavItem } from '../components/Sidebar';
import {
    applyFavoriteFlags,
    normalizeFavoriteDto,
    upsertFavoriteFromLabel,
    upsertFavoriteFromProject,
} from '../api/favoriteMappers';
import * as favoriteApi from '../api/favoriteApi';
import * as labelApi from '../api/labelApi';
import * as projectApi from '../api/projectApi';
import { getApiErrorMessage } from '../api/apiError';
import * as taskApi from '../api/taskApi';
import type { TaskCursor } from '../api/taskApi';
import * as commentApi from '../api/commentApi';
import {
    mapLabel,
    mapProject,
    mapTaskToHabit,
    mergeHabitFromTaskMutation,
    priorityToApi,
    readCompleted,
    repeatLabelToRecurrence,
} from '../api/mappers';
import type { FavoriteDto, ProjectDetailDto, TaskDto, EntityId } from '../api/types';
import { parseProjectAccessType, parseProjectLayoutType } from '../api/projectMappers';
import { habitRouteStateFromPath } from '../utils/appRoutes';
import {
    getUpcomingWeekRange,
    toISODate,
} from '../utils/date';
import {
    mergeVisibleOrder,
    reorderList,
} from '../utils/taskSortOrder';
import type { ReorderHabitRequest } from '../utils/taskSortOrder';

export interface Label {
    id: EntityId;
    name: string;
    color: string;
    taskCount: number;
    favorite: boolean;
}

export interface Attachment {
    id: number;
    originalFileName: string;
    contentType: string | null;
    fileSize: number;
    downloadUrl: string;
}

export interface Subtask {
    id: EntityId;
    name: string;
    description: string;
    completed: boolean;
    childCount: number;
}

export interface CommentItem {
    id: number;
    backendId?: EntityId;
    text: string;
    createdAt: string;
    attachments: Attachment[];
}

export function habitRowKey(habit: Habit): string {
    return String(habit.id);
}

export interface Habit {
    id: EntityId;
    name: string;
    description: string;
    streak: number;
    lastCompletedDate: string | null;
    dueDate: string | null;
    dueTime: string | null;
    dueTime24: string | null;
    hasTime: boolean;
    parentId: EntityId | null;
    userName: string | null;
    projectId: EntityId | null;
    projectName: string | null;
    projectColor: string | null;
    completedToday: boolean;
    priority: 1 | 2 | 3 | 4;
    labels: Label[];
    attachments: Attachment[];
    reminders: string[];
    subtasks: Subtask[];
    comments: CommentItem[];
    subtaskCount: number;
    subtaskCompletedCount: number;
    commentCount: number;
    sortOrder: number;
    isRecurring: boolean;
    recurrenceLabel: string | null;
}

export function attachmentDownloadUrl(path: string): string {
    if (path.startsWith('http')) return path;
    return path.startsWith('/') ? path : `/${path}`;
}

export interface Project {
    id: EntityId;
    name: string;
    color: string;
    sortOrder: number;
    taskCount: number;
    favorite: boolean;
}

export interface ProjectDetail extends Project {
    parentId: EntityId | null;
    parentName: string | null;
    accessType: 'PRIVATE' | 'PUBLIC';
    layoutType: 'LIST' | 'BOARD';
}

function mapProjectDetail(dto: ProjectDetailDto): ProjectDetail {
    return {
        ...mapProject(dto, dto.taskCount ?? 0),
        parentId: dto.parentId ?? null,
        parentName: dto.parentName ?? null,
        accessType: parseProjectAccessType(dto.accessType),
        layoutType: parseProjectLayoutType(dto.layoutType),
    };
}

function applyHabitReorder(state: HabitState, request: ReorderHabitRequest) {
    const reorderedVisible = reorderList(request.contextList, request.fromIndex, request.toIndex).map(
        (habit, index) =>
            index === request.toIndex ? { ...habit, sortOrder: request.sortOrder } : habit,
    );
    state.list = mergeVisibleOrder(state.list, reorderedVisible);
    state.overdueList = mergeVisibleOrder(state.overdueList, reorderedVisible);
}

export interface HabitState {
    list: Habit[];
    projects: Project[];
    labels: Label[];
    favorites: FavoriteDto[];
    favoritesStatus: 'idle' | 'loading' | 'failed';
    status: 'idle' | 'loading' | 'failed';
    loadMoreStatus: 'idle' | 'loading' | 'failed';
    projectsStatus: 'idle' | 'loading' | 'failed';
    labelsStatus: 'idle' | 'loading' | 'failed';
    labelsLoadMoreStatus: 'idle' | 'loading' | 'failed';
    labelsHasNext: boolean;
    labelsNextCursor: EntityId | null;
    error: string | null;
    activeView: ApiView;
    selectedProjectId: EntityId | null;
    selectedLabelId: EntityId | null;
    selectedProjectDetail: ProjectDetail | null;
    selectedProjectDetailStatus: 'idle' | 'loading' | 'failed';
    tasksHasNext: boolean;
    /** 프로젝트 목록 offset page (cursor 미사용) */
    tasksNextPage: number;
    tasksNextCursor: TaskCursor | null;
    overdueList: Habit[];
    overdueHasNext: boolean;
    overdueNextCursor: TaskCursor | null;
    overdueLoadMoreStatus: 'idle' | 'loading' | 'failed';
    inboxTaskCount: number;
    todayTaskCount: number;
    upcomingAnchorDate: string | null;
    upcomingWeekStartIso: string | null;
    upcomingJumpStatus: 'idle' | 'loading' | 'failed';
    upcomingDays: Record<string, UpcomingDayBundle>;
    upcomingDayCounts: Record<string, number> | null;
    upcomingSummaryStatus: 'idle' | 'loading' | 'loaded' | 'failed';
    taskViewCache: Partial<Record<CacheableTaskView, TaskViewCacheSlice>>;
}

export interface UpcomingDayBundle {
    dateKey: string;
    hasNext: boolean;
    nextCursor: TaskCursor | null;
    status: 'idle' | 'loading' | 'loadingMore' | 'failed';
    loaded: boolean;
}

type CacheableTaskView = 'today' | 'inbox';

interface TaskViewCacheSlice {
    list: Habit[];
    tasksHasNext: boolean;
    tasksNextCursor: TaskCursor | null;
    overdueList: Habit[];
    overdueHasNext: boolean;
    overdueNextCursor: TaskCursor | null;
}

function isCacheableTaskView(view: ApiView): view is CacheableTaskView {
    return view === 'today' || view === 'inbox';
}

function snapshotTaskView(state: HabitState): TaskViewCacheSlice {
    return {
        list: state.list,
        tasksHasNext: state.tasksHasNext,
        tasksNextCursor: state.tasksNextCursor,
        overdueList: state.overdueList,
        overdueHasNext: state.overdueHasNext,
        overdueNextCursor: state.overdueNextCursor,
    };
}

function applyTaskViewSnapshot(state: HabitState, snapshot: TaskViewCacheSlice) {
    state.list = snapshot.list;
    state.tasksHasNext = snapshot.tasksHasNext;
    state.tasksNextCursor = snapshot.tasksNextCursor;
    state.overdueList = snapshot.overdueList;
    state.overdueHasNext = snapshot.overdueHasNext;
    state.overdueNextCursor = snapshot.overdueNextCursor;
}

function clearOverdueList(state: HabitState) {
    state.overdueList = [];
    state.overdueHasNext = false;
    state.overdueNextCursor = null;
}

function dedupeLabels(labels: Label[]): Label[] {
    const seen = new Set<EntityId>();
    const result: Label[] = [];
    for (const label of labels) {
        if (seen.has(label.id)) continue;
        seen.add(label.id);
        result.push(label);
    }
    return result;
}

function readInitialRouteState(): Pick<
    HabitState,
    'activeView' | 'selectedProjectId' | 'selectedLabelId'
> {
    if (typeof window === 'undefined') {
        return { activeView: 'today', selectedProjectId: null, selectedLabelId: null };
    }
    return habitRouteStateFromPath(window.location.pathname);
}

const initialState: HabitState = {
    list: [],
    projects: [],
    labels: [],
    favorites: [],
    favoritesStatus: 'idle',
    status: 'idle',
    loadMoreStatus: 'idle',
    projectsStatus: 'idle',
    labelsStatus: 'idle',
    labelsLoadMoreStatus: 'idle',
    labelsHasNext: false,
    labelsNextCursor: null,
    error: null,
    ...readInitialRouteState(),
    selectedProjectDetail: null,
    selectedProjectDetailStatus: 'idle',
    tasksHasNext: false,
    tasksNextPage: 0,
    tasksNextCursor: null,
    overdueList: [],
    overdueHasNext: false,
    overdueNextCursor: null,
    overdueLoadMoreStatus: 'idle',
    inboxTaskCount: 0,
    todayTaskCount: 0,
    upcomingAnchorDate: null,
    upcomingWeekStartIso: null,
    upcomingJumpStatus: 'idle',
    upcomingDays: {},
    upcomingDayCounts: null,
    upcomingSummaryStatus: 'idle',
    taskViewCache: {},
};

export type ApiView = NavItem | 'all';

function taskDtoKey(task: TaskDto): string {
    return String(task.id);
}

function dedupeTasks(tasks: TaskDto[]): TaskDto[] {
    const map = new Map<string, TaskDto>();
    for (const task of tasks) map.set(taskDtoKey(task), task);
    return [...map.values()];
}

function countTasksForLabel(habits: Habit[], labelId: EntityId) {
    return habits.filter(h => h.labels.some(l => l.id === labelId)).length;
}

interface LoadedTasksPage {
    tasks: TaskDto[];
    hasNext: boolean;
    nextCursor: TaskCursor | null;
    overdueTasks: TaskDto[];
    overdueHasNext: boolean;
    overdueNextCursor: TaskCursor | null;
    upcomingAnchorDate?: string | null;
    upcomingWeekStartIso?: string | null;
    upcomingDays?: Record<string, UpcomingDayBundle>;
    upcomingDayCounts?: Record<string, number> | null;
    upcomingSummaryStatus?: 'idle' | 'loading' | 'loaded' | 'failed';
}

async function loadOverdueBundle(): Promise<{
    overdueTasks: TaskDto[];
    overdueHasNext: boolean;
    overdueNextCursor: TaskCursor | null;
}> {
    const overduePage = await taskApi.fetchOverdueTasks();
    return {
        overdueTasks: overduePage.content,
        overdueHasNext: overduePage.hasNext,
        overdueNextCursor: overduePage.nextCursor,
    };
}

async function loadUpcomingDayFirstPage(dateKey: string): Promise<{
    tasks: TaskDto[];
    bundle: UpcomingDayBundle;
}> {
    const page = await taskApi.fetchTasksForUpcomingDay(dateKey);
    return {
        tasks: page.content,
        bundle: {
            dateKey,
            hasNext: page.hasNext,
            nextCursor: page.nextCursor,
            status: 'idle',
            loaded: true,
        },
    };
}

function createEmptyUpcomingDayBundle(dateKey: string): UpcomingDayBundle {
    return {
        dateKey,
        hasNext: false,
        nextCursor: null,
        status: 'idle',
        loaded: true,
    };
}

function shouldFetchUpcomingDay(
    dateKey: string,
    upcomingDayCounts: Record<string, number> | null,
    upcomingSummaryStatus: HabitState['upcomingSummaryStatus'],
): boolean {
    if (upcomingSummaryStatus === 'failed') return true;
    if (upcomingSummaryStatus !== 'loaded' || upcomingDayCounts === null) {
        return false;
    }
    return (upcomingDayCounts[dateKey] ?? 0) > 0;
}

async function loadTasksForView(params: {
    view: ApiView;
    projectId?: EntityId | null;
    labelId?: EntityId | null;
}): Promise<LoadedTasksPage> {
    if (params.labelId != null) {
        const [today, upcoming] = await Promise.all([
            taskApi.fetchAllTaskPages(cursor => taskApi.fetchTodayTasks(cursor)),
            taskApi.fetchAllTaskPages(cursor => taskApi.fetchUpcomingTasks({ cursor })),
        ]);
        const tasks = dedupeTasks([...today, ...upcoming]).filter(t =>
            (t.labels ?? []).some(l => l.id === params.labelId),
        );
        return {
            tasks,
            hasNext: false,
            nextCursor: null,
            overdueTasks: [],
            overdueHasNext: false,
            overdueNextCursor: null,
        };
    }

    switch (params.view) {
        case 'today': {
            const [overdueBundle, todayPage] = await Promise.all([
                loadOverdueBundle(),
                taskApi.fetchTodayTasks(),
            ]);
            return {
                tasks: todayPage.content,
                hasNext: todayPage.hasNext,
                nextCursor: todayPage.nextCursor,
                ...overdueBundle,
            };
        }
        case 'upcoming': {
            const todayIso = toISODate(new Date());
            const [overdueBundle, summaryRows, dayPage] = await Promise.all([
                loadOverdueBundle(),
                taskApi.fetchUpcomingSummary(),
                loadUpcomingDayFirstPage(todayIso),
            ]);
            return {
                tasks: dayPage.tasks,
                hasNext: false,
                nextCursor: null,
                upcomingAnchorDate: todayIso,
                upcomingWeekStartIso: getUpcomingWeekRange(todayIso).weekStartIso,
                upcomingDays: { [todayIso]: dayPage.bundle },
                upcomingDayCounts: taskApi.mapUpcomingSummaryToCounts(summaryRows),
                upcomingSummaryStatus: 'loaded',
                ...overdueBundle,
            };
        }
        case 'inbox': {
            const page = await taskApi.fetchInboxTasks();
            return {
                tasks: page.content,
                hasNext: page.hasNext,
                nextCursor: page.nextCursor,
                overdueTasks: [],
                overdueHasNext: false,
                overdueNextCursor: null,
            };
        }
        case 'filters':
        case 'report':
        case 'all':
        default: {
            const [today, upcoming] = await Promise.all([
                taskApi.fetchAllTaskPages(cursor => taskApi.fetchTodayTasks(cursor)),
                taskApi.fetchAllTaskPages(cursor => taskApi.fetchUpcomingTasks({ cursor })),
            ]);
            return {
                tasks: dedupeTasks([...today, ...upcoming]),
                hasNext: false,
                nextCursor: null,
                overdueTasks: [],
                overdueHasNext: false,
                overdueNextCursor: null,
            };
        }
    }
}

type FetchHabitsParams = { view: ApiView; projectId?: EntityId | null; labelId?: EntityId | null };

type FetchHabitsResult = {
    habits: Habit[];
    view: ApiView;
    projectId: EntityId | null;
    labelId: EntityId | null;
    hasNext: boolean;
    nextCursor: TaskCursor | null;
    overdueHabits: Habit[];
    overdueHasNext: boolean;
    overdueNextCursor: TaskCursor | null;
    upcomingAnchorDate?: string | null;
    upcomingWeekStartIso?: string | null;
    upcomingDays?: Record<string, UpcomingDayBundle>;
    upcomingDayCounts?: Record<string, number> | null;
    upcomingSummaryStatus?: 'idle' | 'loading' | 'loaded' | 'failed';
};

function fetchHabitsKey(params: FetchHabitsParams): string {
    return `${params.view}:${params.projectId ?? ''}:${params.labelId ?? ''}`;
}

function isFetchHabitsResultCurrent(state: HabitState, arg: FetchHabitsParams): boolean {
    if ((arg.projectId ?? null) !== state.selectedProjectId) return false;
    if ((arg.labelId ?? null) !== state.selectedLabelId) return false;
    if (arg.projectId == null && arg.labelId == null && arg.view !== state.activeView) {
        return false;
    }
    return true;
}

const inFlightHabitsFetches = new Map<string, Promise<FetchHabitsResult>>();
const inFlightProjectHabitsFetches = new Map<EntityId, Promise<FetchProjectHabitsResult>>();

type FetchProjectHabitsResult = {
    projectId: EntityId;
    habits: Habit[];
    hasNext: boolean;
    nextPage: number;
};

async function loadHabitsForView(params: FetchHabitsParams): Promise<FetchHabitsResult> {
    const page = await loadTasksForView(params);
    return {
        habits: page.tasks.map(t => mapTaskToHabit(t, params.projectId)),
        view: params.view,
        projectId: params.projectId ?? null,
        labelId: params.labelId ?? null,
        hasNext: page.hasNext,
        nextCursor: page.nextCursor,
        overdueHabits: page.overdueTasks.map(t => mapTaskToHabit(t)),
        overdueHasNext: page.overdueHasNext,
        overdueNextCursor: page.overdueNextCursor,
        upcomingAnchorDate: page.upcomingAnchorDate ?? null,
        upcomingWeekStartIso: page.upcomingWeekStartIso ?? null,
        upcomingDays: page.upcomingDays ?? {},
        upcomingDayCounts: page.upcomingDayCounts ?? null,
        upcomingSummaryStatus: page.upcomingSummaryStatus ?? 'idle',
    };
}

export const fetchHabits = createAsyncThunk(
    'habits/fetch',
    async (params: FetchHabitsParams) => {
        const key = fetchHabitsKey(params);
        const existing = inFlightHabitsFetches.get(key);
        if (existing) return existing;

        const promise = loadHabitsForView(params).finally(() => {
            if (inFlightHabitsFetches.get(key) === promise) {
                inFlightHabitsFetches.delete(key);
            }
        });
        inFlightHabitsFetches.set(key, promise);
        return promise;
    },
);

export const fetchProjectHabits = createAsyncThunk(
    'habits/fetchProjectHabits',
    async (projectId: EntityId, { dispatch }) => {
        const existing = inFlightProjectHabitsFetches.get(projectId);
        if (existing) return existing;

        const promise = (async (): Promise<FetchProjectHabitsResult> => {
            void dispatch(fetchProjectDetail(projectId));
            const page = await taskApi.fetchProjectTasks(projectId, 0);
            return {
                projectId,
                habits: page.content.map(t => mapTaskToHabit(t, projectId)),
                hasNext: page.hasNext,
                nextPage: page.hasNext ? 1 : 0,
            };
        })().finally(() => {
            if (inFlightProjectHabitsFetches.get(projectId) === promise) {
                inFlightProjectHabitsFetches.delete(projectId);
            }
        });

        inFlightProjectHabitsFetches.set(projectId, promise);
        return promise;
    },
);

export const fetchMoreHabits = createAsyncThunk(
    'habits/fetchMore',
    async (viewOverride: ApiView | undefined, { getState }) => {
        const state = getState() as { habits: HabitState };
        const { activeView, selectedProjectId, selectedLabelId, tasksNextPage, tasksNextCursor } = state.habits;
        const view = viewOverride ?? activeView;

        if (selectedLabelId != null) {
            return { habits: [], hasNext: false, nextCursor: null };
        }

        if (selectedProjectId != null) {
            const page = await taskApi.fetchProjectTasks(selectedProjectId, tasksNextPage);
            return {
                habits: page.content.map(t => mapTaskToHabit(t, selectedProjectId)),
                hasNext: page.hasNext,
                nextCursor: null,
            };
        }

        if (view !== 'today' && view !== 'upcoming' && view !== 'inbox') {
            return { habits: [], hasNext: false, nextCursor: null };
        }

        const cursor = tasksNextCursor ? { ...tasksNextCursor, direction: 'NEXT' as const } : null;
        const page = view === 'today'
            ? await taskApi.fetchTodayTasks(cursor)
            : view === 'upcoming'
                ? await taskApi.fetchUpcomingTasks({ cursor })
                : await taskApi.fetchInboxTasks(cursor);

        return {
            habits: page.content.map(t => mapTaskToHabit(t)),
            hasNext: page.hasNext,
            nextCursor: page.nextCursor,
        };
    },
    {
        condition: (_, { getState }) => {
            const { loadMoreStatus, tasksHasNext } = (getState() as { habits: HabitState }).habits;
            return loadMoreStatus !== 'loading' && tasksHasNext;
        },
    },
);

const inFlightUpcomingDays = new Map<string, Promise<{
    dateKey: string;
    habits: Habit[];
    bundle: UpcomingDayBundle;
    skipped: boolean;
}>>();

function mergeHabitsById(existing: Habit[], incoming: Habit[]): Habit[] {
    const map = new Map(existing.map(h => [String(h.id), h]));
    for (const habit of incoming) {
        map.set(String(habit.id), habit);
    }
    return [...map.values()];
}

function patchHabitInLists(
    state: HabitState,
    habitId: EntityId,
    patch: Partial<Habit> | ((habit: Habit) => Habit),
) {
    const apply = (habits: Habit[]) => {
        const index = habits.findIndex(h => h.id === habitId);
        if (index === -1) return;
        const prev = habits[index];
        habits[index] = typeof patch === 'function' ? patch(prev) : { ...prev, ...patch };
    };

    apply(state.list);
    apply(state.overdueList);

    for (const view of ['today', 'inbox'] as const) {
        const cached = state.taskViewCache[view];
        if (!cached) continue;
        apply(cached.list);
        apply(cached.overdueList);
    }
}

function findHabitInState(state: HabitState, habitId: EntityId): Habit | undefined {
    return state.list.find(h => h.id === habitId)
        ?? state.overdueList.find(h => h.id === habitId);
}

export const ensureUpcomingDay = createAsyncThunk(
    'habits/ensureUpcomingDay',
    async (dateKey: string, { getState }) => {
        const habitsState = (getState() as { habits: HabitState }).habits;
        const bundle = habitsState.upcomingDays[dateKey];
        if (bundle?.loaded) {
            return { dateKey, habits: [], bundle, skipped: true as const };
        }

        if (!shouldFetchUpcomingDay(
            dateKey,
            habitsState.upcomingDayCounts,
            habitsState.upcomingSummaryStatus,
        )) {
            if (habitsState.upcomingSummaryStatus === 'loaded') {
                const emptyBundle = createEmptyUpcomingDayBundle(dateKey);
                return { dateKey, habits: [], bundle: emptyBundle, skipped: false as const };
            }
            return { dateKey, habits: [], bundle: bundle ?? createEmptyUpcomingDayBundle(dateKey), skipped: true as const };
        }

        const existing = inFlightUpcomingDays.get(dateKey);
        if (existing) return existing;

        const promise = loadUpcomingDayFirstPage(dateKey)
            .then(result => ({
                dateKey,
                habits: result.tasks.map(t => mapTaskToHabit(t)),
                bundle: result.bundle,
                skipped: false as const,
            }))
            .finally(() => {
                if (inFlightUpcomingDays.get(dateKey) === promise) {
                    inFlightUpcomingDays.delete(dateKey);
                }
            });

        inFlightUpcomingDays.set(dateKey, promise);
        return promise;
    },
);

export const fetchMoreUpcomingDay = createAsyncThunk(
    'habits/fetchMoreUpcomingDay',
    async (dateKey: string, { getState }) => {
        const bundle = (getState() as { habits: HabitState }).habits.upcomingDays[dateKey];
        if (!bundle?.loaded || !bundle.hasNext) {
            return { dateKey, habits: [], hasNext: false, nextCursor: null };
        }

        const cursor = bundle.nextCursor
            ? { ...bundle.nextCursor, direction: 'NEXT' as const }
            : null;
        const page = await taskApi.fetchTasksForUpcomingDay(dateKey, cursor);

        return {
            dateKey,
            habits: page.content.map(t => mapTaskToHabit(t)),
            hasNext: page.hasNext,
            nextCursor: page.nextCursor,
        };
    },
    {
        condition: (dateKey, { getState }) => {
            const bundle = (getState() as { habits: HabitState }).habits.upcomingDays[dateKey];
            return Boolean(bundle?.loaded && bundle.hasNext && bundle.status === 'idle');
        },
    },
);

export const jumpToUpcomingWeek = createAsyncThunk(
    'habits/jumpToUpcomingWeek',
    async (anchorIso: string, { dispatch, getState }) => {
        const bundle = (getState() as { habits: HabitState }).habits.upcomingDays[anchorIso];
        if (!bundle?.loaded && bundle?.status !== 'loading') {
            await dispatch(ensureUpcomingDay(anchorIso));
        }
        return {
            anchorIso,
            weekStartIso: getUpcomingWeekRange(anchorIso).weekStartIso,
        };
    },
);

export const fetchMoreOverdue = createAsyncThunk(
    'habits/fetchMoreOverdue',
    async (_, { getState }) => {
        const { overdueNextCursor } = (getState() as { habits: HabitState }).habits;
        const cursor = overdueNextCursor
            ? { ...overdueNextCursor, direction: 'NEXT' as const }
            : null;
        const page = await taskApi.fetchOverdueTasks(cursor);
        return {
            habits: page.content.map(t => mapTaskToHabit(t)),
            hasNext: page.hasNext,
            nextCursor: page.nextCursor,
        };
    },
    {
        condition: (_, { getState }) => {
            const { overdueLoadMoreStatus, overdueHasNext } = (getState() as { habits: HabitState }).habits;
            return overdueLoadMoreStatus !== 'loading' && overdueHasNext;
        },
    },
);

export const fetchNavTaskCounts = createAsyncThunk('habits/fetchNavTaskCounts', async () => {
    return taskApi.fetchSidebarTaskCounts();
});

export const fetchFavorites = createAsyncThunk('habits/fetchFavorites', async () => {
    return favoriteApi.fetchFavorites();
});

export const fetchProjects = createAsyncThunk('habits/fetchProjects', async () => {
    const list = await projectApi.fetchProjects();
    return list.map(p => mapProject(p, p.taskCount ?? 0));
});

export const fetchProjectDetail = createAsyncThunk(
    'habits/fetchProjectDetail',
    async (projectId: EntityId) => {
        const detail = await projectApi.fetchProjectById(projectId);
        return mapProjectDetail(detail);
    },
);

const LABELS_INITIAL_FETCH_DEBOUNCE_MS = 800;
let labelsInitialFetchStartedAt = 0;

export function resetLabelsInitialFetchDebounce() {
    labelsInitialFetchStartedAt = 0;
}

function claimLabelsInitialFetch(): boolean {
    const now = Date.now();
    if (now - labelsInitialFetchStartedAt < LABELS_INITIAL_FETCH_DEBOUNCE_MS) {
        return false;
    }
    labelsInitialFetchStartedAt = now;
    return true;
}

export const fetchLabels = createAsyncThunk(
    'habits/fetchLabels',
    async () => {
        const slice = await labelApi.fetchLabels();
        return {
            labels: slice.content.map(l => mapLabel(l)),
            hasNext: slice.hasNext,
            nextCursor: slice.nextCursor,
        };
    },
    {
        condition: (_, { getState }) => {
            const { labelsStatus } = (getState() as { habits: HabitState }).habits;
            if (labelsStatus === 'loading') return false;
            return claimLabelsInitialFetch();
        },
    },
);

export const fetchMoreLabels = createAsyncThunk(
    'habits/fetchMoreLabels',
    async (_, { getState }) => {
        const state = getState() as { habits: HabitState };
        const { labelsNextCursor } = state.habits;
        if (labelsNextCursor == null) {
            return { labels: [], hasNext: false, nextCursor: null };
        }

        const slice = await labelApi.fetchLabels(labelsNextCursor);
        return {
            labels: slice.content.map(l => mapLabel(l)),
            hasNext: slice.hasNext,
            nextCursor: slice.nextCursor,
        };
    },
    {
        condition: (_, { getState }) => {
            const { labelsLoadMoreStatus, labelsHasNext, labelsNextCursor } = (
                getState() as { habits: HabitState }
            ).habits;
            return labelsLoadMoreStatus !== 'loading' && labelsHasNext && labelsNextCursor != null;
        },
    },
);

export type SidebarAggregateScope = {
    projects?: boolean;
    labels?: boolean;
    nav?: boolean;
};

/** 태스크 변경 후 사이드바 집계(프로젝트·라벨·관리함/오늘 카운트) 재조회 */
export const invalidateSidebarAggregates = createAsyncThunk(
    'habits/invalidateSidebarAggregates',
    async (scope: SidebarAggregateScope, { dispatch }) => {
        const jobs: Array<ReturnType<typeof dispatch>> = [];

        if (scope.projects) {
            jobs.push(dispatch(fetchProjects()));
            jobs.push(dispatch(fetchFavorites()));
        }
        if (scope.labels) {
            resetLabelsInitialFetchDebounce();
            jobs.push(dispatch(fetchLabels()));
            jobs.push(dispatch(fetchFavorites()));
        }
        if (scope.nav) {
            jobs.push(dispatch(fetchNavTaskCounts()));
        }

        await Promise.all(jobs);
    },
);

/** 현재 화면의 태스크 목록 캐시 무효화 후 재조회 */
export const refetchCurrentTaskList = createAsyncThunk(
    'habits/refetchCurrentTaskList',
    async (_, { dispatch, getState }) => {
        const { activeView, selectedProjectId, selectedLabelId } = (
            getState() as { habits: HabitState }
        ).habits;

        if (selectedProjectId != null) {
            await dispatch(fetchProjectHabits(selectedProjectId));
            return;
        }

        if (activeView === 'report' || activeView === 'filters') {
            return;
        }

        await dispatch(fetchHabits({
            view: selectedLabelId != null ? 'all' : activeView,
            projectId: null,
            labelId: selectedLabelId,
        }));
    },
);

export const fetchHabitDetail = createAsyncThunk(
    'habits/fetchDetail',
    async (taskId: EntityId) => {
        const task = await taskApi.fetchTaskById(taskId);
        return mapTaskToHabit(task);
    },
);

export const addHabit = createAsyncThunk(
    'habits/add',
    async (payload: {
        name: string;
        description: string;
        view: NavItem;
        projectId?: EntityId | null;
        dueDate?: string | null;
        dueTime24?: string | null;
        hasTime?: boolean;
        recurrenceLabel?: string | null;
        labelIds?: EntityId[];
        file?: File | null;
        priority?: 1 | 2 | 3 | 4;
    }, { dispatch, rejectWithValue }) => {
        try {
            const recurrence = repeatLabelToRecurrence(payload.recurrenceLabel, payload.dueDate);
            const task = await taskApi.createTask({
                name: payload.name,
                description: payload.description,
                dueDate: payload.dueDate,
                dueTime24: payload.dueTime24,
                hasTime: payload.hasTime,
                projectId: payload.projectId,
                labelIds: payload.labelIds,
                file: payload.file,
                priorityType: priorityToApi(payload.priority),
                ...recurrence,
            });
            void dispatch(invalidateSidebarAggregates({
                projects: true,
                labels: true,
                nav: true,
            }));
            void dispatch(refetchCurrentTaskList());
            return mapTaskToHabit(task);
        } catch (error) {
            return rejectWithValue(
                getApiErrorMessage(error, '작업을 추가하지 못했습니다.'),
            );
        }
    },
);

export const addProject = createAsyncThunk(
    'habits/addProject',
    async (payload: { name: string; color?: string }, { dispatch }) => {
        const project = await projectApi.createProject(payload.name, payload.color);
        await dispatch(invalidateSidebarAggregates({ projects: true }));
        return mapProject(project);
    },
);

export const updateProject = createAsyncThunk(
    'habits/updateProject',
    async (payload: {
        id: EntityId;
        name: string;
        color?: string;
        parentId?: EntityId | null;
        accessType?: 'PRIVATE' | 'PUBLIC';
        layoutType?: 'LIST' | 'BOARD';
        favorite?: boolean;
    }, { dispatch, getState }) => {
        const { id, ...body } = payload;
        const detail = await projectApi.updateProject(id, body);
        const habitsState = (getState() as { habits: HabitState }).habits;
        const existing = habitsState.projects.find(p => p.id === id);
        const project = mapProject(
            {
                ...detail,
                favorite: detail.favorite ?? body.favorite ?? existing?.favorite,
            },
            existing?.taskCount ?? 0,
        );
        const { selectedProjectId } = habitsState;
        const jobs: Array<Promise<unknown>> = [
            dispatch(invalidateSidebarAggregates({ projects: true })),
            dispatch(refetchCurrentTaskList()),
        ];
        if (selectedProjectId === id) {
            jobs.push(dispatch(fetchProjectDetail(id)));
        }
        await Promise.all(jobs);
        return project;
    },
);

export const deleteProject = createAsyncThunk(
    'habits/deleteProject',
    async (projectId: EntityId, { dispatch }) => {
        await projectApi.deleteProject(projectId);
        await Promise.all([
            dispatch(invalidateSidebarAggregates({ projects: true, nav: true })),
            dispatch(refetchCurrentTaskList()),
        ]);
        return projectId;
    },
);

export const addLabel = createAsyncThunk(
    'habits/addLabel',
    async (payload: { name: string; color?: string; favorite?: boolean }, { dispatch }) => {
        const label = await labelApi.createLabel(payload.name, payload.color, payload.favorite);
        await dispatch(invalidateSidebarAggregates({ labels: true }));
        return mapLabel(label);
    },
);

export const updateLabel = createAsyncThunk(
    'habits/updateLabel',
    async (payload: { id: EntityId; name: string; color?: string; favorite?: boolean }, { dispatch, getState }) => {
        const { id, ...body } = payload;
        const detail = await labelApi.updateLabel(id, body);
        const habitsState = (getState() as { habits: HabitState }).habits;
        const existing = habitsState.labels.find(l => l.id === id);
        const label = mapLabel(
            {
                ...detail,
                favorite: detail.favorite ?? body.favorite ?? existing?.favorite,
            },
            existing?.taskCount ?? countTasksForLabel(habitsState.list, id),
        );
        await dispatch(invalidateSidebarAggregates({ labels: true }));
        return label;
    },
);

export const deleteLabel = createAsyncThunk(
    'habits/deleteLabel',
    async (labelId: EntityId, { dispatch }) => {
        await labelApi.deleteLabel(labelId);
        await Promise.all([
            dispatch(invalidateSidebarAggregates({ labels: true })),
            dispatch(refetchCurrentTaskList()),
        ]);
        return labelId;
    },
);

export interface CheckHabitPayload {
    habitId: EntityId;
    wasCompleted: boolean;
}

export const checkHabit = createAsyncThunk(
    'habits/check',
    async ({ habitId, wasCompleted }: CheckHabitPayload, { dispatch, getState }) => {
        const previous = findHabitInState((getState() as { habits: HabitState }).habits, habitId);
        const task = await taskApi.toggleTaskCompletion(habitId, wasCompleted, previous?.projectId);
        await Promise.all([
            dispatch(invalidateSidebarAggregates({ projects: true, nav: true })),
            dispatch(refetchCurrentTaskList()),
        ]);
        const habit = mergeHabitFromTaskMutation(task, previous);
        return {
            ...habit,
            id: habitId,
            completedToday: !wasCompleted,
        };
    },
);

export const updateHabit = createAsyncThunk(
    'habits/updateHabit',
    async ({ habitId, changes }: { habitId: EntityId; changes: Partial<Habit> }, { dispatch, getState }) => {
        const previous = findHabitInState((getState() as { habits: HabitState }).habits, habitId);
        const task = await taskApi.updateTask(habitId, {
            name: changes.name,
            description: changes.description,
        }, previous?.projectId);
        await dispatch(refetchCurrentTaskList());
        return mergeHabitFromTaskMutation(task, previous);
    },
);

export const patchTaskProject = createAsyncThunk(
    'habits/patchProject',
    async ({ habitId, projectId }: { habitId: EntityId; projectId: EntityId | null }, { dispatch, getState }) => {
        const previous = findHabitInState((getState() as { habits: HabitState }).habits, habitId);
        const task = await taskApi.patchTaskProject(habitId, projectId, previous?.projectId);
        await Promise.all([
            dispatch(invalidateSidebarAggregates({ projects: true, nav: true })),
            dispatch(refetchCurrentTaskList()),
        ]);
        return mergeHabitFromTaskMutation(task, previous);
    },
);

export const patchTaskDueDate = createAsyncThunk(
    'habits/patchDueDate',
    async ({
        habitId,
        dueDate,
        dueTime24,
        hasTime,
        recurrenceLabel,
    }: {
        habitId: EntityId;
        dueDate: string | null;
        dueTime24?: string | null;
        hasTime: boolean;
        recurrenceLabel?: string | null;
    }, { dispatch, getState }) => {
        const previous = findHabitInState((getState() as { habits: HabitState }).habits, habitId);
        const recurrence = recurrenceLabel !== undefined
            ? repeatLabelToRecurrence(recurrenceLabel, dueDate)
            : undefined;
        const task = await taskApi.patchTaskDueDate(habitId, {
            dueDate,
            dueTime24,
            hasTime,
            recurrence,
        }, previous?.projectId);
        await Promise.all([
            dispatch(invalidateSidebarAggregates({ nav: true })),
            dispatch(refetchCurrentTaskList()),
        ]);
        const habit = mergeHabitFromTaskMutation(task, previous);
        return {
            ...habit,
            id: habitId,
            ...(recurrence !== undefined
                ? {
                    isRecurring: recurrence.isRecurring ?? false,
                    recurrenceLabel: recurrenceLabel ?? null,
                }
                : {}),
        };
    },
);

export const patchTaskPriority = createAsyncThunk(
    'habits/patchPriority',
    async ({ habitId, priority }: { habitId: EntityId; priority: 1 | 2 | 3 | 4 }, { dispatch, getState }) => {
        const previous = findHabitInState((getState() as { habits: HabitState }).habits, habitId);
        const task = await taskApi.patchTaskPriority(habitId, priorityToApi(priority), previous?.projectId);
        await dispatch(refetchCurrentTaskList());
        return mergeHabitFromTaskMutation(task, previous);
    },
);

export const patchTaskLabels = createAsyncThunk(
    'habits/patchLabels',
    async ({ habitId, labelIds }: { habitId: EntityId; labelIds: EntityId[] }, { dispatch, getState }) => {
        const previous = findHabitInState((getState() as { habits: HabitState }).habits, habitId);
        const task = await taskApi.patchTaskLabels(habitId, labelIds, previous?.projectId);
        await Promise.all([
            dispatch(invalidateSidebarAggregates({ labels: true })),
            dispatch(refetchCurrentTaskList()),
        ]);
        return mergeHabitFromTaskMutation(task, previous);
    },
);

export const deleteHabit = createAsyncThunk(
    'habits/delete',
    async (habitId: EntityId, { dispatch }) => {
        await taskApi.deleteTask(habitId);
        await Promise.all([
            dispatch(invalidateSidebarAggregates({ projects: true, labels: true, nav: true })),
            dispatch(refetchCurrentTaskList()),
        ]);
        return habitId;
    },
);

export const reorderHabit = createAsyncThunk(
    'habits/reorderHabit',
    async (request: ReorderHabitRequest, { dispatch, getState, rejectWithValue }) => {
        try {
            const previous = findHabitInState(
                (getState() as { habits: HabitState }).habits,
                request.habitId,
            );
            await taskApi.patchTaskSortOrder(
                request.habitId,
                request.sortOrder,
                previous?.projectId,
            );
            return request;
        } catch (error) {
            void dispatch(refetchCurrentTaskList());
            return rejectWithValue(
                getApiErrorMessage(error, '작업 순서를 변경하지 못했습니다.'),
            );
        }
    },
);

export const addSubtask = createAsyncThunk(
    'habits/addSubtask',
    async ({
        habitId,
        name,
        description,
        projectId,
        dueDate,
    }: {
        habitId: EntityId;
        name: string;
        description: string;
        projectId?: EntityId | null;
        dueDate?: string | null;
    }, { dispatch }) => {
        const task = await taskApi.createTask({
            name,
            description,
            parentId: habitId,
            projectId: projectId ?? null,
            dueDate: dueDate ?? null,
        });
        void dispatch(invalidateSidebarAggregates({ projects: true, nav: true }));
        return {
            habitId,
            subtask: {
                id: task.id,
                name: task.name,
                description: description,
                completed: false,
                childCount: 0,
            } satisfies Subtask,
        };
    },
);

export const toggleSubtask = createAsyncThunk(
    'habits/toggleSubtask',
    async (
        { habitId, subtaskId }: { habitId: EntityId; subtaskId: EntityId },
        { getState },
    ) => {
        const state = getState() as { habits: HabitState };
        const habit = state.habits.list.find(h => h.id === habitId);
        const subtask = habit?.subtasks.find(s => s.id === subtaskId);
        const task = await taskApi.toggleTaskCompletion(subtaskId, subtask?.completed);
        return {
            habitId,
            subtaskId,
            completed: readCompleted(task),
        };
    },
);

export const addComment = createAsyncThunk(
    'habits/addComment',
    async ({ habitId, text }: { habitId: EntityId; text: string }) => {
        await commentApi.createComment(habitId, text);
        const commentDtos = await commentApi.fetchTaskComments(habitId);
        return { habitId, commentCount: commentDtos.length };
    },
);

export const syncHabitCommentCount = createAsyncThunk(
    'habits/syncHabitCommentCount',
    async (habitId: EntityId) => {
        const commentDtos = await commentApi.fetchTaskComments(habitId);
        return { habitId, commentCount: commentDtos.length };
    },
);

export const uploadAttachments = createAsyncThunk(
    'habits/uploadAttachments',
    async ({ habitId, files }: { habitId: EntityId; files: File[] }) => {
        for (const file of files) {
            await commentApi.createComment(habitId, '첨부파일이 등록되었습니다.', file);
        }
        const commentDtos = await commentApi.fetchTaskComments(habitId);
        return { habitId, commentCount: commentDtos.length };
    },
);

const habitSlice = createSlice({
    name: 'habits',
    initialState,
    reducers: {
        setActiveView(state, action: { payload: ApiView }) {
            const next = action.payload;
            const prev = state.activeView;

            if (next !== prev) {
                if (isCacheableTaskView(prev)) {
                    state.taskViewCache[prev] = snapshotTaskView(state);
                }

                const cached = isCacheableTaskView(next) ? state.taskViewCache[next] : undefined;
                if (cached) {
                    applyTaskViewSnapshot(state, cached);
                } else if (next === 'inbox' || prev === 'inbox') {
                    state.list = [];
                    if (next === 'inbox' || (next === 'today' && prev === 'inbox')) {
                        clearOverdueList(state);
                    }
                } else if (next !== 'today' && next !== 'upcoming') {
                    clearOverdueList(state);
                }
            }

            state.activeView = next;
            state.selectedProjectId = null;
            state.selectedLabelId = null;
            state.selectedProjectDetail = null;
            state.selectedProjectDetailStatus = 'idle';
        },
        setSelectedProject(state, action: { payload: EntityId | null }) {
            if (action.payload !== state.selectedProjectId) {
                state.selectedProjectDetail = null;
                state.selectedProjectDetailStatus = action.payload == null ? 'idle' : 'loading';
            }
            state.selectedProjectId = action.payload;
            state.selectedLabelId = null;
        },
        setSelectedLabel(state, action: { payload: EntityId | null }) {
            state.selectedLabelId = action.payload;
            if (action.payload != null) {
                state.selectedProjectId = null;
                state.selectedProjectDetail = null;
                state.selectedProjectDetailStatus = 'idle';
            }
        },
        clearHabitError(state) {
            state.error = null;
        },
        setUpcomingAnchorDate(state, action: { payload: string }) {
            state.upcomingAnchorDate = action.payload;
            state.upcomingWeekStartIso = getUpcomingWeekRange(action.payload).weekStartIso;
        },
    },
    extraReducers: builder => {
        builder
            .addCase(fetchHabits.pending, (state, action) => {
                const view = action.meta.arg.view;
                if (view !== 'upcoming' && view !== 'today' && view !== 'inbox') {
                    state.status = 'loading';
                }
                state.loadMoreStatus = 'idle';
                state.overdueLoadMoreStatus = 'idle';
                state.tasksNextPage = 0;
                state.tasksNextCursor = null;
                state.error = null;
                if (view === 'inbox') {
                    state.overdueList = [];
                    state.overdueHasNext = false;
                    state.overdueNextCursor = null;
                }
                if (view === 'upcoming') {
                    state.upcomingSummaryStatus = 'loading';
                }
            })
            .addCase(fetchHabits.fulfilled, (state, action) => {
                if (!isFetchHabitsResultCurrent(state, action.meta.arg)) return;
                state.status = 'idle';
                state.list = action.payload.habits;
                state.activeView = action.payload.view;
                state.tasksHasNext = action.payload.hasNext;
                state.tasksNextCursor = action.payload.nextCursor;
                state.overdueList = action.payload.overdueHabits;
                state.overdueHasNext = action.payload.overdueHasNext;
                state.overdueNextCursor = action.payload.overdueNextCursor;
                if (action.payload.view === 'upcoming') {
                    state.upcomingAnchorDate = action.payload.upcomingAnchorDate ?? null;
                    state.upcomingWeekStartIso = action.payload.upcomingWeekStartIso ?? null;
                    state.upcomingDays = action.payload.upcomingDays ?? {};
                    state.upcomingDayCounts = action.payload.upcomingDayCounts ?? null;
                    state.upcomingSummaryStatus = action.payload.upcomingSummaryStatus ?? 'idle';
                    state.upcomingJumpStatus = 'idle';
                }
                state.labels = state.labels.map(l => ({
                    ...l,
                    taskCount: countTasksForLabel(action.payload.habits, l.id),
                }));
                if (isCacheableTaskView(action.payload.view)) {
                    state.taskViewCache[action.payload.view] = snapshotTaskView(state);
                }
            })
            .addCase(fetchHabits.rejected, (state, action) => {
                if (!isFetchHabitsResultCurrent(state, action.meta.arg)) return;
                state.status = 'failed';
                state.error = action.error.message ?? '작업 목록을 불러오지 못했습니다.';
                if (action.meta.arg.view === 'upcoming') {
                    state.upcomingSummaryStatus = 'failed';
                }
            })
            .addCase(fetchProjectHabits.pending, state => {
                state.status = 'loading';
                state.loadMoreStatus = 'idle';
                state.tasksNextPage = 0;
                state.tasksNextCursor = null;
                state.error = null;
            })
            .addCase(fetchProjectHabits.fulfilled, (state, action) => {
                if (state.selectedProjectId !== action.payload.projectId) return;
                state.status = 'idle';
                state.list = action.payload.habits;
                state.tasksHasNext = action.payload.hasNext;
                state.tasksNextPage = action.payload.hasNext ? action.payload.nextPage : 0;
            })
            .addCase(fetchProjectHabits.rejected, (state, action) => {
                if (state.selectedProjectId !== action.meta.arg) return;
                state.status = 'failed';
                state.error = action.error.message ?? '프로젝트 작업을 불러오지 못했습니다.';
            })
            .addCase(fetchMoreHabits.pending, state => {
                state.loadMoreStatus = 'loading';
            })
            .addCase(fetchMoreHabits.fulfilled, (state, action) => {
                state.loadMoreStatus = 'idle';
                const existingKeys = new Set(state.list.map(h => String(h.id)));
                for (const habit of action.payload.habits) {
                    const key = String(habit.id);
                    if (!existingKeys.has(key)) {
                        state.list.push(habit);
                        existingKeys.add(key);
                    }
                }
                state.tasksHasNext = action.payload.hasNext;
                state.tasksNextCursor = action.payload.nextCursor;
                if (state.selectedProjectId != null && action.payload.hasNext) {
                    state.tasksNextPage += 1;
                }
            })
            .addCase(fetchMoreHabits.rejected, (state, action) => {
                state.loadMoreStatus = 'failed';
                state.error = action.error.message ?? '작업을 더 불러오지 못했습니다.';
            })
            .addCase(jumpToUpcomingWeek.pending, (state, action) => {
                state.upcomingJumpStatus = 'loading';
                state.upcomingAnchorDate = action.meta.arg;
                state.upcomingWeekStartIso = getUpcomingWeekRange(action.meta.arg).weekStartIso;
            })
            .addCase(jumpToUpcomingWeek.fulfilled, (state, action) => {
                state.upcomingJumpStatus = 'idle';
                state.upcomingAnchorDate = action.payload.anchorIso;
                state.upcomingWeekStartIso = action.payload.weekStartIso;
            })
            .addCase(jumpToUpcomingWeek.rejected, (state, action) => {
                state.upcomingJumpStatus = 'failed';
                state.error = action.error.message ?? '일정을 불러오지 못했습니다.';
            })
            .addCase(ensureUpcomingDay.pending, (state, action) => {
                const dateKey = action.meta.arg;
                const prev = state.upcomingDays[dateKey];
                if (prev?.loaded) return;
                state.upcomingDays[dateKey] = {
                    dateKey,
                    hasNext: prev?.hasNext ?? false,
                    nextCursor: prev?.nextCursor ?? null,
                    status: 'loading',
                    loaded: false,
                };
            })
            .addCase(ensureUpcomingDay.fulfilled, (state, action) => {
                const { dateKey, habits, bundle, skipped } = action.payload;
                if (skipped) return;
                state.list = mergeHabitsById(state.list, habits);
                state.upcomingDays[dateKey] = bundle;
            })
            .addCase(ensureUpcomingDay.rejected, (state, action) => {
                const dateKey = action.meta.arg;
                const prev = state.upcomingDays[dateKey];
                if (prev) {
                    state.upcomingDays[dateKey] = { ...prev, status: 'failed', loaded: false };
                }
            })
            .addCase(fetchMoreUpcomingDay.pending, (state, action) => {
                const bundle = state.upcomingDays[action.meta.arg];
                if (bundle) {
                    state.upcomingDays[action.meta.arg] = { ...bundle, status: 'loadingMore' };
                }
            })
            .addCase(fetchMoreUpcomingDay.fulfilled, (state, action) => {
                const { dateKey, habits, hasNext, nextCursor } = action.payload;
                state.list = mergeHabitsById(state.list, habits);
                const prev = state.upcomingDays[dateKey];
                if (prev) {
                    state.upcomingDays[dateKey] = {
                        ...prev,
                        hasNext,
                        nextCursor,
                        status: 'idle',
                    };
                }
            })
            .addCase(fetchMoreUpcomingDay.rejected, (state, action) => {
                const bundle = state.upcomingDays[action.meta.arg];
                if (bundle) {
                    state.upcomingDays[action.meta.arg] = { ...bundle, status: 'idle' };
                }
            })
            .addCase(fetchMoreOverdue.pending, state => {
                state.overdueLoadMoreStatus = 'loading';
            })
            .addCase(fetchMoreOverdue.fulfilled, (state, action) => {
                state.overdueLoadMoreStatus = 'idle';
                const existingKeys = new Set(state.overdueList.map(h => String(h.id)));
                for (const habit of action.payload.habits) {
                    const key = String(habit.id);
                    if (!existingKeys.has(key)) {
                        state.overdueList.push(habit);
                        existingKeys.add(key);
                    }
                }
                state.overdueHasNext = action.payload.hasNext;
                state.overdueNextCursor = action.payload.nextCursor;
            })
            .addCase(fetchMoreOverdue.rejected, (state, action) => {
                state.overdueLoadMoreStatus = 'failed';
                state.error = action.error.message ?? '기한이 지난 작업을 더 불러오지 못했습니다.';
            })
            .addCase(fetchFavorites.pending, state => {
                state.favoritesStatus = 'loading';
            })
            .addCase(fetchFavorites.fulfilled, (state, action) => {
                state.favoritesStatus = 'idle';
                state.favorites = action.payload
                    .map(normalizeFavoriteDto)
                    .filter((f): f is FavoriteDto => f != null);
                state.projects = applyFavoriteFlags(state.projects, state.favorites, 'PROJECT');
                state.labels = applyFavoriteFlags(state.labels, state.favorites, 'LABEL');
            })
            .addCase(fetchFavorites.rejected, state => {
                state.favoritesStatus = 'failed';
            })
            .addCase(fetchProjects.pending, state => {
                state.projectsStatus = 'loading';
            })
            .addCase(fetchNavTaskCounts.fulfilled, (state, action) => {
                state.inboxTaskCount = action.payload.inbox;
                state.todayTaskCount = action.payload.today;
            })
            .addCase(fetchProjects.fulfilled, (state, action) => {
                state.projectsStatus = 'idle';
                state.projects = applyFavoriteFlags(action.payload, state.favorites, 'PROJECT');
            })
            .addCase(fetchProjects.rejected, state => {
                state.projectsStatus = 'failed';
            })
            .addCase(fetchProjectDetail.pending, state => {
                state.selectedProjectDetailStatus = 'loading';
            })
            .addCase(fetchProjectDetail.fulfilled, (state, action) => {
                if (state.selectedProjectId !== action.payload.id) return;
                state.selectedProjectDetailStatus = 'idle';
                state.selectedProjectDetail = action.payload;
                const index = state.projects.findIndex(p => p.id === action.payload.id);
                if (index !== -1) {
                    state.projects[index] = {
                        ...state.projects[index],
                        name: action.payload.name,
                        color: action.payload.color,
                        favorite: action.payload.favorite,
                    };
                }
            })
            .addCase(fetchProjectDetail.rejected, state => {
                state.selectedProjectDetailStatus = 'failed';
            })
            .addCase(fetchLabels.pending, state => {
                state.labelsStatus = 'loading';
                state.labelsLoadMoreStatus = 'idle';
            })
            .addCase(fetchLabels.fulfilled, (state, action) => {
                state.labelsStatus = 'idle';
                const withCounts = dedupeLabels(action.payload.labels.map(l => ({
                    ...l,
                    taskCount: countTasksForLabel(state.list, l.id),
                })));
                state.labels = applyFavoriteFlags(withCounts, state.favorites, 'LABEL');
                state.labelsHasNext = action.payload.hasNext;
                state.labelsNextCursor = action.payload.nextCursor;
            })
            .addCase(fetchLabels.rejected, state => {
                state.labelsStatus = 'failed';
            })
            .addCase(fetchMoreLabels.pending, state => {
                state.labelsLoadMoreStatus = 'loading';
            })
            .addCase(fetchMoreLabels.fulfilled, (state, action) => {
                state.labelsLoadMoreStatus = 'idle';
                const existingIds = new Set(state.labels.map(l => l.id));
                const merged = applyFavoriteFlags(action.payload.labels, state.favorites, 'LABEL');
                for (const label of merged) {
                    if (!existingIds.has(label.id)) {
                        state.labels.push({
                            ...label,
                            taskCount: countTasksForLabel(state.list, label.id),
                        });
                    }
                }
                state.labelsHasNext = action.payload.hasNext;
                state.labelsNextCursor = action.payload.nextCursor;
            })
            .addCase(fetchMoreLabels.rejected, state => {
                state.labelsLoadMoreStatus = 'failed';
            })
            .addCase(addHabit.fulfilled, (state, action) => {
                if (!state.list.some(h => h.id === action.payload.id)) {
                    state.list.push(action.payload);
                }
            })
            .addCase(addProject.fulfilled, (state, action) => {
                if (!state.projects.some(p => p.id === action.payload.id)) {
                    state.projects.push(action.payload);
                }
                state.favorites = upsertFavoriteFromProject(state.favorites, action.payload);
            })
            .addCase(updateProject.fulfilled, (state, action) => {
                const index = state.projects.findIndex(p => p.id === action.payload.id);
                const nextProject = index !== -1
                    ? {
                        ...state.projects[index],
                        name: action.payload.name,
                        color: action.payload.color,
                        favorite: action.payload.favorite,
                    }
                    : action.payload;
                if (index !== -1) {
                    state.projects[index] = nextProject;
                }
                state.favorites = upsertFavoriteFromProject(state.favorites, nextProject);
            })
            .addCase(deleteProject.fulfilled, (state, action) => {
                state.projects = state.projects.filter(p => p.id !== action.payload);
                state.favorites = state.favorites.filter(
                    f => !(f.targetType === 'PROJECT' && f.targetId === action.payload),
                );
                if (state.selectedProjectId === action.payload) {
                    state.selectedProjectId = null;
                }
            })
            .addCase(addLabel.fulfilled, (state, action) => {
                if (!state.labels.some(l => l.id === action.payload.id)) {
                    state.labels.push({
                        ...action.payload,
                        taskCount: countTasksForLabel(state.list, action.payload.id),
                    });
                }
                state.favorites = upsertFavoriteFromLabel(state.favorites, action.payload);
            })
            .addCase(updateLabel.fulfilled, (state, action) => {
                const index = state.labels.findIndex(l => l.id === action.payload.id);
                const nextLabel = index !== -1
                    ? {
                        ...state.labels[index],
                        name: action.payload.name,
                        color: action.payload.color,
                        favorite: action.payload.favorite,
                    }
                    : action.payload;
                if (index !== -1) {
                    state.labels[index] = nextLabel;
                }
                state.favorites = upsertFavoriteFromLabel(state.favorites, nextLabel);
            })
            .addCase(deleteLabel.fulfilled, (state, action) => {
                state.labels = state.labels.filter(l => l.id !== action.payload);
                state.favorites = state.favorites.filter(
                    f => !(f.targetType === 'LABEL' && f.targetId === action.payload),
                );
                if (state.selectedLabelId === action.payload) {
                    state.selectedLabelId = null;
                }
            })
            .addCase(checkHabit.pending, (state, action) => {
                const habit = state.list.find(h => h.id === action.meta.arg.habitId);
                if (habit) habit.completedToday = !action.meta.arg.wasCompleted;
            })
            .addCase(checkHabit.fulfilled, (state, action) => {
                patchHabitInLists(state, action.payload.id, () => action.payload);
            })
            .addCase(checkHabit.rejected, (state, action) => {
                const habit = state.list.find(h => h.id === action.meta.arg.habitId);
                if (habit) habit.completedToday = action.meta.arg.wasCompleted;
                state.error = action.error.message ?? '완료 처리에 실패했습니다.';
            })
            .addCase(updateHabit.fulfilled, (state, action) => {
                patchHabitInLists(state, action.payload.id, () => action.payload);
            })
            .addCase(patchTaskDueDate.fulfilled, (state, action) => {
                patchHabitInLists(state, action.payload.id, prev => ({
                    ...action.payload,
                    recurrenceLabel: action.payload.recurrenceLabel ?? prev.recurrenceLabel,
                    isRecurring: action.payload.isRecurring || prev.isRecurring,
                }));
            })
            .addCase(patchTaskPriority.fulfilled, (state, action) => {
                patchHabitInLists(state, action.payload.id, () => action.payload);
            })
            .addCase(patchTaskLabels.fulfilled, (state, action) => {
                patchHabitInLists(state, action.payload.id, () => action.payload);
            })
            .addCase(patchTaskProject.fulfilled, (state, action) => {
                patchHabitInLists(state, action.payload.id, () => action.payload);
            })
            .addCase(deleteHabit.fulfilled, (state, action) => {
                state.list = state.list.filter(h => h.id !== action.payload);
            })
            .addCase(deleteHabit.rejected, (state, action) => {
                state.error = action.error.message ?? '작업을 삭제하지 못했습니다.';
            })
            .addCase(reorderHabit.pending, (state, action) => {
                applyHabitReorder(state, action.meta.arg);
            })
            .addCase(reorderHabit.rejected, (state, action) => {
                state.error = typeof action.payload === 'string'
                    ? action.payload
                    : action.error.message ?? '작업 순서를 변경하지 못했습니다.';
            })
            .addCase(fetchHabitDetail.fulfilled, (state, action) => {
                patchHabitInLists(state, action.payload.id, () => action.payload);
            })
            .addCase(addSubtask.fulfilled, (state, action) => {
                const habit = state.list.find(h => h.id === action.payload.habitId);
                if (habit) {
                    const { subtask } = action.payload;
                    const existing = habit.subtasks.find(s => s.id === subtask.id);
                    if (!existing) {
                        habit.subtasks = [...(habit.subtasks ?? []), subtask];
                    }
                    habit.subtaskCount = habit.subtasks.length;
                }
            })
            .addCase(toggleSubtask.fulfilled, (state, action) => {
                const { habitId, subtaskId, completed } = action.payload;
                const habit = state.list.find(h => h.id === habitId);
                if (habit) {
                    const subtask = habit.subtasks.find(s => s.id === subtaskId);
                    if (subtask) {
                        subtask.completed = completed;
                    }
                    habit.subtaskCompletedCount = habit.subtasks.filter(s => s.completed).length;
                }
            })
            .addCase(toggleSubtask.rejected, (state, action) => {
                state.error = action.error.message ?? '하위 작업 상태 변경에 실패했습니다.';
            })
            .addCase(addComment.fulfilled, (state, action) => {
                const { habitId, commentCount } = action.payload;
                patchHabitInLists(state, habitId, habit => ({ ...habit, commentCount }));
            })
            .addCase(syncHabitCommentCount.fulfilled, (state, action) => {
                const { habitId, commentCount } = action.payload;
                patchHabitInLists(state, habitId, habit => ({ ...habit, commentCount }));
            })
            .addCase(uploadAttachments.fulfilled, (state, action) => {
                const { habitId, commentCount } = action.payload;
                patchHabitInLists(state, habitId, habit => ({ ...habit, commentCount }));
            });
    },
});

export const { setActiveView, setSelectedProject, setSelectedLabel, clearHabitError, setUpcomingAnchorDate } =
    habitSlice.actions;
export default habitSlice.reducer;
