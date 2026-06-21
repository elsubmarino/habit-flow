import { apiClient } from './client';
import { dedupeInFlight } from './inFlight';
import type { EntityId } from './types';

export async function createComment(
    taskId: EntityId,
    content: string,
    file?: File | null,
): Promise<CommentResponseDto> {
    const form = new FormData();
    form.append(
        'commentRequest',
        new Blob([JSON.stringify({ taskId, content })], { type: 'application/json' }),
    );
    if (file) form.append('file', file);
    const { data } = await apiClient.post<CommentResponseDto>('/api/comments', form, {
        headers: { 'Content-Type': 'multipart/form-data' },
    });
    return data;
}

export interface CommentResponseDto {
    id?: EntityId;
    content: string;
    createdAt?: string;
    attachments?: { fileUrl: string; originalFileName: string }[];
}

export async function fetchTaskComments(taskId: EntityId): Promise<CommentResponseDto[]> {
    return dedupeInFlight(`task-comments:${taskId}`, async () => {
        const { data } = await apiClient.get<CommentResponseDto[]>(`/api/tasks/${taskId}/comments`);
        return data;
    });
}

export async function updateComment(
    commentId: EntityId,
    taskId: EntityId,
    content: string,
): Promise<CommentResponseDto> {
    const { data } = await apiClient.put<CommentResponseDto>(`/api/comments/${commentId}`, {
        id: commentId,
        taskId,
        content,
    });
    return data;
}

export async function deleteComment(commentId: EntityId): Promise<void> {
    await apiClient.delete(`/api/comments/${commentId}`);
}
