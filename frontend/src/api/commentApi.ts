import { apiClient } from './client';

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
    const { data } = await apiClient.get<CommentResponseDto[]>(`/api/tasks/${taskId}/comments`);
    return data;
}

export async function updateComment(id: number, taskId: number, content: string) {
    const { data } = await apiClient.put<CommentResponseDto>(`/api/comments/${id}`, {
        id,
        taskId,
        content,
    });
    return data;
}

export async function deleteComment(id: number): Promise<void> {
    await apiClient.delete(`/api/comments/${id}`);
}
