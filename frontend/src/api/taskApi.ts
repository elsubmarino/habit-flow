import { apiClient } from './client';
import { dedupeInFlight } from './inFlight';
import type { PriorityType, ScrollResponse, TaskDto, TaskFilterType, TaskListDto } from './types';
import { mapTaskListToDto, readCompleted, type RecurrenceApiPayload } from './mappers';
import { buildPageParams, TASK_PAGE_SIZE } from './pagination';

export interface CreateTaskPayload extends RecurrenceApiPayload {
    name: string;
    description?: string;
    dueDate?: string | null;
    priorityType?: PriorityType;
    projectId?: number | null;
    parentId?: number | null;
    labelIds?: number[];
    file?: File | null;
}

export interface UpdateTaskPayload {
    name?: string;
    description?: string;
    dueDate?: string | null;
    priorityType?: PriorityType;
    projectId?: number | null;
    labelIds?: number[];
}

function buildTaskFormData(body: Record<string, unknown>, file?: File | null) {
    const form = new FormData();
    form.append(
        'taskRequest',
        new Blob([JSON.stringify(body)], { type: 'application/json' }),
    );
    if (file) form.append('file', file);
    return form;
}

export async function fetchTaskCount(taskFilterType: TaskFilterType): Promise<number> {
    return dedupeInFlight(`task-count:${taskFilterType}`, async () => {
        const { data } = await apiClient.get<number>('/api/tasks/count', {
            params: { taskFilterType },
        });
        return data;
    });
}

export async function fetchInboxTasks(
    page = 0,
    size = TASK_PAGE_SIZE,
): Promise<ScrollResponse<TaskDto>> {
    return dedupeInFlight(`tasks:inbox:${page}:${size}`, async () => {
        const { data } = await apiClient.get<ScrollResponse<TaskDto>>('/api/tasks/inbox', {
            params: buildPageParams(size, page),
        });
        return data;
    });
}

export async function fetchTodayTasks(
    page = 0,
    size = TASK_PAGE_SIZE,
): Promise<ScrollResponse<TaskDto>> {
    return dedupeInFlight(`tasks:today:${page}:${size}`, async () => {
        const { data } = await apiClient.get<ScrollResponse<TaskDto>>('/api/tasks/today', {
            params: buildPageParams(size, page),
        });
        return data;
    });
}

export async function fetchUpcomingTasks(
    page = 0,
    size = TASK_PAGE_SIZE,
): Promise<ScrollResponse<TaskDto>> {
    return dedupeInFlight(`tasks:upcoming:${page}:${size}`, async () => {
        const { data } = await apiClient.get<ScrollResponse<TaskDto>>('/api/tasks/upcoming', {
            params: buildPageParams(size, page),
        });
        return data;
    });
}

export async function fetchAllTaskPages(
    fetchPage: (page?: number) => Promise<ScrollResponse<TaskDto>>,
): Promise<TaskDto[]> {
    const all: TaskDto[] = [];
    let page = 0;
    let hasNext = true;

    while (hasNext) {
        const result = await fetchPage(page);
        if (result.content.length === 0) break;

        all.push(...result.content);

        if (!result.hasNext) break;
        page += 1;
        hasNext = result.hasNext;
    }

    return all;
}

export async function fetchProjectTasks(
    projectId: number,
    page = 0,
    size = TASK_PAGE_SIZE,
): Promise<ScrollResponse<TaskDto>> {
    return dedupeInFlight(`project-tasks:${projectId}:${page}:${size}`, async () => {
        const { data } = await apiClient.get<ScrollResponse<TaskListDto>>(
            `/api/projects/${projectId}/tasks`,
            { params: buildPageParams(size, page) },
        );
        const content = Array.isArray(data.content) ? data.content : [];
        return {
            content: content.map(task => mapTaskListToDto(task, projectId)),
            hasNext: Boolean(data.hasNext),
            nextCursor: data.nextCursor ?? null,
        };
    });
}

