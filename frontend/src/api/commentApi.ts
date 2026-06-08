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

export async function fetchTaskComments(taskId: number) {
    const { data } = await apiClient.get(`/api/tasks/${taskId}/comments`);
    return data;
}
