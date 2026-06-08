import React from 'react';
import { useAppDispatch } from '../store/hooks';
import { checkHabit } from '../store/habitSlice';
import type { Habit } from '../store/habitSlice';
import { displayLabelName } from '../api/labelMappers';
import { formatDueLabel, toISODate } from '../utils/date';
import { HashIcon } from './icons';

export type TaskRowLayout = 'list' | 'project' | 'upcoming';

interface HabitItemProps {
    habit: Habit;
    layout?: TaskRowLayout;
    onOpenDetails?: (habitId: number) => void;
    onOpenProject?: (projectId: number) => void;
}

const PRIORITY_BORDER: Record<1 | 2 | 3 | 4, string> = {
    1: 'var(--todoist-red)',
    2: '#eb8909',
    3: '#4073ff',
    4: '#ccc',
};

const HabitItem: React.FC<HabitItemProps> = ({
    habit,
    layout = 'list',
    onOpenDetails,
    onOpenProject,
}) => {
    const dispatch = useAppDispatch();
    const completed = habit.completedToday;
    const completedSubtasks = habit.subtasks.filter(s => s.completed).length;
    const totalSubtasks = habit.subtasks.length;
    const projectLabel = habit.projectName ?? '관리함';
    const showProjectAside = layout !== 'project';

    return (
        <li className={`task-item task-item-todoist ${completed ? 'completed' : ''}`}>
            <button
                type="button"
                className="task-check"
                onClick={e => {
                    e.stopPropagation();
                    dispatch(checkHabit(habit.id));
                }}
                aria-label={completed ? '완료 취소' : '완료'}
                aria-pressed={completed}
            >
                <span
                    className="circle-check"
                    style={{ borderColor: PRIORITY_BORDER[habit.priority] }}
                >
                    {completed && (
                        <svg width="12" height="12" viewBox="0 0 12 12" fill="white">
                            <path d="M4.5 6.5L2.5 4.5l.7-.7 1.3 1.3 3.3-3.3.7.7-4 4z" />
                        </svg>
                    )}
                </span>
            </button>

            <div
                className="task-body"
                role="button"
                tabIndex={0}
                onClick={() => onOpenDetails?.(habit.id)}
                onKeyDown={e => {
                    if (e.key === 'Enter' || e.key === ' ') {
                        e.preventDefault();
                        onOpenDetails?.(habit.id);
                    }
                }}
            >
                <div className="task-row-top">
                    <span className="task-name">{habit.name}</span>
                    {showProjectAside && (
                        <button
                            type="button"
                            className="task-project-aside"
                            onClick={e => {
                                e.stopPropagation();
                                if (habit.projectId != null) onOpenProject?.(habit.projectId);
                            }}
                        >
                            <span>{projectLabel}</span>
                            <span className="task-project-hash"><HashIcon /></span>
                        </button>
                    )}
                </div>

                {(layout === 'project' && habit.description) && (
                    <p className="task-desc-inline">{habit.description}</p>
                )}

                <div className="task-meta-row">
                    {totalSubtasks > 0 && (
                        <span className="task-meta-chip subtask-chip">
                            <span className="task-meta-icon">☰</span>
                            {completedSubtasks}/{totalSubtasks}
                        </span>
                    )}
                    {habit.dueDate && (
                        <span className={`task-meta-chip due-chip ${habit.dueDate === toISODate(new Date()) ? 'today' : ''}`}>
                            {habit.dueTime && <span className="task-meta-icon">🕐</span>}
                            {habit.dueTime ?? formatDueLabel(habit.dueDate)}
                        </span>
                    )}
                    {layout === 'project' && habit.labels.map(label => (
                        <span key={label.id} className="task-meta-chip label-chip" style={{ color: label.color }}>
                            {displayLabelName(label.name)}
                        </span>
                    ))}
                </div>
            </div>
        </li>
    );
};

export default HabitItem;
