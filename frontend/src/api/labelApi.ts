import { apiClient } from './client';
import { toLabelUpdateBody, toLabelWriteBody } from './labelMappers';
import type { LabelDetailDto, LabelDto, LabelUpdatePayload } from './types';

export async function fetchLabels(): Promise<LabelDto[]> {
    const { data } = await apiClient.get<LabelDto[]>('/api/labels');
    return data;
}

export async function fetchLabelById(id: number): Promise<LabelDetailDto> {
    const { data } = await apiClient.get<LabelDetailDto>(`/api/labels/${id}`);
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

export async function updateLabel(id: number, payload: LabelUpdatePayload): Promise<LabelDetailDto> {
    const { data } = await apiClient.put<LabelDetailDto>(
        `/api/labels/${id}`,
        toLabelUpdateBody(id, payload),
    );
    return data;
}

export async function deleteLabel(id: number): Promise<void> {
    await apiClient.delete(`/api/labels/${id}`);
}
