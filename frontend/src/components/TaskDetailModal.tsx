import { useMemo, useState } from 'react';
import { useAppDispatch, useAppSelector } from '../store/hooks';
import {
    addComment,
    addSubtask,
    attachmentDownloadUrl,
    toggleSubtask,
    updateHabit,
    type Habit,
} from '../store/habitSlice';
import { formatDueLabel } from '../utils/date';
import { formatFileSize } from '../utils/file';
import DatePickerDropdown from './DatePickerDropdown';

interface TaskDetailModalProps {
    habit: Habit;
    onClose: () => void;
}

const TaskDetailModal: React.FC<TaskDetailModalProps> = ({ habit, onClose }) => {
    const dispatch = useAppDispatch();
    const { projects, labels } = useAppSelector(state => state.habits);
    const [showProjectMenu, setShowProjectMenu] = useState(false);
    const [showDateMenu, setShowDateMenu] = useState(false);
    const [showPriorityMenu, setShowPriorityMenu] = useState(false);
    const [showLabelMenu, setShowLabelMenu] = useState(false);
    const [showReminderMenu, setShowReminderMenu] = useState(false);
    const [showEdit, setShowEdit] = useState(false);
    const [showSubtaskForm, setShowSubtaskForm] = useState(false);
    const [showCommentForm, setShowCommentForm] = useState(false);
    const [editName, setEditName] = useState(habit.name);
    const [editDesc, setEditDesc] = useState(habit.description);
    const [subName, setSubName] = useState('');
    const [subDesc, setSubDesc] = useState('');
    const [comment, setComment] = useState('');
    const [projectQuery, setProjectQuery] = useState('');
    const [labelQuery, setLabelQuery] = useState('');
    const [reminderDraft, setReminderDraft] = useState('작업 시간에');

    const projectText = habit.projectName ?? '받은 편지함';
    const labelText = habit.labels.length > 0 ? habit.labels.map(l => l.name).join(', ') : '없음';
    const reminderText = habit.reminders.length > 0 ? `${habit.reminders.length}개` : '없음';
    const completedSubtasks = useMemo(
        () => habit.subtasks.filter(s => s.completed).length,
        [habit.subtasks],
    );
    const filteredProjects = useMemo(
        () => projects.filter(p => p.name.toLowerCase().includes(projectQuery.toLowerCase())),
        [projects, projectQuery],
    );
    const filteredLabels = useMemo(
        () => labels.filter(l => l.name.toLowerCase().includes(labelQuery.toLowerCase())),
        [labels, labelQuery],
    );

    const closeAllMenus = () => {
        setShowProjectMenu(false);
        setShowDateMenu(false);
        setShowPriorityMenu(false);
        setShowLabelMenu(false);
        setShowReminderMenu(false);
    };

    const handleSaveEdit = async () => {
        await dispatch(updateHabit({
            habitId: habit.id,
            changes: { name: editName.trim() || habit.name, description: editDesc.trim() },
        }));
        setShowEdit(false);
    };

    const handleAddSubtask = async () => {
        if (!subName.trim()) return;
        await dispatch(addSubtask({
            habitId: habit.id,
            name: subName.trim(),
            description: subDesc.trim(),
        }));
        setSubName('');
        setSubDesc('');
        setShowSubtaskForm(false);
    };

    const handleAddComment = async () => {
        if (!comment.trim()) return;
        await dispatch(addComment({ habitId: habit.id, text: comment.trim() }));
        setComment('');
        setShowCommentForm(false);
    };

    return (
        <div
            className="task-detail-overlay"
            onClick={() => {
                closeAllMenus();
                onClose();
            }}
            role="dialog"
            aria-modal="true"
        >
            <div className="task-detail-modal" onClick={e => e.stopPropagation()}>
                <div className="task-detail-main">
                    <div className="task-detail-header">
                        <span className="task-detail-project">{projectText}</span>
                        <button type="button" className="task-detail-close" onClick={onClose} aria-label="닫기">
                            ×
                        </button>
                    </div>

                    {showEdit ? (
                        <div className="task-edit-box">
                            <input
                                className="task-edit-title"
                                value={editName}
                                onChange={e => setEditName(e.target.value)}
                                placeholder="작업 이름"
                            />
                            <textarea
                                className="task-edit-desc"
                                value={editDesc}
                                onChange={e => setEditDesc(e.target.value)}
                                placeholder="설명"
                                rows={3}
                            />
                            <div className="task-edit-actions">
                                <button type="button" className="quick-cancel" onClick={() => setShowEdit(false)}>
                                    취소
                                </button>
                                <button type="button" className="quick-submit" onClick={() => void handleSaveEdit()}>
                                    저장
                                </button>
                            </div>
                        </div>
                    ) : (
                        <>
                            <h2 className="task-detail-title" onClick={() => setShowEdit(true)}>{habit.name}</h2>
                            <p className="task-detail-desc" onClick={() => setShowEdit(true)}>
                                {habit.description || '설명을 추가하세요'}
                            </p>
                        </>
                    )}

                    {habit.subtasks.length > 0 && (
                        <div className="subtask-group">
                            <button type="button" className="subtask-collapse-btn">
                                ˅ 하위 작업 {completedSubtasks}/{habit.subtasks.length}
                            </button>
                            <ul className="subtask-list">
                                {habit.subtasks.map(sub => (
                                    <li key={sub.id} className="subtask-item">
                                        <button
                                            type="button"
                                            className={`subtask-check ${sub.completed ? 'checked' : ''}`}
                                            onClick={() => void dispatch(toggleSubtask({ habitId: habit.id, subtaskId: sub.id }))}
                                        />
                                        <div>
                                            <p>{sub.name}</p>
                                            {sub.description && <small>{sub.description}</small>}
                                        </div>
                                    </li>
                                ))}
                            </ul>
                        </div>
                    )}

                    <button type="button" className="subtask-btn" onClick={() => setShowSubtaskForm(v => !v)}>
                        + 하위 작업 추가
                    </button>
                    {showSubtaskForm && (
                        <div className="subtask-form">
                            <input
                                value={subName}
                                onChange={e => setSubName(e.target.value)}
                                placeholder="작업 이름"
                            />
                            <input
                                value={subDesc}
                                onChange={e => setSubDesc(e.target.value)}
                                placeholder="설명"
                            />
                            <div className="subtask-form-actions">
                                <button type="button" className="quick-cancel" onClick={() => setShowSubtaskForm(false)}>취소</button>
                                <button type="button" className="quick-submit" onClick={() => void handleAddSubtask()}>작업 추가</button>
                            </div>
                        </div>
                    )}

                    <div className="task-comment-box" onClick={() => setShowCommentForm(true)}>
                        {!showCommentForm ? (
                            <input type="text" placeholder="댓글" readOnly />
                        ) : (
                            <div className="comment-form">
                                <textarea
                                    rows={3}
                                    value={comment}
                                    onChange={e => setComment(e.target.value)}
                                    placeholder="댓글"
                                />
                                <div className="subtask-form-actions">
                                    <button type="button" className="quick-cancel" onClick={() => setShowCommentForm(false)}>취소</button>
                                    <button type="button" className="quick-submit" onClick={() => void handleAddComment()}>댓글</button>
                                </div>
                            </div>
                        )}
                    </div>

                    {habit.comments.length > 0 && (
                        <ul className="comment-list">
                            {habit.comments.map(c => (
                                <li key={c.id}>
                                    <p>{c.text}</p>
                                    <small>{new Date(c.createdAt).toLocaleString('ko-KR')}</small>
                                </li>
                            ))}
                        </ul>
                    )}

                    {habit.attachments.length > 0 && (
                        <div className="task-detail-attachments">
                            <h3>첨부 파일</h3>
                            <ul>
                                {habit.attachments.map(att => (
                                    <li key={att.id}>
                                        <a href={attachmentDownloadUrl(att.downloadUrl)} target="_blank" rel="noopener noreferrer">
                                            📎 {att.originalFileName} ({formatFileSize(att.fileSize)})
                                        </a>
                                    </li>
                                ))}
                            </ul>
                        </div>
                    )}
                </div>

                <aside className="task-detail-side">
                    <div className="detail-row interactive" onClick={() => {
                        closeAllMenus();
                        setShowProjectMenu(v => !v);
                    }}>
                        <p className="detail-label">프로젝트</p>
                        <p className="detail-value">{projectText}</p>
                        {showProjectMenu && (
                            <div className="detail-menu">
                                <input
                                    className="detail-menu-search"
                                    placeholder="프로젝트 이름 입력"
                                    value={projectQuery}
                                    onChange={e => setProjectQuery(e.target.value)}
                                    onClick={e => e.stopPropagation()}
                                />
                                <button type="button" onClick={() => void dispatch(updateHabit({
                                    habitId: habit.id,
                                    changes: { projectId: null, projectName: null, projectColor: null },
                                }))}>받은 편지함</button>
                                {filteredProjects.map(project => (
                                    <button
                                        type="button"
                                        key={project.id}
                                        onClick={() => void dispatch(updateHabit({
                                            habitId: habit.id,
                                            changes: {
                                                projectId: project.id,
                                                projectName: project.name,
                                                projectColor: project.color,
                                            },
                                        }))}
                                    >
                                        {project.name}
                                    </button>
                                ))}
                            </div>
                        )}
                    </div>

                    <div className="detail-row interactive" onClick={() => {
                        closeAllMenus();
                        setShowDateMenu(v => !v);
                    }}>
                        <p className="detail-label">날짜</p>
                        <p className="detail-value">{habit.dueDate ? formatDueLabel(habit.dueDate) : '없음'}</p>
                        {showDateMenu && (
                            <DatePickerDropdown
                                value={habit.dueDate}
                                onChange={iso => void dispatch(updateHabit({ habitId: habit.id, changes: { dueDate: iso } }))}
                            />
                        )}
                    </div>

                    <div className="detail-row interactive" onClick={() => {
                        closeAllMenus();
                        setShowPriorityMenu(v => !v);
                    }}>
                        <p className="detail-label">우선순위</p>
                        <p className="detail-value">P{habit.priority}</p>
                        {showPriorityMenu && (
                            <div className="detail-menu">
                                {[
                                    { value: 1, icon: '🚩', label: '우선 순위 1' },
                                    { value: 2, icon: '🟧', label: '우선 순위 2' },
                                    { value: 3, icon: '🟦', label: '우선 순위 3' },
                                    { value: 4, icon: '⚑', label: '우선 순위 4' },
                                ].map(p => (
                                    <button
                                        key={p.value}
                                        type="button"
                                        onClick={() => void dispatch(updateHabit({
                                            habitId: habit.id,
                                            changes: { priority: p.value as 1 | 2 | 3 | 4 },
                                        }))}
                                    >
                                        {p.icon} {p.label}{habit.priority === p.value ? ' ✓' : ''}
                                    </button>
                                ))}
                            </div>
                        )}
                    </div>

                    <div className="detail-row interactive" onClick={() => {
                        closeAllMenus();
                        setShowLabelMenu(v => !v);
                    }}>
                        <p className="detail-label">라벨</p>
                        <p className="detail-value">{labelText}</p>
                        {showLabelMenu && (
                            <div className="detail-menu">
                                <input
                                    className="detail-menu-search"
                                    placeholder="라벨 입력"
                                    value={labelQuery}
                                    onChange={e => setLabelQuery(e.target.value)}
                                    onClick={e => e.stopPropagation()}
                                />
                                {filteredLabels.map(label => {
                                    const selected = habit.labels.some(l => l.id === label.id);
                                    return (
                                        <button
                                            key={label.id}
                                            type="button"
                                            onClick={() => {
                                                const next = selected
                                                    ? habit.labels.filter(l => l.id !== label.id)
                                                    : [...habit.labels, label];
                                                void dispatch(updateHabit({ habitId: habit.id, changes: { labels: next } }));
                                            }}
                                        >
                                            {selected ? '✓ ' : ''}{label.name}
                                        </button>
                                    );
                                })}
                            </div>
                        )}
                    </div>

                    <div className="detail-row interactive" onClick={() => {
                        closeAllMenus();
                        setShowReminderMenu(v => !v);
                    }}>
                        <p className="detail-label">미리 알림</p>
                        <p className="detail-value">{reminderText}</p>
                        {showReminderMenu && (
                            <div className="reminder-panel">
                                <p>미리 알림</p>
                                <select value={reminderDraft} onChange={e => setReminderDraft(e.target.value)}>
                                    <option value="작업 시간에">작업 시간에</option>
                                    <option value="10분 전">10분 전</option>
                                    <option value="1시간 전">1시간 전</option>
                                    <option value="1일 전">1일 전</option>
                                </select>
                                <button
                                    type="button"
                                    className="quick-submit reminder-add-btn"
                                    onClick={() => {
                                        const next = Array.from(new Set([...(habit.reminders ?? []), reminderDraft]));
                                        void dispatch(updateHabit({ habitId: habit.id, changes: { reminders: next } }));
                                    }}
                                >
                                    미리 알림 추가
                                </button>
                                {habit.reminders.length > 0 && (
                                    <ul className="reminder-list">
                                        {habit.reminders.map(reminder => (
                                            <li key={reminder}>
                                                <span>{reminder}</span>
                                                <button
                                                    type="button"
                                                    onClick={() => {
                                                        const next = habit.reminders.filter(r => r !== reminder);
                                                        void dispatch(updateHabit({ habitId: habit.id, changes: { reminders: next } }));
                                                    }}
                                                >
                                                    ×
                                                </button>
                                            </li>
                                        ))}
                                    </ul>
                                )}
                            </div>
                        )}
                    </div>
                </aside>
            </div>
        </div>
    );
};

export default TaskDetailModal;
