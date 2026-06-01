import React from 'react';
import { useAppDispatch } from '../store/hooks';
import { checkHabit } from '../store/habitSlice';
import type { Habit } from '../store/habitSlice';
import { attachmentDownloadUrl } from '../store/habitSlice';
import { formatFileSize } from '../utils/file';
import { formatDueLabel } from '../utils/date';

const HabitItem: React.FC<{ habit: Habit; onOpenDetails?: (habitId: number) => void }> = ({
    habit,
    onOpenDetails,
}) => {
    const dispatch = useAppDispatch();
    const completed = habit.completedToday;

    return (
        <li className={`task-item ${completed ? 'completed' : ''}`}>
            <button
                type="button"
                className="task-check"
                onClick={() => dispatch(checkHabit(habit.id))}
                aria-label={completed ? '완료 취소' : '완료'}
                aria-pressed={completed}
            >
                <span className="circle-check">
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
                <span className="task-name">{habit.name}</span>
                {habit.description && <p className="task-desc">{habit.description}</p>}
                <div className="task-meta">
                    {habit.projectName && (
                        <span className="project-tag">
                            <span className="project-tag-dot" style={{ background: habit.projectColor ?? '#4073ff' }} />
                            {habit.projectName}
                        </span>
                    )}
                    {habit.dueDate && (
                        <span className="due-date-tag">{formatDueLabel(habit.dueDate)}</span>
                    )}
                    {habit.labels?.map(label => (
                        <span key={label.id} className="label-tag" style={{ color: label.color }}>
                            {label.name}
                        </span>
                    ))}
                    {habit.streak > 0 && (
                        <span className="streak-badge">🔥 {habit.streak}일 연속</span>
                    )}
                </div>
                {habit.attachments?.length > 0 && (
                    <ul className="task-attachments">
                        {habit.attachments.map(att => (
                            <li key={att.id}>
                                <a
                                    href={attachmentDownloadUrl(att.downloadUrl)}
                                    className="attachment-link"
                                    target="_blank"
                                    rel="noopener noreferrer"
                                    onClick={e => e.stopPropagation()}
                                >
                                    📎 {att.originalFileName}
                                    <span className="attachment-size">{formatFileSize(att.fileSize)}</span>
                                </a>
                            </li>
                        ))}
                    </ul>
                )}
            </div>
        </li>
    );
};

export default HabitItem;
