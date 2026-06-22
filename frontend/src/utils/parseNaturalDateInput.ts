import {
    addDays,
    formatTime12From24,
    getRelativeDueDayLabel,
    toISODate,
} from './date';
import {
    getLaterThisWeek,
    getNextWeekMonday,
    getThisWeekend,
} from './datePresets';

export interface ParsedNaturalDate {
    date?: string | null;
    time?: string | null;
    hasTime?: boolean;
    repeat?: string | null;
    displayText: string;
}

const WEEKDAY_SHORT_TO_FULL: Record<string, string> = {
    일: '일요일',
    월: '월요일',
    화: '화요일',
    수: '수요일',
    목: '목요일',
    금: '금요일',
    토: '토요일',
};

const WEEKDAY_FULL = ['일요일', '월요일', '화요일', '수요일', '목요일', '금요일', '토요일'];

/** JS `\b`는 한글에 동작하지 않아 토큰 경계를 직접 검사 */
function containsToken(text: string, token: string): boolean {
    const trimmed = text.trim();
    if (trimmed === token) return true;
    const escaped = token.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
    return new RegExp(`(^|\\s)${escaped}(\\s|$)`).test(trimmed);
}

function removeToken(text: string, token: string): string {
    const escaped = token.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
    return text
        .replace(new RegExp(`(^|\\s)${escaped}(\\s|$)`, 'g'), ' ')
        .replace(/\s+/g, ' ')
        .trim();
}

function parseMonthDay(month: number, day: number, ref: Date): string {
    let year = ref.getFullYear();
    let candidate = new Date(year, month - 1, day);
    const weekAgo = addDays(ref, -7);
    if (candidate < weekAgo) {
        year += 1;
        candidate = new Date(year, month - 1, day);
    }
    return toISODate(candidate);
}

function parseTime12(period: '오전' | '오후', hourText: string, minuteText = '00'): string | null {
    let hour = Number(hourText);
    const minute = Number(minuteText);
    if (Number.isNaN(hour) || Number.isNaN(minute) || hour < 1 || hour > 12 || minute < 0 || minute > 59) {
        return null;
    }
    if (period === '오후' && hour < 12) hour += 12;
    if (period === '오전' && hour === 12) hour = 0;
    return `${String(hour).padStart(2, '0')}:${String(minute).padStart(2, '0')}`;
}

function parseTime24(hourText: string, minuteText: string): string | null {
    const hour = Number(hourText);
    const minute = Number(minuteText);
    if (Number.isNaN(hour) || Number.isNaN(minute) || hour < 0 || hour > 23 || minute < 0 || minute > 59) {
        return null;
    }
    return `${String(hour).padStart(2, '0')}:${String(minute).padStart(2, '0')}`;
}

function nextWeekday(ref: Date, weekdayIndex: number): string {
    const day = ref.getDay();
    let delta = weekdayIndex - day;
    if (delta <= 0) delta += 7;
    return toISODate(addDays(ref, delta));
}

