import { useEffect, useState } from 'react';
import { getUserProfile, onUserProfileUpdate, type UserProfile } from '../utils/userProfile';

export function useUserProfile(): UserProfile {
    const [profile, setProfile] = useState(getUserProfile);

    useEffect(() => onUserProfileUpdate(() => {
        setProfile(getUserProfile());
    }), []);

    return profile;
}
