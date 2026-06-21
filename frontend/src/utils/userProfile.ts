import type { MemberDto, EntityId } from '../api/types';
import { getLoggedInMemberId } from '../api/client';

export interface UserProfile {
    id?: EntityId;
    displayName: string;
    fullName: string;
    email: string;
    karma: number;
    plan: string;
}

const DEFAULT_PROFILE: UserProfile = {
    displayName: '지완',
    fullName: 'Jiwan Kim',
    email: 'jiwan@habitflow.app',
    karma: 0,
    plan: 'P',
};

export function getUserProfile(): UserProfile {
    const raw = localStorage.getItem('habitflow.user');
    if (!raw) return DEFAULT_PROFILE;
    try {
        return { ...DEFAULT_PROFILE, ...JSON.parse(raw) as Partial<UserProfile> };
    } catch {
        return DEFAULT_PROFILE;
    }
}

export function saveUserProfile(profile: Partial<UserProfile>) {
    localStorage.setItem('habitflow.user', JSON.stringify({ ...getUserProfile(), ...profile }));
}

export function applyMemberProfile(member: MemberDto) {
    saveUserProfile({
        id: member.id ?? getLoggedInMemberId() ?? undefined,
        displayName: member.name ?? '사용자',
        fullName: member.name ?? '사용자',
        email: member.email ?? '',
    });
}
