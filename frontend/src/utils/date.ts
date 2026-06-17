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

export function combineDateAndTime(isoDate: string, time24: string): string {
    if (!isoDate) return isoDate;
    if (!time24) return isoDate.slice(0, 10);
    return `${isoDate.slice(0, 10)}T${time24}:00`;
}

export function localTimeTo24(value?: string | null): string | null {
    if (!value) return null;
    const part = value.slice(0, 5);
    return /^\d{2}:\d{2}$/.test(part) ? part : null;
}

export function formatTime12From24(time24: string): string {
    const [hourText, minuteText] = time24.split(':');
    const hour = Number(hourText);
    const minute = Number(minuteText);
    if (Number.isNaN(hour) || Number.isNaN(minute)) return time24;
    const date = new Date();
    date.setHours(hour, minute, 0, 0);
    return date.toLocaleTimeString('ko-KR', {
        hour: 'numeric',
        minute: '2-digit',
        hour12: true,
    });
}

/** API LocalDateTime 문자열 (시간 없으면 T00:00:00) */
export function toLocalDateTime(
    date: string | null | undefined,
    time24?: string | null,
    hasTime = false,
): string | null {
    if (!date) return null;
    const day = date.slice(0, 10);
    if (!hasTime || !time24) return `${day}T00:00:00`;
    const hhmm = time24.slice(0, 5);
    return `${day}T${hhmm}:00`;
}

export function datePartFromDue(value?: string | null): string | null {
    if (!value) return null;
    return value.slice(0, 10);
}

export function formatTaskDetailDue(
    date: string | null,
    hasTime: boolean,
    time24?: string | null,
): string {
    if (!date) return '';
    const d = new Date(`${date.slice(0, 10)}T00:00:00`);
    const base = d.toLocaleDateString('ko-KR', {
        month: 'long',
        day: 'numeric',
        weekday: 'short',
    });
    if (!hasTime || !time24) return base;
    return `${base} ${formatTime12From24(time24)}`;
}

