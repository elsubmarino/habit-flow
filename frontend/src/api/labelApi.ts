import { apiClient } from './client';
import { dedupeInFlight } from './inFlight';
import { toLabelUpdateBody, toLabelWriteBody } from './labelMappers';
import { buildScrollParams, LABEL_PAGE_SIZE } from './pagination';
import type { LabelDetailDto, LabelDto, LabelUpdatePayload, ScrollResponse } from './types';

export async function fetchLabels(
    lastLabelId?: number,
    size = LABEL_PAGE_SIZE,
): Promise<ScrollResponse<LabelDto>> {
    const cursorKey = lastLabelId ?? 'first';
    return dedupeInFlight(`labels:${cursorKey}:${size}`, async () => {
        const { data } = await apiClient.get<ScrollResponse<LabelDto>>('/api/labels', {
            params: buildScrollParams(lastLabelId, 'lastLabelId', size),
        });
        return data;
    });
}

export async function fetchLabelById(labelId: number): Promise<LabelDetailDto> {
    return dedupeInFlight(`label:${labelId}`, async () => {
        const { data } = await apiClient.get<LabelDetailDto>(`/api/labels/${labelId}`);
        return data;
    });
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
