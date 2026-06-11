import { apiClient } from './client';
import { INTEGRATED_SEARCH_PAGE_SIZE } from './pagination';
import type { IntegratedSearchDto } from './types';

export async function searchIntegrated(
    keyword: string,
    size = INTEGRATED_SEARCH_PAGE_SIZE,
): Promise<IntegratedSearchDto> {
    const { data } = await apiClient.get<IntegratedSearchDto>('/api/search', {
        params: { keyword, size },
    });
    return data;
}
