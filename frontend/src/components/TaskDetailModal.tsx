import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { mapCommentDtos, mapTaskToHabit } from '../api/mappers';
import * as commentApi from '../api/commentApi';
import * as taskApi from '../api/taskApi';
import { useAppDispatch, useAppSelector } from '../store/hooks';
import {
    addComment,
    addSubtask,
    checkHabit,
    patchTaskDueDate,
    patchTaskLabels,
    patchTaskPriority,
    patchTaskProject,
    syncHabitCommentCount,
    toggleSubtask,
    updateHabit,
    type CommentItem,
    type Habit,
    type Subtask,
} from '../store/habitSlice';
import { getApiErrorMessage } from '../api/apiError';
import type { EntityId } from '../api/types';
import { displayLabelName } from '../api/labelMappers';
import { useDialog } from '../context/DialogContext';
import { formatTaskDetailDue } from '../utils/date';
import CommentListItem from './CommentListItem';
import DatePickerDropdown, { type DatePickerChange } from './DatePickerDropdown';
import TaskEditBox from './TaskEditBox';
import TaskQuickAddForm, { type TaskQuickAddSubmitPayload } from './TaskQuickAddForm';
import { CloseIcon, HashIcon } from './icons';

interface TaskDetailModalProps {
    taskId: EntityId;
    onClose: () => void;
    onTaskCompleted?: (habit: Habit) => void;
}

/** 루트 포함 탐색 스택 최대 깊이 (루트 → 하위 4단계) */
const MAX_SUBTASK_DEPTH = 4;
/** 부모 작업당 직계 하위 작업 최대 개수 (백엔드와 동일) */
const MAX_SUBTASKS_PER_PARENT = 4;

const PRIORITY_OPTIONS = [
    { value: 1, icon: '🚩', label: '우선 순위 1' },
    { value: 2, icon: '🟧', label: '우선 순위 2' },
    { value: 3, icon: '🟦', label: '우선 순위 3' },
    { value: 4, icon: '⚑', label: '우선 순위 4' },
] as const;

