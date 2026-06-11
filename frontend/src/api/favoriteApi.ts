import { apiClient } from './client';
import type { FavoriteDto } from './types';

export async function fetchFavorites(): Promise<FavoriteDto[]> {
    const { data } = await apiClient.get<FavoriteDto[]>('/api/favorite');
    return data;
}
