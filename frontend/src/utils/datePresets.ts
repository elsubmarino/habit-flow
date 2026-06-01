import { addDays, toISODate } from './date';

export interface DatePreset {
    id: string;
    label: string;
    date: Date | null;
    hint: string;
}

function weekdayShort(date: Date): string {
    return date.toLocaleDateString('ko-KR', { weekday: 'short' });
}

function monthDayShort(date: Date): string {
    return date.toLocaleDateString('ko-KR', { month: 'short', day: 'numeric', weekday: 'short' });
}

/** 이번 주 중 남은 평일 (보통 수·목·금) */
export function getLaterThisWeek(from = new Date()): Date {
    const day = from.getDay();
    const d = new Date(from);
    if (day === 0) {
        d.setDate(d.getDate() + 3);
    } else if (day <= 3) {
        d.setDate(d.getDate() + (5 - day));
    } else if (day === 4) {
        d.setDate(d.getDate() + 1);
    } else {
        d.setDate(d.getDate() + (8 - day));
    }
    return d;
}

export function getThisWeekend(from = new Date()): Date {
    const day = from.getDay();
    const d = new Date(from);
    if (day === 6) return d;
    d.setDate(d.getDate() + (6 - day));
    return d;
}

export function getNextWeekMonday(from = new Date()): Date {
    const day = from.getDay();
    const d = new Date(from);
    const add = day === 0 ? 1 : 8 - day;
    d.setDate(d.getDate() + add);
    return d;
}

export function buildDatePresets(from = new Date()): DatePreset[] {
    const tomorrow = addDays(from, 1);
    const later = getLaterThisWeek(from);
    const weekend = getThisWeekend(from);
    const nextWeek = getNextWeekMonday(from);

    return [
        {
            id: 'tomorrow',
            label: '내일',
            date: tomorrow,
            hint: weekdayShort(tomorrow),
        },
        {
            id: 'later-week',
            label: '이번 주 후반',
            date: later,
            hint: weekdayShort(later),
        },
        {
            id: 'weekend',
            label: '이번 주말',
            date: weekend,
            hint: weekdayShort(weekend),
        },
        {
            id: 'next-week',
            label: '다음 주',
            date: nextWeek,
            hint: monthDayShort(nextWeek),
        },
        {
            id: 'none',
            label: '날짜 없음',
            date: null,
            hint: '',
        },
    ];
}

export function formatPickerHeader(iso: string | null): string {
    if (!iso) return '날짜 선택';
    const date = new Date(iso + 'T00:00:00');
    const today = toISODate(new Date());
    const tomorrow = toISODate(addDays(new Date(), 1));
    if (iso === today) return '오늘';
    if (iso === tomorrow) return '내일';
    return date.toLocaleDateString('ko-KR', { month: 'long', day: 'numeric' });
}
