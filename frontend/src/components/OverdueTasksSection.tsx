import { useState } from 'react';
import { useAppDispatch, useAppSelector } from '../store/hooks';
import { fetchMoreOverdue, patchTaskDueDate } from '../store/habitSlice';
import type { Habit } from '../store/habitSlice';
import { rescheduleHabitToToday } from '../utils/overdueTasks';
import type { ReorderHabitRequest } from '../utils/taskSortOrder';
import SortableTaskList from './SortableTaskList';
import type { TaskRowLayout } from './HabitItem';
import { ChevronDownIcon } from './icons';

interface OverdueTasksSectionProps {
    habits: Habit[];
    layout?: TaskRowLayout;
    sortable?: boolean;
    onReorder?: (request: ReorderHabitRequest) => void;
    onOpenDetails?: (habit: Habit) => void;
    onOpenProject?: (projectId: number) => void;
    onTaskCompleted?: (habit: Habit) => void;
    onTaskDeleted?: (habitId: number) => void;
}

const OverdueTasksSection: React.FC<OverdueTasksSectionProps> = ({
    habits,
    layout = 'list',
    sortable = false,
    onReorder,
    onOpenDetails,
    onOpenProject,
    onTaskCompleted,
    onTaskDeleted,
}) => {
    const dispatch = useAppDispatch();
    const { overdueHasNext, overdueLoadMoreStatus } = useAppSelector(state => state.habits);
    const [collapsed, setCollapsed] = useState(false);
    const [rescheduling, setRescheduling] = useState(false);

    if (habits.length === 0) return null;

    const handleReschedule = async () => {
        setRescheduling(true);
        try {
            await Promise.all(
                habits.map(habit => {
                    const next = rescheduleHabitToToday(habit);
                    return dispatch(patchTaskDueDate({
                        habitId: habit.id,
                        dueDate: next.dueDate,
                        dueTime24: next.dueTime24,
                        hasTime: next.hasTime,
                    })).unwrap();
                }),
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
                    disabled={rescheduling || habits.length === 0}
                >
                    {rescheduling ? '변경 중…' : '일정 변경'}
                </button>
            </div>
            {!collapsed && (
                <>
                    <SortableTaskList
                        habits={habits}
                        layout={layout}
                        variant="overdue"
                        sortable={sortable}
                        onReorder={onReorder}
                        onOpenDetails={onOpenDetails}
                        onOpenProject={onOpenProject}
                        onTaskCompleted={onTaskCompleted}
                        onTaskDeleted={onTaskDeleted}
                    />
                    {overdueHasNext && (
                        <button
                            type="button"
                            className="overdue-load-more"
                            disabled={overdueLoadMoreStatus === 'loading'}
                            onClick={() => void dispatch(fetchMoreOverdue())}
                        >
                            {overdueLoadMoreStatus === 'loading' ? '불러오는 중…' : '더 보기'}
                        </button>
                    )}
                </>
            )}
        </section>
    );
};

export default OverdueTasksSection;
