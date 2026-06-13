import { apiClient } from './client';
import { dedupeInFlight } from './inFlight';
import { INTEGRATED_SEARCH_PAGE_SIZE } from './pagination';
import type { IntegratedSearchDto } from './types';

export async function searchIntegrated(
    keyword: string,
    size = INTEGRATED_SEARCH_PAGE_SIZE,
): Promise<IntegratedSearchDto> {
    const normalized = keyword.trim();
    return dedupeInFlight(`search:${normalized}:${size}`, async () => {
        const { data } = await apiClient.get<IntegratedSearchDto>('/api/search', {
            params: { keyword: normalized, size },
        });
        return data;
    });
}
