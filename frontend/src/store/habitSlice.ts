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
import * as taskApi from '../api/taskApi';
import * as commentApi from '../api/commentApi';
import {
    mapLabel,
    mapProject,
    mapTaskToHabit,
    priorityToApi,
    readCompleted,
    repeatLabelToRecurrence,
} from '../api/mappers';
import type { FavoriteDto, TaskDto } from '../api/types';

export interface Label {
    id: number;
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
    id: number;
    name: string;
    description: string;
    completed: boolean;
    childCount: number;
}

export interface CommentItem {
    id: number;
    backendId?: number;
    text: string;
    createdAt: string;
    attachments: Attachment[];
}

export function habitRowKey(habit: Habit): string {
    return String(habit.id);
}

export interface Habit {
    id: number;
    name: string;
    description: string;
    streak: number;
    lastCompletedDate: string | null;
    dueDate: string | null;
    dueTime: string | null;
    parentId: number | null;
    userName: string | null;
    projectId: number | null;
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
    isRecurring: boolean;
    recurrenceLabel: string | null;
}

export function attachmentDownloadUrl(path: string): string {
    if (path.startsWith('http')) return path;
    return path.startsWith('/') ? path : `/${path}`;
}

export interface Project {
    id: number;
    name: string;
    color: string;
    sortOrder: number;
    taskCount: number;
    favorite: boolean;
}

