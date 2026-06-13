import { apiClient } from './client';
import { dedupeInFlight } from './inFlight';
import type { FavoriteDto } from './types';

export async function fetchFavorites(): Promise<FavoriteDto[]> {
    return dedupeInFlight('favorites', async () => {
        const { data } = await apiClient.get<FavoriteDto[]>('/api/favorite');
        return data;
    });
}
