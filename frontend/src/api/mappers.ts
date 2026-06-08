import type { Habit, Label, Project, CommentItem, Subtask, Attachment } from '../store/habitSlice';
import type { PriorityType, TaskDto, LabelDto, ProjectDto } from './types';

export function readCompleted(task: TaskDto): boolean {
    return task.completed ?? task.isCompleted ?? false;
}

export function priorityFromApi(value?: PriorityType | null): 1 | 2 | 3 | 4 {
    switch (value) {
        case 'FIRST_PRIORITY':
            return 1;
        case 'SECOND_PRIORITY':
            return 2;
        case 'THIRD_PRIORITY':
            return 3;
        case 'FOURTH_PRIORITY':
        default:
            return 4;
    }
}

export function priorityToApi(value?: 1 | 2 | 3 | 4): PriorityType {
    switch (value) {
        case 1:
            return 'FIRST_PRIORITY';
        case 2:
            return 'SECOND_PRIORITY';
        case 3:
            return 'THIRD_PRIORITY';
        case 4:
        default:
            return 'FOURTH_PRIORITY';
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

export function mapLabel(dto: LabelDto, taskCount = 0): Label {
    return {
        id: dto.id,
        name: dto.name,
        color: dto.color ?? '#808080',
        taskCount,
    };
}

export function mapProject(dto: ProjectDto, taskCount = 0): Project {
    return {
        id: dto.id,
        name: dto.name,
        color: dto.color ?? '#4073ff',
        sortOrder: 0,
        taskCount,
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

function mapComments(comments?: TaskDto['comments']): CommentItem[] {
    if (!comments) return [];
    return comments
        .filter(c => c.content && c.content !== '첨부파일이 등록되었습니다.')
        .map((c, i) => ({
            id: i + 1,
            text: c.content,
            createdAt: new Date().toISOString(),
        }));
}

function mapSubtasks(subTasks?: TaskDto[]): Subtask[] {
    return (subTasks ?? []).map(st => ({
        id: st.id,
        name: st.name,
        description: '',
        completed: readCompleted(st),
    }));
}

export function mapTaskToHabit(task: TaskDto): Habit {
    const completed = readCompleted(task);
    return {
        id: task.id,
        name: task.name,
        description: task.description ?? '',
        streak: 0,
        lastCompletedDate: completed ? dueDateFromApi(task.dueDate) : null,
        dueDate: dueDateFromApi(task.dueDate),
        projectId: task.projectId ?? null,
        projectName: task.projectName ?? null,
        projectColor: task.projectColor ?? null,
        completedToday: completed,
        priority: priorityFromApi(task.priorityType),
        labels: (task.labels ?? []).map(l => mapLabel(l)),
        attachments: mapAttachments(task.comments),
        reminders: [],
        subtasks: mapSubtasks(task.subTasks),
        comments: mapComments(task.comments),
    };
}
