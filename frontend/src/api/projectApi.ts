import { apiClient } from './client';
import { toProjectWriteBody } from './projectMappers';
import type { ProjectDetailDto, ProjectDto, ProjectUpdatePayload } from './types';

export async function fetchProjects(): Promise<ProjectDto[]> {
    const { data } = await apiClient.get<ProjectDto[]>('/api/projects');
    return data;
}

export async function createProject(name: string, color?: string): Promise<ProjectDto> {
    const { data } = await apiClient.post<ProjectDto>('/api/projects', toProjectWriteBody({
        name,
        color: color ?? '#4073ff',
        accessType: 'PRIVATE',
        layoutType: 'LIST',
        favorite: false,
    }));
    return data;
}

export async function fetchProjectById(projectId: number): Promise<ProjectDetailDto> {
    const { data } = await apiClient.get<ProjectDetailDto>(`/api/projects/${projectId}`);
    return data;
}

export async function updateProject(projectId: number, payload: ProjectUpdatePayload): Promise<ProjectDetailDto> {
    const { data } = await apiClient.put<ProjectDetailDto>(
        `/api/projects/${projectId}`,
        toProjectWriteBody(payload),
    );
    return data;
}

export async function deleteProject(projectId: number): Promise<void> {
    await apiClient.delete(`/api/projects/${projectId}`);
}
