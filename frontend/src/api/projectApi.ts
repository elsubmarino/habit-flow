import { apiClient } from './client';
import type { ProjectDto } from './types';

export async function fetchProjects(): Promise<ProjectDto[]> {
    const { data } = await apiClient.get<ProjectDto[]>('/api/projects');
    return data;
}

export async function createProject(name: string, color?: string): Promise<ProjectDto> {
    const { data } = await apiClient.post<ProjectDto>('/api/projects', {
        name,
        color: color ?? '#4073ff',
        accessType: 'PRIVATE',
        layoutType: 'LIST',
        isFavorite: false,
    });
    return data;
}

export async function deleteProject(id: number): Promise<void> {
    await apiClient.delete(`/api/projects/${id}`);
}
