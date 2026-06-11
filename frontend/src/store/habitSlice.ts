import { createSlice, createAsyncThunk } from '@reduxjs/toolkit';
import type { NavItem } from '../components/Sidebar';
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
    readInstanceId,
    repeatLabelToRecurrence,
} from '../api/mappers';
import type { TaskDto } from '../api/types';

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
    /** TaskMaster ID */
    id: number;
    /** TaskInstance ID — 완료 토글용 */
    instanceId: number | null;
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

/** 모달·인스턴스 API 호출에 필요한 작업 식별자 */
export interface TaskSelection {
    masterId: number;
    instanceId: number;
}

export function selectionFromHabit(habit: Habit): TaskSelection | null {
    if (habit.instanceId == null) return null;
    return { masterId: habit.id, instanceId: habit.instanceId };
}

export interface Habit {
    /** TaskMaster ID — 수정·삭제·댓글·프로젝트 이동 등 */
    id: number;
    /** TaskInstance ID — 완료 토글·마감일·상세 조회 등 */
    instanceId: number | null;
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
    tasksNextCursor: number | null;
    inboxTaskCount: number;
    todayTaskCount: number;
}

const initialState: HabitState = {
    list: [],
    projects: [],
    labels: [],
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
    tasksNextCursor: null,
    inboxTaskCount: 0,
    todayTaskCount: 0,
};

export type ApiView = NavItem | 'all';

function taskDtoKey(task: TaskDto): string {
    const instanceId = readInstanceId(task);
    return instanceId != null ? `i:${instanceId}` : `m:${task.id}`;
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
    nextCursor: number | null;
}

async function loadTasksForView(params: {
    view: ApiView;
    projectId?: number | null;
    labelId?: number | null;
}): Promise<LoadedTasksPage> {
    if (params.projectId != null) {
        const tasks = await taskApi.fetchProjectTasks(params.projectId);
        return { tasks, hasNext: false, nextCursor: null };
    }

    if (params.labelId != null) {
        const [today, upcoming] = await Promise.all([
            taskApi.fetchAllTaskPages(taskApi.fetchTodayTasks),
            taskApi.fetchAllTaskPages(taskApi.fetchUpcomingTasks),
        ]);
        const tasks = dedupeTasks([...today, ...upcoming]).filter(t =>
            (t.labels ?? []).some(l => l.id === params.labelId),
        );
        return { tasks, hasNext: false, nextCursor: null };
    }

    switch (params.view) {
        case 'today': {
            const page = await taskApi.fetchTodayTasks();
            return {
                tasks: page.content,
                hasNext: page.hasNext,
                nextCursor: page.nextCursor,
            };
        }
        case 'upcoming': {
            const page = await taskApi.fetchUpcomingTasks();
            return {
                tasks: page.content,
                hasNext: page.hasNext,
                nextCursor: page.nextCursor,
            };
        }
        case 'inbox': {
            const page = await taskApi.fetchInboxTasks();
            return {
                tasks: page.content,
                hasNext: page.hasNext,
                nextCursor: page.nextCursor,
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
                nextCursor: null,
            };
        }
    }
}

export const fetchHabits = createAsyncThunk(
    'habits/fetch',
    async (params: { view: ApiView; projectId?: number | null; labelId?: number | null }) => {
        const page = await loadTasksForView(params);
        return {
            habits: page.tasks.map(t => mapTaskToHabit(t, readInstanceId(t))),
            view: params.view,
            projectId: params.projectId ?? null,
            labelId: params.labelId ?? null,
            hasNext: page.hasNext,
            nextCursor: page.nextCursor,
        };
    },
);

export const fetchMoreHabits = createAsyncThunk(
    'habits/fetchMore',
    async (_, { getState }) => {
        const state = getState() as { habits: HabitState };
        const { activeView, selectedProjectId, selectedLabelId, tasksNextCursor } = state.habits;

        if (
            tasksNextCursor == null
            || selectedProjectId != null
            || selectedLabelId != null
            || (activeView !== 'today' && activeView !== 'upcoming' && activeView !== 'inbox')
        ) {
            return { habits: [], hasNext: false, nextCursor: null };
        }

        const page = activeView === 'today'
            ? await taskApi.fetchTodayTasks(tasksNextCursor)
            : activeView === 'upcoming'
                ? await taskApi.fetchUpcomingTasks(tasksNextCursor)
                : await taskApi.fetchInboxTasks(tasksNextCursor);

        return {
            habits: page.content.map(t => mapTaskToHabit(t, readInstanceId(t))),
            hasNext: page.hasNext,
            nextCursor: page.nextCursor,
        };
    },
);

