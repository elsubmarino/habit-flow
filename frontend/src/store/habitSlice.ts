import { createSlice, createAsyncThunk } from '@reduxjs/toolkit';
import type { NavItem } from '../components/Sidebar';
import { logActivity } from '../utils/activityLog';
import { formatDueLabel } from '../utils/date';

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
    downloadUrl: string; // object URL in frontend-only mode
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
    return path;
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
    activeView: 'today',
    selectedProjectId: null,
    selectedLabelId: null,
};

export type ApiView = NavItem | 'all';

interface PersistedData {
    habits: Habit[];
    projects: Project[];
    labels: Label[];
    seq: number;
}

const STORAGE_KEY = 'habitflow.frontend.v1';

function nowIsoDate(): string {
    const d = new Date();
    const y = d.getFullYear();
    const m = String(d.getMonth() + 1).padStart(2, '0');
    const day = String(d.getDate()).padStart(2, '0');
    return `${y}-${m}-${day}`;
}

function nextId(data: PersistedData): number {
    data.seq += 1;
    return data.seq;
}

function seedData(): PersistedData {
    return {
        seq: 1000,
        projects: [
            { id: 1, name: '습관', color: '#4073ff', sortOrder: 0, taskCount: 0 },
        ],
        labels: [
            { id: 11, name: '@업무', color: '#4073ff', taskCount: 0 },
            { id: 12, name: '@개인', color: '#299438', taskCount: 0 },
            { id: 13, name: '@중요', color: '#db4c3f', taskCount: 0 },
        ],
        habits: [],
    };
}

function readData(): PersistedData {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) return seedData();
    try {
        const parsed = JSON.parse(raw) as PersistedData;
        return {
            seq: parsed.seq ?? 1000,
            habits: parsed.habits ?? [],
            projects: parsed.projects ?? [],
            labels: parsed.labels ?? [],
        };
    } catch {
        return seedData();
    }
}

function recalcCounts(data: PersistedData) {
    data.projects = data.projects.map(project => ({
        ...project,
        taskCount: data.habits.filter(h => h.projectId === project.id).length,
    }));
    data.labels = data.labels.map(label => ({
        ...label,
        taskCount: data.habits.filter(h => h.labels.some(l => l.id === label.id)).length,
    }));
}

function writeData(data: PersistedData) {
    recalcCounts(data);
    localStorage.setItem(STORAGE_KEY, JSON.stringify(data));
}

function getHabitsByFilter(
    habits: Habit[],
    params: { view: ApiView; projectId?: number | null; labelId?: number | null },
): Habit[] {
    const today = nowIsoDate();
    if (params.labelId != null) {
        return habits.filter(h => h.labels.some(l => l.id === params.labelId));
    }
    if (params.projectId != null) {
        return habits.filter(h => h.projectId === params.projectId);
    }
    switch (params.view) {
        case 'inbox':
            return habits.filter(h => !h.projectId && !h.dueDate);
        case 'today':
            return habits.filter(h => !h.dueDate || h.dueDate <= today);
        case 'upcoming': {
            const end = new Date();
            end.setDate(end.getDate() + 7);
            const endIso = `${end.getFullYear()}-${String(end.getMonth() + 1).padStart(2, '0')}-${String(end.getDate()).padStart(2, '0')}`;
            return habits.filter(h => !!h.dueDate && h.dueDate >= today && h.dueDate <= endIso);
        }
        case 'all':
        default:
            return habits;
    }
}

export const fetchHabits = createAsyncThunk(
    'habits/fetch',
    async (params: { view: ApiView; projectId?: number | null; labelId?: number | null }) => {
        const data = readData();
        const habits = getHabitsByFilter(data.habits, params);
        return {
            habits,
            view: params.view,
            projectId: params.projectId ?? null,
            labelId: params.labelId ?? null,
        };
    },
);

export const fetchProjects = createAsyncThunk('habits/fetchProjects', async () => {
    const data = readData();
    recalcCounts(data);
    writeData(data);
    return data.projects;
});

export const fetchLabels = createAsyncThunk('habits/fetchLabels', async () => {
    const data = readData();
    recalcCounts(data);
    writeData(data);
    return data.labels;
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
    }) => {
        const data = readData();
        const labelMap = new Map(data.labels.map(l => [l.id, l]));
        const selectedLabels = (payload.labelIds ?? [])
            .map(id => labelMap.get(id))
            .filter((x): x is Label => !!x);
        const project = payload.projectId != null
            ? data.projects.find(p => p.id === payload.projectId) ?? null
            : null;
        const habit: Habit = {
            id: nextId(data),
            name: payload.name,
            description: payload.description,
            streak: 0,
            lastCompletedDate: null,
            dueDate: payload.dueDate ?? null,
            projectId: project?.id ?? null,
            projectName: project?.name ?? null,
            projectColor: project?.color ?? null,
            completedToday: false,
            priority: 4,
            labels: selectedLabels,
            attachments: [],
            reminders: [],
            subtasks: [],
            comments: [],
        };
        data.habits.push(habit);
        writeData(data);
        logActivity({
            type: 'added',
            taskId: habit.id,
            taskName: habit.name,
            projectId: habit.projectId,
            projectName: habit.projectName ?? '받은 편지함',
            projectColor: habit.projectColor,
        });
        return habit;
    },
);

