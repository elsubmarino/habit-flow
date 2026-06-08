import { apiClient } from './client';
import { toLabelUpdateBody, toLabelWriteBody } from './labelMappers';
import type { LabelDetailDto, LabelDto, LabelUpdatePayload } from './types';

export async function fetchLabels(): Promise<LabelDto[]> {
    const { data } = await apiClient.get<LabelDto[]>('/api/labels');
    return data;
}

export async function fetchLabelById(labelId: number): Promise<LabelDetailDto> {
    const { data } = await apiClient.get<LabelDetailDto>(`/api/labels/${labelId}`);
    return data;
}

export async function createLabel(
    name: string,
    color?: string,
    favorite?: boolean,
): Promise<LabelDetailDto> {
    const { data } = await apiClient.post<LabelDetailDto>(
        '/api/labels',
        toLabelWriteBody({ name, color, favorite }),
    );
    return data;
}

export async function updateLabel(labelId: number, payload: LabelUpdatePayload): Promise<LabelDetailDto> {
    const { data } = await apiClient.put<LabelDetailDto>(
        `/api/labels/${labelId}`,
        toLabelUpdateBody(labelId, payload),
    );
    return data;
}

export async function deleteLabel(labelId: number): Promise<void> {
    await apiClient.delete(`/api/labels/${labelId}`);
}
