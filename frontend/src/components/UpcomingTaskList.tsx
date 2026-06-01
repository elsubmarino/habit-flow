import React from 'react';
import type { Habit } from '../store/habitSlice';
import HabitItem from './HabitItem';
import { formatSectionDate } from '../utils/date';

interface UpcomingTaskListProps {
    habits: Habit[];
    onOpenDetails?: (habitId: number) => void;
}

const UpcomingTaskList: React.FC<UpcomingTaskListProps> = ({ habits, onOpenDetails }) => {
    const grouped = habits.reduce<Record<string, Habit[]>>((acc, habit) => {
        const key = habit.dueDate ?? 'none';
        if (!acc[key]) acc[key] = [];
        acc[key].push(habit);
        return acc;
    }, {});

    const sortedKeys = Object.keys(grouped).sort();

    return (
        <>
            {sortedKeys.map(dateKey => (
                <section key={dateKey}>
                    <h2 className="section-label upcoming-date">
                        {dateKey === 'none' ? '날짜 없음' : formatSectionDate(dateKey)}
                    </h2>
                    <ul className="task-list">
                        {grouped[dateKey].map(habit => (
                            <HabitItem
                                key={habit.id}
                                habit={habit}
                                onOpenDetails={onOpenDetails}
                            />
                        ))}
                    </ul>
                </section>
            ))}
        </>
    );
};

export default UpcomingTaskList;
