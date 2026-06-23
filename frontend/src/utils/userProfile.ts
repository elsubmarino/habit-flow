import type { MemberDto, EntityId } from '../api/types';
import { getAccessToken, registerOnAccessTokenSet } from '../api/client';

export interface UserProfile {
    id?: EntityId;
    displayName: string;
    fullName: string;
    email: string;
    karma: number;
    plan: string;
}

const LEGACY_USER_STORAGE_KEY = 'habitflow.user';
const PROFILE_UPDATE_EVENT = 'habitflow:profile-updated';

const GUEST_PROFILE: UserProfile = {
    displayName: '사용자',
    fullName: '사용자',
    email: '',
    karma: 0,
    plan: 'P',
};

let memoryProfile: UserProfile | null = null;

function clearLegacyUserStorage() {
    localStorage.removeItem(LEGACY_USER_STORAGE_KEY);
}

function notifyProfileUpdate() {
    window.dispatchEvent(new CustomEvent(PROFILE_UPDATE_EVENT));
}

function parseTokenPayload(token: string): Record<string, unknown> | null {
    try {
        const payloadPart = token.split('.')[1];
        if (!payloadPart) return null;
        const normalized = payloadPart.replace(/-/g, '+').replace(/_/g, '/');
        return JSON.parse(atob(normalized)) as Record<string, unknown>;
    } catch {
        return null;
    }
}

function readEmailFromPayload(payload: Record<string, unknown>): string {
    if (typeof payload.sub === 'string' && payload.sub.trim() !== '') {
        return payload.sub.trim();
    }
    if (typeof payload.email === 'string' && payload.email.trim() !== '') {
        return payload.email.trim();
    }
    return '';
}

function displayNameFromEmail(email: string): string {
    const local = email.split('@')[0]?.trim();
    return local || GUEST_PROFILE.displayName;
}

export function syncProfileFromAccessToken(token: string) {
    const payload = parseTokenPayload(token);
    if (!payload) return;

    const email = readEmailFromPayload(payload);
    const displayName = email ? displayNameFromEmail(email) : GUEST_PROFILE.displayName;

    memoryProfile = {
        ...GUEST_PROFILE,
        email,
        displayName,
        fullName: displayName,
    };
    notifyProfileUpdate();
}

export function clearUserProfile() {
    memoryProfile = null;
    notifyProfileUpdate();
}

export function getUserProfile(): UserProfile {
    return memoryProfile ?? GUEST_PROFILE;
}

export function onUserProfileUpdate(listener: () => void): () => void {
    window.addEventListener(PROFILE_UPDATE_EVENT, listener);
    return () => window.removeEventListener(PROFILE_UPDATE_EVENT, listener);
}

export function applyMemberProfile(member: MemberDto) {
    const current = getUserProfile();
    memoryProfile = {
        ...current,
        id: member.id,
        displayName: member.name?.trim() || current.displayName,
        fullName: member.name?.trim() || current.fullName,
        email: member.email?.trim() || current.email,
    };
    notifyProfileUpdate();
}

clearLegacyUserStorage();

registerOnAccessTokenSet(syncProfileFromAccessToken);

const bootToken = getAccessToken();
if (bootToken) {
    syncProfileFromAccessToken(bootToken);
}

if (typeof window !== 'undefined') {
    window.addEventListener('habitflow:logout', () => {
        clearUserProfile();
    });
}