function extractRepeat(
    text: string,
    dueDate: string | null,
    ref: Date,
): { repeat: string | null; found: boolean; rest: string } {
    let rest = text;

    const yearly = rest.match(/매년\s*(\d{1,2})월\s*(\d{1,2})일/);
    if (yearly) {
        rest = rest.replace(yearly[0], ' ').replace(/\s+/g, ' ').trim();
        return { repeat: yearly[0], found: true, rest };
    }

    const monthly = rest.match(/매월\s*(\d{1,2})일/);
    if (monthly) {
        rest = rest.replace(monthly[0], ' ').replace(/\s+/g, ' ').trim();
        return { repeat: monthly[0], found: true, rest };
    }

    const weeklyLong = rest.match(/매주\s*(일|월|화|수|목|금|토)요일/);
    if (weeklyLong) {
        const label = `매주 ${WEEKDAY_SHORT_TO_FULL[weeklyLong[1]!]}`;
        rest = rest.replace(weeklyLong[0], ' ').replace(/\s+/g, ' ').trim();
        return { repeat: label, found: true, rest };
    }

    if (containsToken(rest, '평일마다') || /평일마다(?:\s*\(월-금\))?/.test(rest)) {
        rest = rest.replace(/평일마다(?:\s*\(월-금\))?/, ' ').replace(/\s+/g, ' ').trim();
        return { repeat: '평일마다 (월-금)', found: true, rest };
    }

    if (containsToken(rest, '매일')) {
        rest = removeToken(rest, '매일');
        return { repeat: '매일', found: true, rest };
    }

    if (containsToken(rest, '반복')) {
        rest = removeToken(rest, '반복');
        const base = dueDate ? new Date(`${dueDate.slice(0, 10)}T00:00:00`) : ref;
        const weekday = base.toLocaleDateString('ko-KR', { weekday: 'long' });
        return { repeat: `매주 ${weekday}`, found: true, rest };
    }

    return { repeat: null, found: false, rest };
}

function extractTime(text: string): { time: string | null; found: boolean; rest: string } {
    let rest = text;

    const ampmClock = rest.match(/(오전|오후)\s*(\d{1,2}):(\d{2})/);
    if (ampmClock) {
        const time = parseTime12(ampmClock[1] as '오전' | '오후', ampmClock[2]!, ampmClock[3]!);
        rest = rest.replace(ampmClock[0], ' ').replace(/\s+/g, ' ').trim();
        return { time, found: true, rest };
    }

    const ampmHour = rest.match(/(오전|오후)\s*(\d{1,2})시(?:\s*(\d{1,2})분)?/);
    if (ampmHour) {
        const time = parseTime12(
            ampmHour[1] as '오전' | '오후',
            ampmHour[2]!,
            ampmHour[3] ?? '00',
        );
        rest = rest.replace(ampmHour[0], ' ').replace(/\s+/g, ' ').trim();
        return { time, found: true, rest };
    }

    const clock24 = rest.match(/\b(\d{1,2}):(\d{2})\b/);
    if (clock24) {
        const time = parseTime24(clock24[1]!, clock24[2]!);
        rest = rest.replace(clock24[0], ' ').replace(/\s+/g, ' ').trim();
        return { time, found: true, rest };
    }

    const hourOnly = rest.match(/\b(\d{1,2})시(?:\s*(\d{1,2})분)?/);
    if (hourOnly) {
        const hour = Number(hourOnly[1]);
        const minute = hourOnly[2] ?? '00';
        const period: '오전' | '오후' = hour >= 12 ? '오후' : '오전';
        const hour12 = hour > 12 ? hour - 12 : hour === 0 ? 12 : hour;
        const time = parseTime12(period, String(hour12), minute);
        rest = rest.replace(hourOnly[0], ' ').replace(/\s+/g, ' ').trim();
        return { time, found: true, rest };
    }

    return { time: null, found: false, rest };
}

