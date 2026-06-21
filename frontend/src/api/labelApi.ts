import { apiClient } from './client';
import { dedupeInFlight } from './inFlight';
import { toLabelUpdateBody, toLabelWriteBody } from './labelMappers';
import { buildLabelCursorParams, LABEL_PAGE_SIZE } from './pagination';
import type { EntityId, LabelDetailDto, LabelDto, LabelUpdatePayload } from './types';
import { parseSlicePage, type PaginatedResult, type SpringSlice } from './slice';

export async function fetchLabels(
    lastLabelId?: EntityId,
    size = LABEL_PAGE_SIZE,
): Promise<PaginatedResult<LabelDto>> {
    const cursorKey = lastLabelId ?? 'first';
    return dedupeInFlight(`labels:${cursorKey}:${size}`, async () => {
        const { data } = await apiClient.get<SpringSlice<LabelDto>>('/api/labels', {
            params: buildLabelCursorParams(lastLabelId, size),
        });
        return parseSlicePage(data, label => label.id);
    });
}

export async function fetchLabelById(labelId: EntityId): Promise<LabelDetailDto> {
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

export async function updateLabel(labelId: EntityId, payload: LabelUpdatePayload): Promise<LabelDetailDto> {
    const { data } = await apiClient.put<LabelDetailDto>(
        `/api/labels/${labelId}`,
        toLabelUpdateBody(labelId, payload),
    );
    return data;
}

export async function deleteLabel(labelId: EntityId): Promise<void> {
    await apiClient.delete(`/api/labels/${labelId}`);
}
