import React, { useEffect, useRef, useState } from 'react';
import { useAppDispatch } from '../store/hooks';
import { checkHabit, deleteHabit } from '../store/habitSlice';
import type { EntityId } from '../api/types';
import type { Habit } from '../store/habitSlice';
import { useDialog } from '../context/DialogContext';
import { displayLabelName } from '../api/labelMappers';
import { formatDueLabel, formatTaskDetailDue, toISODate } from '../utils/date';
import { formatOverdueDueLabel, isOverdueHabit } from '../utils/overdueTasks';
import { CommentBubbleIcon, HashIcon, SubtaskCountIcon } from './icons';

export type TaskRowLayout = 'list' | 'project' | 'upcoming';

interface HabitItemProps {
    habit: Habit;
    layout?: TaskRowLayout;
    variant?: 'default' | 'overdue';
    showDragHandle?: boolean;
    isDragging?: boolean;
    isDragOver?: boolean;
    onDragHandleStart?: (event: React.DragEvent<HTMLButtonElement>) => void;
    onDragEnter?: (event: React.DragEvent<HTMLLIElement>) => void;
    onDragOver?: (event: React.DragEvent<HTMLLIElement>) => void;
    onDragEnd?: (event: React.DragEvent<HTMLLIElement>) => void;
    onDrop?: (event: React.DragEvent<HTMLLIElement>) => void;
    onOpenDetails?: (habit: Habit) => void;
    onOpenProject?: (projectId: EntityId) => void;
    onTaskCompleted?: (habit: Habit) => void;
    onTaskDeleted?: (habitId: EntityId) => void;
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
    variant = 'default',
    showDragHandle = false,
    isDragging = false,
    isDragOver = false,
    onDragHandleStart,
    onDragEnter,
    onDragOver,
    onDragEnd,
    onDrop,
    onOpenDetails,
    onOpenProject,
    onTaskCompleted,
    onTaskDeleted,
}) => {
    const dispatch = useAppDispatch();
    const { confirm, showErrorAlert } = useDialog();
    const [menuOpen, setMenuOpen] = useState(false);
    const menuRef = useRef<HTMLDivElement>(null);
    const completed = habit.completedToday;

    useEffect(() => {
        if (!menuOpen) return;
        const handleClick = (e: MouseEvent) => {
            if (!menuRef.current?.contains(e.target as Node)) {
                setMenuOpen(false);
            }
        };
        document.addEventListener('mousedown', handleClick);
        return () => document.removeEventListener('mousedown', handleClick);
    }, [menuOpen]);

    const handleCheck = async (e: React.MouseEvent) => {
        e.stopPropagation();
        const payload = {
            habitId: habit.id,
            wasCompleted: completed,
        };
        const result = await dispatch(checkHabit(payload));
        if (checkHabit.fulfilled.match(result) && !completed && result.payload.completedToday) {
            onTaskCompleted?.(result.payload);
        }
    };

    const handleEdit = (e: React.MouseEvent) => {
        e.stopPropagation();
        setMenuOpen(false);
        onOpenDetails?.(habit);
    };

    const handleDelete = async (e: React.MouseEvent) => {
        e.stopPropagation();
        setMenuOpen(false);
        if (!(await confirm({
            title: '작업 삭제',
            message: `"${habit.name}" 작업을 삭제할까요?`,
            confirmLabel: '삭제',
            variant: 'danger',
        }))) return;

        const result = await dispatch(deleteHabit(habit.id));
        if (deleteHabit.fulfilled.match(result)) {
            onTaskDeleted?.(habit.id);
        } else {
            await showErrorAlert('작업을 삭제하지 못했습니다.');
        }
    };

    const completedSubtasks = habit.subtaskCompletedCount;
    const totalSubtasks = habit.subtaskCount;
    const commentCount = habit.commentCount;
    const projectLabel = habit.projectName ?? '관리함';
    const showProjectAside = layout !== 'project';
    const overdue = variant === 'overdue' || isOverdueHabit(habit);
    const dueChipClass = overdue
        ? 'overdue'
        : habit.dueDate === toISODate(new Date()) ? 'today' : '';
    const dueLabel = habit.dueDate
        ? overdue
            ? formatOverdueDueLabel(habit)
            : habit.hasTime && habit.dueTime24
                ? formatTaskDetailDue(habit.dueDate, true, habit.dueTime24)
                : formatDueLabel(habit.dueDate)
        : null;

    return (
        <li
            className={[
                'task-item',
                'task-item-todoist',
                completed ? 'completed' : '',
                isDragging ? 'is-dragging' : '',
                isDragOver ? 'is-drag-over' : '',
            ].filter(Boolean).join(' ')}
            onMouseLeave={() => setMenuOpen(false)}
            onDragEnter={onDragEnter}
            onDragOver={onDragOver}
            onDragEnd={onDragEnd}
            onDrop={onDrop}
        >
            {showDragHandle && (
                <button
                    type="button"
                    className="task-drag-handle"
                    draggable
                    onDragStart={onDragHandleStart}
                    onClick={event => event.stopPropagation()}
                    aria-label="순서 변경"
                >
                    <span aria-hidden>⋮⋮</span>
                </button>
            )}
            <button
                type="button"
                className="task-check"
                onClick={e => void handleCheck(e)}
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
                onClick={() => onOpenDetails?.(habit)}
                onKeyDown={e => {
                    if (e.key === 'Enter' || e.key === ' ') {
                        e.preventDefault();
                        onOpenDetails?.(habit);
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

                {(layout === 'project' || layout === 'list' || layout === 'upcoming') && habit.description && (
                    <p className="task-desc-inline">{habit.description}</p>
                )}

                <div className="task-meta-row">
                    {totalSubtasks > 0 && (
                        <span className="task-meta-chip subtask-chip">
                            <span className="task-meta-icon"><SubtaskCountIcon /></span>
                            {completedSubtasks}/{totalSubtasks}
                        </span>
                    )}
                    {commentCount > 0 && (
                        <span className="task-meta-chip comment-chip">
                            <span className="task-meta-icon"><CommentBubbleIcon /></span>
                            {commentCount}
                        </span>
                    )}
                    {dueLabel && (
                        <span className={`task-meta-chip due-chip ${dueChipClass}`.trim()}>
                            {habit.hasTime && !overdue && <span className="task-meta-icon">🕐</span>}
                            {dueLabel}
                        </span>
                    )}
                    {habit.labels.map(label => (
                        <span
                            key={label.id}
                            className="task-meta-chip label-chip"
                            style={{ color: label.color, borderColor: label.color }}
                        >
                            {displayLabelName(label.name)}
                        </span>
                    ))}
                </div>
            </div>

            <div className="task-item-actions" ref={menuRef}>
                <button
                    type="button"
                    className="task-more-btn"
                    aria-label="작업 메뉴"
                    onClick={e => {
                        e.stopPropagation();
                        setMenuOpen(v => !v);
                    }}
                >
                    ···
                </button>
                {menuOpen && (
                    <div className="task-action-menu">
                        <button type="button" onClick={handleEdit}>
                            편집
                        </button>
                        <button type="button" className="danger" onClick={e => void handleDelete(e)}>
                            삭제
                        </button>
                    </div>
                )}
            </div>
        </li>
    );
};

export default HabitItem;
