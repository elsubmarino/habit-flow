import type { FavoriteDto, FavoriteTargetType, EntityId } from './types';
import type { Label, Project } from '../store/habitSlice';

export function normalizeFavoriteTargetType(value: unknown): FavoriteTargetType | null {
    const raw = String(value ?? '').toUpperCase();
    if (raw === 'PROJECT') return 'PROJECT';
    if (raw === 'LABEL') return 'LABEL';
    return null;
}

function isValidEntityId(value: unknown): value is EntityId {
    return typeof value === 'string' && value.trim() !== '';
}

export function normalizeFavoriteDto(favorite: FavoriteDto): FavoriteDto | null {
    const targetType = normalizeFavoriteTargetType(favorite.targetType);
    if (targetType == null || !isValidEntityId(favorite.targetId)) return null;
    return {
        ...favorite,
        targetType,
        targetCount: favorite.targetCount ?? 0,
    };
}

function favoriteKey(targetType: FavoriteTargetType, targetId: EntityId): string {
    return `${targetType}:${targetId}`;
}

function localFavoriteId(targetType: FavoriteTargetType, targetId: EntityId): EntityId {
    return `local:${targetType}:${targetId}`;
}

/** API 목록 + 로컬 favorite 플래그를 합쳐 사이드바에 표시할 목록 생성 */
export function buildSidebarFavorites(
    favorites: FavoriteDto[],
    projects: Project[],
    labels: Label[],
): FavoriteDto[] {
    const fromApi = favorites
        .map(normalizeFavoriteDto)
        .filter((f): f is FavoriteDto => f != null);

    const keys = new Set(fromApi.map(f => favoriteKey(f.targetType, f.targetId)));

    const fromProjects = projects
        .filter(p => p.favorite && !keys.has(favoriteKey('PROJECT', p.id)))
        .map(p => ({
            id: localFavoriteId('PROJECT', p.id),
            targetType: 'PROJECT' as const,
            targetId: p.id,
            targetName: p.name,
            targetCount: p.taskCount,
        }));

    for (const item of fromProjects) {
        keys.add(favoriteKey(item.targetType, item.targetId));
    }

    const fromLabels = labels
        .filter(l => l.favorite && !keys.has(favoriteKey('LABEL', l.id)))
        .map(l => ({
            id: localFavoriteId('LABEL', l.id),
            targetType: 'LABEL' as const,
            targetId: l.id,
            targetName: l.name,
            targetCount: l.taskCount,
        }));

    return [...fromApi, ...fromProjects, ...fromLabels];
}

export function readFavoriteTargetId(favorite: FavoriteDto): EntityId {
    return favorite.targetId;
}

export function resolveFavoriteProject(favorite: FavoriteDto, project?: Project): Project {
    if (project) {
        return { ...project, taskCount: favorite.targetCount, favorite: true };
    }
    return {
        id: favorite.targetId,
        name: favorite.targetName,
        color: '#808080',
        sortOrder: 0,
        taskCount: favorite.targetCount,
        favorite: true,
    };
}

export function resolveFavoriteLabel(favorite: FavoriteDto, label?: Label): Label {
    if (label) {
        return { ...label, taskCount: favorite.targetCount, favorite: true };
    }
    return {
        id: favorite.targetId,
        name: favorite.targetName,
        color: '#808080',
        taskCount: favorite.targetCount,
        favorite: true,
        sortOrder: 0,
    };
}

export function favoriteTargetIds(
    favorites: FavoriteDto[],
    targetType: FavoriteTargetType,
): Set<EntityId> {
    return new Set(
        favorites
            .map(normalizeFavoriteDto)
            .filter((f): f is FavoriteDto => f != null && f.targetType === targetType)
            .map(readFavoriteTargetId),
    );
}

export function upsertFavoriteFromProject(favorites: FavoriteDto[], project: Project): FavoriteDto[] {
    const rest = favorites.filter(
        f => !(normalizeFavoriteTargetType(f.targetType) === 'PROJECT' && f.targetId === project.id),
    );
    if (!project.favorite) return rest;

    const existing = favorites.find(
        f => normalizeFavoriteTargetType(f.targetType) === 'PROJECT' && f.targetId === project.id,
    );
    return [
        ...rest,
        {
            id: existing?.id ?? localFavoriteId('PROJECT', project.id),
            targetType: 'PROJECT',
            targetId: project.id,
            targetName: project.name,
            targetCount: existing?.targetCount ?? project.taskCount,
        },
    ];
}

export function upsertFavoriteFromLabel(favorites: FavoriteDto[], label: Label): FavoriteDto[] {
    const rest = favorites.filter(
        f => !(normalizeFavoriteTargetType(f.targetType) === 'LABEL' && f.targetId === label.id),
    );
    if (!label.favorite) return rest;

    const existing = favorites.find(
        f => normalizeFavoriteTargetType(f.targetType) === 'LABEL' && f.targetId === label.id,
    );
    return [
        ...rest,
        {
            id: existing?.id ?? localFavoriteId('LABEL', label.id),
            targetType: 'LABEL',
            targetId: label.id,
            targetName: label.name,
            targetCount: existing?.targetCount ?? label.taskCount,
        },
    ];
}

export function applyFavoriteFlags<T extends { id: EntityId; favorite: boolean }>(
    items: T[],
    favorites: FavoriteDto[],
    targetType: FavoriteTargetType,
): T[] {
    const ids = favoriteTargetIds(favorites, targetType);
    return items.map(item => ({ ...item, favorite: ids.has(item.id) }));
}
