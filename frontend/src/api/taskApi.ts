import { apiClient } from './client';
import type { PriorityType, ScrollResponse, TaskDto, TaskFilterType } from './types';
import { readCompleted, readInstanceId, type RecurrenceApiPayload } from './mappers';
import { buildScrollParams, TASK_PAGE_SIZE } from './pagination';

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
    const { data } = await apiClient.get<number>('/api/tasks/count', {
        params: { taskFilterType },
    });
    return data;
}

export async function fetchInboxTasks(
    lastTaskId?: number,
    size = TASK_PAGE_SIZE,
): Promise<ScrollResponse<TaskDto>> {
    const { data } = await apiClient.get<ScrollResponse<TaskDto>>('/api/task-instances/inbox', {
        params: buildScrollParams(lastTaskId, 'lastTaskId', size),
    });
    return data;
}

export async function fetchTodayTasks(
    lastTaskId?: number,
    size = TASK_PAGE_SIZE,
): Promise<ScrollResponse<TaskDto>> {
    const { data } = await apiClient.get<ScrollResponse<TaskDto>>('/api/task-instances/today', {
        params: buildScrollParams(lastTaskId, 'lastTaskId', size),
    });
    return data;
}

export async function fetchUpcomingTasks(
    lastTaskId?: number,
    size = TASK_PAGE_SIZE,
): Promise<ScrollResponse<TaskDto>> {
    const { data } = await apiClient.get<ScrollResponse<TaskDto>>('/api/task-instances/upcoming', {
        params: buildScrollParams(lastTaskId, 'lastTaskId', size),
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

/** TaskInstance 상세 조회 */
export async function fetchTaskInstanceById(instanceId: number): Promise<TaskDto> {
    const { data } = await apiClient.get<TaskDto>(`/api/task-instances/${instanceId}`);
    return data;
}

/** @deprecated TaskMaster ID로 조회 불가 — fetchTaskInstanceById 사용 */
export async function fetchTaskById(instanceId: number): Promise<TaskDto> {
    return fetchTaskInstanceById(instanceId);
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

export async function updateTask(taskId: number, payload: UpdateTaskPayload): Promise<TaskDto> {
    const body: Record<string, unknown> = {};
    if (payload.name !== undefined) body.name = payload.name;
    if (payload.description !== undefined) body.description = payload.description;
    const { data } = await apiClient.put<TaskDto>(`/api/tasks/${taskId}`, body);
    return data;
}

export interface PatchTaskDueDatePayload {
    dueDate: string | null;
    recurrence?: RecurrenceApiPayload;
}

export async function patchTaskDueDate(
    instanceId: number,
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
    const { data } = await apiClient.patch<TaskDto>(`/api/task-instances/${instanceId}/due-date`, body);
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

/** 목록 API에서 masterId로 TaskInstance ID 조회 (하위 작업 탐색용) */
export async function findInstanceIdByMasterId(masterId: number): Promise<number | null> {
    const loaders = [fetchTodayTasks, fetchUpcomingTasks, fetchInboxTasks];
    for (const loadPage of loaders) {
        const tasks = await fetchAllTaskPages(loadPage);
        const match = tasks.find(t => t.id === masterId);
        const instanceId = match ? readInstanceId(match) : null;
        if (instanceId != null) return instanceId;
    }
    return null;
}

/** @param instanceId TaskInstance ID (백엔드 toggle API는 인스턴스 기준) */
export async function toggleTaskCompletion(
    instanceId: number,
    previousCompleted?: boolean,
): Promise<TaskDto> {
    const { data } = await apiClient.patch<TaskDto>(`/api/task-instances/${instanceId}/toggle`);
    if (
        previousCompleted !== undefined
        && readCompleted(data) === previousCompleted
    ) {
        return { ...data, completed: !previousCompleted };
    }
    return data;
}
