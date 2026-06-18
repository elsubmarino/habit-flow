import type {
    ActivityLogChangeSetDto,
    ActivityLogDto,
    ActivityTargetType,
    ActivityType,
} from '../api/types';
import { getLoggedInMemberId } from '../api/client';
import { getUserProfile } from './userProfile';
import { formatSectionDate } from './date';

export type ActivityLogEntry = ActivityLogDto & {
    changes: ActivityLogChangeSetDto[];
};

function targetName(entry: ActivityLogEntry): string {
    return entry.target.name?.trim() || '항목';
}

function formatChangeDateValue(value: string | null | undefined): string {
    if (!value) return '없음';
    const dateOnly = value.slice(0, 10);
    const hasExplicitTime = /T\d{2}:\d{2}/.test(value) && !value.endsWith('T00:00:00');
    if (hasExplicitTime) {
        const date = new Date(value);
        if (!Number.isNaN(date.getTime())) {
            return date.toLocaleString('ko-KR', {
                month: 'long',
                day: 'numeric',
                hour: '2-digit',
                minute: '2-digit',
            });
        }
    }
    return formatSectionDate(dateOnly);
}

function formatChangeClause(
    change: ActivityLogChangeSetDto,
    name: string,
    targetType: ActivityTargetType,
): string {
    switch (change.field) {
        case 'name':
            if (targetType === 'PROJECT') {
                return `프로젝트 ${change.from ?? name}의 이름을 ${change.to ?? name}(으)로 변경`;
            }
            return `${change.from ?? name}의 이름을 ${change.to ?? name}(으)로 변경`;
        case 'description':
            return `${name}의 설명을 수정`;
        case 'dueDate':
            return `${name}의 날짜를 ${formatChangeDateValue(change.to)}(으)로 변경`;
        case 'priority':
            return `${name}의 우선순위를 ${change.to ?? '새 값'}(으)로 변경`;
        case 'labels':
            return `${name}의 라벨을 ${change.to ?? '새 값'}(으)로 변경`;
        case 'project':
            return `${name}을(를) ${change.to ?? '다른 위치'}(으)로 이동`;
        default:
            return `${name}의 ${change.field}을(를) 수정`;
    }
}

function formatChangesRest(entry: ActivityLogEntry): string {
    const clauses = entry.changes.map(change =>
        formatChangeClause(change, targetName(entry), entry.target.type),
    );
    if (clauses.length === 1) {
        return `님이 ${clauses[0]}했습니다`;
    }
    const last = clauses[clauses.length - 1]!;
    const head = clauses.slice(0, -1).join(', ');
    return `님이 ${head} 및 ${last}했습니다`;
}

export function activityLabel(type: ActivityType): string {
    switch (type) {
        case 'ADDED':
            return '추가했습니다';
        case 'COMPLETED':
            return '완료했습니다';
        case 'UNCOMPLETED':
            return '완료를 취소했습니다';
        case 'INVITED':
            return '초대했습니다';
        case 'JOINED':
            return '합류했습니다';
        case 'MOVED':
            return '이동시켰습니다';
        case 'DELETED':
            return '삭제했습니다';
        case 'UPDATED':
        default:
            return '수정했습니다';
    }
}

export function activityBadge(type: ActivityType): string {
    switch (type) {
        case 'ADDED':
            return '+';
        case 'COMPLETED':
            return '✓';
        case 'UNCOMPLETED':
            return '○';
        case 'INVITED':
            return '@';
        case 'JOINED':
            return '＋';
        case 'DELETED':
            return '−';
        case 'MOVED':
        case 'UPDATED':
        default:
            return '↻';
    }
}

