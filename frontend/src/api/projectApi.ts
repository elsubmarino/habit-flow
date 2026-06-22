import { apiClient } from './client';
import { dedupeInFlight } from './inFlight';
import { toProjectWriteBody } from './projectMappers';
import type { EntityId, ProjectDetailDto, ProjectDto, ProjectMemberListDto, ProjectUpdatePayload } from './types';

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

export async function fetchProjectById(projectId: EntityId): Promise<ProjectDetailDto> {
    return dedupeInFlight(`project:${projectId}`, async () => {
        const { data } = await apiClient.get<ProjectDetailDto>(`/api/projects/${projectId}`);
        return data;
    });
}

export async function updateProject(
    projectId: EntityId,
    payload: ProjectUpdatePayload,
): Promise<ProjectDetailDto> {
    const { data } = await apiClient.put<ProjectDetailDto>(
        `/api/projects/${projectId}`,
        toProjectWriteBody(payload),
    );
    return data;
}

export async function deleteProject(projectId: EntityId): Promise<void> {
    await apiClient.delete(`/api/projects/${projectId}`);
}

export async function patchProjectSortOrder(
    projectId: EntityId,
    sortOrder: number,
): Promise<ProjectDto> {
    const { data } = await apiClient.patch<ProjectDto>(`/api/projects/${projectId}/sort-order`, {
        sortOrder,
    });
    return data;
}

export interface ProjectInvitePayload {
    id: EntityId;
    emails: string[];
}

export async function fetchProjectMembers(projectId: EntityId): Promise<ProjectMemberListDto[]> {
    return dedupeInFlight(`project-members:${projectId}`, async () => {
        const { data } = await apiClient.get<ProjectMemberListDto[]>(`/api/projects/${projectId}/members`);
        return data;
    });
}

export async function inviteToProject(projectId: EntityId, emails: string[]): Promise<void> {
    const body: ProjectInvitePayload = {
        id: projectId,
        emails,
    };
    await apiClient.post(`/api/projects/${projectId}/invitation`, body, {
        headers: { 'Content-Type': 'application/json' },
    });
}

/** DELETE /api/projects/{projectId}/members — body: { memberId } */
export async function removeProjectMember(projectId: EntityId, memberId: EntityId): Promise<void> {
    await apiClient.delete(`/api/projects/${projectId}/members`, {
        data: { memberId },
    });
}

export async function acceptProjectInvitation(token: string): Promise<void> {
    await apiClient.post('/api/projects/invitation/accept', null, {
        params: { token },
    });
}