export const addProject = createAsyncThunk(
    'habits/addProject',
    async (payload: { name: string; color?: string }) => {
        const data = readData();
        const project: Project = {
            id: nextId(data),
            name: payload.name,
            color: payload.color ?? '#4073ff',
            sortOrder: data.projects.length,
            taskCount: 0,
        };
        data.projects.push(project);
        writeData(data);
        return project;
    },
);

export const deleteProject = createAsyncThunk('habits/deleteProject', async (projectId: number) => {
    const data = readData();
    data.projects = data.projects.filter(p => p.id !== projectId);
    data.habits = data.habits.map(h => h.projectId === projectId ? {
        ...h,
        projectId: null,
        projectName: null,
        projectColor: null,
    } : h);
    writeData(data);
    return projectId;
});

export const addLabel = createAsyncThunk(
    'habits/addLabel',
    async (payload: { name: string; color?: string }) => {
        const data = readData();
        const normalized = payload.name.startsWith('@') ? payload.name : `@${payload.name}`;
        if (data.labels.some(l => l.name.toLowerCase() === normalized.toLowerCase())) {
            throw new Error('이미 존재하는 라벨입니다.');
        }
        const label: Label = {
            id: nextId(data),
            name: normalized,
            color: payload.color ?? '#808080',
            taskCount: 0,
        };
        data.labels.push(label);
        writeData(data);
        return label;
    },
);

export const deleteLabel = createAsyncThunk('habits/deleteLabel', async (labelId: number) => {
    const data = readData();
    data.labels = data.labels.filter(l => l.id !== labelId);
    data.habits = data.habits.map(h => ({
        ...h,
        labels: h.labels.filter(l => l.id !== labelId),
    }));
    writeData(data);
    return labelId;
});

export const checkHabit = createAsyncThunk('habits/check', async (habitId: number) => {
    const data = readData();
    const before = data.habits.find(h => h.id === habitId);
    const today = nowIsoDate();
    data.habits = data.habits.map(h => {
        if (h.id !== habitId) return h;
        if (h.completedToday) {
            return {
                ...h,
                completedToday: false,
                lastCompletedDate: null,
                streak: Math.max(0, h.streak - 1),
            };
        }
        const prevDate = h.lastCompletedDate;
        const yesterday = new Date();
        yesterday.setDate(yesterday.getDate() - 1);
        const yIso = `${yesterday.getFullYear()}-${String(yesterday.getMonth() + 1).padStart(2, '0')}-${String(yesterday.getDate()).padStart(2, '0')}`;
        const nextStreak = prevDate === yIso ? h.streak + 1 : 1;
        return {
            ...h,
            completedToday: true,
            lastCompletedDate: today,
            streak: nextStreak,
        };
    });
    writeData(data);
    const updated = data.habits.find(h => h.id === habitId);
    if (!updated) throw new Error('작업을 찾을 수 없습니다.');
    if (before) {
        logActivity({
            type: before.completedToday ? 'uncompleted' : 'completed',
            taskId: updated.id,
            taskName: updated.name,
            projectId: updated.projectId,
            projectName: updated.projectName ?? '받은 편지함',
            projectColor: updated.projectColor,
        });
    }
    return updated;
});

export const updateHabit = createAsyncThunk(
    'habits/updateHabit',
    async ({ habitId, changes }: { habitId: number; changes: Partial<Habit> }) => {
        const data = readData();
        const before = data.habits.find(h => h.id === habitId);
        data.habits = data.habits.map(h => (h.id === habitId ? { ...h, ...changes } : h));
        writeData(data);
        const updated = data.habits.find(h => h.id === habitId);
        if (!updated || !before) throw new Error('작업을 찾을 수 없습니다.');

        if (changes.dueDate !== undefined && changes.dueDate !== before.dueDate) {
            logActivity({
                type: 'date_changed',
                taskId: updated.id,
                taskName: updated.name,
                projectId: updated.projectId,
                projectName: updated.projectName ?? '받은 편지함',
                projectColor: updated.projectColor,
                meta: changes.dueDate ? formatDueLabel(changes.dueDate) : '날짜 없음',
            });
        }
        if (
            (changes.projectId !== undefined && changes.projectId !== before.projectId)
            || (changes.projectName !== undefined && changes.projectName !== before.projectName)
        ) {
            logActivity({
                type: 'moved',
                taskId: updated.id,
                taskName: updated.name,
                projectId: updated.projectId,
                projectName: updated.projectName ?? '받은 편지함',
                projectColor: updated.projectColor,
            });
        }

        return updated;
    },
);