function extractDate(text: string, ref: Date): { date: string | null; found: boolean } {
    const normalized = text.replace(/\s+/g, ' ').trim();
    if (!normalized) return { date: null, found: false };

    if (/날짜\s*없음/.test(normalized)) return { date: null, found: true };

    if (containsToken(normalized, '오늘')) return { date: toISODate(ref), found: true };
    if (containsToken(normalized, '내일')) return { date: toISODate(addDays(ref, 1)), found: true };
    if (containsToken(normalized, '어제')) return { date: toISODate(addDays(ref, -1)), found: true };
    if (containsToken(normalized, '모레')) return { date: toISODate(addDays(ref, 2)), found: true };

    if (/이번\s*주\s*후반/.test(normalized)) return { date: toISODate(getLaterThisWeek(ref)), found: true };
    if (/이번\s*주말/.test(normalized)) return { date: toISODate(getThisWeekend(ref)), found: true };
    if (/다음\s*주/.test(normalized)) return { date: toISODate(getNextWeekMonday(ref)), found: true };

    const isoFull = normalized.match(/(\d{4})[-./년\s]*(\d{1,2})[-./월\s]*(\d{1,2})일?/);
    if (isoFull) {
        return {
            date: toISODate(new Date(Number(isoFull[1]), Number(isoFull[2]) - 1, Number(isoFull[3]))),
            found: true,
        };
    }

    const monthDay = normalized.match(/(\d{1,2})월\s*(\d{1,2})일/);
    if (monthDay) {
        return {
            date: parseMonthDay(Number(monthDay[1]), Number(monthDay[2]), ref),
            found: true,
        };
    }

    const slash = normalized.match(/\b(\d{1,2})\/(\d{1,2})\b/);
    if (slash) {
        return {
            date: parseMonthDay(Number(slash[1]), Number(slash[2]), ref),
            found: true,
        };
    }

    const weekdayOnly = normalized.match(/\b(일|월|화|수|목|금|토)요일\b/);
    if (weekdayOnly) {
        const full = WEEKDAY_SHORT_TO_FULL[weekdayOnly[1]!]!;
        const index = WEEKDAY_FULL.indexOf(full);
        if (index >= 0) return { date: nextWeekday(ref, index), found: true };
    }

    return { date: null, found: false };
}

export function formatNaturalDateInput(
    date: string | null,
    hasTime: boolean,
    time24: string | null,
    repeat: string | null,
): string {
    const parts: string[] = [];

    if (date) {
        const day = date.slice(0, 10);
        const relative = getRelativeDueDayLabel(day);
        if (relative) {
            parts.push(relative);
        } else {
            const parsed = new Date(`${day}T00:00:00`);
            parts.push(parsed.toLocaleDateString('ko-KR', { month: 'long', day: 'numeric' }));
        }
    }

    if (hasTime && time24) {
        parts.push(formatTime12From24(time24));
    }

    if (repeat) {
        parts.push(repeat);
    }

    return parts.join(' ');
}

export function parseNaturalDateInput(
    raw: string,
    options: {
        referenceDate?: Date;
        fallbackDate?: string | null;
        fallbackHasTime?: boolean;
        fallbackTime?: string | null;
        fallbackRepeat?: string | null;
    } = {},
): ParsedNaturalDate | null {
    const ref = options.referenceDate ?? new Date();
    const trimmed = raw.replace(/\s+/g, ' ').trim();

    if (!trimmed) {
        return {
            date: null,
            time: null,
            hasTime: false,
            repeat: null,
            displayText: '',
        };
    }

    const fallbackDate = options.fallbackDate ?? null;
    const repeatPart = extractRepeat(trimmed, fallbackDate, ref);
    const timePart = extractTime(repeatPart.rest);
    const datePart = extractDate(timePart.rest, ref);

    const result: ParsedNaturalDate = { displayText: '' };

    if (datePart.found) {
        result.date = datePart.date;
    }
    if (timePart.found) {
        result.time = timePart.time;
        result.hasTime = timePart.time != null;
    }
    if (repeatPart.found) {
        result.repeat = repeatPart.repeat;
    }

    if (!datePart.found && !timePart.found && !repeatPart.found) {
        return null;
    }

    const resolvedDate = result.date !== undefined ? result.date : fallbackDate;
    const resolvedHasTime = result.hasTime !== undefined
        ? result.hasTime
        : Boolean(options.fallbackHasTime);
    const resolvedTime = result.time !== undefined
        ? result.time
        : options.fallbackTime ?? null;
    const resolvedRepeat = result.repeat !== undefined
        ? result.repeat
        : options.fallbackRepeat ?? null;

    result.displayText = formatNaturalDateInput(
        resolvedDate,
        resolvedHasTime,
        resolvedTime,
        resolvedRepeat,
    );

    return result;
}
