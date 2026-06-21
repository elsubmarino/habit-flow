import type { Habit, Label, Project, CommentItem, Subtask, Attachment } from '../store/habitSlice';
import type { EntityId, PriorityType, TaskDto, TaskListDto, LabelDto, ProjectDto } from './types';
import { datePartFromDue, formatTime12From24, localTimeTo24 } from '../utils/date';

export function readCompleted(task: TaskDto): boolean {
    return task.completed ?? task.isCompleted ?? false;
}

export interface RecurrenceApiPayload {
    isRecurring?: boolean;
    recurrenceRule?: string;
    recurrenceInterval?: number;
    recurrenceDays?: string;
    recurrenceDayOfMonth?: number;
}

const WEEKDAY_CODES = ['SUN', 'MON', 'TUE', 'WED', 'THU', 'FRI', 'SAT'] as const;

export function repeatLabelToRecurrence(
    label: string | null | undefined,
    dueDate: string | null | undefined,
): RecurrenceApiPayload {
    if (!label) return { isRecurring: false };

    const base = dueDate ? new Date(`${dueDate.slice(0, 10)}T00:00:00`) : new Date();
    const weekdayCode = WEEKDAY_CODES[base.getDay()];
    const dayOfMonth = base.getDate();

    if (label === '매일') {
        return { isRecurring: true, recurrenceRule: 'DAILY', recurrenceInterval: 1 };
    }
    if (label === '평일마다 (월-금)') {
        return {
            isRecurring: true,
            recurrenceRule: 'WEEKLY',
            recurrenceInterval: 1,
            recurrenceDays: 'MON,TUE,WED,THU,FRI',
        };
    }
    if (label.startsWith('매주 ')) {
        return {
            isRecurring: true,
            recurrenceRule: 'WEEKLY',
            recurrenceInterval: 1,
            recurrenceDays: weekdayCode,
        };
    }
    if (label.startsWith('매월 ')) {
        return {
            isRecurring: true,
            recurrenceRule: 'MONTHLY',
            recurrenceInterval: 1,
            recurrenceDayOfMonth: dayOfMonth,
        };
    }
    if (label.startsWith('매년 ')) {
        return {
            isRecurring: true,
            recurrenceRule: 'MONTHLY',
            recurrenceInterval: 12,
            recurrenceDayOfMonth: dayOfMonth,
        };
    }

    return { isRecurring: false };
}

const WEEKDAY_LABELS: Record<string, string> = {
    SUN: '일요일',
    MON: '월요일',
    TUE: '화요일',
    WED: '수요일',
    THU: '목요일',
    FRI: '금요일',
    SAT: '토요일',
};

function readRecurring(task: TaskDto): boolean {
    return Boolean(task.recurring ?? task.isRecurring);
}

export function recurrenceToLabel(task: TaskDto): string | null {
    if (!readRecurring(task)) return null;

    const interval = task.recurrenceInterval && task.recurrenceInterval > 0
        ? task.recurrenceInterval
        : 1;

    switch (task.recurrenceRule) {
        case 'DAILY':
            return interval > 1 ? `${interval}일마다` : '매일';
        case 'WEEKLY':
            if (task.recurrenceDays === 'MON,TUE,WED,THU,FRI') {
                return '평일마다 (월-금)';
            }
            if (task.recurrenceDays) {
                const firstDay = task.recurrenceDays.split(',')[0]?.trim();
                const weekday = firstDay ? WEEKDAY_LABELS[firstDay] : null;
                return weekday ? `매주 ${weekday}` : '매주';
            }
            return interval > 1 ? `${interval}주마다` : '매주';
        case 'MONTHLY':
            if (task.recurrenceDayOfMonth != null) {
                return interval > 1
                    ? `${interval}개월마다 ${task.recurrenceDayOfMonth}일`
                    : `매월 ${task.recurrenceDayOfMonth}일`;
            }
            return interval > 1 ? `${interval}개월마다` : '매월';
        default:
            return '반복';
    }
}

export function priorityFromApi(value?: PriorityType | null): 1 | 2 | 3 | 4 {
    switch (value) {
        case 'P1':
            return 1;
        case 'P2':
            return 2;
        case 'P3':
            return 3;
        case 'P4':
        default:
            return 4;
    }
}

export function priorityToApi(value?: 1 | 2 | 3 | 4): PriorityType {
    switch (value) {
        case 1:
            return 'P1';
        case 2:
            return 'P2';
        case 3:
            return 'P3';
        case 4:
        default:
            return 'P4';
    }
}

export function dueDateToApi(isoDate: string | null | undefined): string | undefined {
    if (!isoDate) return undefined;
    if (isoDate.includes('T')) return isoDate;
    return `${isoDate}T00:00:00`;
}

export function dueDateFromApi(value?: string | null): string | null {
    if (!value) return null;
    return value.slice(0, 10);
}

