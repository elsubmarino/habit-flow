import React, { useMemo, useState } from 'react';
import { toISODate } from '../utils/date';
import { buildDatePresets, formatPickerHeader } from '../utils/datePresets';

interface DatePickerDropdownProps {
    value: string | null;
    onChange: (iso: string | null) => void;
}

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

const DatePickerDropdown: React.FC<DatePickerDropdownProps> = ({ value, onChange }) => {
    const today = useMemo(() => new Date(), []);
    const todayIso = toISODate(today);

    const initialView = value ? parseISO(value) : today;
    const [viewYear, setViewYear] = useState(initialView.getFullYear());
    const [viewMonth, setViewMonth] = useState(initialView.getMonth());

    const presets = useMemo(() => buildDatePresets(today), [today]);
    const grid = useMemo(() => buildMonthGrid(viewYear, viewMonth), [viewYear, viewMonth]);

    const monthLabel = new Date(viewYear, viewMonth, 1).toLocaleDateString('ko-KR', {
        year: 'numeric',
        month: 'long',
    });

    const selectDate = (date: Date | null) => {
        onChange(date ? toISODate(date) : null);
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

    return (
        <div className="date-picker-dropdown" onClick={e => e.stopPropagation()} role="dialog" aria-label="날짜 선택">
            <div className="date-picker-header">
                <span className="date-picker-selected">{formatPickerHeader(value)}</span>
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
                        const isSelected = value === iso;
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

            <div className="date-picker-footer">
                <button type="button" className="date-footer-btn disabled" disabled title="준비 중">
                    <span>🕐</span> 시간
                </button>
                <button type="button" className="date-footer-btn disabled" disabled title="준비 중">
                    <span>🔁</span> 반복
                </button>
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
