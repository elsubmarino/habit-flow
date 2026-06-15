import { apiClient } from './client';
import { dedupeInFlight } from './inFlight';

export async function createComment(taskId: number, content: string, file?: File | null) {
    const form = new FormData();
    form.append(
        'commentRequest',
        new Blob([JSON.stringify({ taskId, content })], { type: 'application/json' }),
    );
    if (file) form.append('file', file);
    const { data } = await apiClient.post('/api/comments', form, {
        headers: { 'Content-Type': 'multipart/form-data' },
    });
    return data;
}

export interface CommentResponseDto {
    id?: number;
    content: string;
    createdAt?: string;
    attachments?: { fileUrl: string; originalFileName: string }[];
}

export async function fetchTaskComments(taskId: number): Promise<CommentResponseDto[]> {
    return dedupeInFlight(`task-comments:${taskId}`, async () => {
        const { data } = await apiClient.get<CommentResponseDto[]>(`/api/tasks/${taskId}/comments`);
        return data;
    });
}

export async function updateComment(commentId: number, taskId: number, content: string): Promise<void> {
    await apiClient.put(`/api/comments/${commentId}`, {
        id: commentId,
        taskId,
        content,
    });
}

export async function deleteComment(commentId: number): Promise<void> {
    await apiClient.delete(`/api/comments/${commentId}`);
}
