export type PriorityType = 'P1' | 'P2' | 'P3' | 'P4';

export type ActivityType = 'ADDED' | 'COMPLETED' | 'UPDATED' | 'DELETED' | 'MOVED' | 'INVITED';

export type NotificationType = 'PROJECT' | 'TASK';

export interface LabelDto {
    id: number;
    name: string;
    color: string;
    sortOrder?: number;
}

export interface LabelDetailDto extends LabelDto {
    favorite?: boolean;
}

export interface LabelUpdatePayload {
    name: string;
    color?: string;
    favorite?: boolean;
}

export interface CommentDto {
    id?: number;
    content: string;
    createdAt?: string;
    attachments?: { fileUrl: string; originalFileName: string }[];
}

export interface ScrollResponse<T> {
    content: T[];
    hasNext: boolean;
    nextCursor: number | null;
}

export interface TaskDto {
    id: number;
    name: string;
    description?: string | null;
    isCompleted?: boolean;
    completed?: boolean;
    priorityType?: PriorityType | null;
    taskPriorityType?: PriorityType | null;
    dueDate?: string | null;
    sortOrder?: number;
    userId?: number | null;
    userName?: string | null;
    projectId?: number | null;
    projectName?: string | null;
    projectColor?: string | null;
    parentId?: number | null;
    countSubTasks?: number;
    countSubTasksCompleted?: number;
    countComments?: number;
    subTasks?: TaskDto[];
    labels?: LabelDto[];
    comments?: CommentDto[];
}

export type TaskFilterType = 'INBOX' | 'TODAY' | 'UPCOMING';

export interface ProjectDto {
    id: number;
    name: string;
    color: string;
    taskCount?: number;
}

export interface ProjectDetailDto extends ProjectDto {
    favorite?: boolean;
    parentId?: number | null;
    parentName?: string | null;
    accessType?: ProjectAccessType | string;
    layoutType?: ProjectLayoutType | 'CALENDAR' | string;
}

export interface ProjectMemberListDto {
    memberName: string;
    email: string;
}

export type ProjectAccessType = 'PRIVATE' | 'PUBLIC';
export type ProjectLayoutType = 'LIST' | 'BOARD';

export interface ProjectUpdatePayload {
    name: string;
    color?: string;
    parentId?: number | null;
    accessType?: ProjectAccessType;
    layoutType?: ProjectLayoutType;
    favorite?: boolean;
}

export interface MemberDto {
    id: number;
    name?: string;
    email?: string;
    role?: string;
}

export interface NotificationDto {
    id: number;
    receiverId: number;
    actorId: number;
    actorName: string;
    targetId: number;
    notificationType: NotificationType;
    activityType: ActivityType;
    isConfirmed: boolean;
    createdAt: string;
    customMessage?: string | null;
}

export interface ActivityLogDto {
    id: number;
    activityType: ActivityType;
    userName: string;
    projectName: string;
    createdAt: string;
    customMessage?: string | null;
}

export interface IntegratedSearchDto {
    projects: ProjectDto[];
    tasks: TaskDto[];
    labels: LabelDto[];
}
