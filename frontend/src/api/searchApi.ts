import { apiClient } from './client';
import { INTEGRATED_SEARCH_PAGE_SIZE } from './pagination';
import type { IntegratedSearchDto } from './types';

export async function searchIntegrated(
    keyword: string,
    size = INTEGRATED_SEARCH_PAGE_SIZE,
    signal?: AbortSignal,
): Promise<IntegratedSearchDto> {
    const normalized = keyword.trim();
    const { data } = await apiClient.get<IntegratedSearchDto>('/api/search', {
        params: { keyword: normalized, size },
        signal,
    });
    return data;
}
