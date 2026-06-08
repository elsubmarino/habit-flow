import { useEffect, useMemo, useState } from 'react';
import { mapTaskToHabit } from '../api/mappers';
import * as taskApi from '../api/taskApi';
import { useAppDispatch, useAppSelector } from '../store/hooks';
import {
    addComment,
    addSubtask,
    checkHabit,
    toggleSubtask,
    updateHabit,
    type Habit,
} from '../store/habitSlice';
import { displayLabelName } from '../api/labelMappers';
import { formatDueLabel } from '../utils/date';
import DatePickerDropdown from './DatePickerDropdown';
import TaskEditBox from './TaskEditBox';
import { CloseIcon, HashIcon } from './icons';

interface TaskDetailModalProps {
    taskId: number;
    onClose: () => void;
}

const PRIORITY_OPTIONS = [
    { value: 1, icon: '🚩', label: '우선 순위 1' },
    { value: 2, icon: '🟧', label: '우선 순위 2' },
    { value: 3, icon: '🟦', label: '우선 순위 3' },
    { value: 4, icon: '⚑', label: '우선 순위 4' },
] as const;

const TaskDetailModal: React.FC<TaskDetailModalProps> = ({ taskId, onClose }) => {
    const dispatch = useAppDispatch();
    const { projects, labels } = useAppSelector(state => state.habits);
    const [stack, setStack] = useState<number[]>([taskId]);
    const [tasks, setTasks] = useState<Record<number, Habit>>({});
    const [loading, setLoading] = useState(true);
    const [subtasksExpanded, setSubtasksExpanded] = useState(true);
    const [showSubtaskForm, setShowSubtaskForm] = useState(false);
    const [showCommentForm, setShowCommentForm] = useState(false);
    const [subName, setSubName] = useState('');
    const [subDesc, setSubDesc] = useState('');
    const [comment, setComment] = useState('');
    const [activeMenu, setActiveMenu] = useState<'project' | 'date' | 'priority' | 'label' | 'reminder' | null>(null);
    const [projectQuery, setProjectQuery] = useState('');
    const [labelQuery, setLabelQuery] = useState('');
    const [reminderDraft, setReminderDraft] = useState('작업 시간에');
    const [isEditingMain, setIsEditingMain] = useState(false);
    const [editName, setEditName] = useState('');
    const [editDesc, setEditDesc] = useState('');
    const [savingMain, setSavingMain] = useState(false);
    const [editingSubtaskId, setEditingSubtaskId] = useState<number | null>(null);
    const [subEditName, setSubEditName] = useState('');
    const [subEditDesc, setSubEditDesc] = useState('');
    const [savingSubtask, setSavingSubtask] = useState(false);

    const currentId = stack[stack.length - 1];
    const habit = tasks[currentId];

    useEffect(() => {
        setStack([taskId]);
    }, [taskId]);

    useEffect(() => {
        setIsEditingMain(false);
        setEditingSubtaskId(null);
    }, [currentId]);

    useEffect(() => {
        let cancelled = false;
        setLoading(true);
        void taskApi.fetchTaskById(currentId)
            .then(task => {
                if (cancelled) return;
                setTasks(prev => ({ ...prev, [currentId]: mapTaskToHabit(task) }));
            })
            .catch(() => undefined)
            .finally(() => {
                if (!cancelled) setLoading(false);
            });
        return () => {
            cancelled = true;
        };
    }, [currentId]);

    const completedSubtasks = useMemo(
        () => habit?.subtasks.filter(s => s.completed).length ?? 0,
        [habit?.subtasks],
    );

    const filteredProjects = useMemo(
        () => projects.filter(p => p.name.toLowerCase().includes(projectQuery.toLowerCase())),
        [projects, projectQuery],
    );
    const filteredLabels = useMemo(
        () => labels.filter(l => l.name.toLowerCase().includes(labelQuery.toLowerCase())),
        [labels, labelQuery],
    );

    const refreshCurrent = async () => {
        const task = await taskApi.fetchTaskById(currentId);
        setTasks(prev => ({ ...prev, [currentId]: mapTaskToHabit(task) }));
    };

    const navigateToTask = (id: number) => {
        setEditingSubtaskId(null);
        setStack(prev => [...prev, id]);
        setActiveMenu(null);
    };

    const startMainEdit = () => {
        if (!habit) return;
        setEditName(habit.name);
        setEditDesc(habit.description ?? '');
        setIsEditingMain(true);
    };

    const cancelMainEdit = () => {
        setIsEditingMain(false);
        setEditName('');
        setEditDesc('');
    };

    const saveMainEdit = async () => {
        if (!habit || !editName.trim()) return;
        setSavingMain(true);
        try {
            await dispatch(updateHabit({
                habitId: habit.id,
                changes: { name: editName.trim(), description: editDesc.trim() },
            }));
            await refreshCurrent();
            setIsEditingMain(false);
        } finally {
            setSavingMain(false);
        }
    };

    const startSubtaskEdit = async (subtaskId: number) => {
        setEditingSubtaskId(subtaskId);
        setSavingSubtask(false);
        const cached = habit?.subtasks.find(s => s.id === subtaskId);
        setSubEditName(cached?.name ?? '');
        setSubEditDesc(cached?.description ?? '');
        try {
            const task = await taskApi.fetchTaskById(subtaskId);
            const mapped = mapTaskToHabit(task);
            setSubEditName(mapped.name);
            setSubEditDesc(mapped.description ?? '');
        } catch {
            // keep cached values
        }
    };

    const cancelSubtaskEdit = () => {
        setEditingSubtaskId(null);
        setSubEditName('');
        setSubEditDesc('');
    };

    const saveSubtaskEdit = async () => {
        if (!editingSubtaskId || !subEditName.trim()) return;
        setSavingSubtask(true);
        try {
            await dispatch(updateHabit({
                habitId: editingSubtaskId,
                changes: { name: subEditName.trim(), description: subEditDesc.trim() },
            }));
            await refreshCurrent();
            setEditingSubtaskId(null);
        } finally {
            setSavingSubtask(false);
        }
    };

    const handleToggleCurrent = async () => {
        if (!habit) return;
        const previousCompleted = habit.completedToday;
        const result = await dispatch(checkHabit(habit.id));
        if (checkHabit.fulfilled.match(result)) {
            const updated = result.payload.completedToday === previousCompleted
                ? { ...result.payload, completedToday: !previousCompleted }
                : result.payload;
            setTasks(prev => ({ ...prev, [currentId]: updated }));
        }
    };

    const handleToggleSubtask = async (subtaskId: number) => {
        if (!habit) return;
        const previousCompleted = habit.subtasks.find(s => s.id === subtaskId)?.completed;
        const result = await dispatch(toggleSubtask({ habitId: habit.id, subtaskId }));
        if (toggleSubtask.fulfilled.match(result)) {
            let { subtaskId: id, completed } = result.payload;
            if (previousCompleted !== undefined && completed === previousCompleted) {
                completed = !previousCompleted;
            }
            setTasks(prev => {
                const current = prev[currentId];
                if (!current) return prev;
                return {
                    ...prev,
                    [currentId]: {
                        ...current,
                        subtasks: current.subtasks.map(s =>
                            s.id === id ? { ...s, completed } : s,
                        ),
                    },
                };
            });
        }
    };

    const navigateToStackIndex = (index: number) => {
        setStack(prev => prev.slice(0, index + 1));
        setActiveMenu(null);
    };

    const handleAddSubtask = async () => {
        if (!habit || !subName.trim()) return;
        await dispatch(addSubtask({
            habitId: habit.id,
            name: subName.trim(),
            description: subDesc.trim(),
        }));
        await refreshCurrent();
        setSubName('');
        setSubDesc('');
        setShowSubtaskForm(false);
    };

    const handleAddComment = async () => {
        if (!habit || !comment.trim()) return;
        await dispatch(addComment({ habitId: habit.id, text: comment.trim() }));
        await refreshCurrent();
        setComment('');
        setShowCommentForm(false);
    };

    const projectText = habit?.projectName ?? '관리함';
    const dateText = habit?.dueDate
        ? `${formatDueLabel(habit.dueDate)}${habit.dueTime ? ` ${habit.dueTime}` : ''}`
        : null;

    return (
        <div className="task-detail-overlay" onClick={onClose} role="dialog" aria-modal="true">
            <div className="task-detail-modal" onClick={e => e.stopPropagation()}>
                {loading && !habit ? (
                    <p className="task-detail-loading">불러오는 중…</p>
                ) : habit ? (
                    <>
                        <div className="task-detail-main">
                            <div className="task-detail-header">
                                <div className="task-detail-header-left">
                                    {stack.length > 1 ? (
                                        <div className="task-detail-breadcrumb">
                                            {stack.slice(0, -1).map((id, index) => (
                                                <span key={id} className="task-detail-crumb-wrap">
                                                    {index > 0 && <span className="task-detail-crumb-sep">›</span>}
                                                    <button
                                                        type="button"
                                                        className="task-detail-crumb-btn"
                                                        onClick={() => navigateToStackIndex(index)}
                                                    >
                                                        {tasks[id]?.name ?? '작업'}
                                                    </button>
                                                </span>
                                            ))}
                                        </div>
                                    ) : (
                                        <span className="task-detail-project-line">
                                            <HashIcon />
                                            <span>{projectText}</span>
                                        </span>
                                    )}
                                </div>
                                <div className="task-detail-header-actions">
                                    <button type="button" className="task-detail-icon-btn" aria-label="닫기" onClick={onClose}>
                                        <CloseIcon />
                                    </button>
                                </div>
                            </div>

                            <div className="task-detail-title-row">
                                <button
                                    type="button"
                                    className={`task-detail-check${habit.completedToday ? ' checked' : ''}`}
                                    aria-label={habit.completedToday ? '완료 취소' : '완료'}
                                    aria-pressed={habit.completedToday}
                                    onClick={() => void handleToggleCurrent()}
                                />
                                {isEditingMain ? (
                                    <div className="task-detail-edit-wrap">
                                        <TaskEditBox
                                            name={editName}
                                            description={editDesc}
                                            onNameChange={setEditName}
                                            onDescriptionChange={setEditDesc}
                                            onCancel={cancelMainEdit}
                                            onSave={() => void saveMainEdit()}
                                            saving={savingMain}
                                        />
                                    </div>
                                ) : (
                                    <div className="task-detail-text-wrap">
                                        <button
                                            type="button"
                                            className="task-detail-title-btn"
                                            onClick={startMainEdit}
                                        >
                                            {habit.name}
                                        </button>
                                        <button
                                            type="button"
                                            className={`task-detail-desc-btn${habit.description ? '' : ' is-placeholder'}`}
                                            onClick={startMainEdit}
                                        >
                                            {habit.description || '설명 추가'}
                                        </button>
                                    </div>
                                )}
                            </div>

                            {habit.subtasks.length > 0 && (
                                <div className="subtask-group">
                                    <button
                                        type="button"
                                        className="subtask-collapse-btn"
                                        onClick={() => setSubtasksExpanded(v => !v)}
                                    >
                                        {subtasksExpanded ? '˅' : '˃'} 하위 작업 {completedSubtasks}/{habit.subtasks.length}
                                    </button>
                                    {subtasksExpanded && (
                                        <ul className="subtask-list">
                                            {habit.subtasks.map(sub => (
                                                <li key={sub.id} className="subtask-item">
                                                    <button
                                                        type="button"
                                                        className={`subtask-check ${sub.completed ? 'checked' : ''}`}
                                                        onClick={e => {
                                                            e.stopPropagation();
                                                            void handleToggleSubtask(sub.id);
                                                        }}
                                                    />
                                                    {editingSubtaskId === sub.id ? (
                                                        <div className="subtask-edit-inline">
                                                            <TaskEditBox
                                                                name={subEditName}
                                                                description={subEditDesc}
                                                                onNameChange={setSubEditName}
                                                                onDescriptionChange={setSubEditDesc}
                                                                onCancel={cancelSubtaskEdit}
                                                                onSave={() => void saveSubtaskEdit()}
                                                                saving={savingSubtask}
                                                            />
                                                        </div>
                                                    ) : (
                                                        <div className="subtask-item-body">
                                                            <button
                                                                type="button"
                                                                className="subtask-name-edit-btn"
                                                                onClick={() => void startSubtaskEdit(sub.id)}
                                                            >
                                                                <p>{sub.name}</p>
                                                                {sub.childCount > 0 && (
                                                                    <small>{sub.childCount}개 하위 작업</small>
                                                                )}
                                                            </button>
                                                            <button
                                                                type="button"
                                                                className="subtask-nav-btn"
                                                                aria-label="하위 작업 열기"
                                                                onClick={() => navigateToTask(sub.id)}
                                                            >
                                                                ›
                                                            </button>
                                                        </div>
                                                    )}
                                                </li>
                                            ))}
                                        </ul>
                                    )}
                                </div>
                            )}

                            <button type="button" className="subtask-btn" onClick={() => setShowSubtaskForm(v => !v)}>
                                + 하위 작업 추가
                            </button>
                            {showSubtaskForm && (
                                <div className="subtask-form">
                                    <input value={subName} onChange={e => setSubName(e.target.value)} placeholder="작업 이름" />
                                    <input value={subDesc} onChange={e => setSubDesc(e.target.value)} placeholder="설명" />
                                    <div className="subtask-form-actions">
                                        <button type="button" className="quick-cancel" onClick={() => setShowSubtaskForm(false)}>취소</button>
                                        <button type="button" className="quick-submit" onClick={() => void handleAddSubtask()}>작업 추가</button>
                                    </div>
                                </div>
                            )}

                            <div className="task-comment-box">
                                {!showCommentForm ? (
                                    <button type="button" className="task-comment-trigger" onClick={() => setShowCommentForm(true)}>
                                        <span className="task-comment-avatar">👤</span>
                                        <span>댓글</span>
                                        <span className="task-comment-attach">📎</span>
                                    </button>
                                ) : (
                                    <div className="comment-form">
                                        <textarea rows={3} value={comment} onChange={e => setComment(e.target.value)} placeholder="댓글" />
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
                        </div>

                        <aside className="task-detail-side">
                            <div className="detail-row interactive" onClick={() => setActiveMenu(m => m === 'project' ? null : 'project')}>
                                <p className="detail-label">프로젝트</p>
                                <p className="detail-value">{projectText}</p>
                                {activeMenu === 'project' && (
                                    <div className="detail-menu">
                                        <input className="detail-menu-search" placeholder="프로젝트 이름 입력" value={projectQuery} onChange={e => setProjectQuery(e.target.value)} onClick={e => e.stopPropagation()} />
                                        <button type="button" onClick={() => void dispatch(updateHabit({ habitId: habit.id, changes: { projectId: null, projectName: null, projectColor: null } })).then(refreshCurrent)}>관리함</button>
                                        {filteredProjects.map(project => (
                                            <button key={project.id} type="button" onClick={() => void dispatch(updateHabit({ habitId: habit.id, changes: { projectId: project.id, projectName: project.name, projectColor: project.color } })).then(refreshCurrent)}>
                                                {project.name}
                                            </button>
                                        ))}
                                    </div>
                                )}
                            </div>

                            {habit.userName && (
                                <div className="detail-row">
                                    <p className="detail-label">할당된 사람</p>
                                    <p className="detail-value">{habit.userName}</p>
                                </div>
                            )}

                            <div className="detail-row interactive" onClick={() => setActiveMenu(m => m === 'date' ? null : 'date')}>
                                <p className="detail-label">날짜</p>
                                <p className="detail-value">{dateText ?? <span className="detail-add">+</span>}</p>
                                {activeMenu === 'date' && (
                                    <DatePickerDropdown value={habit.dueDate} onChange={iso => void dispatch(updateHabit({ habitId: habit.id, changes: { dueDate: iso } })).then(refreshCurrent)} />
                                )}
                            </div>

                            <div className="detail-row interactive" onClick={() => setActiveMenu(m => m === 'priority' ? null : 'priority')}>
                                <p className="detail-label">우선 순위</p>
                                <p className="detail-value">P{habit.priority}</p>
                                {activeMenu === 'priority' && (
                                    <div className="detail-menu">
                                        {PRIORITY_OPTIONS.map(p => (
                                            <button key={p.value} type="button" onClick={() => void dispatch(updateHabit({ habitId: habit.id, changes: { priority: p.value } })).then(refreshCurrent)}>
                                                {p.icon} {p.label}{habit.priority === p.value ? ' ✓' : ''}
                                            </button>
                                        ))}
                                    </div>
                                )}
                            </div>

                            <div className="detail-row interactive" onClick={() => setActiveMenu(m => m === 'label' ? null : 'label')}>
                                <p className="detail-label">라벨</p>
                                <p className="detail-value">
                                    {habit.labels.length > 0
                                        ? habit.labels.map(l => displayLabelName(l.name)).join(', ')
                                        : <span className="detail-add">+</span>}
                                </p>
                                {activeMenu === 'label' && (
                                    <div className="detail-menu">
                                        <input className="detail-menu-search" placeholder="라벨 입력" value={labelQuery} onChange={e => setLabelQuery(e.target.value)} onClick={e => e.stopPropagation()} />
                                        {filteredLabels.map(label => {
                                            const selected = habit.labels.some(l => l.id === label.id);
                                            return (
                                                <button key={label.id} type="button" onClick={() => {
                                                    const next = selected ? habit.labels.filter(l => l.id !== label.id) : [...habit.labels, label];
                                                    void dispatch(updateHabit({ habitId: habit.id, changes: { labels: next } })).then(refreshCurrent);
                                                }}>
                                                    {selected ? '✓ ' : ''}{displayLabelName(label.name)}
                                                </button>
                                            );
                                        })}
                                    </div>
                                )}
                            </div>

                            <div className="detail-row interactive" onClick={() => setActiveMenu(m => m === 'reminder' ? null : 'reminder')}>
                                <p className="detail-label">미리 알림</p>
                                <p className="detail-value">
                                    {habit.reminders.length > 0 ? `${habit.reminders.length}개` : <span className="detail-add">+</span>}
                                </p>
                                {activeMenu === 'reminder' && (
                                    <div className="reminder-panel">
                                        <select value={reminderDraft} onChange={e => setReminderDraft(e.target.value)}>
                                            <option value="작업 시간에">작업 시간에</option>
                                            <option value="10분 전">10분 전</option>
                                            <option value="1시간 전">1시간 전</option>
                                        </select>
                                        <button type="button" className="quick-submit reminder-add-btn" onClick={() => {
                                            const next = Array.from(new Set([...(habit.reminders ?? []), reminderDraft]));
                                            void dispatch(updateHabit({ habitId: habit.id, changes: { reminders: next } })).then(refreshCurrent);
                                        }}>
                                            미리 알림 추가
                                        </button>
                                    </div>
                                )}
                            </div>
                        </aside>
                    </>
                ) : (
                    <p className="task-detail-loading">작업을 불러오지 못했습니다.</p>
                )}
            </div>
        </div>
    );
};

export default TaskDetailModal;
