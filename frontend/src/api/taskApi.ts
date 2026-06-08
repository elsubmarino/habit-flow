import { apiClient } from './client';
import type { PriorityType, TaskDto } from './types';
import { dueDateToApi } from './mappers';

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

export async function fetchTodayTasks(): Promise<TaskDto[]> {
    const { data } = await apiClient.get<TaskDto[]>('/api/tasks/today');
    return data;
}

export async function fetchUpcomingTasks(): Promise<TaskDto[]> {
    const { data } = await apiClient.get<TaskDto[]>('/api/tasks/upcoming');
    return data;
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
