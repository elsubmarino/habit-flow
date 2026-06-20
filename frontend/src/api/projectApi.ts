import { apiClient } from './client';
import { dedupeInFlight } from './inFlight';
import { toProjectWriteBody } from './projectMappers';
import type { ProjectDetailDto, ProjectDto, ProjectMemberListDto, ProjectUpdatePayload } from './types';

export async function fetchProjects(): Promise<ProjectDto[]> {
    return dedupeInFlight('projects', async () => {
        const { data } = await apiClient.get<ProjectDto[]>('/api/projects');
        return data;
    });
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
    return dedupeInFlight(`project:${projectId}`, async () => {
        const { data } = await apiClient.get<ProjectDetailDto>(`/api/projects/${projectId}`);
        return data;
    });
}

export async function updateProject(
    projectId: number,
    payload: ProjectUpdatePayload,
): Promise<ProjectDetailDto> {
    const { data } = await apiClient.put<ProjectDetailDto>(
        `/api/projects/${projectId}`,
        toProjectWriteBody(payload),
    );
    return data;
}

export async function deleteProject(projectId: number): Promise<void> {
    await apiClient.delete(`/api/projects/${projectId}`);
}

export interface ProjectInvitePayload {
    id: number;
    emails: string[];
}

export async function fetchProjectMembers(projectId: number): Promise<ProjectMemberListDto[]> {
    return dedupeInFlight(`project-members:${projectId}`, async () => {
        const { data } = await apiClient.get<ProjectMemberListDto[]>(`/api/projects/${projectId}/members`);
        return data;
    });
}

export async function inviteToProject(projectId: number, emails: string[]): Promise<void> {
    const body: ProjectInvitePayload = {
        id: projectId,
        emails,
    };
    await apiClient.post(`/api/projects/${projectId}/invitation`, body, {
        headers: { 'Content-Type': 'application/json' },
    });
}

export async function acceptProjectInvitation(token: string): Promise<void> {
    await apiClient.post('/api/projects/invitation/accept', null, {
        params: { token },
    });
}
