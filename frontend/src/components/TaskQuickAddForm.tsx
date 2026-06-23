import { useEffect, useRef, useState } from 'react';
import type { EntityId } from '../api/types';
import { useAppSelector } from '../store/hooks';
import { formatFileSize, validateFile } from '../utils/file';
import { formatDueLabel, formatTaskDetailDue } from '../utils/date';
import DatePickerDropdown from './DatePickerDropdown';

export interface TaskQuickAddSubmitPayload {
    name: string;
    description: string;
    dueDate: string | null;
    dueTime24: string | null;
    hasTime: boolean;
    recurrenceLabel: string | null;
    projectId: EntityId | null;
    labelIds: EntityId[];
    priority: 1 | 2 | 3 | 4;
    file: File | null;
}

interface TaskQuickAddFormProps {
    variant?: 'modal' | 'inline';
    submitLabel?: string;
    showHint?: boolean;
    initialProjectId?: EntityId | null;
    initialLabelIds?: EntityId[];
    initialDueDate?: string | null;
    submitting?: boolean;
    onCancel: () => void;
    onSubmit: (payload: TaskQuickAddSubmitPayload) => void | Promise<void>;
}

const PRIORITY_OPTIONS = [
    { value: 1 as const, icon: '🚩', label: '우선 순위 1' },
    { value: 2 as const, icon: '🟧', label: '우선 순위 2' },
    { value: 3 as const, icon: '🟦', label: '우선 순위 3' },
    { value: 4 as const, icon: '⚑', label: '우선 순위 4' },
];

const PRIORITY_COLORS: Record<1 | 2 | 3 | 4, string> = {
    1: 'var(--todoist-red)',
    2: '#eb8909',
    3: '#4073ff',
    4: '#808080',
};

