import { useMemo, useState } from 'react';
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

const UpcomingTaskList: React.FC<UpcomingTaskListProps> = ({
    habits,
    onOpenDetails,
    onOpenProject,
    onAddTask,
}) => {
    const [weekStart, setWeekStart] = useState(() => startOfWeekMonday(new Date()));
    const [selectedDate, setSelectedDate] = useState(() => toISODate(new Date()));

    const weekDays = useMemo(() => getWeekStrip(weekStart, 7), [weekStart]);

    const grouped = useMemo(() => {
        const map = new Map<string, Habit[]>();
        const sorted = [...habits].sort((a, b) => {
            const ad = a.dueDate ?? '9999-12-31';
            const bd = b.dueDate ?? '9999-12-31';
            return ad.localeCompare(bd);
        });
        for (const habit of sorted) {
            const key = habit.dueDate ?? 'none';
            if (!map.has(key)) map.set(key, []);
            map.get(key)!.push(habit);
        }
        return map;
    }, [habits]);

    const goToday = () => {
        const today = new Date();
        setWeekStart(startOfWeekMonday(today));
        setSelectedDate(toISODate(today));
    };

    return (
        <div className="upcoming-view">
            <div className="upcoming-toolbar">
                <button type="button" className="upcoming-month-btn">
                    {formatMonthYear(new Date(selectedDate + 'T00:00:00'))}
                    <span className="upcoming-caret">▾</span>
                </button>
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
                        onClick={() => setSelectedDate(day.iso)}
                    >
                        <span className="upcoming-day-weekday">{day.weekday}</span>
                        <span className="upcoming-day-num">{day.dayNum}</span>
                    </button>
                ))}
            </div>

            {Array.from(grouped.entries()).map(([dateKey, list]) => (
                <section key={dateKey} className="upcoming-date-section">
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