export function mapLabel(dto: LabelDto & { favorite?: boolean }, taskCount = 0): Label {
    return {
        id: dto.id,
        name: dto.name,
        color: dto.color ?? '#808080',
        taskCount,
        favorite: Boolean(dto.favorite),
    };
}

export function mapProject(
    dto: ProjectDto & { favorite?: boolean },
    taskCount = dto.taskCount ?? 0,
): Project {
    return {
        id: dto.id,
        name: dto.name,
        color: dto.color ?? '#4073ff',
        sortOrder: 0,
        taskCount,
        favorite: Boolean(dto.favorite),
    };
}

function mapAttachments(comments?: TaskDto['comments']): Attachment[] {
    if (!comments) return [];
    let seq = 0;
    return comments.flatMap(comment =>
        (comment.attachments ?? []).map(att => {
            seq += 1;
            return {
                id: seq,
                originalFileName: att.originalFileName,
                contentType: null,
                fileSize: 0,
                downloadUrl: att.fileUrl.startsWith('http') ? att.fileUrl : att.fileUrl,
            };
        }),
    );
}

const ATTACHMENT_COMMENT_PLACEHOLDER = '첨부파일이 등록되었습니다.';

type CommentDtoLike = {
    id?: EntityId;
    content: string;
    createdAt?: string;
    attachments?: { fileUrl: string; originalFileName: string }[];
};

function mapCommentAttachments(
    attachments?: { fileUrl: string; originalFileName: string }[],
): Attachment[] {
    return (attachments ?? []).map((att, i) => ({
        id: i + 1,
        originalFileName: att.originalFileName,
        contentType: null,
        fileSize: 0,
        downloadUrl: att.fileUrl.startsWith('http') ? att.fileUrl : att.fileUrl,
    }));
}

function mapSingleComment(comment: CommentDtoLike, index: number): CommentItem | null {
    const attachments = mapCommentAttachments(comment.attachments);
    const isPlaceholder = comment.content === ATTACHMENT_COMMENT_PLACEHOLDER;
    const hasText = Boolean(comment.content?.trim()) && !isPlaceholder;

    if (!hasText && attachments.length === 0) return null;

    return {
        id: index + 1,
        backendId: comment.id,
        text: hasText ? comment.content : '',
        createdAt: comment.createdAt ?? new Date().toISOString(),
        attachments,
    };
}

function mapComments(comments?: TaskDto['comments']): CommentItem[] {
    if (!comments) return [];
    return comments
        .map((c, i) => mapSingleComment(c, i))
        .filter((c): c is CommentItem => c != null);
}

export function mapCommentDtos(comments: CommentDtoLike[]): CommentItem[] {
    return comments
        .map((c, i) => mapSingleComment(c, i))
        .filter((c): c is CommentItem => c != null);
}

function mapSubtasks(subTasks?: TaskDto[]): Subtask[] {
    return (subTasks ?? []).map(st => ({
        id: st.id,
        name: st.name,
        description: st.description ?? '',
        completed: readCompleted(st),
        childCount: st.subTasks?.length ?? 0,
    }));
}

function parseDueFromApi(task: Pick<TaskDto, 'dueDate' | 'dueTime' | 'hasTime' | 'timeSpecified'>): {
    dueDate: string | null;
    dueTime24: string | null;
    hasTime: boolean;
} {
    const date = datePartFromDue(task.dueDate);
    const fromDueTime = localTimeTo24(task.dueTime);
    if (fromDueTime) {
        return { dueDate: date, dueTime24: fromDueTime, hasTime: true };
    }

    const embedded = task.dueDate?.includes('T') ? localTimeTo24(task.dueDate.split('T')[1]) : null;
    if (embedded && !task.dueDate?.endsWith('T00:00:00')) {
        return { dueDate: date, dueTime24: embedded, hasTime: true };
    }

    const timeSpecified = task.timeSpecified ?? task.hasTime ?? false;
    return { dueDate: date, dueTime24: null, hasTime: timeSpecified };
}

function resolveTaskCounts(task: TaskDto): {
    subtaskCount: number;
    subtaskCompletedCount: number;
    commentCount: number;
} {
    if (task.countSubTasks != null) {
        return {
            subtaskCount: task.countSubTasks,
            subtaskCompletedCount: task.countSubTasksCompleted ?? 0,
            commentCount: task.countComments ?? 0,
        };
    }

    const subtasks = task.subTasks ?? [];
    return {
        subtaskCount: subtasks.length,
        subtaskCompletedCount: subtasks.filter(st => readCompleted(st)).length,
        commentCount: task.comments?.length ?? 0,
    };
}

function readTaskPriority(task: TaskDto): PriorityType | null | undefined {
    return task.priorityType ?? task.taskPriorityType;
}

