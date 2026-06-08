import { apiClient } from './client';
import type { PriorityType, ScrollResponse, TaskDto } from './types';
import { dueDateToApi, readCompleted } from './mappers';

export interface CreateTaskPayload {
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

export async function fetchInboxTasks(
    lastTaskId?: number,
): Promise<ScrollResponse<TaskDto>> {
    const { data } = await apiClient.get<ScrollResponse<TaskDto>>('/api/tasks/inbox', {
        params: lastTaskId != null ? { lastTaskId } : undefined,
    });
    return data;
}

export async function fetchTodayTasks(
    lastTaskId?: number,
): Promise<ScrollResponse<TaskDto>> {
    const { data } = await apiClient.get<ScrollResponse<TaskDto>>('/api/tasks/today', {
        params: lastTaskId != null ? { lastTaskId } : undefined,
    });
    return data;
}

export async function fetchUpcomingTasks(
    lastTaskId?: number,
): Promise<ScrollResponse<TaskDto>> {
    const { data } = await apiClient.get<ScrollResponse<TaskDto>>('/api/tasks/upcoming', {
        params: lastTaskId != null ? { lastTaskId } : undefined,
    });
    return data;
}

export async function fetchAllTaskPages(
    fetchPage: (lastTaskId?: number) => Promise<ScrollResponse<TaskDto>>,
): Promise<TaskDto[]> {
    const all: TaskDto[] = [];
    let lastTaskId: number | undefined;
    let hasNext = true;

    while (hasNext) {
        const page = await fetchPage(lastTaskId);
        if (page.content.length === 0) break;

        all.push(...page.content);

        if (!page.hasNext || page.nextCursor == null) break;
        if (page.nextCursor === lastTaskId) break;

        lastTaskId = page.nextCursor;
        hasNext = page.hasNext;
    }

    return all;
}

export async function fetchProjectTasks(projectId: number): Promise<TaskDto[]> {
    const { data } = await apiClient.get<TaskDto[]>(`/api/projects/${projectId}/tasks`);
    return data;
}

export async function fetchTaskById(id: number): Promise<TaskDto> {
    const { data } = await apiClient.get<TaskDto>(`/api/tasks/${id}`);
    return data;
}

export async function createTask(payload: CreateTaskPayload): Promise<TaskDto> {
    const body = {
        name: payload.name,
        description: payload.description ?? '',
        dueDate: dueDateToApi(payload.dueDate),
        priorityType: payload.priorityType ?? 'FOURTH_PRIORITY',
        projectId: payload.projectId ?? null,
        parentId: payload.parentId ?? null,
        labelIds: payload.labelIds ?? [],
    };
    const { data } = await apiClient.post<TaskDto>(
        '/api/tasks',
        buildTaskFormData(body, payload.file),
        { headers: { 'Content-Type': 'multipart/form-data' } },
    );
    return data;
}

export async function updateTask(id: number, payload: UpdateTaskPayload): Promise<TaskDto> {
    const body: Record<string, unknown> = {};
    if (payload.name !== undefined) body.name = payload.name;
    if (payload.description !== undefined) body.description = payload.description;
    if (payload.dueDate !== undefined) body.dueDate = dueDateToApi(payload.dueDate);
    if (payload.priorityType !== undefined) body.priorityType = payload.priorityType;
    if (payload.projectId !== undefined) body.projectId = payload.projectId;
    if (payload.labelIds !== undefined) body.labelIds = payload.labelIds;
    const { data } = await apiClient.put<TaskDto>(`/api/tasks/${id}`, body);
    return data;
}

export async function deleteTask(id: number): Promise<void> {
    await apiClient.delete(`/api/tasks/${id}`);
}

export async function toggleTaskCompletion(
    id: number,
    previousCompleted?: boolean,
): Promise<TaskDto> {
    const { data } = await apiClient.patch<TaskDto>(`/api/tasks/${id}/toggle`);
    if (
        previousCompleted !== undefined
        && readCompleted(data) === previousCompleted
    ) {
        return { ...data, completed: !previousCompleted };
    }
    return data;
}
