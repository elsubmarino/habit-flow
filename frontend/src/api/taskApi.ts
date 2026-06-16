import { apiClient } from './client';
import { dedupeInFlight } from './inFlight';
import type { PriorityType, TaskDto, TaskFilterType, TaskListDto } from './types';
import { mapTaskListToDto, readCompleted, type RecurrenceApiPayload } from './mappers';
import { buildPageParams, TASK_PAGE_SIZE } from './pagination';
import { toLocalDateTime } from '../utils/date';
import { parseSlicePage, type PaginatedResult, type SpringSlice } from './slice';

export interface CreateTaskPayload extends RecurrenceApiPayload {
    name: string;
    description?: string;
    dueDate?: string | null;
    dueTime24?: string | null;
    hasTime?: boolean;
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

function mapTaskListPage(
    slice: SpringSlice<TaskListDto>,
    projectId?: number,
): PaginatedResult<TaskDto> {
    const page = parseSlicePage(slice);
    return {
        ...page,
        content: page.content.map(task => mapTaskListToDto(task, projectId)),
    };
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
): Promise<PaginatedResult<TaskDto>> {
    return dedupeInFlight(`tasks:inbox:${page}:${size}`, async () => {
        const { data } = await apiClient.get<SpringSlice<TaskListDto>>('/api/tasks/inbox', {
            params: buildPageParams(size, page),
        });
        return mapTaskListPage(data);
    });
}

export async function fetchTodayTasks(
    page = 0,
    size = TASK_PAGE_SIZE,
): Promise<PaginatedResult<TaskDto>> {
    return dedupeInFlight(`tasks:today:${page}:${size}`, async () => {
        const { data } = await apiClient.get<SpringSlice<TaskListDto>>('/api/tasks/today', {
            params: buildPageParams(size, page),
        });
        return mapTaskListPage(data);
    });
}

export async function fetchUpcomingTasks(
    page = 0,
    size = TASK_PAGE_SIZE,
): Promise<PaginatedResult<TaskDto>> {
    return dedupeInFlight(`tasks:upcoming:${page}:${size}`, async () => {
        const { data } = await apiClient.get<SpringSlice<TaskListDto>>('/api/tasks/upcoming', {
            params: buildPageParams(size, page),
        });
        return mapTaskListPage(data);
    });
}

export async function fetchAllTaskPages(
    fetchPage: (page?: number) => Promise<PaginatedResult<TaskDto>>,
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
): Promise<PaginatedResult<TaskDto>> {
    return dedupeInFlight(`project-tasks:${projectId}:${page}:${size}`, async () => {
        const { data } = await apiClient.get<SpringSlice<TaskListDto>>(
            `/api/projects/${projectId}/tasks`,
            { params: buildPageParams(size, page) },
        );
        return mapTaskListPage(data, projectId);
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
        dueDate: payload.dueDate
            ? toLocalDateTime(payload.dueDate, payload.dueTime24, payload.hasTime ?? false)
            : undefined,
        hasTime: payload.hasTime ?? false,
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
    dueTime24?: string | null;
    hasTime: boolean;
    recurrence?: RecurrenceApiPayload;
}

export async function patchTaskDueDate(
    taskId: number,
    payload: PatchTaskDueDatePayload,
): Promise<TaskDto> {
    const recurrence = payload.recurrence;
    const body: Record<string, unknown> = {
        dueDate: payload.dueDate == null
            ? null
            : toLocalDateTime(payload.dueDate, payload.dueTime24, payload.hasTime),
        hasTime: payload.hasTime,
        recurring: recurrence?.isRecurring ?? false,
        recurrenceInterval: recurrence?.recurrenceInterval ?? 0,
        recurrenceRule: recurrence?.recurrenceRule ?? null,
        recurrenceDays: recurrence?.recurrenceDays ?? null,
        recurrenceDayOfMonth: recurrence?.recurrenceDayOfMonth ?? null,
    };
    await apiClient.patch(`/api/tasks/${taskId}/due-date`, body);
    return fetchTaskById(taskId);
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
