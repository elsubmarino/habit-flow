export type EntityId = string;

export type PriorityType = 'P1' | 'P2' | 'P3' | 'P4';

export type FavoriteTargetType = 'PROJECT' | 'LABEL';

export interface FavoriteDto {
    id: EntityId;
    targetType: FavoriteTargetType;
    targetId: EntityId;
    targetName: string;
    targetCount: number;
}

export type ActivityType =
    | 'ADDED'
    | 'COMPLETED'
    | 'UPDATED'
    | 'DELETED'
    | 'MOVED'
    | 'INVITED'
    | 'UNCOMPLETED'
    | 'JOINED';

export type ActivityTargetType = 'PROJECT' | 'TASK' | 'COMMENT';

export interface ActivityLogActorDto {
    id: EntityId;
    name: string;
}

export interface ActivityLogTargetDto {
    type: ActivityTargetType;
    id: EntityId;
    name: string | null;
}

export interface ActivityLogChangeSetDto {
    field: string;
    from: string | null;
    to: string | null;
}

export type NotificationType = 'PROJECT' | 'TASK';

export interface LabelDto {
    id: EntityId;
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
    id?: EntityId;
    content: string;
    createdAt?: string;
    attachments?: { fileUrl: string; originalFileName: string }[];
}

/** @deprecated PaginatedResult 사용 */
export type ScrollResponse<T> = import('./slice').PaginatedResult<T>;

export interface UpcomingDateCountDto {
    upcomingDate: string;
    count: number;
}

/** Task ID */
export interface TaskDto {
    id: EntityId;
    name: string;
    description?: string | null;
    isCompleted?: boolean;
    completed?: boolean;
    priorityType?: PriorityType | null;
    taskPriorityType?: PriorityType | null;
    dueDate?: string | null;
    dueTime?: string | null;
    hasTime?: boolean;
    timeSpecified?: boolean;
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
    recurring?: boolean;
    /** @deprecated API는 recurring 사용 */
    isRecurring?: boolean;
    recurrenceRule?: string | null;
    recurrenceInterval?: number;
    recurrenceDays?: string | null;
    recurrenceDayOfMonth?: number | null;
    subTasks?: TaskDto[];
    labels?: LabelDto[];
    comments?: CommentDto[];
}

/** PUT/PATCH mutation — Detail 또는 Summary */
export type TaskMutationDto = TaskDto | TaskListDto;

/** GET /api/tasks/*, /api/projects/{id}/tasks 목록·toggle/sort-order Summary 응답 */
export interface TaskListDto {
    id: EntityId;
    name: string;
    description?: string | null;
    taskPriorityType?: PriorityType | null;
    dueDate?: string | null;
    dueTime?: string | null;
    hasTime?: boolean;
    timeSpecified?: boolean;
    sortOrder?: number;
    projectName?: string | null;
    countSubTasks?: number;
    countSubTasksCompleted?: number;
    countComments?: number;
    completed?: boolean;
    labels?: LabelDto[];
}

export type TaskFilterType = 'INBOX' | 'TODAY' | 'UPCOMING' | 'OVERDUE';

/** GET /api/tasks/sidebar-count — 사이드바 관리함·오늘 건수 */
export interface SidebarTasksCountDto {
    inbox: number;
    today: number;
}

export interface ProjectDto {
    id: EntityId;
    name: string;
    color: string;
    taskCount?: number;
    sortOrder?: number;
}

export interface ProjectDetailDto extends ProjectDto {
    favorite?: boolean;
    parentId?: EntityId | null;
    parentName?: string | null;
    accessType?: ProjectAccessType | string;
    layoutType?: ProjectLayoutType | 'CALENDAR' | string;
}

export interface ProjectMemberListDto {
    memberId?: EntityId;
    memberName: string;
    email: string;
}

export type ProjectAccessType = 'PRIVATE' | 'PUBLIC';
export type ProjectLayoutType = 'LIST' | 'BOARD';

export interface ProjectUpdatePayload {
    name: string;
    color?: string;
    parentId?: EntityId | null;
    accessType?: ProjectAccessType;
    layoutType?: ProjectLayoutType;
    favorite?: boolean;
}

export interface MemberDto {
    id: EntityId;
    name?: string;
    email?: string;
    role?: string;
}

export interface NotificationDto {
    id: EntityId;
    receiverId: EntityId;
    actorId: EntityId;
    actorName: string;
    targetId: EntityId;
    notificationType: NotificationType;
    activityType: ActivityType;
    isConfirmed: boolean;
    createdAt: string;
    customMessage?: string | null;
}

export interface ActivityLogDto {
    id: EntityId;
    activityType: ActivityType;
    actor: ActivityLogActorDto;
    target: ActivityLogTargetDto;
    createdAt: string;
    changes?: ActivityLogChangeSetDto[] | null;
}

export interface IntegratedSearchDto {
    projects: ProjectDto[];
    tasks: TaskDto[];
    labels: LabelDto[];
}