export const addSubtask = createAsyncThunk(
    'habits/addSubtask',
    async ({ habitId, name, description }: { habitId: number; name: string; description: string }) => {
        const data = readData();
        const habit = data.habits.find(h => h.id === habitId);
        if (!habit) throw new Error('작업을 찾을 수 없습니다.');
        const subtask: Subtask = {
            id: nextId(data),
            name,
            description,
            completed: false,
        };
        habit.subtasks = [...(habit.subtasks ?? []), subtask];
        writeData(data);
        return { habitId, subtask };
    },
);

export const toggleSubtask = createAsyncThunk(
    'habits/toggleSubtask',
    async ({ habitId, subtaskId }: { habitId: number; subtaskId: number }) => {
        const data = readData();
        const habit = data.habits.find(h => h.id === habitId);
        if (!habit) throw new Error('작업을 찾을 수 없습니다.');
        habit.subtasks = (habit.subtasks ?? []).map(s =>
            s.id === subtaskId ? { ...s, completed: !s.completed } : s,
        );
        writeData(data);
        return { habitId, subtasks: habit.subtasks };
    },
);

export const addComment = createAsyncThunk(
    'habits/addComment',
    async ({ habitId, text }: { habitId: number; text: string }) => {
        const data = readData();
        const habit = data.habits.find(h => h.id === habitId);
        if (!habit) throw new Error('작업을 찾을 수 없습니다.');
        const comment: CommentItem = {
            id: nextId(data),
            text,
            createdAt: new Date().toISOString(),
        };
        habit.comments = [...(habit.comments ?? []), comment];
        writeData(data);
        return { habitId, comment };
    },
);

export const uploadAttachments = createAsyncThunk(
    'habits/uploadAttachments',
    async ({ habitId, files }: { habitId: number; files: File[] }) => {
        const data = readData();
        const habit = data.habits.find(h => h.id === habitId);
        if (!habit) throw new Error('작업을 찾을 수 없습니다.');
        const uploaded: Attachment[] = files.map(file => ({
            id: nextId(data),
            originalFileName: file.name,
            contentType: file.type || null,
            fileSize: file.size,
            downloadUrl: URL.createObjectURL(file),
        }));
        habit.attachments = [...(habit.attachments ?? []), ...uploaded];
        writeData(data);
        return { habitId, attachments: uploaded };
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
    },
    extraReducers: builder => {
        builder
            .addCase(fetchHabits.pending, state => { state.status = 'loading'; })
            .addCase(fetchHabits.fulfilled, (state, action) => {
                state.status = 'idle';
                state.list = action.payload.habits.map(h => ({
                    ...h,
                    priority: h.priority ?? 4,
                    attachments: h.attachments ?? [],
                    reminders: h.reminders ?? [],
                    subtasks: h.subtasks ?? [],
                    comments: h.comments ?? [],
                }));
                state.activeView = action.payload.view;
                state.selectedProjectId = action.payload.projectId;
                state.selectedLabelId = action.payload.labelId;
            })
            .addCase(fetchHabits.rejected, state => { state.status = 'failed'; })
            .addCase(fetchProjects.fulfilled, (state, action) => {
                state.projectsStatus = 'idle';
                state.projects = action.payload;
            })
            .addCase(fetchLabels.fulfilled, (state, action) => {
                state.labelsStatus = 'idle';
                state.labels = action.payload;
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
            .addCase(checkHabit.fulfilled, (state, action) => {
                const updated = action.payload;
                if (state.activeView === 'inbox' && updated.completedToday) {
                    state.list = state.list.filter(h => h.id !== updated.id);
                    return;
                }
                const index = state.list.findIndex(h => h.id === updated.id);
                if (index !== -1) state.list[index] = updated;
            })
            .addCase(uploadAttachments.fulfilled, (state, action) => {
                const habit = state.list.find(h => h.id === action.payload.habitId);
                if (habit) {
                    habit.attachments = [...(habit.attachments ?? []), ...action.payload.attachments];
                }
            })
            .addCase(updateHabit.fulfilled, (state, action) => {
                const index = state.list.findIndex(h => h.id === action.payload.id);
                if (index !== -1) state.list[index] = action.payload;
            })
            .addCase(addSubtask.fulfilled, (state, action) => {
                const habit = state.list.find(h => h.id === action.payload.habitId);
                if (habit) habit.subtasks = [...(habit.subtasks ?? []), action.payload.subtask];
            })
            .addCase(toggleSubtask.fulfilled, (state, action) => {
                const habit = state.list.find(h => h.id === action.payload.habitId);
                if (habit) habit.subtasks = action.payload.subtasks;
            })
            .addCase(addComment.fulfilled, (state, action) => {
                const habit = state.list.find(h => h.id === action.payload.habitId);
                if (habit) habit.comments = [...(habit.comments ?? []), action.payload.comment];
            });
    },
});

export const { setActiveView, setSelectedProject, setSelectedLabel } = habitSlice.actions;
export default habitSlice.reducer;
