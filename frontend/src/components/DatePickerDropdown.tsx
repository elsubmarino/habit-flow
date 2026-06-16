import React, { useMemo, useState } from 'react';
import { dueDateToTimeInput, formatTime12From24, toISODate } from '../utils/date';
import { buildDatePresets, formatPickerHeader } from '../utils/datePresets';

export interface DatePickerChange {
    date: string | null;
    time?: string | null;
    hasTime?: boolean;
    repeat?: string | null;
}

interface DatePickerDropdownProps {
    value: string | null;
    timeValue?: string | null;
    hasTimeValue?: boolean;
    repeatValue?: string | null;
    onChange: (change: DatePickerChange) => void;
}

type Panel = 'main' | 'time' | 'repeat';

const WEEKDAYS = ['일', '월', '화', '수', '목', '금', '토'];

function parseISO(iso: string): Date {
    return new Date(iso + 'T00:00:00');
}

function buildMonthGrid(year: number, month: number): { date: Date; inMonth: boolean }[] {
    const first = new Date(year, month, 1);
    const startOffset = first.getDay();
    const start = new Date(year, month, 1 - startOffset);

    const cells: { date: Date; inMonth: boolean }[] = [];
    for (let i = 0; i < 42; i++) {
        const d = new Date(start);
        d.setDate(start.getDate() + i);
        cells.push({ date: d, inMonth: d.getMonth() === month });
    }
    return cells;
}

function buildRepeatOptions(selectedDate: string | null) {
    const base = selectedDate ? parseISO(selectedDate) : new Date();
    const weekday = base.toLocaleDateString('ko-KR', { weekday: 'long' });
    const monthDay = base.getDate();
    const month = base.getMonth() + 1;
    return [
        { id: 'daily', label: '매일' },
        { id: 'weekly', label: `매주 ${weekday}` },
        { id: 'weekdays', label: '평일마다 (월-금)' },
        { id: 'monthly', label: `매월 ${monthDay}일` },
        { id: 'yearly', label: `매년 ${month}월 ${monthDay}일` },
        { id: 'custom', label: '사용자 정의...' },
    ];
}