const TaskDetailModal: React.FC<TaskDetailModalProps> = ({ taskId, onClose, onTaskCompleted }) => {
    const dispatch = useAppDispatch();
    const { confirm, showAlert, showErrorAlert } = useDialog();
    const { projects, labels } = useAppSelector(state => state.habits);
    const [stack, setStack] = useState<EntityId[]>([taskId]);
    const [tasks, setTasks] = useState<Record<EntityId, Habit>>({});
    const [loading, setLoading] = useState(true);
    const [subtasksExpanded, setSubtasksExpanded] = useState(true);
    const [showSubtaskForm, setShowSubtaskForm] = useState(false);
    const [subtaskSubmitting, setSubtaskSubmitting] = useState(false);
    const [showCommentForm, setShowCommentForm] = useState(false);
    const [comment, setComment] = useState('');
    const [activeMenu, setActiveMenu] = useState<'project' | 'date' | 'priority' | 'label' | null>(null);
    const [projectQuery, setProjectQuery] = useState('');
    const [labelQuery, setLabelQuery] = useState('');
    const [isEditingMain, setIsEditingMain] = useState(false);
    const [editName, setEditName] = useState('');
    const [editDesc, setEditDesc] = useState('');
    const [savingMain, setSavingMain] = useState(false);
    const [editingSubtaskId, setEditingSubtaskId] = useState<EntityId | null>(null);
    const [subEditName, setSubEditName] = useState('');
    const [subEditDesc, setSubEditDesc] = useState('');
    const [savingSubtask, setSavingSubtask] = useState(false);
    const [comments, setComments] = useState<CommentItem[]>([]);
    const [recurrence, setRecurrence] = useState<string | null>(null);
    const [draftLabelIds, setDraftLabelIds] = useState<EntityId[]>([]);
    const [savingLabels, setSavingLabels] = useState(false);
    const labelMenuRef = useRef<HTMLDivElement>(null);
    const dateMenuRef = useRef<HTMLDivElement>(null);

    const currentTaskId = stack[stack.length - 1];
    const habit = tasks[currentTaskId];

    useEffect(() => {
        setStack([taskId]);
    }, [taskId]);

    useEffect(() => {
        setIsEditingMain(false);
        setEditingSubtaskId(null);
    }, [currentTaskId]);

    useEffect(() => {
        if (habit) setRecurrence(habit.recurrenceLabel);
    }, [habit?.recurrenceLabel, currentTaskId]);

    useEffect(() => {
        let cancelled = false;
        setLoading(true);
        void taskApi.fetchTaskById(currentTaskId)
            .then(async task => {
                if (cancelled) return;
                const mapped = mapTaskToHabit(task);
                setTasks(prev => ({ ...prev, [currentTaskId]: mapped }));
                try {
                    const commentDtos = await commentApi.fetchTaskComments(currentTaskId);
                    if (!cancelled) setComments(mapCommentDtos(commentDtos));
                } catch {
                    if (!cancelled) setComments(mapped.comments);
                }
            })
            .catch(() => undefined)
            .finally(() => {
                if (!cancelled) setLoading(false);
            });
        return () => {
            cancelled = true;
        };
    }, [currentTaskId]);

    const completedSubtasks = useMemo(
        () => habit?.subtasks.filter(s => s.completed).length ?? 0,
        [habit?.subtasks],
    );
    const canAddSubtaskByDepth = stack.length <= MAX_SUBTASK_DEPTH;
    const canAddSubtaskByCount = (habit?.subtasks.length ?? 0) < MAX_SUBTASKS_PER_PARENT;
    const canAddSubtask = canAddSubtaskByDepth && canAddSubtaskByCount;
    const subtaskLimitHint = !canAddSubtaskByDepth
        ? `하위 작업은 최대 ${MAX_SUBTASK_DEPTH}단계까지만 추가할 수 있습니다.`
        : `하위 작업은 부모당 최대 ${MAX_SUBTASKS_PER_PARENT}개까지 추가할 수 있습니다.`;

    useEffect(() => {
        if (!canAddSubtask) {
            setShowSubtaskForm(false);
        }
    }, [canAddSubtask, currentTaskId, stack.length]);

    const filteredProjects = useMemo(
        () => projects.filter(p => p.name.toLowerCase().includes(projectQuery.toLowerCase())),
        [projects, projectQuery],
    );
    const filteredLabels = useMemo(
        () => labels.filter(l => l.name.toLowerCase().includes(labelQuery.toLowerCase())),
        [labels, labelQuery],
    );

    const applyLocalHabit = useCallback((mapped: Habit) => {
        setTasks(prev => ({ ...prev, [currentTaskId]: mapped }));
    }, [currentTaskId]);

    const refreshCurrent = async () => {
        const task = await taskApi.fetchTaskById(currentTaskId);
        const mapped = mapTaskToHabit(task);
        setTasks(prev => ({ ...prev, [currentTaskId]: mapped }));
        try {
            const commentDtos = await commentApi.fetchTaskComments(currentTaskId);
            setComments(mapCommentDtos(commentDtos));
        } catch {
            setComments(mapped.comments);
        }
    };

    const navigateToSubtask = (sub: Subtask) => {
        setEditingSubtaskId(null);
        setStack(prev => [...prev, sub.id]);
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

    const startSubtaskEdit = async (subtaskId: EntityId) => {
        setEditingSubtaskId(subtaskId);
        setSavingSubtask(false);
        const cached = habit?.subtasks.find(s => s.id === subtaskId);
        setSubEditName(cached?.name ?? '');
        setSubEditDesc(cached?.description ?? '');
        if (cached) {
            try {
                const task = await taskApi.fetchTaskById(cached.id);
                const mapped = mapTaskToHabit(task);
                setSubEditName(mapped.name);
                setSubEditDesc(mapped.description ?? '');
            } catch {
                // keep cached values
            }
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
        const wasCompleted = habit.completedToday;
        const result = await dispatch(checkHabit({
            habitId: habit.id,
            wasCompleted,
        }));
        if (checkHabit.fulfilled.match(result)) {
            setTasks(prev => ({ ...prev, [currentTaskId]: result.payload }));
            if (!wasCompleted && result.payload.completedToday) {
                onTaskCompleted?.(result.payload);
            }
        }
    };

    const handleToggleSubtask = async (subtaskId: EntityId) => {
        if (!habit) return;
        const subtask = habit.subtasks.find(s => s.id === subtaskId);
        const previousCompleted = subtask?.completed;
        const result = await dispatch(toggleSubtask({
            habitId: habit.id,
            subtaskId,
        }));
        if (toggleSubtask.fulfilled.match(result)) {
            let { subtaskId: id, completed } = result.payload;
            if (previousCompleted !== undefined && completed === previousCompleted) {
                completed = !previousCompleted;
            }
            setTasks(prev => {
                const currentHabit = prev[currentTaskId];
                if (!currentHabit) return prev;
                return {
                    ...prev,
                    [currentTaskId]: {
                        ...currentHabit,
                        subtasks: currentHabit.subtasks.map(s =>
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

    const resolveProjectIdForSubtask = (): EntityId | null => {
        for (let i = stack.length - 1; i >= 0; i -= 1) {
            const projectId = tasks[stack[i]]?.projectId;
            if (projectId != null) return projectId;
        }
        return habit?.projectId ?? null;
    };

    const handleAddSubtask = async (payload: TaskQuickAddSubmitPayload) => {
        if (!habit) return;
        if (!canAddSubtaskByDepth) {
            await showAlert(`하위 작업은 최대 ${MAX_SUBTASK_DEPTH}단계까지만 추가할 수 있습니다.`);
            setShowSubtaskForm(false);
            return;
        }
        if (!canAddSubtaskByCount) {
            await showAlert(`하위 작업은 부모당 최대 ${MAX_SUBTASKS_PER_PARENT}개까지 추가할 수 있습니다.`);
            setShowSubtaskForm(false);
            return;
        }

        setSubtaskSubmitting(true);
        try {
            const result = await dispatch(addSubtask({
                habitId: habit.id,
                name: payload.name,
                description: payload.description,
                projectId: payload.projectId ?? resolveProjectIdForSubtask(),
                dueDate: payload.dueDate,
                dueTime24: payload.dueTime24,
                hasTime: payload.hasTime,
                recurrenceLabel: payload.recurrenceLabel,
                labelIds: payload.labelIds,
                file: payload.file,
                priority: payload.priority,
            }));

            if (addSubtask.rejected.match(result)) {
                await showErrorAlert(getApiErrorMessage(result.error, '하위 작업을 추가하지 못했습니다.'));
                return;
            }

            await refreshCurrent();
            setShowSubtaskForm(false);
        } finally {
            setSubtaskSubmitting(false);
        }
    };

    const handleAddComment = async () => {
        if (!habit || !comment.trim()) return;
        await dispatch(addComment({ habitId: habit.id, text: comment.trim() }));
        await refreshCurrent();
        setComment('');
        setShowCommentForm(false);
    };

    const handleEditComment = async (item: CommentItem, text: string) => {
        if (!habit) return;
        if (!item.backendId) {
            throw new Error('댓글 ID가 없어 수정할 수 없습니다.');
        }
        const updated = await commentApi.updateComment(item.backendId, habit.id, text);
        setComments(prev => prev.map(comment => (
            comment.backendId === item.backendId
                ? { ...comment, text: updated.content }
                : comment
        )));
    };

    const handleDeleteComment = async (item: CommentItem) => {
        if (!item.backendId) {
            throw new Error('댓글 ID가 없어 삭제할 수 없습니다.');
        }
        if (!(await confirm({
            title: '댓글 삭제',
            message: '이 댓글을 삭제할까요?',
            confirmLabel: '삭제',
            variant: 'danger',
        }))) return;
        if (!habit) return;
        await commentApi.deleteComment(item.backendId);
        await refreshCurrent();
        await dispatch(syncHabitCommentCount(habit.id));
    };

    const handleDatePickerChange = (change: DatePickerChange) => {
        if (!habit) return;

        const nextDate = change.date !== undefined ? change.date : habit.dueDate;
        const nextHasTime = change.hasTime !== undefined ? change.hasTime : habit.hasTime;
        const nextTime24 = change.time !== undefined ? change.time : habit.dueTime24;
        const nextRecurrence = change.repeat !== undefined ? change.repeat : recurrence;
        setRecurrence(nextRecurrence);

        void dispatch(patchTaskDueDate({
            habitId: habit.id,
            dueDate: nextDate,
            dueTime24: nextTime24,
            hasTime: nextHasTime,
            recurrenceLabel: nextRecurrence,
        })).then(result => {
            if (patchTaskDueDate.fulfilled.match(result)) {
                applyLocalHabit(result.payload);
                if (change.repeat !== undefined) {
                    setRecurrence(result.payload.recurrenceLabel);
                }
            }
        });
    };

    const handlePriorityChange = (priority: 1 | 2 | 3 | 4) => {
        if (!habit) return;
        void dispatch(patchTaskPriority({ habitId: habit.id, priority })).then(result => {
            if (patchTaskPriority.fulfilled.match(result)) {
                applyLocalHabit(result.payload);
            }
        });
    };

    const handleProjectChange = (projectId: EntityId | null) => {
        if (!habit) return;
        void dispatch(patchTaskProject({ habitId: habit.id, projectId })).then(result => {
            if (patchTaskProject.fulfilled.match(result)) {
                applyLocalHabit(result.payload);
            } else {
                void showErrorAlert('프로젝트를 변경하지 못했습니다.');
            }
            setActiveMenu(null);
        });
    };

    const openLabelMenu = () => {
        if (!habit) return;
        setDraftLabelIds(habit.labels.map(l => l.id));
        setLabelQuery('');
        setActiveMenu('label');
    };

    const commitLabelSelection = useCallback(async () => {
        if (!habit) return;

        const savedIds = habit.labels.map(l => l.id).sort((a, b) => a.localeCompare(b));
        const draftSorted = [...draftLabelIds].sort((a, b) => a.localeCompare(b));
        const unchanged = savedIds.length === draftSorted.length
            && savedIds.every((id, index) => id === draftSorted[index]);

        if (unchanged) return;

        setSavingLabels(true);
        try {
            const result = await dispatch(patchTaskLabels({
                habitId: habit.id,
                labelIds: draftLabelIds,
            }));
            if (patchTaskLabels.fulfilled.match(result)) {
                applyLocalHabit(result.payload);
            } else {
                await showErrorAlert('라벨을 저장하지 못했습니다.');
            }
        } finally {
            setSavingLabels(false);
        }
    }, [applyLocalHabit, dispatch, draftLabelIds, habit]);

    const closeLabelMenu = useCallback(async () => {
        await commitLabelSelection();
        setActiveMenu(null);
    }, [commitLabelSelection]);

    const toggleDraftLabel = (labelId: EntityId) => {
        setDraftLabelIds(prev =>
            prev.includes(labelId) ? prev.filter(id => id !== labelId) : [...prev, labelId],
        );
    };

    const removeLabelFromTask = useCallback(async (labelId: EntityId, event: React.MouseEvent) => {
        event.stopPropagation();
        if (!habit) return;

        if (activeMenu === 'label') {
            setDraftLabelIds(prev => prev.filter(id => id !== labelId));
            return;
        }

        const nextIds = habit.labels.filter(label => label.id !== labelId).map(label => label.id);
        setSavingLabels(true);
        try {
            const result = await dispatch(patchTaskLabels({
                habitId: habit.id,
                labelIds: nextIds,
            }));
            if (patchTaskLabels.fulfilled.match(result)) {
                applyLocalHabit(result.payload);
            } else {
                await showErrorAlert('라벨을 제거하지 못했습니다.');
            }
        } finally {
            setSavingLabels(false);
        }
    }, [activeMenu, applyLocalHabit, dispatch, habit]);

    useEffect(() => {
        if (activeMenu !== 'label') return;

        const handlePointerDown = (event: MouseEvent) => {
            if (!labelMenuRef.current?.contains(event.target as Node)) {
                void closeLabelMenu();
            }
        };

        document.addEventListener('mousedown', handlePointerDown);
        return () => document.removeEventListener('mousedown', handlePointerDown);
    }, [activeMenu, closeLabelMenu]);

    useEffect(() => {
        if (activeMenu !== 'date') return;

        const handlePointerDown = (event: MouseEvent) => {
            if (!dateMenuRef.current?.contains(event.target as Node)) {
                setActiveMenu(null);
            }
        };

        document.addEventListener('mousedown', handlePointerDown);
        return () => document.removeEventListener('mousedown', handlePointerDown);
    }, [activeMenu]);

    const projectText = habit?.projectName ?? '관리함';
    const visibleLabels = useMemo(() => {
        if (!habit) return [];
        if (activeMenu === 'label') {
            return labels.filter(l => draftLabelIds.includes(l.id));
        }
        return habit.labels;
    }, [activeMenu, draftLabelIds, habit, labels]);
    const dateText = habit?.dueDate
        ? `${formatTaskDetailDue(habit.dueDate, habit.hasTime, habit.dueTime24)}${recurrence ? ` · ${recurrence}` : ''}`
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
                                                                onClick={() => navigateToSubtask(sub)}
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

                            {canAddSubtask ? (
                                <button type="button" className="subtask-btn" onClick={() => setShowSubtaskForm(v => !v)}>
                                    + 하위 작업 추가
                                </button>
                            ) : (
                                <p className="subtask-limit-hint">{subtaskLimitHint}</p>
                            )}
                            {showSubtaskForm && canAddSubtask && habit && (
                                <div className="subtask-quick-add-wrap">
                                    <TaskQuickAddForm
                                        key={`subtask-${currentTaskId}`}
                                        variant="inline"
                                        showHint={false}
                                        submitting={subtaskSubmitting}
                                        initialProjectId={resolveProjectIdForSubtask()}
                                        initialDueDate={habit.dueDate}
                                        onCancel={() => setShowSubtaskForm(false)}
                                        onSubmit={handleAddSubtask}
                                    />
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

                            {comments.length > 0 && (
                                <>
                                    <h3 className="task-comments-title">댓글 {comments.length}</h3>
                                    <ul className="comment-list">
                                        {comments.map(c => (
                                            <CommentListItem
                                                key={c.id}
                                                comment={c}
                                                authorName={habit.userName}
                                                canManage
                                                onEdit={handleEditComment}
                                                onDelete={handleDeleteComment}
                                            />
                                        ))}
                                    </ul>
                                </>
                            )}
                        </div>

                        <aside className="task-detail-side">
                            <div className="detail-row interactive" onClick={() => setActiveMenu(m => m === 'project' ? null : 'project')}>
                                <p className="detail-label">프로젝트</p>
                                <p className="detail-value">{projectText}</p>
                                {activeMenu === 'project' && (
                                    <div className="detail-menu">
                                        <input className="detail-menu-search" placeholder="프로젝트 이름 입력" value={projectQuery} onChange={e => setProjectQuery(e.target.value)} onClick={e => e.stopPropagation()} />
                                        <button type="button" onClick={() => handleProjectChange(null)}>관리함</button>
                                        {filteredProjects.map(project => (
                                            <button key={project.id} type="button" onClick={() => handleProjectChange(project.id)}>
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

                            <div
                                ref={dateMenuRef}
                                className={`detail-row interactive${activeMenu === 'date' ? ' date-menu-open' : ''}`}
                                onClick={() => setActiveMenu(m => m === 'date' ? null : 'date')}
                            >
                                <p className="detail-label">날짜</p>
                                <p className="detail-value">{dateText ?? <span className="detail-add">+</span>}</p>
                                {activeMenu === 'date' && (
                                    <DatePickerDropdown
                                        value={habit.dueDate}
                                        timeValue={habit.dueTime}
                                        hasTimeValue={habit.hasTime}
                                        repeatValue={recurrence}
                                        onChange={handleDatePickerChange}
                                    />
                                )}
                            </div>

                            <div className="detail-row interactive" onClick={() => setActiveMenu(m => m === 'priority' ? null : 'priority')}>
                                <p className="detail-label">우선 순위</p>
                                <p className="detail-value">P{habit.priority}</p>
                                {activeMenu === 'priority' && (
                                    <div className="detail-menu">
                                        {PRIORITY_OPTIONS.map(p => (
                                            <button key={p.value} type="button" onClick={() => handlePriorityChange(p.value)}>
                                                {p.icon} {p.label}{habit.priority === p.value ? ' ✓' : ''}
                                            </button>
                                        ))}
                                    </div>
                                )}
                            </div>

                            <div
                                ref={labelMenuRef}
                                className="detail-row interactive"
                                onClick={() => {
                                    if (activeMenu === 'label') {
                                        void closeLabelMenu();
                                    } else {
                                        openLabelMenu();
                                    }
                                }}
                            >
                                <p className="detail-label">라벨</p>
                                <p className="detail-value">
                                    {visibleLabels.length > 0 ? (
                                        <span className="detail-label-chips">
                                            {visibleLabels.map(label => (
                                                <span
                                                    key={label.id}
                                                    className="detail-label-chip"
                                                    style={{ borderColor: label.color, color: label.color }}
                                                >
                                                    <span className="detail-label-chip-name">
                                                        {displayLabelName(label.name)}
                                                    </span>
                                                    <button
                                                        type="button"
                                                        className="detail-label-chip-remove"
                                                        aria-label={`${displayLabelName(label.name)} 라벨 제거`}
                                                        disabled={savingLabels}
                                                        onClick={event => void removeLabelFromTask(label.id, event)}
                                                    >
                                                        ×
                                                    </button>
                                                </span>
                                            ))}
                                        </span>
                                    ) : (
                                        <span className="detail-add">+</span>
                                    )}
                                    {savingLabels && <span className="detail-saving"> 저장 중…</span>}
                                </p>
                                {activeMenu === 'label' && (
                                    <div className="detail-menu detail-menu-labels" onClick={e => e.stopPropagation()}>
                                        <input
                                            className="detail-menu-search"
                                            placeholder="라벨 입력"
                                            value={labelQuery}
                                            onChange={e => setLabelQuery(e.target.value)}
                                        />
                                        {filteredLabels.map(label => (
                                            <label key={label.id} className="detail-menu-label-option">
                                                <input
                                                    type="checkbox"
                                                    checked={draftLabelIds.includes(label.id)}
                                                    onChange={() => toggleDraftLabel(label.id)}
                                                />
                                                <span>{displayLabelName(label.name)}</span>
                                            </label>
                                        ))}
                                        {filteredLabels.length === 0 && (
                                            <p className="detail-menu-empty">일치하는 라벨이 없습니다.</p>
                                        )}
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
