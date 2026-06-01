import React, { useEffect, useRef, useState } from 'react';
import { useAppDispatch, useAppSelector } from '../store/hooks';
import { addHabit, fetchHabits, uploadAttachments, type ApiView } from '../store/habitSlice';
import { formatFileSize, validateFile } from '../utils/file';
import type { NavItem } from './Sidebar';
import { defaultDueDateForView, formatDueLabel } from '../utils/date';
import DatePickerDropdown from './DatePickerDropdown';

interface QuickAddModalProps {
    isOpen: boolean;
    onClose: () => void;
    view: NavItem;
    projectId: number | null;
    labelId: number | null;
}

const QuickAddModal: React.FC<QuickAddModalProps> = ({
    isOpen,
    onClose,
    view,
    projectId,
    labelId,
}) => {
    const dispatch = useAppDispatch();
    const { projects, labels } = useAppSelector(state => state.habits);

    const nameRef = useRef<HTMLInputElement>(null);
    const fileInputRef = useRef<HTMLInputElement>(null);

    const [name, setName] = useState('');
    const [description, setDescription] = useState('');
    const [dueDate, setDueDate] = useState<string | null>(null);
    const [selectedProjectId, setSelectedProjectId] = useState<number | ''>('');
    const [selectedLabelIds, setSelectedLabelIds] = useState<number[]>([]);
    const [showDatePicker, setShowDatePicker] = useState(false);
    const [showLabelPicker, setShowLabelPicker] = useState(false);
    const [pendingFiles, setPendingFiles] = useState<File[]>([]);
    const [fileError, setFileError] = useState<string | null>(null);
    const [submitting, setSubmitting] = useState(false);

    const reset = () => {
        setName('');
        setDescription('');
        setDueDate(null);
        setSelectedProjectId(projectId ?? '');
        setSelectedLabelIds(labelId ? [labelId] : []);
        setShowDatePicker(false);
        setShowLabelPicker(false);
        setPendingFiles([]);
        setFileError(null);
    };

    const initForm = () => {
        const defaultDate = defaultDueDateForView(view);
        setDueDate(defaultDate);
        setSelectedProjectId(projectId ?? '');
        setSelectedLabelIds(labelId ? [labelId] : []);
        setName('');
        setDescription('');
        setShowDatePicker(false);
        setShowLabelPicker(false);
        setPendingFiles([]);
        setFileError(null);
    };

    useEffect(() => {
        if (isOpen) {
            initForm();
            requestAnimationFrame(() => nameRef.current?.focus());
        }
    }, [isOpen, view, projectId, labelId]);

    useEffect(() => {
        if (!isOpen) return;
        const onKey = (e: KeyboardEvent) => {
            if (e.key === 'Escape') onClose();
        };
        window.addEventListener('keydown', onKey);
        return () => window.removeEventListener('keydown', onKey);
    }, [isOpen, onClose]);

    const handleSubmit = async () => {
        if (!name.trim() || submitting) return;
        setSubmitting(true);
        try {
            const created = await dispatch(addHabit({
                name: name.trim(),
                description: description.trim(),
                view,
                projectId: selectedProjectId === '' ? null : selectedProjectId,
                dueDate,
                labelIds: selectedLabelIds,
            })).unwrap();

            if (pendingFiles.length > 0) {
                await dispatch(uploadAttachments({ habitId: created.id, files: pendingFiles })).unwrap();
            }

            const apiView: ApiView = view === 'filters' ? 'all' : view;
            await dispatch(fetchHabits({
                view: apiView,
                projectId,
                labelId,
            }));
            reset();
            onClose();
        } finally {
            setSubmitting(false);
        }
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

        if (errors.length > 0) setFileError(errors[0]);
        else setFileError(null);

        setPendingFiles(prev => {
            const combined = [...prev, ...valid];
            return combined.slice(0, 10);
        });
        e.target.value = '';
    };

    const removePendingFile = (index: number) => {
        setPendingFiles(prev => prev.filter((_, i) => i !== index));
    };

    const handleNameKeyDown = (e: React.KeyboardEvent) => {
        if (e.key === 'Enter' && !e.shiftKey && name.trim()) {
            e.preventDefault();
            void handleSubmit();
        }
    };

    const toggleLabel = (id: number) => {
        setSelectedLabelIds(prev =>
            prev.includes(id) ? prev.filter(x => x !== id) : [...prev, id],
        );
    };

    const clearDate = (e: React.MouseEvent) => {
        e.stopPropagation();
        setDueDate(null);
    };

    const selectedProject = projects.find(p => p.id === selectedProjectId);
    const selectedLabels = labels.filter(l => selectedLabelIds.includes(l.id));

    if (!isOpen) return null;

    return (
        <div
            className="quick-add-overlay"
            onClick={onClose}
            role="dialog"
            aria-modal="true"
            aria-label="작업 빠른 추가"
        >
            <div className="quick-add-card" onClick={e => e.stopPropagation()}>
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
                                setShowLabelPicker(false);
                                fileInputRef.current?.blur();
                            }}
                        >
                            <CalendarIcon />
                            <span>{dueDate ? formatDueLabel(dueDate) : '날짜'}</span>
                            {dueDate && (
                                <span className="pill-clear" onClick={clearDate} aria-label="날짜 지우기">×</span>
                            )}
                        </button>
                        {showDatePicker && (
                            <DatePickerDropdown value={dueDate} onChange={setDueDate} />
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

                    {labels.length > 0 && (
                        <div className="pill-wrapper">
                            <button
                                type="button"
                                className={`quick-pill ${selectedLabelIds.length > 0 ? 'active label-pill' : ''}`}
                                onClick={() => {
                                    setShowLabelPicker(v => !v);
                                    setShowDatePicker(false);
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
                                e.target.value === '' ? '' : Number(e.target.value),
                            )}
                        >
                            <option value="">받은 편지함</option>
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
                        <button type="button" className="quick-cancel" onClick={onClose}>
                            취소
                        </button>
                        <button
                            type="button"
                            className="quick-submit"
                            onClick={() => void handleSubmit()}
                            disabled={!name.trim() || submitting}
                        >
                            작업 추가
                        </button>
                    </div>
                </div>

                <p className="quick-add-hint">
                    <kbd>Enter</kbd> 추가 · <kbd>Esc</kbd> 닫기 · <kbd>Q</kbd> 빠른 추가
                </p>
            </div>
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

function InboxIcon() {
    return (
        <svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor" opacity="0.6">
            <path d="M19 3H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm0 16H5V8h14v11z" />
        </svg>
    );
}

export default QuickAddModal;