const TaskQuickAddForm: React.FC<TaskQuickAddFormProps> = ({
    variant = 'modal',
    submitLabel = '작업 추가',
    showHint = variant === 'modal',
    initialProjectId = null,
    initialLabelIds = [],
    initialDueDate = null,
    submitting = false,
    onCancel,
    onSubmit,
}) => {
    const { projects, labels } = useAppSelector(state => state.habits);

    const nameRef = useRef<HTMLInputElement>(null);
    const fileInputRef = useRef<HTMLInputElement>(null);

    const [name, setName] = useState('');
    const [description, setDescription] = useState('');
    const [dueDate, setDueDate] = useState<string | null>(initialDueDate);
    const [dueTime24, setDueTime24] = useState<string | null>(null);
    const [hasTime, setHasTime] = useState(false);
    const [recurrenceLabel, setRecurrenceLabel] = useState<string | null>(null);
    const [selectedProjectId, setSelectedProjectId] = useState<EntityId | ''>(initialProjectId ?? '');
    const [selectedLabelIds, setSelectedLabelIds] = useState<EntityId[]>(initialLabelIds);
    const [priority, setPriority] = useState<1 | 2 | 3 | 4>(4);
    const [showDatePicker, setShowDatePicker] = useState(false);
    const [showPriorityPicker, setShowPriorityPicker] = useState(false);
    const [showLabelPicker, setShowLabelPicker] = useState(false);
    const [pendingFiles, setPendingFiles] = useState<File[]>([]);
    const [fileError, setFileError] = useState<string | null>(null);

    useEffect(() => {
        requestAnimationFrame(() => nameRef.current?.focus());
    }, []);

    const handleSubmit = () => {
        if (!name.trim() || submitting) return;
        void onSubmit({
            name: name.trim(),
            description: description.trim(),
            dueDate,
            dueTime24,
            hasTime,
            recurrenceLabel,
            projectId: selectedProjectId === '' ? null : selectedProjectId,
            labelIds: selectedLabelIds,
            priority,
            file: pendingFiles[0] ?? null,
        });
    };

    const handleFileSelect = (e: React.ChangeEvent<HTMLInputElement>) => {
        const files = Array.from(e.target.files ?? []);
        if (files.length === 0) return;

        const errors: string[] = [];
        const valid: File[] = [];
        for (const file of files) {
            const err = validateFile(file);
            if (err) errors.push(err);
            else valid.push(file);
        }

        if (valid.length === 0) {
            if (errors.length > 0) setFileError(errors[0]);
            e.target.value = '';
            return;
        }

        if (files.length > 1 || pendingFiles.length > 0) {
            setFileError('작업당 파일은 하나만 첨부할 수 있습니다.');
        } else {
            setFileError(null);
        }

        setPendingFiles([valid[0]]);
        e.target.value = '';
    };

    const removePendingFile = (index: number) => {
        setPendingFiles(prev => prev.filter((_, i) => i !== index));
    };

    const handleNameKeyDown = (e: React.KeyboardEvent) => {
        if (e.key === 'Enter' && !e.shiftKey && name.trim()) {
            e.preventDefault();
            handleSubmit();
        }
    };

    const toggleLabel = (id: EntityId) => {
        setSelectedLabelIds(prev =>
            prev.includes(id) ? prev.filter(x => x !== id) : [...prev, id],
        );
    };

    const clearDate = (e: React.MouseEvent) => {
        e.stopPropagation();
        setDueDate(null);
        setDueTime24(null);
        setHasTime(false);
        setRecurrenceLabel(null);
    };

    const dueDateLabel = dueDate
        ? hasTime && dueTime24
            ? formatTaskDetailDue(dueDate, true, dueTime24)
            : formatDueLabel(dueDate)
        : '날짜';

    const selectedProject = projects.find(p => p.id === selectedProjectId);
    const selectedLabels = labels.filter(l => selectedLabelIds.includes(l.id));

    return (
        <div className={`quick-add-card ${variant === 'inline' ? 'quick-add-card-inline' : ''}`}>
            <div className="quick-add-body">
                <input
                    ref={nameRef}
                    className="quick-add-name"
                    placeholder="작업 이름"
                    value={name}
                    onChange={e => setName(e.target.value)}
                    onKeyDown={handleNameKeyDown}
                />
                <textarea
                    className="quick-add-desc"
                    placeholder="설명"
                    value={description}
                    onChange={e => setDescription(e.target.value)}
                    rows={2}
                />
            </div>

            <div className="quick-add-pills">
                <div className="pill-wrapper">
                    <button
                        type="button"
                        className={`quick-pill date-pill ${dueDate ? 'active' : ''}`}
                        onClick={() => {
                            setShowDatePicker(v => !v);
                            setShowPriorityPicker(false);
                            setShowLabelPicker(false);
                            fileInputRef.current?.blur();
                        }}
                    >
                        <CalendarIcon />
                        <span>{dueDateLabel}</span>
                        {dueDate && (
                            <span className="pill-clear" onClick={clearDate} aria-label="날짜 지우기">×</span>
                        )}
                    </button>
                    {showDatePicker && (
                        <DatePickerDropdown
                            value={dueDate}
                            timeValue={hasTime && dueTime24 ? dueTime24 : null}
                            hasTimeValue={hasTime}
                            repeatValue={recurrenceLabel}
                            onChange={change => {
                                if (change.date !== undefined) setDueDate(change.date);
                                if (change.time !== undefined) setDueTime24(change.time);
                                if (change.hasTime !== undefined) setHasTime(change.hasTime);
                                if (change.repeat !== undefined) setRecurrenceLabel(change.repeat);
                            }}
                        />
                    )}
                </div>

                <div className="pill-wrapper">
                    <input
                        ref={fileInputRef}
                        type="file"
                        className="file-input-hidden"
                        multiple
                        accept=".pdf,.png,.jpg,.jpeg,.gif,.webp,.txt,.md,.doc,.docx,.xls,.xlsx,.ppt,.pptx,.zip"
                        onChange={handleFileSelect}
                    />
                    <button
                        type="button"
                        className={`quick-pill ${pendingFiles.length > 0 ? 'active attachment-pill' : ''}`}
                        onClick={() => {
                            setShowDatePicker(false);
                            setShowPriorityPicker(false);
                            setShowLabelPicker(false);
                            fileInputRef.current?.click();
                        }}
                    >
                        <PaperclipIcon />
                        <span>
                            {pendingFiles.length > 0
                                ? `첨부 파일 (${pendingFiles.length})`
                                : '첨부 파일'}
                        </span>
                    </button>
                </div>

                <div className="pill-wrapper">
                    <button
                        type="button"
                        className={`quick-pill priority-pill${priority !== 4 ? ' active' : ''}`}
                        style={priority !== 4 ? {
                            borderColor: PRIORITY_COLORS[priority],
                            color: PRIORITY_COLORS[priority],
                            background: `${PRIORITY_COLORS[priority]}14`,
                        } : undefined}
                        onClick={() => {
                            setShowPriorityPicker(v => !v);
                            setShowDatePicker(false);
                            setShowLabelPicker(false);
                        }}
                    >
                        <FlagIcon color={priority !== 4 ? PRIORITY_COLORS[priority] : undefined} />
                        <span>{priority !== 4 ? `우선 순위 ${priority}` : '우선 순위'}</span>
                    </button>
                    {showPriorityPicker && (
                        <div className="quick-popover priority-popover">
                            {PRIORITY_OPTIONS.map(option => (
                                <button
                                    key={option.value}
                                    type="button"
                                    className={`popover-item priority-option${priority === option.value ? ' selected' : ''}`}
                                    onClick={() => {
                                        setPriority(option.value);
                                        setShowPriorityPicker(false);
                                    }}
                                >
                                    <span className="priority-option-icon">{option.icon}</span>
                                    <span>{option.label}</span>
                                    {priority === option.value && <span className="priority-option-check">✓</span>}
                                </button>
                            ))}
                        </div>
                    )}
                </div>

                {labels.length > 0 && (
                    <div className="pill-wrapper">
                        <button
                            type="button"
                            className={`quick-pill ${selectedLabelIds.length > 0 ? 'active label-pill' : ''}`}
                            onClick={() => {
                                setShowLabelPicker(v => !v);
                                setShowDatePicker(false);
                                setShowPriorityPicker(false);
                            }}
                        >
                            <LabelIcon />
                            <span>
                                {selectedLabels.length > 0
                                    ? selectedLabels.map(l => l.name).join(', ')
                                    : '라벨'}
                            </span>
                        </button>
                        {showLabelPicker && (
                            <div className="quick-popover label-popover">
                                {labels.map(label => (
                                    <button
                                        key={label.id}
                                        type="button"
                                        className={`popover-item label-option ${selectedLabelIds.includes(label.id) ? 'selected' : ''}`}
                                        onClick={() => toggleLabel(label.id)}
                                    >
                                        <span className="label-dot" style={{ background: label.color }} />
                                        {label.name}
                                    </button>
                                ))}
                            </div>
                        )}
                    </div>
                )}
            </div>

            {pendingFiles.length > 0 && (
                <ul className="pending-attachments">
                    {pendingFiles.map((file, index) => (
                        <li key={`${file.name}-${index}`} className="pending-attachment-item">
                            <PaperclipIcon />
                            <span className="pending-file-name">{file.name}</span>
                            <span className="pending-file-size">{formatFileSize(file.size)}</span>
                            <button
                                type="button"
                                className="pending-file-remove"
                                onClick={() => removePendingFile(index)}
                                aria-label="첨부 제거"
                            >
                                ×
                            </button>
                        </li>
                    ))}
                </ul>
            )}
            {fileError && <p className="file-error">{fileError}</p>}

            <div className="quick-add-footer">
                <div className="quick-add-project">
                    <InboxIcon />
                    <select
                        className="quick-project-select"
                        value={selectedProjectId}
                        onChange={e => setSelectedProjectId(
                            e.target.value === '' ? '' : e.target.value,
                        )}
                    >
                        <option value="">관리함</option>
                        {projects.map(p => (
                            <option key={p.id} value={p.id}>{p.name}</option>
                        ))}
                    </select>
                    {selectedProject && (
                        <span
                            className="project-preview-dot"
                            style={{ background: selectedProject.color }}
                        />
                    )}
                </div>
                <div className="quick-add-actions">
                    <button type="button" className="quick-cancel" onClick={onCancel}>
                        취소
                    </button>
                    <button
                        type="button"
                        className="quick-submit"
                        onClick={handleSubmit}
                        disabled={!name.trim() || submitting}
                    >
                        {submitting ? '추가 중…' : submitLabel}
                    </button>
                </div>
            </div>

            {showHint && (
                <p className="quick-add-hint">
                    <kbd>Enter</kbd> 추가 · <kbd>Esc</kbd> 닫기 · <kbd>Q</kbd> 빠른 추가
                </p>
            )}
        </div>
    );
};

