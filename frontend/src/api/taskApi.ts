import { apiClient } from './client';
import { dedupeInFlight } from './inFlight';
import type { PriorityType, TaskDto, TaskFilterType, TaskListDto, UpcomingDateCountDto } from './types';
import { mapTaskListToDto, readCompleted, type RecurrenceApiPayload } from './mappers';
import { buildPageParams, TASK_PAGE_SIZE } from './pagination';
import { getUpcomingDayRange, getUpcomingSummaryDateRange, toLocalDateTime } from '../utils/date';
import { parseSlicePage, type PaginatedResult, type SpringSlice } from './slice';
import {
    buildTaskCursorParams,
    type TaskCursor,
    type TaskListSliceDto,
    type TaskPageResult,
} from './taskCursor';

export type { TaskCursor, TaskPageResult };

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

export interface UpcomingTasksQuery {
    fromDate?: string | null;
    toDate?: string | null;
    cursor?: TaskCursor | null;
    size?: number;
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

function mapTaskListSlice(
    slice: TaskListSliceDto<TaskListDto>,
    projectId?: number,
): TaskPageResult<TaskDto> {
    const content = (slice.content ?? []).map(task => mapTaskListToDto(task, projectId));
    return {
        content,
        hasNext: slice.hasNext ?? false,
        hasPrev: slice.hasPrev ?? false,
        nextCursor: slice.nextCursor ?? null,
        prevCursor: slice.prevCursor ?? null,
    };
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

async function fetchTaskListSlice(
    path: string,
    cacheKey: string,
    cursor?: TaskCursor | null,
    size = TASK_PAGE_SIZE,
    extra?: Record<string, string | number | undefined>,
): Promise<TaskPageResult<TaskDto>> {
    return dedupeInFlight(cacheKey, async () => {
        const { data } = await apiClient.get<TaskListSliceDto<TaskListDto>>(path, {
            params: buildTaskCursorParams(cursor, size, extra),
        });
        return mapTaskListSlice(data);
    });
}

export async function fetchTaskCount(taskFilterType: TaskFilterType): Promise<number> {
    return dedupeInFlight(`task-count:${taskFilterType}`, async () => {
        const { data } = await apiClient.get<number>('/api/tasks/count', {
            params: { taskFilterType },
        });
        return data;
    });
}

export async function fetchOverdueTasks(
    cursor?: TaskCursor | null,
    size = TASK_PAGE_SIZE,
): Promise<TaskPageResult<TaskDto>> {
    const key = `tasks:overdue:${cursor?.lastTaskId ?? 'start'}:${cursor?.direction ?? 'NEXT'}:${size}`;
    return fetchTaskListSlice('/api/tasks/overdue', key, cursor, size);
}

export async function fetchInboxTasks(
    cursor?: TaskCursor | null,
    size = TASK_PAGE_SIZE,
): Promise<TaskPageResult<TaskDto>> {
    const key = `tasks:inbox:${cursor?.lastTaskId ?? 'start'}:${cursor?.direction ?? 'NEXT'}:${size}`;
    return fetchTaskListSlice('/api/tasks/inbox', key, cursor, size);
}

export async function fetchTodayTasks(
    cursor?: TaskCursor | null,
    size = TASK_PAGE_SIZE,
): Promise<TaskPageResult<TaskDto>> {
    const key = `tasks:today:${cursor?.lastTaskId ?? 'start'}:${cursor?.direction ?? 'NEXT'}:${size}`;
    return fetchTaskListSlice('/api/tasks/today', key, cursor, size);
}

export async function fetchUpcomingTasks(
    query: UpcomingTasksQuery = {},
): Promise<TaskPageResult<TaskDto>> {
    const { fromDate, toDate, cursor, size = TASK_PAGE_SIZE } = query;
    const key = [
        'tasks:upcoming',
        fromDate ?? '',
        toDate ?? '',
        cursor?.lastTaskId ?? 'start',
        cursor?.direction ?? 'NEXT',
        size,
    ].join(':');

    return fetchTaskListSlice('/api/tasks/upcoming', key, cursor, size, {
        fromDate: fromDate ?? undefined,
        toDate: toDate ?? undefined,
    });
}

export function mapUpcomingSummaryToCounts(rows: UpcomingDateCountDto[]): Record<string, number> {
    const counts: Record<string, number> = {};
    for (const row of rows) {
        const key = row.upcomingDate.slice(0, 10);
        counts[key] = row.count;
    }
    return counts;
}

export async function fetchUpcomingSummary(
    fromDate?: string,
    toDate?: string,
): Promise<UpcomingDateCountDto[]> {
    const range = getUpcomingSummaryDateRange();
    const from = fromDate ?? range.fromDate;
    const to = toDate ?? range.toDate;
    const key = `tasks:upcoming:summary:${from}:${to}`;

    return dedupeInFlight(key, async () => {
        const { data } = await apiClient.get<UpcomingDateCountDto[]>('/api/tasks/upcoming/summary', {
            params: { fromDate: from, toDate: to },
        });
        return data ?? [];
    });
}

/** 다음 뷰 날짜별 페이징: /upcoming + 하루 범위 (오늘 포함) */
export async function fetchTasksForUpcomingDay(
    dateKey: string,
    cursor?: TaskCursor | null,
    size = TASK_PAGE_SIZE,
): Promise<TaskPageResult<TaskDto>> {
    const { fromDate, toDate } = getUpcomingDayRange(dateKey);
    return fetchUpcomingTasks({ fromDate, toDate, cursor, size });
}

export async function fetchAllTaskPages(
    fetchPage: (cursor?: TaskCursor | null) => Promise<TaskPageResult<TaskDto>>,
): Promise<TaskDto[]> {
    const all: TaskDto[] = [];
    let cursor: TaskCursor | null = null;
    let hasNext = true;

    while (hasNext) {
        const result = await fetchPage(cursor);
        if (result.content.length === 0) break;

        all.push(...result.content);

        if (!result.hasNext || !result.nextCursor) break;
        cursor = { ...result.nextCursor, direction: 'NEXT' };
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
