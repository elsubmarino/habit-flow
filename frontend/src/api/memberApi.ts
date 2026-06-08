import { apiClient } from './client';
import type { MemberDto } from './types';

export async function fetchMember(): Promise<MemberDto> {
    const { data } = await apiClient.get<MemberDto>('/api/members');
    return data;
}

export async function signUpMember(payload: {
    email: string;
    password: string;
    name: string;
}) {
    const { data } = await apiClient.post<MemberDto>('/api/members', payload);
    return data;
}
