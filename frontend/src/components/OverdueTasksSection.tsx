import { useState } from 'react';
import { useAppDispatch } from '../store/hooks';
import { patchTaskDueDate } from '../store/habitSlice';
import type { Habit } from '../store/habitSlice';
import { rescheduleHabitToToday } from '../utils/overdueTasks';
import HabitItem, { type TaskRowLayout } from './HabitItem';
import { ChevronDownIcon } from './icons';

interface OverdueTasksSectionProps {
    habits: Habit[];
    layout?: TaskRowLayout;
    onOpenDetails?: (habitId: number) => void;
    onOpenProject?: (projectId: number) => void;
    onTaskCompleted?: (habit: Habit) => void;
    onTaskDeleted?: (habitId: number) => void;
}

const OverdueTasksSection: React.FC<OverdueTasksSectionProps> = ({
    habits,
    layout = 'list',
    onOpenDetails,
    onOpenProject,
    onTaskCompleted,
    onTaskDeleted,
}) => {
    const dispatch = useAppDispatch();
    const [collapsed, setCollapsed] = useState(false);
    const [rescheduling, setRescheduling] = useState(false);

    if (habits.length === 0) return null;

    const handleReschedule = async () => {
        setRescheduling(true);
        try {
            await Promise.all(
                habits.map(habit =>
                    dispatch(patchTaskDueDate({
                        habitId: habit.id,
                        dueDate: rescheduleHabitToToday(habit),
                    })).unwrap(),
                ),
            );
        } catch {
            window.alert('일정 변경에 실패했습니다.');
        } finally {
            setRescheduling(false);
        }
    };

    return (
        <section className="overdue-section">
            <div className="overdue-section-header">
                <button
                    type="button"
                    className={`overdue-section-toggle ${collapsed ? 'collapsed' : ''}`}
                    onClick={() => setCollapsed(v => !v)}
                    aria-expanded={!collapsed}
                >
                    <span className="overdue-section-chevron">
                        <ChevronDownIcon />
                    </span>
                    기한이 지난
                </button>
                <button
                    type="button"
                    className="overdue-reschedule-btn"
                    onClick={() => void handleReschedule()}
                    disabled={rescheduling}
                >
                    {rescheduling ? '변경 중…' : '일정 변경'}
                </button>
            </div>
            {!collapsed && (
                <ul className="task-list">
                    {habits.map(habit => (
                        <HabitItem
                            key={habit.id}
                            habit={habit}
                            layout={layout}
                            variant="overdue"
                            onOpenDetails={onOpenDetails}
                            onOpenProject={onOpenProject}
                            onTaskCompleted={onTaskCompleted}
                            onTaskDeleted={onTaskDeleted}
                        />
                    ))}
                </ul>
            )}
        </section>
    );
};

export default OverdueTasksSection;