interface HabitState {
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
    labelsNextCursor: number | null;
    error: string | null;
    activeView: ApiView;
    selectedProjectId: number | null;
    selectedLabelId: number | null;
    tasksHasNext: boolean;
    /** 다음 요청에 사용할 0-based page 번호 (첫 페이지 로드 후 hasNext면 1) */
    tasksNextPage: number;
    inboxTaskCount: number;
    todayTaskCount: number;
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
    activeView: 'today',
    selectedProjectId: null,
    selectedLabelId: null,
    tasksHasNext: false,
    tasksNextPage: 0,
    inboxTaskCount: 0,
    todayTaskCount: 0,
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

function countTasksForLabel(habits: Habit[], labelId: number) {
    return habits.filter(h => h.labels.some(l => l.id === labelId)).length;
}

interface LoadedTasksPage {
    tasks: TaskDto[];
    hasNext: boolean;
    nextPage: number;
}

async function loadTasksForView(params: {
    view: ApiView;
    projectId?: number | null;
    labelId?: number | null;
}): Promise<LoadedTasksPage> {
    if (params.projectId != null) {
        const tasks = await taskApi.fetchProjectTasks(params.projectId);
        return { tasks, hasNext: false, nextPage: 0 };
    }

    if (params.labelId != null) {
        const [today, upcoming] = await Promise.all([
            taskApi.fetchAllTaskPages(taskApi.fetchTodayTasks),
            taskApi.fetchAllTaskPages(taskApi.fetchUpcomingTasks),
        ]);
        const tasks = dedupeTasks([...today, ...upcoming]).filter(t =>
            (t.labels ?? []).some(l => l.id === params.labelId),
        );
        return { tasks, hasNext: false, nextPage: 0 };
    }

    switch (params.view) {
        case 'today': {
            const page = await taskApi.fetchTodayTasks(0);
            return {
                tasks: page.content,
                hasNext: page.hasNext,
                nextPage: page.hasNext ? 1 : 0,
            };
        }
        case 'upcoming': {
            const page = await taskApi.fetchUpcomingTasks(0);
            return {
                tasks: page.content,
                hasNext: page.hasNext,
                nextPage: page.hasNext ? 1 : 0,
            };
        }
        case 'inbox': {
            const page = await taskApi.fetchInboxTasks(0);
            return {
                tasks: page.content,
                hasNext: page.hasNext,
                nextPage: page.hasNext ? 1 : 0,
            };
        }
        case 'filters':
        case 'report':
        case 'all':
        default: {
            const [today, upcoming] = await Promise.all([
                taskApi.fetchAllTaskPages(taskApi.fetchTodayTasks),
                taskApi.fetchAllTaskPages(taskApi.fetchUpcomingTasks),
            ]);
            return {
                tasks: dedupeTasks([...today, ...upcoming]),
                hasNext: false,
                nextPage: 0,
            };
        }
    }
}

type FetchHabitsParams = { view: ApiView; projectId?: number | null; labelId?: number | null };

type FetchHabitsResult = {
    habits: Habit[];
    view: ApiView;
    projectId: number | null;
    labelId: number | null;
    hasNext: boolean;
    nextPage: number;
};

function fetchHabitsKey(params: FetchHabitsParams): string {
    return `${params.view}:${params.projectId ?? ''}:${params.labelId ?? ''}`;
}

const inFlightHabitsFetches = new Map<string, Promise<FetchHabitsResult>>();

async function loadHabitsForView(params: FetchHabitsParams): Promise<FetchHabitsResult> {
    const page = await loadTasksForView(params);
    return {
        habits: page.tasks.map(t => mapTaskToHabit(t)),
        view: params.view,
        projectId: params.projectId ?? null,
        labelId: params.labelId ?? null,
        hasNext: page.hasNext,
        nextPage: page.nextPage,
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

export const fetchMoreHabits = createAsyncThunk(
    'habits/fetchMore',
    async (viewOverride: ApiView | undefined, { getState }) => {
        const state = getState() as { habits: HabitState };
        const { activeView, selectedProjectId, selectedLabelId, tasksNextPage } = state.habits;
        const view = viewOverride ?? activeView;

        if (
            selectedProjectId != null
            || selectedLabelId != null
            || (view !== 'today' && view !== 'upcoming' && view !== 'inbox')
        ) {
            return { habits: [], hasNext: false };
        }

        const page = view === 'today'
            ? await taskApi.fetchTodayTasks(tasksNextPage)
            : view === 'upcoming'
                ? await taskApi.fetchUpcomingTasks(tasksNextPage)
                : await taskApi.fetchInboxTasks(tasksNextPage);

        return {
            habits: page.content.map(t => mapTaskToHabit(t)),
            hasNext: page.hasNext,
        };
    },
    {
        condition: (_, { getState }) => {
            const { loadMoreStatus, tasksHasNext } = (getState() as { habits: HabitState }).habits;
            return loadMoreStatus === 'idle' && tasksHasNext;
        },
    },
);

export const fetchNavTaskCounts = createAsyncThunk('habits/fetchNavTaskCounts', async () => {
    const [inbox, today] = await Promise.all([
        taskApi.fetchTaskCount('INBOX'),
        taskApi.fetchTaskCount('TODAY'),
    ]);
    return { inbox, today };
});

export const fetchFavorites = createAsyncThunk('habits/fetchFavorites', async () => {
    return favoriteApi.fetchFavorites();
});

export const fetchProjects = createAsyncThunk('habits/fetchProjects', async () => {
    const list = await projectApi.fetchProjects();
    return list.map(p => mapProject(p, p.taskCount ?? 0));
});

export const fetchLabels = createAsyncThunk('habits/fetchLabels', async () => {
    const page = await labelApi.fetchLabels();
    return {
        labels: page.content.map(l => mapLabel(l)),
        hasNext: page.hasNext,
        nextCursor: page.nextCursor,
    };
});

export const fetchMoreLabels = createAsyncThunk('habits/fetchMoreLabels', async (_, { getState }) => {
    const state = getState() as { habits: HabitState };
    const { labelsNextCursor } = state.habits;
    if (labelsNextCursor == null) {
        return { labels: [], hasNext: false, nextCursor: null };
    }

    const page = await labelApi.fetchLabels(labelsNextCursor);
    return {
        labels: page.content.map(l => mapLabel(l)),
        hasNext: page.hasNext,
        nextCursor: page.nextCursor,
    };
});

export const fetchHabitDetail = createAsyncThunk(
    'habits/fetchDetail',
    async (taskId: number) => {
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
        projectId?: number | null;
        dueDate?: string | null;
        recurrenceLabel?: string | null;
        labelIds?: number[];
        file?: File | null;
        priority?: 1 | 2 | 3 | 4;
    }) => {
        const recurrence = repeatLabelToRecurrence(payload.recurrenceLabel, payload.dueDate);
        const task = await taskApi.createTask({
            name: payload.name,
            description: payload.description,
            dueDate: payload.dueDate,
            projectId: payload.projectId,
            labelIds: payload.labelIds,
            file: payload.file,
            priorityType: priorityToApi(payload.priority),
            ...recurrence,
        });
        return mapTaskToHabit(task);
    },
);

export const addProject = createAsyncThunk(
    'habits/addProject',
    async (payload: { name: string; color?: string }) => {
        const project = await projectApi.createProject(payload.name, payload.color);
        return mapProject(project);
    },
);

export const updateProject = createAsyncThunk(
    'habits/updateProject',
    async (payload: {
        id: number;
        name: string;
        color?: string;
        parentId?: number | null;
        accessType?: 'PRIVATE' | 'PUBLIC';
        layoutType?: 'LIST' | 'BOARD';
        favorite?: boolean;
    }) => {
        const { id, ...body } = payload;
        const project = await projectApi.updateProject(id, body);
        return mapProject(project);
    },
);

export const deleteProject = createAsyncThunk('habits/deleteProject', async (projectId: number) => {
    await projectApi.deleteProject(projectId);
    return projectId;
});

export const addLabel = createAsyncThunk(
    'habits/addLabel',
    async (payload: { name: string; color?: string; favorite?: boolean }) => {
        const label = await labelApi.createLabel(payload.name, payload.color, payload.favorite);
        return mapLabel(label);
    },
);

export const updateLabel = createAsyncThunk(
    'habits/updateLabel',
    async (payload: { id: number; name: string; color?: string; favorite?: boolean }) => {
        const { id, ...body } = payload;
        const label = await labelApi.updateLabel(id, body);
        return mapLabel(label);
    },
);

export const deleteLabel = createAsyncThunk('habits/deleteLabel', async (labelId: number) => {
    await labelApi.deleteLabel(labelId);
    return labelId;
});

export interface CheckHabitPayload {
    habitId: number;
    wasCompleted: boolean;
}

export const checkHabit = createAsyncThunk(
    'habits/check',
    async ({ habitId, wasCompleted }: CheckHabitPayload) => {
        const task = await taskApi.toggleTaskCompletion(habitId, wasCompleted);
        const habit = mapTaskToHabit(task);
        return {
            ...habit,
            id: habitId,
            completedToday: !wasCompleted,
        };
    },
);

export const updateHabit = createAsyncThunk(
    'habits/updateHabit',
    async ({ habitId, changes }: { habitId: number; changes: Partial<Habit> }) => {
        const task = await taskApi.updateTask(habitId, {
            name: changes.name,
            description: changes.description,
        });
        return mapTaskToHabit(task);
    },
);

export const patchTaskProject = createAsyncThunk(
    'habits/patchProject',
    async ({ habitId, projectId }: { habitId: number; projectId: number | null }) => {
        const task = await taskApi.patchTaskProject(habitId, projectId);
        return mapTaskToHabit(task);
    },
);

export const patchTaskDueDate = createAsyncThunk(
    'habits/patchDueDate',
    async ({
        habitId,
        dueDate,
        recurrenceLabel,
    }: {
        habitId: number;
        dueDate: string | null;
        recurrenceLabel?: string | null;
    }) => {
        const recurrence = recurrenceLabel !== undefined
            ? repeatLabelToRecurrence(recurrenceLabel, dueDate)
            : undefined;
        const task = await taskApi.patchTaskDueDate(habitId, { dueDate, recurrence });
        const habit = mapTaskToHabit(task);
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
    async ({ habitId, priority }: { habitId: number; priority: 1 | 2 | 3 | 4 }) => {
        const task = await taskApi.patchTaskPriority(habitId, priorityToApi(priority));
        return mapTaskToHabit(task);
    },
);

export const patchTaskLabels = createAsyncThunk(
    'habits/patchLabels',
    async ({ habitId, labelIds }: { habitId: number; labelIds: number[] }) => {
        const task = await taskApi.patchTaskLabels(habitId, labelIds);
        return mapTaskToHabit(task);
    },
);

export const deleteHabit = createAsyncThunk('habits/delete', async (habitId: number) => {
    await taskApi.deleteTask(habitId);
    return habitId;
});

export const addSubtask = createAsyncThunk(
    'habits/addSubtask',
    async ({
        habitId,
        name,
        description,
        projectId,
        dueDate,
    }: {
        habitId: number;
        name: string;
        description: string;
        projectId?: number | null;
        dueDate?: string | null;
    }) => {
        const task = await taskApi.createTask({
            name,
            description,
            parentId: habitId,
            projectId: projectId ?? null,
            dueDate: dueDate ?? null,
        });
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
        { habitId, subtaskId }: { habitId: number; subtaskId: number },
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
    async ({ habitId, text }: { habitId: number; text: string }) => {
        await commentApi.createComment(habitId, text);
        const task = await taskApi.fetchTaskById(habitId);
        return { habitId, habit: mapTaskToHabit(task) };
    },
);

export const uploadAttachments = createAsyncThunk(
    'habits/uploadAttachments',
    async ({ habitId, files }: { habitId: number; files: File[] }) => {
        for (const file of files) {
            await commentApi.createComment(habitId, '첨부파일이 등록되었습니다.', file);
        }
        const task = await taskApi.fetchTaskById(habitId);
        return { habitId, habit: mapTaskToHabit(task) };
    },
);

const habitSlice = createSlice({
    name: 'habits',
    initialState,
    reducers: {
        setActiveView(state, action: { payload: ApiView }) {
            state.activeView = action.payload;
            state.selectedProjectId = null;
            state.selectedLabelId = null;
        },
        setSelectedProject(state, action: { payload: number | null }) {
            state.selectedProjectId = action.payload;
            state.selectedLabelId = null;
        },
        setSelectedLabel(state, action: { payload: number | null }) {
            state.selectedLabelId = action.payload;
            state.selectedProjectId = null;
        },
        clearHabitError(state) {
            state.error = null;
        },
    },
    extraReducers: builder => {
        builder
            .addCase(fetchHabits.pending, state => {
                state.status = 'loading';
                state.loadMoreStatus = 'idle';
                state.tasksNextPage = 0;
                state.error = null;
            })
            .addCase(fetchHabits.fulfilled, (state, action) => {
                state.status = 'idle';
                state.list = action.payload.habits;
                state.activeView = action.payload.view;
                state.selectedProjectId = action.payload.projectId;
                state.selectedLabelId = action.payload.labelId;
                state.tasksHasNext = action.payload.hasNext;
                state.tasksNextPage = action.payload.hasNext ? action.payload.nextPage : 0;
                state.labels = state.labels.map(l => ({
                    ...l,
                    taskCount: countTasksForLabel(action.payload.habits, l.id),
                }));
            })
            .addCase(fetchHabits.rejected, (state, action) => {
                state.status = 'failed';
                state.error = action.error.message ?? '작업 목록을 불러오지 못했습니다.';
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
                if (action.payload.hasNext) {
                    state.tasksNextPage += 1;
                }
            })
            .addCase(fetchMoreHabits.rejected, (state, action) => {
                state.loadMoreStatus = 'failed';
                state.error = action.error.message ?? '작업을 더 불러오지 못했습니다.';
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
            .addCase(fetchLabels.pending, state => {
                state.labelsStatus = 'loading';
                state.labelsLoadMoreStatus = 'idle';
            })
            .addCase(fetchLabels.fulfilled, (state, action) => {
                state.labelsStatus = 'idle';
                const withCounts = action.payload.labels.map(l => ({
                    ...l,
                    taskCount: countTasksForLabel(state.list, l.id),
                }));
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
                state.labels.push(action.payload);
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
                const index = state.list.findIndex(h => h.id === action.payload.id);
                if (index !== -1) {
                    const prev = state.list[index];
                    state.list[index] = {
                        ...action.payload,
                        recurrenceLabel: action.payload.recurrenceLabel ?? prev.recurrenceLabel,
                        isRecurring: action.payload.isRecurring || prev.isRecurring,
                    };
                }
            })
            .addCase(checkHabit.rejected, (state, action) => {
                const habit = state.list.find(h => h.id === action.meta.arg.habitId);
                if (habit) habit.completedToday = action.meta.arg.wasCompleted;
                state.error = action.error.message ?? '완료 처리에 실패했습니다.';
            })
            .addCase(updateHabit.fulfilled, (state, action) => {
                const index = state.list.findIndex(h => h.id === action.payload.id);
                if (index !== -1) {
                    state.list[index] = action.payload;
                }
            })
            .addCase(patchTaskDueDate.fulfilled, (state, action) => {
                const index = state.list.findIndex(h => h.id === action.payload.id);
                if (index !== -1) {
                    const prev = state.list[index];
                    state.list[index] = {
                        ...action.payload,
                        recurrenceLabel: action.payload.recurrenceLabel ?? prev.recurrenceLabel,
                        isRecurring: action.payload.isRecurring || prev.isRecurring,
                    };
                }
            })
            .addCase(patchTaskPriority.fulfilled, (state, action) => {
                const index = state.list.findIndex(h => h.id === action.payload.id);
                if (index !== -1) {
                    state.list[index] = action.payload;
                }
            })
            .addCase(patchTaskLabels.fulfilled, (state, action) => {
                const index = state.list.findIndex(h => h.id === action.payload.id);
                if (index !== -1) {
                    state.list[index] = action.payload;
                }
            })
            .addCase(patchTaskProject.fulfilled, (state, action) => {
                const index = state.list.findIndex(h => h.id === action.payload.id);
                if (index !== -1) {
                    state.list[index] = action.payload;
                }
            })
            .addCase(deleteHabit.fulfilled, (state, action) => {
                state.list = state.list.filter(h => h.id !== action.payload);
            })
            .addCase(deleteHabit.rejected, (state, action) => {
                state.error = action.error.message ?? '작업을 삭제하지 못했습니다.';
            })
            .addCase(fetchHabitDetail.fulfilled, (state, action) => {
                const index = state.list.findIndex(h => h.id === action.payload.id);
                if (index !== -1) state.list[index] = action.payload;
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
                const index = state.list.findIndex(h => h.id === action.payload.habitId);
                if (index !== -1) state.list[index] = action.payload.habit;
            })
            .addCase(uploadAttachments.fulfilled, (state, action) => {
                const index = state.list.findIndex(h => h.id === action.payload.habitId);
                if (index !== -1) state.list[index] = action.payload.habit;
            });
    },
});

export const { setActiveView, setSelectedProject, setSelectedLabel, clearHabitError } =
    habitSlice.actions;
export default habitSlice.reducer;