function PaperclipIcon() {
    return (
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <path d="M21.44 11.05l-9.19 9.19a6 6 0 01-8.49-8.49l9.19-9.19a4 4 0 015.66 5.66l-9.2 9.19a2 2 0 01-2.83-2.83l8.49-8.48" />
        </svg>
    );
}

function CalendarIcon() {
    return (
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <rect x="3" y="4" width="18" height="18" rx="2" />
            <path d="M16 2v4M8 2v4M3 10h18" />
        </svg>
    );
}

function LabelIcon() {
    return (
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <path d="M20.59 13.41l-7.17 7.17a2 2 0 01-2.83 0L2 12V2h10l8.59 8.59a2 2 0 010 2.82z" />
            <circle cx="7" cy="7" r="1.5" fill="currentColor" stroke="none" />
        </svg>
    );
}

function FlagIcon({ color }: { color?: string }) {
    return (
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke={color ?? 'currentColor'} strokeWidth="2">
            <path d="M4 15s1-1 4-1 5 2 8 2 4-1 4-1V3s-1 1-4 1-5-2-8-2-4 1-4 1z" />
            <line x1="4" y1="22" x2="4" y2="15" />
        </svg>
    );
}

function InboxIcon() {
    return (
        <svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor" opacity="0.6">
            <path d="M19 3H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm0 16H5V8h14v11z" />
        </svg>
    );
}

export default TaskQuickAddForm;