export const fetchNavTaskCounts = createAsyncThunk('habits/fetchNavTaskCounts', async () => {
    const [inbox, today] = await Promise.all([
        taskApi.fetchTaskCount('INBOX'),
        taskApi.fetchTaskCount('TODAY'),
    ]);
    return { inbox, today };
});

export const fetchProjects = createAsyncThunk('habits/fetchProjects', async () => {
    const list = await projectApi.fetchProjects();
    const details = await Promise.all(
        list.map(p =>
            projectApi.fetchProjectById(p.id).catch(() => ({ ...p, favorite: false })),
        ),
    );
    return list.map((p, i) => mapProject(
        { ...details[i], id: p.id, name: p.name, color: p.color },
        p.taskCount ?? 0,
    ));
});

async function enrichLabels(labels: { id: number; name: string; color: string }[]) {
    const details = await Promise.all(
        labels.map(l =>
            labelApi.fetchLabelById(l.id).catch(() => ({ ...l, favorite: false })),
        ),
    );
    return details.map(l => mapLabel(l));
}

export const fetchLabels = createAsyncThunk('habits/fetchLabels', async () => {
    const page = await labelApi.fetchLabels();
    const labels = await enrichLabels(page.content);
    return {
        labels,
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
    const labels = await enrichLabels(page.content);
    return {
        labels,
        hasNext: page.hasNext,
        nextCursor: page.nextCursor,
    };
});

export const fetchHabitDetail = createAsyncThunk(
    'habits/fetchDetail',
    async ({ instanceId }: TaskSelection) => {
        const task = await taskApi.fetchTaskInstanceById(instanceId);
        return mapTaskToHabit(task, instanceId);
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
    /** TaskMaster ID */
    habitId: number;
    wasCompleted: boolean;
    /** TaskInstance ID — 없으면 목록에서 조회 */
    instanceId?: number | null;
}

function resolveInstanceId(
    habitId: number,
    instanceId: number | null | undefined,
    habits: Habit[],
): number {
    if (instanceId != null) return instanceId;
    const habit = habits.find(h => h.id === habitId);
    if (habit?.instanceId != null) return habit.instanceId;
    return habitId;
}

export const checkHabit = createAsyncThunk(
    'habits/check',
    async ({ habitId, wasCompleted, instanceId }: CheckHabitPayload, { getState }) => {
        const state = getState() as { habits: HabitState };
        const toggleId = resolveInstanceId(habitId, instanceId, state.habits.list);
        const task = await taskApi.toggleTaskCompletion(toggleId, wasCompleted);
        const habit = mapTaskToHabit(task, toggleId);
        const previousInstanceId = resolveInstanceId(habitId, instanceId, state.habits.list);
        return {
            ...habit,
            id: habitId,
            instanceId: readInstanceId(task) ?? previousInstanceId,
            completedToday: !wasCompleted,
        };
    },
);

export const updateHabit = createAsyncThunk(
    'habits/updateHabit',
    async ({ habitId, changes }: { habitId: number; changes: Partial<Habit> }, { getState }) => {
        const state = getState() as { habits: HabitState };
        const prev = state.habits.list.find(h => h.id === habitId);
        const task = await taskApi.updateTask(habitId, {
            name: changes.name,
            description: changes.description,
        });
        return mapTaskToHabit(task, prev?.instanceId);
    },
);

export const patchTaskProject = createAsyncThunk(
    'habits/patchProject',
    async ({ habitId, projectId }: { habitId: number; projectId: number | null }, { getState }) => {
        const state = getState() as { habits: HabitState };
        const prev = state.habits.list.find(h => h.id === habitId);
        const task = await taskApi.patchTaskProject(habitId, projectId);
        return mapTaskToHabit(task, prev?.instanceId);
    },
);

export const patchTaskDueDate = createAsyncThunk(
    'habits/patchDueDate',
    async ({
        habitId,
        instanceId,
        dueDate,
        recurrenceLabel,
    }: {
        habitId: number;
        instanceId: number;
        dueDate: string | null;
        recurrenceLabel?: string | null;
    }) => {
        const recurrence = repeatLabelToRecurrence(recurrenceLabel, dueDate);
        const task = await taskApi.patchTaskDueDate(instanceId, { dueDate, recurrence });
        const habit = mapTaskToHabit(task, instanceId);
        return {
            ...habit,
            id: habitId,
            instanceId,
            isRecurring: recurrence.isRecurring ?? false,
            recurrenceLabel: recurrenceLabel ?? null,
        };
    },
);

export const patchTaskPriority = createAsyncThunk(
    'habits/patchPriority',
    async ({ habitId, priority }: { habitId: number; priority: 1 | 2 | 3 | 4 }, { getState }) => {
        const state = getState() as { habits: HabitState };
        const prev = state.habits.list.find(h => h.id === habitId);
        const task = await taskApi.patchTaskPriority(habitId, priorityToApi(priority));
        return mapTaskToHabit(task, prev?.instanceId);
    },
);

export const patchTaskLabels = createAsyncThunk(
    'habits/patchLabels',
    async ({ habitId, labelIds }: { habitId: number; labelIds: number[] }, { getState }) => {
        const state = getState() as { habits: HabitState };
        const prev = state.habits.list.find(h => h.id === habitId);
        const task = await taskApi.patchTaskLabels(habitId, labelIds);
        return mapTaskToHabit(task, prev?.instanceId);
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
        const instanceId = readInstanceId(task)
            ?? await taskApi.findInstanceIdByMasterId(task.id);
        return {
            habitId,
            subtask: {
                id: task.id,
                instanceId,
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
        {
            habitId,
            subtaskId,
            instanceId,
        }: { habitId: number; subtaskId: number; instanceId?: number | null },
        { getState },
    ) => {
        const state = getState() as { habits: HabitState };
        const habit = state.habits.list.find(h => h.id === habitId);
        const subtask = habit?.subtasks.find(s => s.id === subtaskId);
        const toggleId = instanceId ?? subtask?.instanceId ?? subtaskId;
        const task = await taskApi.toggleTaskCompletion(toggleId, subtask?.completed);
        return {
            habitId,
            subtaskId,
            completed: readCompleted(task),
            instanceId: readInstanceId(task) ?? toggleId,
        };
    },
);

export const addComment = createAsyncThunk(
    'habits/addComment',
    async ({ habitId, text }: { habitId: number; text: string }, { getState }) => {
        const state = getState() as { habits: HabitState };
        await commentApi.createComment(habitId, text);
        const habit = state.habits.list.find(h => h.id === habitId);
        const instanceId = habit?.instanceId;
        if (instanceId == null) {
            throw new Error('작업 일정 정보를 찾을 수 없습니다.');
        }
        const task = await taskApi.fetchTaskInstanceById(instanceId);
        return { habitId, habit: mapTaskToHabit(task, instanceId) };
    },
);

export const uploadAttachments = createAsyncThunk(
    'habits/uploadAttachments',
    async ({ habitId, files }: { habitId: number; files: File[] }, { getState }) => {
        const state = getState() as { habits: HabitState };
        const habit = state.habits.list.find(h => h.id === habitId);
        const instanceId = habit?.instanceId;
        if (instanceId == null) {
            throw new Error('작업 일정 정보를 찾을 수 없습니다.');
        }
        for (const file of files) {
            await commentApi.createComment(habitId, '첨부파일이 등록되었습니다.', file);
        }
        const task = await taskApi.fetchTaskInstanceById(instanceId);
        return { habitId, habit: mapTaskToHabit(task, instanceId) };
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
                state.error = null;
            })
            .addCase(fetchHabits.fulfilled, (state, action) => {
                state.status = 'idle';
                state.list = action.payload.habits;
                state.activeView = action.payload.view;
                state.selectedProjectId = action.payload.projectId;
                state.selectedLabelId = action.payload.labelId;
                state.tasksHasNext = action.payload.hasNext;
                state.tasksNextCursor = action.payload.nextCursor;
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
                const existingKeys = new Set(
                    state.list.map(h => (h.instanceId != null ? `i:${h.instanceId}` : `m:${h.id}`)),
                );
                for (const habit of action.payload.habits) {
                    const key = habit.instanceId != null ? `i:${habit.instanceId}` : `m:${habit.id}`;
                    if (!existingKeys.has(key)) {
                        state.list.push(habit);
                        existingKeys.add(key);
                    }
                }
                state.tasksHasNext = action.payload.hasNext;
                state.tasksNextCursor = action.payload.nextCursor;
            })
            .addCase(fetchMoreHabits.rejected, (state, action) => {
                state.loadMoreStatus = 'failed';
                state.error = action.error.message ?? '작업을 더 불러오지 못했습니다.';
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
                state.projects = action.payload;
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
                state.labels = action.payload.labels.map(l => ({
                    ...l,
                    taskCount: countTasksForLabel(state.list, l.id),
                }));
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
                for (const label of action.payload.labels) {
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
            })
            .addCase(updateProject.fulfilled, (state, action) => {
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
            .addCase(deleteProject.fulfilled, (state, action) => {
                state.projects = state.projects.filter(p => p.id !== action.payload);
                if (state.selectedProjectId === action.payload) {
                    state.selectedProjectId = null;
                }
            })
            .addCase(addLabel.fulfilled, (state, action) => {
                state.labels.push(action.payload);
            })
            .addCase(updateLabel.fulfilled, (state, action) => {
                const index = state.labels.findIndex(l => l.id === action.payload.id);
                if (index !== -1) {
                    state.labels[index] = {
                        ...state.labels[index],
                        name: action.payload.name,
                        color: action.payload.color,
                        favorite: action.payload.favorite,
                    };
                }
            })
            .addCase(deleteLabel.fulfilled, (state, action) => {
                state.labels = state.labels.filter(l => l.id !== action.payload);
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
                        instanceId: action.payload.instanceId ?? prev.instanceId,
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
                    const prev = state.list[index];
                    state.list[index] = {
                        ...action.payload,
                        instanceId: action.payload.instanceId ?? prev.instanceId,
                    };
                }
            })
            .addCase(patchTaskDueDate.fulfilled, (state, action) => {
                const index = state.list.findIndex(h =>
                    h.instanceId != null && action.payload.instanceId != null
                        ? h.instanceId === action.payload.instanceId
                        : h.id === action.payload.id,
                );
                if (index !== -1) {
                    const prev = state.list[index];
                    state.list[index] = {
                        ...action.payload,
                        instanceId: action.payload.instanceId ?? prev.instanceId,
                        recurrenceLabel: action.payload.recurrenceLabel ?? prev.recurrenceLabel,
                        isRecurring: action.payload.isRecurring || prev.isRecurring,
                    };
                }
            })
            .addCase(patchTaskPriority.fulfilled, (state, action) => {
                const index = state.list.findIndex(h => h.id === action.payload.id);
                if (index !== -1) {
                    const prev = state.list[index];
                    state.list[index] = {
                        ...action.payload,
                        instanceId: action.payload.instanceId ?? prev.instanceId,
                    };
                }
            })
            .addCase(patchTaskLabels.fulfilled, (state, action) => {
                const index = state.list.findIndex(h => h.id === action.payload.id);
                if (index !== -1) {
                    const prev = state.list[index];
                    state.list[index] = {
                        ...action.payload,
                        instanceId: action.payload.instanceId ?? prev.instanceId,
                    };
                }
            })
            .addCase(patchTaskProject.fulfilled, (state, action) => {
                const index = state.list.findIndex(h => h.id === action.payload.id);
                if (index !== -1) {
                    const prev = state.list[index];
                    state.list[index] = {
                        ...action.payload,
                        instanceId: action.payload.instanceId ?? prev.instanceId,
                    };
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
                    if (existing) {
                        if (subtask.instanceId != null) existing.instanceId = subtask.instanceId;
                    } else {
                        habit.subtasks = [...(habit.subtasks ?? []), subtask];
                    }
                    habit.subtaskCount = habit.subtasks.length;
                }
            })
            .addCase(toggleSubtask.fulfilled, (state, action) => {
                const { habitId, subtaskId, completed, instanceId } = action.payload;
                const habit = state.list.find(h => h.id === habitId);
                if (habit) {
                    const subtask = habit.subtasks.find(s => s.id === subtaskId);
                    if (subtask) {
                        subtask.completed = completed;
                        if (instanceId != null) subtask.instanceId = instanceId;
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
