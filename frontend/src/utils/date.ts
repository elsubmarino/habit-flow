export function formatTodayHeader(date = new Date()): string {
    return date.toLocaleDateString('ko-KR', {
        month: 'long',
        day: 'numeric',
        weekday: 'long',
    });
}

export function isToday(isoDate: string | null): boolean {
    if (!isoDate) return false;
    return isoDate === toISODate(new Date());
}

export function toISODate(date: Date): string {
    const y = date.getFullYear();
    const m = String(date.getMonth() + 1).padStart(2, '0');
    const d = String(date.getDate()).padStart(2, '0');
    return `${y}-${m}-${d}`;
}

export function addDays(date: Date, days: number): Date {
    const next = new Date(date);
    next.setDate(next.getDate() + days);
    return next;
}

export function formatDueLabel(iso: string): string {
    const date = new Date(iso + 'T00:00:00');
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    const diff = Math.round((date.getTime() - today.getTime()) / 86_400_000);
    if (diff === 0) return '오늘';
    if (diff === 1) return '내일';
    if (diff === -1) return '어제';
    return date.toLocaleDateString('ko-KR', { month: 'short', day: 'numeric', weekday: 'short' });
}

export function formatSectionDate(iso: string): string {
    const date = new Date(iso + 'T00:00:00');
    const today = toISODate(new Date());
    const tomorrow = toISODate(addDays(new Date(), 1));
    if (iso === today) return '오늘';
    if (iso === tomorrow) return '내일';
    return date.toLocaleDateString('ko-KR', {
        month: 'long',
        day: 'numeric',
        weekday: 'long',
    });
}

export function defaultDueDateForView(view: string): string | null {
    if (view === 'today') return toISODate(new Date());
    if (view === 'upcoming') return toISODate(addDays(new Date(), 1));
    return null;
}
