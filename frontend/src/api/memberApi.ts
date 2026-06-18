import { apiClient } from './client';
import { dedupeInFlight } from './inFlight';
import type { MemberDto } from './types';

export interface MemberSignUpPayload {
    email: string;
    password: string;
    name: string;
}

export interface MemberLoginPayload {
    email: string;
    password: string;
}

export interface AuthTokenResponse {
    accessToken: string;
}

export async function fetchMember(): Promise<MemberDto> {
    return dedupeInFlight('member', async () => {
        const { data } = await apiClient.get<MemberDto>('/api/members');
        return data;
    });
}

export async function loginMember(payload: MemberLoginPayload): Promise<AuthTokenResponse> {
    const { data } = await apiClient.post<AuthTokenResponse>(
        '/api/auth/login',
        {
            email: payload.email,
            password: payload.password,
        },
        { headers: { 'Content-Type': 'application/json' } },
    );
    return data;
}

export async function logoutMember(): Promise<void> {
    await apiClient.post('/api/auth/logout');
}

export async function signUpMember(payload: MemberSignUpPayload): Promise<MemberDto> {
    const { data } = await apiClient.post<MemberDto>(
        '/api/auth/signup',
        {
            email: payload.email,
            password: payload.password,
            name: payload.name,
        },
        { headers: { 'Content-Type': 'application/json' } },
    );
    return data;
}
