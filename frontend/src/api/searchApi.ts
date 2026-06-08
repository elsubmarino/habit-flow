import { apiClient } from './client';
import type { IntegratedSearchDto } from './types';

export async function searchIntegrated(keyword: string): Promise<IntegratedSearchDto> {
    const { data } = await apiClient.get<IntegratedSearchDto>('/api/search', {
        params: { keyword },
    });
    return data;
}