/** toggle·sort-order 등 TaskResponse.Summary 응답 (count* 필드 + subTasks 없음) */
export function isTaskSummaryMutationResponse(task: TaskDto | TaskListDto): task is TaskListDto {
    const dto = task as TaskDto;
    return (task.countSubTasks != null || task.countComments != null)
        && dto.subTasks === undefined;
}

export function normalizeTaskMutationResponse(
    data: TaskDto | TaskListDto,
    _projectIdHint?: EntityId | null,
): TaskDto {
    if (isTaskSummaryMutationResponse(data)) {
        return mapTaskListToDto(data);
    }
    return data;
}

function isTaskSummaryMutation(task: TaskDto): boolean {
    return (task.countSubTasks != null || task.countComments != null)
        && task.subTasks === undefined;
}

/** PATCH/PUT 후 Summary·Detail 혼용 응답을 목록용 Habit으로 병합 */
export function mergeHabitFromTaskMutation(task: TaskDto, previous?: Habit, projectIdHint?: EntityId | null): Habit {
    const next = mapTaskToHabit(task, projectIdHint ?? previous?.projectId);
    if (!previous) return next;

    if (isTaskSummaryMutation(task)) {
        return {
            ...next,
            projectId: next.projectId ?? previous.projectId,
            projectColor: next.projectColor ?? previous.projectColor,
            recurrenceLabel: next.recurrenceLabel ?? previous.recurrenceLabel,
            isRecurring: next.isRecurring || previous.isRecurring,
        };
    }

    return {
        ...next,
        commentCount: (task.comments?.length ?? 0) > 0 || task.countComments != null
            ? next.commentCount
            : previous.commentCount,
        subtaskCount: (task.subTasks?.length ?? 0) > 0 || task.countSubTasks != null
            ? next.subtaskCount
            : previous.subtaskCount,
        subtaskCompletedCount: task.countSubTasksCompleted != null
            ? next.subtaskCompletedCount
            : previous.subtaskCompletedCount,
        recurrenceLabel: next.recurrenceLabel ?? previous.recurrenceLabel,
        isRecurring: next.isRecurring || previous.isRecurring,
        subtasks: next.subtasks.length > 0 ? next.subtasks : previous.subtasks,
        comments: next.comments.length > 0 ? next.comments : previous.comments,
        attachments: next.attachments.length > 0 ? next.attachments : previous.attachments,
        labels: next.labels.length > 0 ? next.labels : previous.labels,
        projectId: next.projectId ?? previous.projectId,
        projectName: next.projectName ?? previous.projectName,
        projectColor: next.projectColor ?? previous.projectColor,
        description: next.description || previous.description,
    };
}

export function mapTaskListToDto(task: TaskListDto): TaskDto {
    return {
        id: task.id,
        name: task.name,
        description: task.description ?? '',
        taskPriorityType: task.taskPriorityType,
        dueDate: task.dueDate ?? null,
        dueTime: task.dueTime ?? null,
        hasTime: task.timeSpecified ?? task.hasTime ?? Boolean(task.dueTime),
        sortOrder: task.sortOrder,
        projectName: task.projectName ?? null,
        countSubTasks: task.countSubTasks,
        countSubTasksCompleted: task.countSubTasksCompleted,
        countComments: task.countComments,
        completed: task.completed,
        labels: task.labels ?? [],
    };
}

export function mapTaskToHabit(task: TaskDto, projectIdHint?: EntityId | null): Habit {
    const completed = readCompleted(task);
    const { dueDate, dueTime24, hasTime } = parseDueFromApi(task);
    const counts = resolveTaskCounts(task);
    return {
        id: task.id,
        name: task.name,
        description: task.description ?? '',
        streak: 0,
        lastCompletedDate: completed ? dueDate : null,
        dueDate,
        dueTime: hasTime && dueTime24 ? formatTime12From24(dueTime24) : null,
        dueTime24,
        hasTime,
        parentId: task.parentId != null ? String(task.parentId) : null,
        userName: task.userName ?? null,
        projectId: projectIdHint ?? (task.projectId != null ? String(task.projectId) : null),
        projectName: task.projectName ?? null,
        projectColor: task.projectColor ?? null,
        completedToday: completed,
        priority: priorityFromApi(readTaskPriority(task)),
        labels: (task.labels ?? []).map(l => mapLabel(l)),
        attachments: mapAttachments(task.comments),
        reminders: [],
        subtasks: mapSubtasks(task.subTasks),
        comments: mapComments(task.comments),
        subtaskCount: counts.subtaskCount,
        subtaskCompletedCount: counts.subtaskCompletedCount,
        commentCount: counts.commentCount,
        sortOrder: task.sortOrder ?? 0,
        isRecurring: readRecurring(task),
        recurrenceLabel: recurrenceToLabel(task),
    };
}
