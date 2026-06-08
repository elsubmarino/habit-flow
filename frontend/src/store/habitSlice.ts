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
} from '../api/mappers';
import type { TaskDto } from '../api/types';

export interface Label {
    id: number;
    name: string;
    color: string;
    taskCount: number;
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
}

export interface CommentItem {
    id: number;
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
}

interface HabitState {
    list: Habit[];
    projects: Project[];
    labels: Label[];
    status: 'idle' | 'loading' | 'failed';
    projectsStatus: 'idle' | 'loading' | 'failed';
    labelsStatus: 'idle' | 'loading' | 'failed';
    error: string | null;
    activeView: ApiView;
    selectedProjectId: number | null;
    selectedLabelId: number | null;
}

const initialState: HabitState = {
    list: [],
    projects: [],
    labels: [],
    status: 'idle',
    projectsStatus: 'idle',
    labelsStatus: 'idle',
    error: null,
    activeView: 'today',
    selectedProjectId: null,
    selectedLabelId: null,
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

async function loadTasksForView(params: {
    view: ApiView;
    projectId?: number | null;
    labelId?: number | null;
}): Promise<TaskDto[]> {
    if (params.projectId != null) {
        return taskApi.fetchProjectTasks(params.projectId);
    }

    let tasks: TaskDto[] = [];
    if (params.labelId != null) {
        const [today, upcoming] = await Promise.all([
            taskApi.fetchTodayTasks(),
            taskApi.fetchUpcomingTasks(),
        ]);
        tasks = dedupeTasks([...today, ...upcoming]).filter(t =>
            (t.labels ?? []).some(l => l.id === params.labelId),
        );
        return tasks;
    }

    switch (params.view) {
        case 'today':
            return taskApi.fetchTodayTasks();
        case 'upcoming':
            return taskApi.fetchUpcomingTasks();
        case 'inbox':
            return [];
        case 'filters':
        case 'report':
        case 'all':
        default: {
            const [today, upcoming] = await Promise.all([
                taskApi.fetchTodayTasks(),
                taskApi.fetchUpcomingTasks(),
            ]);
            return dedupeTasks([...today, ...upcoming]);
        }
    }
}

export const fetchHabits = createAsyncThunk(
    'habits/fetch',
    async (params: { view: ApiView; projectId?: number | null; labelId?: number | null }) => {
        const tasks = await loadTasksForView(params);
        return {
            habits: tasks.map(mapTaskToHabit),
            view: params.view,
            projectId: params.projectId ?? null,
            labelId: params.labelId ?? null,
        };
    },
);

export const fetchProjects = createAsyncThunk('habits/fetchProjects', async () => {
    const projects = await projectApi.fetchProjects();
    return projects.map(p => mapProject(p));
});

export const fetchLabels = createAsyncThunk('habits/fetchLabels', async () => {
    const labels = await labelApi.fetchLabels();
    return labels.map(l => mapLabel(l));
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

export const deleteProject = createAsyncThunk('habits/deleteProject', async (projectId: number) => {
    await projectApi.deleteProject(projectId);
    return projectId;
});

export const addLabel = createAsyncThunk(
    'habits/addLabel',
    async (payload: { name: string; color?: string }) => {
        const label = await labelApi.createLabel(payload.name, payload.color);
        return mapLabel(label);
    },
);

export const deleteLabel = createAsyncThunk('habits/deleteLabel', async (labelId: number) => {
    await labelApi.deleteLabel(labelId);
    return labelId;
});

export const checkHabit = createAsyncThunk('habits/check', async (_habitId: number) => {
    throw new Error('백엔드에 작업 완료/취소 API가 없습니다. (TaskUpdateRequest에 isCompleted 없음)');
});

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
    async ({ habitId, name, description }: { habitId: number; name: string; description: string }) => {
        const task = await taskApi.createTask({
            name,
            description,
            parentId: habitId,
        });
        return {
            habitId,
            subtask: {
                id: task.id,
                name: task.name,
                description: description,
                completed: false,
            } satisfies Subtask,
        };
    },
);

export const toggleSubtask = createAsyncThunk(
    'habits/toggleSubtask',
    async (_payload: { habitId: number; subtaskId: number }) => {
        throw new Error('백엔드에 하위 작업 완료 토글 API가 없습니다.');
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
                state.error = null;
            })
            .addCase(fetchHabits.fulfilled, (state, action) => {
                state.status = 'idle';
                state.list = action.payload.habits;
                state.activeView = action.payload.view;
                state.selectedProjectId = action.payload.projectId;
                state.selectedLabelId = action.payload.labelId;
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
            })
            .addCase(fetchLabels.fulfilled, (state, action) => {
                state.labelsStatus = 'idle';
                state.labels = action.payload.map(l => ({
                    ...l,
                    taskCount: countTasksForLabel(state.list, l.id),
                }));
            })
            .addCase(fetchLabels.rejected, state => {
                state.labelsStatus = 'failed';
            })
            .addCase(addHabit.fulfilled, (state, action) => {
                if (!state.list.some(h => h.id === action.payload.id)) {
                    state.list.push(action.payload);
                }
            })
            .addCase(addProject.fulfilled, (state, action) => {
                state.projects.push(action.payload);
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
            .addCase(deleteLabel.fulfilled, (state, action) => {
                state.labels = state.labels.filter(l => l.id !== action.payload);
                if (state.selectedLabelId === action.payload) {
                    state.selectedLabelId = null;
                }
            })
            .addCase(checkHabit.rejected, (state, action) => {
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
