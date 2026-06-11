import type { Habit, Label, Project, CommentItem, Subtask, Attachment } from '../store/habitSlice';
import type { PriorityType, TaskDto, LabelDto, ProjectDto } from './types';

export function readCompleted(task: TaskDto): boolean {
    return task.completed ?? task.isCompleted ?? false;
}

/** TaskMaster ID */
export function readMasterId(task: TaskDto): number {
    return task.masterId ?? task.id;
}

/** TaskInstance ID (완료 토글·일정 변경에 사용) */
export function readInstanceId(task: TaskDto): number | null {
    const id = task.instanceId ?? task.taskInstanceId;
    return id != null ? id : null;
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

export function recurrenceToLabel(task: TaskDto): string | null {
    if (!task.isRecurring) return null;

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
    id?: number;
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
        id: comment.id ?? index + 1,
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

function mapSubtasks(subTasks?: TaskDto[], parentInstanceId?: number | null): Subtask[] {
    return (subTasks ?? []).map(st => ({
        id: readMasterId(st),
        instanceId: readInstanceId(st) ?? parentInstanceId ?? null,
        name: st.name,
        description: st.description ?? '',
        completed: readCompleted(st),
        childCount: st.subTasks?.length ?? 0,
    }));
}

function parseDueDateTime(value?: string | null): { date: string | null; time: string | null } {
    if (!value) return { date: null, time: null };
    const date = value.slice(0, 10);
    if (!value.includes('T')) return { date, time: null };
    const timePart = value.split('T')[1] ?? '';
    if (!timePart || timePart.startsWith('00:00:00')) return { date, time: null };
    const parsed = new Date(value);
    if (Number.isNaN(parsed.getTime())) return { date, time: null };
    const time = parsed.toLocaleTimeString('ko-KR', {
        hour: 'numeric',
        minute: '2-digit',
        hour12: true,
    });
    return { date, time };
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

export function mapTaskToHabit(task: TaskDto, knownInstanceId?: number | null): Habit {
    const completed = readCompleted(task);
    const { date: dueDate, time: dueTime } = parseDueDateTime(task.dueDate);
    const counts = resolveTaskCounts(task);
    const instanceId = readInstanceId(task) ?? knownInstanceId ?? null;
    return {
        id: readMasterId(task),
        instanceId,
        name: task.name,
        description: task.description ?? '',
        streak: 0,
        lastCompletedDate: completed ? dueDate : null,
        dueDate,
        dueTime,
        parentId: task.parentId ?? null,
        userName: task.userName ?? null,
        projectId: task.projectId ?? null,
        projectName: task.projectName ?? null,
        projectColor: task.projectColor ?? null,
        completedToday: completed,
        priority: priorityFromApi(readTaskPriority(task)),
        labels: (task.labels ?? []).map(l => mapLabel(l)),
        attachments: mapAttachments(task.comments),
        reminders: [],
        subtasks: mapSubtasks(task.subTasks, instanceId),
        comments: mapComments(task.comments),
        subtaskCount: counts.subtaskCount,
        subtaskCompletedCount: counts.subtaskCompletedCount,
        commentCount: counts.commentCount,
        isRecurring: Boolean(task.isRecurring),
        recurrenceLabel: recurrenceToLabel(task),
    };
}
