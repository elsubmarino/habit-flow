import { useEffect, useMemo, useRef, useState } from 'react';
import { formatSectionDate, toISODate } from '../utils/date';

interface ActivityLogDateRangePickerProps {
    fromDate: string | null;
    toDate: string | null;
    onChange: (range: { fromDate: string | null; toDate: string | null }) => void;
}

const WEEKDAYS = ['일', '월', '화', '수', '목', '금', '토'];

function parseISO(iso: string): Date {
    return new Date(`${iso}T00:00:00`);
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

function formatRangeLabel(fromDate: string | null, toDate: string | null): string {
    if (!fromDate) return '모든 날짜';
    if (!toDate || fromDate === toDate) {
        return formatSectionDate(fromDate);
    }
    return `${formatSectionDate(fromDate)} – ${formatSectionDate(toDate)}`;
}

function isBetween(iso: string, from: string, to: string): boolean {
    return iso >= from && iso <= to;
}

const ActivityLogDateRangePicker: React.FC<ActivityLogDateRangePickerProps> = ({
    fromDate,
    toDate,
    onChange,
}) => {
    const rootRef = useRef<HTMLDivElement>(null);
    const [open, setOpen] = useState(false);
    const [anchorDate, setAnchorDate] = useState<string | null>(null);

    const today = useMemo(() => new Date(), []);
    const initialView = fromDate ? parseISO(fromDate) : today;
    const [viewYear, setViewYear] = useState(initialView.getFullYear());
    const [viewMonth, setViewMonth] = useState(initialView.getMonth());

    const grid = useMemo(() => buildMonthGrid(viewYear, viewMonth), [viewYear, viewMonth]);
    const monthLabel = new Date(viewYear, viewMonth, 1).toLocaleDateString('ko-KR', {
        year: 'numeric',
        month: 'long',
    });

    useEffect(() => {
        if (!open) {
            setAnchorDate(null);
            return;
        }
        const onDocClick = (event: MouseEvent) => {
            if (rootRef.current && !rootRef.current.contains(event.target as Node)) {
                setOpen(false);
            }
        };
        document.addEventListener('mousedown', onDocClick);
        return () => document.removeEventListener('mousedown', onDocClick);
    }, [open]);

    useEffect(() => {
        if (!open || !fromDate) return;
        const next = parseISO(fromDate);
        setViewYear(next.getFullYear());
        setViewMonth(next.getMonth());
    }, [open, fromDate]);

    const applySingleDate = (iso: string) => {
        onChange({ fromDate: iso, toDate: null });
        setAnchorDate(null);
        setOpen(false);
    };

    const applyRange = (start: string, end: string) => {
        const [from, to] = start <= end ? [start, end] : [end, start];
        if (from === to) {
            applySingleDate(from);
            return;
        }
        onChange({ fromDate: from, toDate: to });
        setAnchorDate(null);
        setOpen(false);
    };

    const handleDayClick = (iso: string) => {
        if (!anchorDate) {
            setAnchorDate(iso);
            return;
        }
        applyRange(anchorDate, iso);
    };

    const handleApplyAnchor = () => {
        if (!anchorDate) return;
        applySingleDate(anchorDate);
    };

    const clearRange = () => {
        onChange({ fromDate: null, toDate: null });
        setAnchorDate(null);
        setOpen(false);
    };

    const prevMonth = () => {
        if (viewMonth === 0) {
            setViewYear(year => year - 1);
            setViewMonth(11);
            return;
        }
        setViewMonth(month => month - 1);
    };

    const nextMonth = () => {
        if (viewMonth === 11) {
            setViewYear(year => year + 1);
            setViewMonth(0);
            return;
        }
        setViewMonth(month => month + 1);
    };

    const goToday = () => {
        setViewYear(today.getFullYear());
        setViewMonth(today.getMonth());
    };

    return (
        <div className="activity-filter activity-date-filter" ref={rootRef}>
            <button
                type="button"
                className="activity-date-filter-btn"
                onClick={() => setOpen(prev => !prev)}
                aria-expanded={open}
            >
                <span className="activity-filter-icon" aria-hidden>📅</span>
                <span className="activity-date-filter-label">
                    {formatRangeLabel(fromDate, toDate)}
                </span>
            </button>

            {open && (
                <div className="activity-date-dropdown" onClick={e => e.stopPropagation()}>
                    <div className="activity-date-dropdown-hint">
                        {anchorDate
                            ? '종료 날짜를 선택하거나, 하단에서 이 날짜만 적용하세요.'
                            : '날짜를 한 번 선택하면 하루, 두 번 선택하면 기간으로 적용됩니다.'}
                    </div>

                    <div className="date-picker-calendar activity-date-calendar">
                        <div className="calendar-nav">
                            <button type="button" className="cal-nav-btn" onClick={prevMonth} aria-label="이전 달">‹</button>
                            <span className="cal-month-label">{monthLabel}</span>
                            <button type="button" className="cal-nav-btn" onClick={nextMonth} aria-label="다음 달">›</button>
                            <button type="button" className="cal-today-btn" onClick={goToday} title="오늘" aria-label="오늘">◎</button>
                        </div>

                        <div className="calendar-weekdays">
                            {WEEKDAYS.map(day => (
                                <span key={day} className="calendar-weekday">{day}</span>
                            ))}
                        </div>

                        <div className="calendar-grid">
                            {grid.map(({ date, inMonth }) => {
                                const iso = toISODate(date);
                                const isAnchor = anchorDate === iso;
                                const isSelectedStart = !anchorDate && fromDate === iso;
                                const isSelectedEnd = !anchorDate && toDate === iso;
                                const inSelectedRange = !anchorDate
                                    && fromDate
                                    && toDate
                                    && fromDate !== toDate
                                    && isBetween(iso, fromDate, toDate);
                                return (
                                    <button
                                        key={iso}
                                        type="button"
                                        className={[
                                            'calendar-day',
                                            !inMonth && 'other-month',
                                            isAnchor && 'range-anchor',
                                            isSelectedStart && 'range-start is-selected',
                                            isSelectedEnd && 'range-end is-selected',
                                            inSelectedRange && 'in-range',
                                        ].filter(Boolean).join(' ')}
                                        onClick={() => handleDayClick(iso)}
                                    >
                                        {date.getDate()}
                                    </button>
                                );
                            })}
                        </div>
                    </div>

                    <div className="activity-date-dropdown-actions">
                        <button type="button" className="quick-cancel" onClick={clearRange}>
                            모든 날짜
                        </button>
                        {anchorDate && (
                            <button type="button" className="quick-submit" onClick={handleApplyAnchor}>
                                {formatSectionDate(anchorDate)}만
                            </button>
                        )}
                    </div>
                </div>
            )}
        </div>
    );
};

export default ActivityLogDateRangePicker;
