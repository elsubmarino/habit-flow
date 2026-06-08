import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import type { Habit } from '../store/habitSlice';
import {
    formatMonthYear,
    formatUpcomingSectionTitle,
    getWeekStrip,
    startOfWeekMonday,
    toISODate,
} from '../utils/date';
import HabitItem from './HabitItem';
import InlineAddTaskButton from './InlineAddTaskButton';

interface UpcomingTaskListProps {
    habits: Habit[];
    onOpenDetails?: (habitId: number) => void;
    onOpenProject?: (projectId: number) => void;
    onAddTask?: () => void;
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

const UpcomingTaskList: React.FC<UpcomingTaskListProps> = ({
    habits,
    onOpenDetails,
    onOpenProject,
    onAddTask,
}) => {
    const [weekStart, setWeekStart] = useState(() => startOfWeekMonday(new Date()));
    const [selectedDate, setSelectedDate] = useState(() => toISODate(new Date()));
    const [monthOpen, setMonthOpen] = useState(false);
    const [viewYear, setViewYear] = useState(() => new Date().getFullYear());
    const [viewMonth, setViewMonth] = useState(() => new Date().getMonth());
    const sectionRefs = useRef<Record<string, HTMLElement | null>>({});
    const monthPopoverRef = useRef<HTMLDivElement>(null);

    const weekDays = useMemo(() => getWeekStrip(weekStart, 7), [weekStart]);
    const todayIso = toISODate(new Date());

    const grouped = useMemo(() => {
        const map = new Map<string, Habit[]>();
        const sorted = [...habits].sort((a, b) => {
            const ad = a.dueDate ?? '9999-12-31';
            const bd = b.dueDate ?? '9999-12-31';
            return ad.localeCompare(bd);
        });
        for (const habit of sorted) {
            const key = habit.dueDate?.slice(0, 10) ?? 'none';
            if (!map.has(key)) map.set(key, []);
            map.get(key)!.push(habit);
        }
        return map;
    }, [habits]);

    const groupedKeys = useMemo(
        () => Array.from(grouped.keys()).filter(k => k !== 'none').sort(),
        [grouped],
    );

    const scrollToDate = useCallback((iso: string) => {
        let el = sectionRefs.current[iso];
        if (!el) {
            const next = groupedKeys.find(k => k >= iso);
            if (next) el = sectionRefs.current[next];
        }
        el?.scrollIntoView({ behavior: 'smooth', block: 'start' });
    }, [groupedKeys]);

    const selectDate = useCallback((iso: string) => {
        setSelectedDate(iso);
        setWeekStart(startOfWeekMonday(parseISO(iso)));
        scrollToDate(iso);
    }, [scrollToDate]);

    const goToday = () => {
        const today = new Date();
        const iso = toISODate(today);
        setViewYear(today.getFullYear());
        setViewMonth(today.getMonth());
        selectDate(iso);
        setMonthOpen(false);
    };

    useEffect(() => {
        if (!monthOpen) return;
        const handleClick = (e: MouseEvent) => {
            if (!monthPopoverRef.current?.contains(e.target as Node)) {
                setMonthOpen(false);
            }
        };
        document.addEventListener('mousedown', handleClick);
        return () => document.removeEventListener('mousedown', handleClick);
    }, [monthOpen]);

    const monthLabel = new Date(viewYear, viewMonth, 1).toLocaleDateString('ko-KR', {
        year: 'numeric',
        month: 'long',
    });
    const grid = useMemo(() => buildMonthGrid(viewYear, viewMonth), [viewYear, viewMonth]);

    return (
        <div className="upcoming-view">
            <div className="upcoming-toolbar">
                <div className="upcoming-month-wrap" ref={monthPopoverRef}>
                    <button
                        type="button"
                        className="upcoming-month-btn"
                        onClick={() => {
                            const base = parseISO(selectedDate);
                            setViewYear(base.getFullYear());
                            setViewMonth(base.getMonth());
                            setMonthOpen(v => !v);
                        }}
                    >
                        {formatMonthYear(parseISO(selectedDate))}
                        <span className="upcoming-caret">▾</span>
                    </button>
                    {monthOpen && (
                        <div className="upcoming-month-popover" onClick={e => e.stopPropagation()}>
                            <div className="calendar-nav">
                                <button
                                    type="button"
                                    className="cal-nav-btn"
                                    onClick={() => {
                                        const d = new Date(viewYear, viewMonth - 1, 1);
                                        setViewYear(d.getFullYear());
                                        setViewMonth(d.getMonth());
                                    }}
                                    aria-label="이전 달"
                                >
                                    ‹
                                </button>
                                <span className="cal-month-label">{monthLabel}</span>
                                <button
                                    type="button"
                                    className="cal-nav-btn"
                                    onClick={() => {
                                        const d = new Date(viewYear, viewMonth + 1, 1);
                                        setViewYear(d.getFullYear());
                                        setViewMonth(d.getMonth());
                                    }}
                                    aria-label="다음 달"
                                >
                                    ›
                                </button>
                                <button type="button" className="cal-today-btn" onClick={goToday} aria-label="오늘">◎</button>
                            </div>
                            <div className="calendar-weekdays">
                                {WEEKDAYS.map(w => (
                                    <span key={w} className="calendar-weekday">{w}</span>
                                ))}
                            </div>
                            <div className="calendar-grid">
                                {grid.map(({ date, inMonth }) => {
                                    const iso = toISODate(date);
                                    const isSelected = selectedDate === iso;
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
                                            onClick={() => {
                                                selectDate(iso);
                                                setMonthOpen(false);
                                            }}
                                        >
                                            {date.getDate()}
                                        </button>
                                    );
                                })}
                            </div>
                        </div>
                    )}
                </div>
                <div className="upcoming-today-nav">
                    <button type="button" onClick={() => setWeekStart(d => startOfWeekMonday(new Date(d.getTime() - 7 * 86400000)))}>
                        ‹
                    </button>
                    <button type="button" className="upcoming-today-btn" onClick={goToday}>
                        오늘
                    </button>
                    <button type="button" onClick={() => setWeekStart(d => startOfWeekMonday(new Date(d.getTime() + 7 * 86400000)))}>
                        ›
                    </button>
                </div>
            </div>

            <div className="upcoming-week-strip" role="tablist">
                {weekDays.map(day => (
                    <button
                        key={day.iso}
                        type="button"
                        role="tab"
                        aria-selected={selectedDate === day.iso}
                        className={`upcoming-day-btn ${day.isToday ? 'is-today' : ''} ${selectedDate === day.iso ? 'selected' : ''}`}
                        onClick={() => selectDate(day.iso)}
                    >
                        <span className="upcoming-day-weekday">{day.weekday}</span>
                        <span className="upcoming-day-num">{day.dayNum}</span>
                    </button>
                ))}
            </div>

            {Array.from(grouped.entries()).map(([dateKey, list]) => (
                <section
                    key={dateKey}
                    ref={el => { sectionRefs.current[dateKey] = el; }}
                    className="upcoming-date-section"
                    data-date-key={dateKey}
                >
                    <h2 className="upcoming-date-title">
                        {dateKey === 'none' ? '날짜 없음' : formatUpcomingSectionTitle(dateKey)}
                    </h2>
                    <ul className="task-list">
                        {list.map(habit => (
                            <HabitItem
                                key={habit.id}
                                habit={habit}
                                layout="upcoming"
                                onOpenDetails={onOpenDetails}
                                onOpenProject={onOpenProject}
                            />
                        ))}
                    </ul>
                    {onAddTask && <InlineAddTaskButton onClick={onAddTask} />}
                </section>
            ))}

            {grouped.size === 0 && onAddTask && <InlineAddTaskButton onClick={onAddTask} />}
        </div>
    );
};

export default UpcomingTaskList;
