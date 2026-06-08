import type { LabelUpdatePayload } from './types';

export function formatLabelApiName(name: string): string {
    const trimmed = name.trim();
    return trimmed.startsWith('@') ? trimmed : `@${trimmed}`;
}

export function displayLabelName(name: string): string {
    return name.startsWith('@') ? name.slice(1) : name;
}

export function toLabelWriteBody(payload: { name: string; color?: string; favorite?: boolean }) {
    return {
        name: formatLabelApiName(payload.name),
        color: payload.color ?? '#808080',
        favorite: payload.favorite ?? false,
    };
}

export function toLabelUpdateBody(id: number, payload: LabelUpdatePayload) {
    return {
        id,
        ...toLabelWriteBody(payload),
    };
}