const DatePickerDropdown: React.FC<DatePickerDropdownProps> = ({
    value,
    timeValue = null,
    hasTimeValue = false,
    repeatValue = null,
    onChange,
}) => {
    const today = useMemo(() => new Date(), []);
    const todayIso = toISODate(today);

    const initialView = value ? parseISO(value) : today;
    const [viewYear, setViewYear] = useState(initialView.getFullYear());
    const [viewMonth, setViewMonth] = useState(initialView.getMonth());
    const [panel, setPanel] = useState<Panel>('main');
    const [timeDraft, setTimeDraft] = useState(() => dueDateToTimeInput(value, timeValue));
    const [repeatDraft, setRepeatDraft] = useState(repeatValue);

    const presets = useMemo(() => buildDatePresets(today), [today]);
    const grid = useMemo(() => buildMonthGrid(viewYear, viewMonth), [viewYear, viewMonth]);
    const repeatOptions = useMemo(() => buildRepeatOptions(value), [value]);
    const timeLabel = hasTimeValue && timeDraft ? formatTime12From24(timeDraft) : '시간';
    const repeatLabel = repeatDraft ?? '반복';

    const monthLabel = new Date(viewYear, viewMonth, 1).toLocaleDateString('ko-KR', {
        year: 'numeric',
        month: 'long',
    });

    const selectDate = (date: Date | null) => {
        const iso = date ? toISODate(date) : null;
        const nextHasTime = iso != null && hasTimeValue && Boolean(timeDraft);
        onChange({
            date: iso,
            time: nextHasTime ? timeDraft : null,
            hasTime: nextHasTime,
            repeat: repeatDraft,
        });
    };

    const goToday = () => {
        setViewYear(today.getFullYear());
        setViewMonth(today.getMonth());
        selectDate(today);
    };

    const prevMonth = () => {
        const d = new Date(viewYear, viewMonth - 1, 1);
        setViewYear(d.getFullYear());
        setViewMonth(d.getMonth());
    };

    const nextMonth = () => {
        const d = new Date(viewYear, viewMonth + 1, 1);
        setViewYear(d.getFullYear());
        setViewMonth(d.getMonth());
    };

    const saveTime = () => {
        const datePart = value?.slice(0, 10) ?? todayIso;
        onChange({
            date: datePart,
            time: timeDraft,
            hasTime: true,
            repeat: repeatDraft,
        });
        setPanel('main');
    };

    const saveRepeat = (repeat: string | null) => {
        setRepeatDraft(repeat);
        onChange({
            date: value,
            time: hasTimeValue ? timeDraft : null,
            hasTime: hasTimeValue,
            repeat,
        });
        setPanel('main');
    };

    const clearTime = (e: React.MouseEvent<HTMLButtonElement>) => {
        e.stopPropagation();
        onChange({
            date: value,
            time: null,
            hasTime: false,
            repeat: repeatDraft,
        });
    };

    const clearRepeat = (e: React.MouseEvent<HTMLButtonElement>) => {
        e.stopPropagation();
        setRepeatDraft(null);
        onChange({
            date: value,
            time: hasTimeValue ? timeDraft : null,
            hasTime: hasTimeValue,
            repeat: null,
        });
    };

    if (panel === 'time') {
        return (
            <div className="date-picker-dropdown date-picker-panel-view" onClick={e => e.stopPropagation()} role="dialog" aria-label="시간 선택">
                <div className="date-picker-subheader">
                    <button type="button" className="date-picker-back" onClick={() => setPanel('main')}>‹</button>
                    <span>시간</span>
                </div>
                <div className="date-time-panel">
                    <label className="date-time-field">
                        <span>시간</span>
                        <input
                            type="time"
                            value={timeDraft}
                            onChange={e => setTimeDraft(e.target.value)}
                        />
                    </label>
                    <label className="date-time-field muted">
                        <span>기간</span>
                        <input type="text" value="기간 없음" readOnly disabled />
                    </label>
                    <label className="date-time-field muted">
                        <span>표준 시간대</span>
                        <select disabled value="floating">
                            <option value="floating">플로팅 시간대</option>
                        </select>
                    </label>
                </div>
                <div className="date-picker-subactions">
                    <button type="button" className="quick-cancel" onClick={() => setPanel('main')}>취소</button>
                    <button type="button" className="quick-submit" onClick={saveTime}>저장</button>
                </div>
            </div>
        );
    }

    if (panel === 'repeat') {
        return (
            <div className="date-picker-dropdown date-picker-panel-view" onClick={e => e.stopPropagation()} role="dialog" aria-label="반복 선택">
                <div className="date-picker-subheader">
                    <button type="button" className="date-picker-back" onClick={() => setPanel('main')}>‹</button>
                    <span>반복</span>
                </div>
                <div className="date-repeat-panel">
                    {repeatOptions.map(option => (
                        <button
                            key={option.id}
                            type="button"
                            className={`date-repeat-row${repeatDraft === option.label ? ' selected' : ''}`}
                            onClick={() => saveRepeat(option.label)}
                        >
                            {option.label}
                            {repeatDraft === option.label && <span className="date-repeat-check">✓</span>}
                        </button>
                    ))}
                    {repeatDraft && (
                        <button type="button" className="date-repeat-row danger" onClick={() => saveRepeat(null)}>
                            반복 제거
                        </button>
                    )}
                </div>
            </div>
        );
    }

    return (
        <div className="date-picker-dropdown" onClick={e => e.stopPropagation()} role="dialog" aria-label="날짜 선택">
            <div className="date-picker-header">
                <span className="date-picker-selected">
                    {value
                        ? formatPickerHeader(value)
                        : '날짜 선택'}
                </span>
            </div>

            <button type="button" className="date-preset-row" onClick={() => selectDate(today)}>
                <span className="preset-icon">☀️</span>
                <span className="preset-label">오늘</span>
                <span className="preset-hint">{WEEKDAYS[today.getDay()]}</span>
            </button>

            {presets.map(preset => (
                <button
                    key={preset.id}
                    type="button"
                    className="date-preset-row"
                    onClick={() => selectDate(preset.date)}
                >
                    <span className="preset-icon">{presetIcon(preset.id)}</span>
                    <span className="preset-label">{preset.label}</span>
                    {preset.hint && <span className="preset-hint">{preset.hint}</span>}
                </button>
            ))}

            <div className="date-picker-calendar">
                <div className="calendar-nav">
                    <button type="button" className="cal-nav-btn" onClick={prevMonth} aria-label="이전 달">‹</button>
                    <span className="cal-month-label">{monthLabel}</span>
                    <button type="button" className="cal-nav-btn" onClick={nextMonth} aria-label="다음 달">›</button>
                    <button type="button" className="cal-today-btn" onClick={goToday} title="오늘" aria-label="오늘">◎</button>
                </div>

                <div className="calendar-weekdays">
                    {WEEKDAYS.map(w => (
                        <span key={w} className="calendar-weekday">{w}</span>
                    ))}
                </div>

                <div className="calendar-grid">
                    {grid.map(({ date, inMonth }) => {
                        const iso = toISODate(date);
                        const isSelected = value?.slice(0, 10) === iso;
                        const isToday = iso === todayIso;
                        return (
                            <button
                                key={iso + String(inMonth)}
                                type="button"
                                className={[
                                    'calendar-day',
                                    !inMonth && 'other-month',
                                    isToday && 'is-today',
                                    isSelected && 'is-selected',
                                ].filter(Boolean).join(' ')}
                                onClick={() => selectDate(date)}
                            >
                                {date.getDate()}
                            </button>
                        );
                    })}
                </div>
            </div>

            <div className="date-picker-footer date-picker-footer-stack">
                <div
                    className="date-footer-btn"
                    role="button"
                    tabIndex={0}
                    onClick={() => setPanel('time')}
                    onKeyDown={event => {
                        if (event.key === 'Enter' || event.key === ' ') {
                            event.preventDefault();
                            setPanel('time');
                        }
                    }}
                >
                    <span>🕐</span>
                    <span className="date-footer-label">{timeLabel}</span>
                    {hasTimeValue && (
                        <button
                            type="button"
                            className="date-footer-clear"
                            aria-label="시간 해제"
                            onClick={clearTime}
                        >
                            ×
                        </button>
                    )}
                </div>
                <div
                    className="date-footer-btn"
                    role="button"
                    tabIndex={0}
                    onClick={() => setPanel('repeat')}
                    onKeyDown={event => {
                        if (event.key === 'Enter' || event.key === ' ') {
                            event.preventDefault();
                            setPanel('repeat');
                        }
                    }}
                >
                    <span>🔁</span>
                    <span className="date-footer-label">{repeatLabel}</span>
                    {repeatValue && (
                        <button
                            type="button"
                            className="date-footer-clear"
                            aria-label="반복 해제"
                            onClick={clearRepeat}
                        >
                            ×
                        </button>
                    )}
                </div>
            </div>
        </div>
    );
};

function presetIcon(id: string): string {
    switch (id) {
        case 'tomorrow': return '🌅';
        case 'later-week': return '📅';
        case 'weekend': return '🛋️';
        case 'next-week': return '➡️';
        case 'none': return '⊘';
        default: return '•';
    }
}

export default DatePickerDropdown;