export function formatActivityLogMessageRest(entry: ActivityLogEntry): string {
    if (entry.activityType === 'UPDATED' && entry.changes.length > 0) {
        return formatChangesRest(entry);
    }

    const name = targetName(entry);
    const { activityType, target } = entry;

    if (target.type === 'PROJECT') {
        switch (activityType) {
            case 'ADDED':
                return `님이 프로젝트 ${name}을(를) 추가했습니다`;
            case 'UPDATED':
                return `님이 프로젝트 ${name}을(를) 변경했습니다`;
            case 'DELETED':
                return `님이 프로젝트 ${name}을(를) 삭제했습니다`;
            case 'INVITED':
                return `님이 [${name}] 프로젝트에 초대했습니다`;
            case 'JOINED':
                return `님이 [${name}] 프로젝트에 합류했습니다`;
            default:
                return `님이 프로젝트 ${name}에 대해 활동을 ${activityLabel(activityType)}`;
        }
    }

    if (target.type === 'COMMENT') {
        switch (activityType) {
            case 'ADDED':
                return `님이 ${name}에 댓글을 추가했습니다`;
            case 'DELETED':
                return `님이 ${name}에서 댓글을 삭제했습니다`;
            default:
                return `님이 ${name}의 댓글을 ${activityLabel(activityType)}`;
        }
    }

    switch (activityType) {
        case 'ADDED':
            return `님이 ${name}을(를) 추가했습니다`;
        case 'COMPLETED':
            return `님이 ${name}을(를) 완료했습니다`;
        case 'UNCOMPLETED':
            return `님이 ${name} 완료를 취소했습니다`;
        case 'DELETED':
            return `님이 ${name}을(를) 삭제했습니다`;
        case 'MOVED': {
            const moveChange = entry.changes.find(change => change.field === 'project');
            if (moveChange?.to) {
                return `님이 ${name}을(를) ${moveChange.to}(으)로 이동시켰습니다`;
            }
            return `님이 ${name}을(를) 이동시켰습니다`;
        }
        case 'INVITED':
            return `님이 ${name}을(를) 초대했습니다`;
        case 'JOINED':
            return `님이 ${name}에 합류했습니다`;
        case 'UPDATED':
        default:
            return `님이 ${name}을(를) 수정했습니다`;
    }
}

export function resolveCurrentMemberId(explicit?: number | null): number | null {
    if (explicit != null) return explicit;
    const profileId = getUserProfile().id;
    if (profileId != null) return profileId;
    return getLoggedInMemberId();
}

export function isSelfActivityActor(
    entry: ActivityLogEntry,
    currentMemberId?: number | null,
): boolean {
    const memberId = resolveCurrentMemberId(currentMemberId);
    if (memberId == null || entry.actor.id == null) return false;
    return Number(entry.actor.id) === Number(memberId);
}

export function getActivityActorLabel(
    entry: ActivityLogEntry,
    currentMemberId: number | null | undefined,
): string {
    return isSelfActivityActor(entry, currentMemberId) ? '당신' : entry.actor.name;
}

const SELF_ACTOR_PARTICLE = '님이';

/** actor 라벨을 제외한 나머지 문장 (본인이면 '님이' → '이') */
export function formatActivityLogMessageSuffix(
    entry: ActivityLogEntry,
    currentMemberId: number | null | undefined,
): string {
    const rest = formatActivityLogMessageRest(entry);
    if (isSelfActivityActor(entry, currentMemberId) && rest.startsWith(SELF_ACTOR_PARTICLE)) {
        return `이${rest.slice(SELF_ACTOR_PARTICLE.length)}`;
    }
    return rest;
}

export function formatActivityLogMessage(
    entry: ActivityLogEntry,
    currentMemberId?: number | null,
): string {
    return `${getActivityActorLabel(entry, currentMemberId)}${formatActivityLogMessageSuffix(entry, currentMemberId)}`;
}

export function normalizeActivityLog(dto: ActivityLogDto): ActivityLogEntry {
    return {
        id: dto.id,
        activityType: dto.activityType,
        actor: dto.actor ?? { id: 0, name: '사용자' },
        target: dto.target ?? { type: 'TASK' as ActivityTargetType, id: 0, name: null },
        createdAt: dto.createdAt,
        changes: dto.changes ?? [],
    };
}

/** 프로젝트 필터: target이 해당 프로젝트인 로그만 매칭 */
export function matchesActivityProjectFilter(
    entry: ActivityLogEntry,
    projectId: number,
): boolean {
    return entry.target.type === 'PROJECT' && entry.target.id === projectId;
}