export function dueDateToTimeInput(dueDate: string | null, dueTime: string | null): string {
    if (dueDate?.includes('T')) {
        const timePart = dueDate.split('T')[1] ?? '';
        if (timePart && !timePart.startsWith('00:00:00')) {
            return timePart.slice(0, 5);
        }
    }
    if (!dueTime) return '09:00';
    const match = dueTime.match(/(오전|오후)\s*(\d{1,2}):(\d{2})/);
    if (!match) return '09:00';
    let hour = Number(match[2]);
    const minute = match[3];
    if (match[1] === '오후' && hour < 12) hour += 12;
    if (match[1] === '오전' && hour === 12) hour = 0;
    return `${String(hour).padStart(2, '0')}:${minute}`;
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

export const UPCOMING_CALENDAR_YEARS_LIMIT = 2;

export function addYears(date: Date, years: number): Date {
    const next = new Date(date);
    next.setFullYear(next.getFullYear() + years);
    return next;
}

export function getUpcomingCalendarMaxDate(from = new Date()): Date {
    const max = addYears(from, UPCOMING_CALENDAR_YEARS_LIMIT);
    max.setHours(23, 59, 59, 999);
    return max;
}

export function isWithinUpcomingCalendarRange(iso: string, from = new Date()): boolean {
    const day = iso.slice(0, 10);
    const maxIso = toISODate(getUpcomingCalendarMaxDate(from));
    return day <= maxIso;
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

export function formatMonthYear(date = new Date()): string {
    return date.toLocaleDateString('ko-KR', { year: 'numeric', month: 'long' });
}

export function formatUpcomingSectionTitle(iso: string): string {
    const date = new Date(iso + 'T00:00:00');
    const today = toISODate(new Date());
    const tomorrow = toISODate(addDays(new Date(), 1));
    const currentYear = new Date().getFullYear();
    const showYear = date.getFullYear() !== currentYear;
    const dateLabel = showYear
        ? date.toLocaleDateString('ko-KR', { year: 'numeric', month: 'long', day: 'numeric' })
        : date.toLocaleDateString('ko-KR', { month: 'long', day: 'numeric' });
    const weekday = date.toLocaleDateString('ko-KR', { weekday: 'long' });
    if (iso === today) return `${dateLabel} · 오늘 · ${weekday}`;
    if (iso === tomorrow) return `${dateLabel} · 내일 · ${weekday}`;
    return `${dateLabel} · ${weekday}`;
}

export function getWeekdayShort(date: Date): string {
    return date.toLocaleDateString('ko-KR', { weekday: 'short' }).replace('요일', '');
}

export function getWeekStrip(start = new Date(), days = 7): Array<{
    iso: string;
    dayNum: number;
    weekday: string;
    isToday: boolean;
}> {
    const today = toISODate(new Date());
    return Array.from({ length: days }, (_, i) => {
        const d = addDays(start, i);
        const iso = toISODate(d);
        return {
            iso,
            dayNum: d.getDate(),
            weekday: getWeekdayShort(d),
            isToday: iso === today,
        };
    });
}

export function getWeekDateKeys(weekStart: Date, days = 7): string[] {
    return Array.from({ length: days }, (_, i) => toISODate(addDays(weekStart, i)));
}

export function isDateInWeek(iso: string, weekStartIso: string): boolean {
    const start = new Date(`${weekStartIso}T00:00:00`);
    const end = addDays(start, 6);
    const day = new Date(`${iso}T00:00:00`);
    return day >= start && day <= end;
}

export function isAfterToday(iso: string, todayIso = toISODate(new Date())): boolean {
    return iso > todayIso;
}

export function getUpcomingWeekRange(anchorIso: string): {
    weekStartIso: string;
    fromDate: string;
    toDate: string;
} {
    const anchor = new Date(`${anchorIso}T00:00:00`);
    const weekStart = startOfWeekMonday(anchor);
    const weekEnd = addDays(weekStart, 6);
    return {
        weekStartIso: toISODate(weekStart),
        fromDate: `${toISODate(weekStart)}T00:00:00`,
        toDate: `${toISODate(weekEnd)}T23:59:59`,
    };
}

/** upcoming 날짜별 API용 하루 범위 (LocalDateTime) */
export function getUpcomingDayRange(dateKey: string): { fromDate: string; toDate: string } {
    return {
        fromDate: `${dateKey}T00:00:00`,
        toDate: `${dateKey}T23:59:59`,
    };
}

/** /upcoming/summary 요청 범위: 오늘 ~ 달력 최대일 */
export function getUpcomingSummaryDateRange(from = new Date()): { fromDate: string; toDate: string } {
    const todayIso = toISODate(from);
    const maxIso = toISODate(getUpcomingCalendarMaxDate(from));
    return {
        fromDate: `${todayIso}T00:00:00`,
        toDate: `${maxIso}T23:59:59`,
    };
}

export function isTodayDateKey(dateKey: string, todayIso = toISODate(new Date())): boolean {
    return dateKey === todayIso;
}

let cachedTimelineKeys: string[] | null = null;
let cachedTimelineAnchorDay: string | null = null;

/** 오늘 ~ 달력 최대일(2년)까지 모든 날짜 키. upcoming 목록 빈 섹션용 */
export function getUpcomingTimelineDateKeys(from = new Date()): string[] {
    const anchorDay = toISODate(from);
    if (cachedTimelineKeys && cachedTimelineAnchorDay === anchorDay) {
        return cachedTimelineKeys;
    }

    const keys: string[] = [];
    const maxDate = getUpcomingCalendarMaxDate(from);
    let cursor = new Date(from);
    cursor.setHours(0, 0, 0, 0);

    while (cursor <= maxDate) {
        keys.push(toISODate(cursor));
        cursor = addDays(cursor, 1);
    }

    cachedTimelineKeys = keys;
    cachedTimelineAnchorDay = anchorDay;
    return keys;
}

export function startOfWeekMonday(date = new Date()): Date {
    const d = new Date(date);
    const day = d.getDay();
    const diff = day === 0 ? -6 : 1 - day;
    d.setDate(d.getDate() + diff);
    d.setHours(0, 0, 0, 0);
    return d;
}
