export type PriorityType =
    | 'FIRST_PRIORITY'
    | 'SECOND_PRIORITY'
    | 'THIRD_PRIORITY'
    | 'FOURTH_PRIORITY';

export type ActivityType = 'ADDED' | 'COMPLETED' | 'UPDATED' | 'DELETED' | 'MOVED';

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
    content: string;
    attachments?: { fileUrl: string; originalFileName: string }[];
}

export interface TaskDto {
    id: number;
    name: string;
    description?: string | null;
    isCompleted?: boolean;
    completed?: boolean;
    priorityType?: PriorityType | null;
    dueDate?: string | null;
    sortOrder?: number;
    userId?: number | null;
    userName?: string | null;
    projectId?: number | null;
    projectName?: string | null;
    projectColor?: string | null;
    parentId?: number | null;
    subTasks?: TaskDto[];
    labels?: LabelDto[];
    comments?: CommentDto[];
}

export interface ProjectDto {
    id: number;
    name: string;
    color: string;
}

export interface ProjectDetailDto extends ProjectDto {
    favorite?: boolean;
    parentId?: number | null;
    parentName?: string | null;
    accessType?: ProjectAccessType | string;
    layoutType?: ProjectLayoutType | 'CALENDAR' | string;
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
    taskId: number;
    activityType: ActivityType;
    isConfirmed: boolean;
    confirmed?: boolean;
}

export interface ActivityLogDto {
    id: number;
    activityType: ActivityType;
    userName: string;
    projectName: string;
    createdAt: string;
}

export interface IntegratedSearchDto {
    projects: ProjectDto[];
    tasks: TaskDto[];
    labels: LabelDto[];
}
