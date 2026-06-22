const STORAGE_KEY = 'habitflow.recentSearches.v1';
export const MAX_RECENT_SEARCHES = 5;

export function readRecentSearches(): string[] {
    try {
        const raw = localStorage.getItem(STORAGE_KEY);
        if (!raw) return [];
        const parsed: unknown = JSON.parse(raw);
        if (!Array.isArray(parsed)) return [];
        return parsed.filter((item): item is string => typeof item === 'string' && item.trim() !== '');
    } catch {
        return [];
    }
}

export function addRecentSearch(query: string): string[] {
    const trimmed = query.trim();
    if (!trimmed) return readRecentSearches();

    const normalized = trimmed.toLowerCase();
    const prev = readRecentSearches().filter(item => item.toLowerCase() !== normalized);
    const next = [trimmed, ...prev].slice(0, MAX_RECENT_SEARCHES);
    localStorage.setItem(STORAGE_KEY, JSON.stringify(next));
    return next;
}