export async function fetchTaskById(taskId: number): Promise<TaskDto> {
    return dedupeInFlight(`task:${taskId}`, async () => {
        const { data } = await apiClient.get<TaskDto>(`/api/tasks/${taskId}`);
        return data;
    });
}

export async function createTask(payload: CreateTaskPayload): Promise<TaskDto> {
    const body = {
        name: payload.name,
        description: payload.description ?? '',
        dueDate: payload.dueDate ? payload.dueDate.slice(0, 10) : undefined,
        taskPriorityType: payload.priorityType ?? 'P4',
        projectId: payload.projectId ?? null,
        parentId: payload.parentId ?? null,
        labelIds: payload.labelIds ?? [],
        recurring: payload.isRecurring ?? false,
        recurrenceRule: payload.recurrenceRule,
        recurrenceInterval: payload.recurrenceInterval ?? 0,
        recurrenceDays: payload.recurrenceDays,
        recurrenceDayOfMonth: payload.recurrenceDayOfMonth,
    };
    const { data } = await apiClient.post<TaskDto>(
        '/api/tasks',
        buildTaskFormData(body, payload.file),
        { headers: { 'Content-Type': 'multipart/form-data' } },
    );
    return data;
}

export async function updateTask(taskId: number, payload: UpdateTaskPayload): Promise<void> {
    const body: Record<string, unknown> = {};
    if (payload.name !== undefined) body.name = payload.name;
    if (payload.description !== undefined) body.description = payload.description;
    await apiClient.put(`/api/tasks/${taskId}`, body);
}

export interface PatchTaskDueDatePayload {
    dueDate: string | null;
    recurrence?: RecurrenceApiPayload;
}

export async function patchTaskDueDate(
    taskId: number,
    payload: PatchTaskDueDatePayload,
): Promise<TaskDto> {
    const date = payload.dueDate == null ? null : payload.dueDate.slice(0, 10);
    const recurrence = payload.recurrence;
    const body: Record<string, unknown> = { dueDate: date };
    if (recurrence != null) {
        body.recurring = recurrence.isRecurring ?? false;
        body.recurrenceInterval = recurrence.recurrenceInterval ?? 0;
        body.recurrenceRule = recurrence.recurrenceRule ?? null;
        body.recurrenceDays = recurrence.recurrenceDays ?? null;
        if (recurrence.recurrenceDayOfMonth != null) {
            body.recurrenceDayOfMonth = recurrence.recurrenceDayOfMonth;
        }
    }
    const { data } = await apiClient.patch<TaskDto>(`/api/tasks/${taskId}/due-date`, body);
    return data;
}

export async function patchTaskPriority(taskId: number, priorityType: PriorityType): Promise<TaskDto> {
    const { data } = await apiClient.patch<TaskDto>(`/api/tasks/${taskId}/priority`, {
        taskPriorityType: priorityType,
    });
    return data;
}

export async function patchTaskLabels(taskId: number, labelIds: number[]): Promise<TaskDto> {
    const { data } = await apiClient.patch<TaskDto>(`/api/tasks/${taskId}/labels`, {
        labelIds,
    });
    return data;
}

export async function patchTaskProject(taskId: number, projectId: number | null): Promise<TaskDto> {
    const { data } = await apiClient.patch<TaskDto>(`/api/tasks/${taskId}/project`, {
        projectId,
    });
    return data;
}

export async function deleteTask(taskId: number): Promise<void> {
    await apiClient.delete(`/api/tasks/${taskId}`);
}

export async function toggleTaskCompletion(
    taskId: number,
    previousCompleted?: boolean,
): Promise<TaskDto> {
    const { data } = await apiClient.patch<TaskDto>(`/api/tasks/${taskId}/toggle`);
    if (
        previousCompleted !== undefined
        && readCompleted(data) === previousCompleted
    ) {
        return { ...data, completed: !previousCompleted };
    }
    return data;
}
