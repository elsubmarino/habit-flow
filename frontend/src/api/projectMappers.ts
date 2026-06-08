import type { ProjectAccessType, ProjectLayoutType, ProjectUpdatePayload } from './types';

export function parseProjectAccessType(value?: string | null): ProjectAccessType {
    return value === 'PUBLIC' ? 'PUBLIC' : 'PRIVATE';
}

export function parseProjectLayoutType(value?: string | null): ProjectLayoutType {
    return value === 'BOARD' ? 'BOARD' : 'LIST';
}

export function toProjectWriteBody(payload: ProjectUpdatePayload) {
    return {
        name: payload.name,
        color: payload.color ?? '#4073ff',
        parentId: payload.parentId ?? null,
        accessType: payload.accessType ?? 'PRIVATE',
        layoutType: payload.layoutType ?? 'LIST',
        favorite: payload.favorite ?? false,
    };
}
