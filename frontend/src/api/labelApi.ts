import { apiClient } from './client';
import type { LabelDto } from './types';

export async function fetchLabels(): Promise<LabelDto[]> {
    const { data } = await apiClient.get<LabelDto[]>('/api/labels');
    return data;
}

export async function createLabel(name: string, color?: string): Promise<LabelDto> {
    const { data } = await apiClient.post<LabelDto>('/api/labels', {
        name: name.startsWith('@') ? name : `@${name}`,
        color: color ?? '#808080',
        isFavorite: false,
    });
    return data;
}

export async function deleteLabel(id: number): Promise<void> {
    await apiClient.delete(`/api/labels/${id}`);
}
