import { apiClient } from './client';
import type { PriorityType, ScrollResponse, TaskDto, TaskFilterType } from './types';
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

export async function fetchTaskCount(taskFilterType: TaskFilterType): Promise<number> {
    const { data } = await apiClient.get<number>('/api/tasks/count', {
        params: { taskFilterType },
    });
    return data;
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

export async function fetchTaskById(taskId: number): Promise<TaskDto> {
    const { data } = await apiClient.get<TaskDto>(`/api/tasks/${taskId}`);
    return data;
}

export async function createTask(payload: CreateTaskPayload): Promise<TaskDto> {
    const body = {
        name: payload.name,
        description: payload.description ?? '',
        dueDate: dueDateToApi(payload.dueDate),
        priorityType: payload.priorityType ?? 'P4',
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

export async function updateTask(taskId: number, payload: UpdateTaskPayload): Promise<TaskDto> {
    const body: Record<string, unknown> = {};
    if (payload.name !== undefined) body.name = payload.name;
    if (payload.description !== undefined) body.description = payload.description;
    const { data } = await apiClient.put<TaskDto>(`/api/tasks/${taskId}`, body);
    return data;
}

export async function patchTaskDueDate(taskId: number, dueDate: string | null): Promise<TaskDto> {
    const { data } = await apiClient.patch<TaskDto>(`/api/tasks/${taskId}/due-date`, {
        dueDate: dueDate == null ? null : dueDateToApi(dueDate),
    });
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
