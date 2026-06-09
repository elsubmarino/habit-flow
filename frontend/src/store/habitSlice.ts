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
};

export type ApiView = NavItem | 'all';

function dedupeTasks(tasks: TaskDto[]): TaskDto[] {
    const map = new Map<number, TaskDto>();
    for (const task of tasks) map.set(task.id, task);
    return [...map.values()];
}

function countTasksForProject(habits: Habit[], projectId: number) {
    return habits.filter(h => h.projectId === projectId).length;
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
            habits: page.tasks.map(mapTaskToHabit),
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
            habits: page.content.map(mapTaskToHabit),
            hasNext: page.hasNext,
            nextCursor: page.nextCursor,
        };
    },
);

export const fetchProjects = createAsyncThunk('habits/fetchProjects', async () => {
    const projects = await projectApi.fetchProjects();
    const details = await Promise.all(
        projects.map(p =>
            projectApi.fetchProjectById(p.id).catch(() => ({ ...p, favorite: false })),
        ),
    );
    return details.map(p => mapProject(p));
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

export const fetchHabitDetail = createAsyncThunk('habits/fetchDetail', async (habitId: number) => {
    const task = await taskApi.fetchTaskById(habitId);
    return mapTaskToHabit(task);
});

export const addHabit = createAsyncThunk(
    'habits/add',
    async (payload: {
        name: string;
        description: string;
        view: NavItem;
        projectId?: number | null;
        dueDate?: string | null;
        labelIds?: number[];
        file?: File | null;
        priority?: 1 | 2 | 3 | 4;
    }) => {
        const task = await taskApi.createTask({
            name: payload.name,
            description: payload.description,
            dueDate: payload.dueDate,
            projectId: payload.projectId,
            labelIds: payload.labelIds,
            file: payload.file,
            priorityType: priorityToApi(payload.priority),
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
            dueDate: changes.dueDate,
            priorityType: changes.priority != null ? priorityToApi(changes.priority) : undefined,
            projectId: changes.projectId,
            labelIds: changes.labels?.map(l => l.id),
        });
        return mapTaskToHabit(task);
    },
);

export const addSubtask = createAsyncThunk(
    'habits/addSubtask',
    async ({
        habitId,
        name,
        description,
        projectId,
    }: {
        habitId: number;
        name: string;
        description: string;
        projectId?: number | null;
    }) => {
        const task = await taskApi.createTask({
            name,
            description,
            parentId: habitId,
            projectId: projectId ?? null,
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
    async ({ habitId, subtaskId }: { habitId: number; subtaskId: number }, { getState }) => {
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
                state.projects = state.projects.map(p => ({
                    ...p,
                    taskCount: countTasksForProject(action.payload.habits, p.id),
                }));
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
                const existingIds = new Set(state.list.map(h => h.id));
                for (const habit of action.payload.habits) {
                    if (!existingIds.has(habit.id)) {
                        state.list.push(habit);
                        existingIds.add(habit.id);
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
            .addCase(fetchProjects.fulfilled, (state, action) => {
                state.projectsStatus = 'idle';
                state.projects = action.payload.map(p => ({
                    ...p,
                    taskCount: countTasksForProject(state.list, p.id),
                }));
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
                if (index !== -1) state.list[index] = action.payload;
            })
            .addCase(checkHabit.rejected, (state, action) => {
                const habit = state.list.find(h => h.id === action.meta.arg.habitId);
                if (habit) habit.completedToday = action.meta.arg.wasCompleted;
                state.error = action.error.message ?? '완료 처리에 실패했습니다.';
            })
            .addCase(updateHabit.fulfilled, (state, action) => {
                const index = state.list.findIndex(h => h.id === action.payload.id);
                if (index !== -1) state.list[index] = action.payload;
            })
            .addCase(fetchHabitDetail.fulfilled, (state, action) => {
                const index = state.list.findIndex(h => h.id === action.payload.id);
                if (index !== -1) state.list[index] = action.payload;
            })
            .addCase(addSubtask.fulfilled, (state, action) => {
                const habit = state.list.find(h => h.id === action.payload.habitId);
                if (habit) habit.subtasks = [...(habit.subtasks ?? []), action.payload.subtask];
            })
            .addCase(toggleSubtask.fulfilled, (state, action) => {
                const { habitId, subtaskId, completed } = action.payload;
                const habit = state.list.find(h => h.id === habitId);
                if (habit) {
                    const subtask = habit.subtasks.find(s => s.id === subtaskId);
                    if (subtask) subtask.completed = completed;
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
