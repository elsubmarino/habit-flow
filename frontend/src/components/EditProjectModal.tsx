import { useEffect, useRef, useState } from 'react';
import { fetchProjectById } from '../api/projectApi';
import { parseProjectAccessType, parseProjectLayoutType } from '../api/projectMappers';
import type { ProjectAccessType, ProjectLayoutType, EntityId } from '../api/types';
import type { Project } from '../store/habitSlice';
import {
    getColorName,
    normalizeProjectColor,
    PROJECT_COLORS,
} from '../utils/projectColors';
import {
    BoardLayoutIcon,
    ChevronDownIcon,
    CloseIcon,
    HelpCircleIcon,
    ListLayoutIcon,
    LockIcon,
} from './icons';
import { useDialog } from '../context/DialogContext';

const NAME_MAX = 120;

export interface ProjectEditSavePayload {
    id: EntityId;
    name: string;
    color: string;
    parentId: EntityId | null;
    accessType: ProjectAccessType;
    layoutType: ProjectLayoutType;
    favorite: boolean;
}

interface EditProjectModalProps {
    project: Project;
    allProjects: Project[];
    onClose: () => void;
    onSave: (payload: ProjectEditSavePayload) => void;
}

const ACCESS_OPTIONS: { value: ProjectAccessType; label: string }[] = [
    { value: 'PRIVATE', label: '비공개' },
    { value: 'PUBLIC', label: '공개' },
];

const EditProjectModal: React.FC<EditProjectModalProps> = ({
    project,
    allProjects,
    onClose,
    onSave,
}) => {
    const { showAlert } = useDialog();
    const [name, setName] = useState(project.name);
    const [color, setColor] = useState(normalizeProjectColor(project.color));
    const [parentId, setParentId] = useState<EntityId | null>(null);
    const [parentName, setParentName] = useState<string | null>(null);
    const [accessType, setAccessType] = useState<ProjectAccessType>('PRIVATE');
    const [layoutType, setLayoutType] = useState<ProjectLayoutType>('LIST');
    const [favorite, setFavorite] = useState(false);
    const [loading, setLoading] = useState(true);

    const [colorOpen, setColorOpen] = useState(false);
    const [accessOpen, setAccessOpen] = useState(false);
    const [parentOpen, setParentOpen] = useState(false);

    const colorRef = useRef<HTMLDivElement>(null);
    const accessRef = useRef<HTMLDivElement>(null);
    const parentRef = useRef<HTMLDivElement>(null);

    const parentOptions = allProjects.filter(p => p.id !== project.id);

    useEffect(() => {
        let cancelled = false;
        setLoading(true);
        void fetchProjectById(project.id)
            .then(detail => {
                if (cancelled) return;
                setName(detail.name);
                setColor(normalizeProjectColor(detail.color));
                setParentId(detail.parentId ?? null);
                setParentName(detail.parentName ?? null);
                setAccessType(parseProjectAccessType(detail.accessType));
                setLayoutType(parseProjectLayoutType(detail.layoutType));
                setFavorite(Boolean(detail.favorite));
            })
            .catch(() => undefined)
            .finally(() => {
                if (!cancelled) setLoading(false);
            });
        return () => {
            cancelled = true;
        };
    }, [project.id]);

    useEffect(() => {
        const onKey = (e: KeyboardEvent) => {
            if (e.key === 'Escape') onClose();
        };
        window.addEventListener('keydown', onKey);
        return () => window.removeEventListener('keydown', onKey);
    }, [onClose]);

    useEffect(() => {
        const closeOnOutside = (e: MouseEvent) => {
            const target = e.target as Node;
            if (colorRef.current && !colorRef.current.contains(target)) setColorOpen(false);
            if (accessRef.current && !accessRef.current.contains(target)) setAccessOpen(false);
            if (parentRef.current && !parentRef.current.contains(target)) setParentOpen(false);
        };
        document.addEventListener('mousedown', closeOnOutside);
        return () => document.removeEventListener('mousedown', closeOnOutside);
    }, []);

    const handleSave = () => {
        const trimmed = name.trim();
        if (!trimmed) return;
        onSave({
            id: project.id,
            name: trimmed,
            color,
            parentId,
            accessType,
            layoutType,
            favorite,
        });
        onClose();
    };

    const selectedAccess = ACCESS_OPTIONS.find(o => o.value === accessType) ?? ACCESS_OPTIONS[0];
    const selectedParent = parentOptions.find(p => p.id === parentId);
    const parentLabel = selectedParent?.name ?? parentName ?? '모체 없음';

    return (
        <div className="modal-overlay" onClick={onClose}>
            <div className="project-edit-modal" onClick={e => e.stopPropagation()} role="dialog" aria-labelledby="project-edit-title">
                <header className="project-edit-header">
                    <div className="project-edit-title-wrap">
                        <h2 id="project-edit-title" className="project-edit-title">편집</h2>
                        <button type="button" className="project-edit-help-btn" aria-label="도움말" title="프로젝트 설정을 변경합니다.">
                            <HelpCircleIcon />
                        </button>
                    </div>
                    <button type="button" className="project-edit-close-btn" aria-label="닫기" onClick={onClose}>
                        <CloseIcon />
                    </button>
                </header>

                {loading ? (
                    <p className="project-edit-loading">불러오는 중…</p>
                ) : (
                    <div className="project-edit-body">
                        <label className="project-edit-field">
                            <span className="project-edit-label">이름</span>
                            <div className="project-edit-name-wrap">
                                <input
                                    className="project-edit-input"
                                    value={name}
                                    maxLength={NAME_MAX}
                                    onChange={e => setName(e.target.value)}
                                    autoFocus
                                />
                                <span className="project-edit-counter">
                                    {name.length}/{NAME_MAX}
                                </span>
                            </div>
                        </label>

                        <div className="project-edit-field">
                            <span className="project-edit-label">색상</span>
                            <div className="project-edit-select-wrap" ref={colorRef}>
                                <button
                                    type="button"
                                    className="project-edit-select"
                                    aria-expanded={colorOpen}
                                    onClick={() => {
                                        setColorOpen(open => !open);
                                        setAccessOpen(false);
                                        setParentOpen(false);
                                    }}
                                >
                                    <span className="project-edit-select-leading">
                                        <span className="project-edit-color-dot" style={{ background: color }} />
                                        <span>{getColorName(color)}</span>
                                    </span>
                                    <ChevronDownIcon />
                                </button>
                                {colorOpen && (
                                    <ul className="project-edit-dropdown" role="listbox">
                                        {PROJECT_COLORS.map(option => (
                                            <li key={option.hex}>
                                                <button
                                                    type="button"
                                                    className={`project-edit-dropdown-item ${color === option.hex ? 'active' : ''}`}
                                                    onClick={() => {
                                                        setColor(option.hex);
                                                        setColorOpen(false);
                                                    }}
                                                >
                                                    <span className="project-edit-color-dot" style={{ background: option.hex }} />
                                                    <span>{option.name}</span>
                                                </button>
                                            </li>
                                        ))}
                                    </ul>
                                )}
                            </div>
                        </div>

                        <div className="project-edit-field">
                            <span className="project-edit-label">모체 프로젝트</span>
                            <div className="project-edit-select-wrap" ref={parentRef}>
                                <button
                                    type="button"
                                    className="project-edit-select"
                                    aria-expanded={parentOpen}
                                    onClick={() => {
                                        setParentOpen(open => !open);
                                        setColorOpen(false);
                                        setAccessOpen(false);
                                    }}
                                >
                                    <span>{parentLabel}</span>
                                    <ChevronDownIcon />
                                </button>
                                {parentOpen && (
                                    <ul className="project-edit-dropdown" role="listbox">
                                        <li>
                                            <button
                                                type="button"
                                                className={`project-edit-dropdown-item ${parentId == null ? 'active' : ''}`}
                                                onClick={() => {
                                                    setParentId(null);
                                                    setParentOpen(false);
                                                }}
                                            >
                                                모체 없음
                                            </button>
                                        </li>
                                        {parentOptions.map(option => (
                                            <li key={option.id}>
                                                <button
                                                    type="button"
                                                    className={`project-edit-dropdown-item ${parentId === option.id ? 'active' : ''}`}
                                                    onClick={() => {
                                                        setParentId(option.id);
                                                        setParentOpen(false);
                                                    }}
                                                >
                                                    {option.name}
                                                </button>
                                            </li>
                                        ))}
                                    </ul>
                                )}
                            </div>
                            <p className="project-edit-hint">
                                팀 간에 프로젝트를 이동시키겠습니까? 아래 버튼을 사용하세요.
                            </p>
                        </div>

                        <div className="project-edit-field">
                            <span className="project-edit-label">접근</span>
                            <div className="project-edit-select-wrap" ref={accessRef}>
                                <button
                                    type="button"
                                    className="project-edit-select"
                                    aria-expanded={accessOpen}
                                    onClick={() => {
                                        setAccessOpen(open => !open);
                                        setColorOpen(false);
                                        setParentOpen(false);
                                    }}
                                >
                                    <span className="project-edit-select-leading">
                                        {accessType === 'PRIVATE' && <LockIcon />}
                                        <span>{selectedAccess.label}</span>
                                    </span>
                                    <ChevronDownIcon />
                                </button>
                                {accessOpen && (
                                    <ul className="project-edit-dropdown" role="listbox">
                                        {ACCESS_OPTIONS.map(option => (
                                            <li key={option.value}>
                                                <button
                                                    type="button"
                                                    className={`project-edit-dropdown-item ${accessType === option.value ? 'active' : ''}`}
                                                    onClick={() => {
                                                        setAccessType(option.value);
                                                        setAccessOpen(false);
                                                    }}
                                                >
                                                    {option.value === 'PRIVATE' && <LockIcon />}
                                                    <span>{option.label}</span>
                                                </button>
                                            </li>
                                        ))}
                                    </ul>
                                )}
                            </div>
                        </div>

                        <label className="project-edit-favorite">
                            <span className="view-toggle">
                                <input
                                    type="checkbox"
                                    checked={favorite}
                                    onChange={e => setFavorite(e.target.checked)}
                                />
                                <span className="view-toggle-track" />
                            </span>
                            <span>즐겨찾기에 추가</span>
                        </label>

                        <div className="project-edit-field">
                            <span className="project-edit-label">레이아웃</span>
                            <div className="project-edit-layout-group" role="group" aria-label="레이아웃">
                                <button
                                    type="button"
                                    className={`project-edit-layout-btn ${layoutType === 'LIST' ? 'active' : ''}`}
                                    aria-pressed={layoutType === 'LIST'}
                                    onClick={() => setLayoutType('LIST')}
                                >
                                    <ListLayoutIcon />
                                    <span>목록</span>
                                </button>
                                <button
                                    type="button"
                                    className={`project-edit-layout-btn ${layoutType === 'BOARD' ? 'active' : ''}`}
                                    aria-pressed={layoutType === 'BOARD'}
                                    onClick={() => setLayoutType('BOARD')}
                                >
                                    <BoardLayoutIcon />
                                    <span>보드</span>
                                </button>
                            </div>
                        </div>
                    </div>
                )}

                <footer className="project-edit-footer">
                    <button
                        type="button"
                        className="project-edit-move-btn"
                        onClick={() => void showAlert('프로젝트 이동 기능은 아직 지원되지 않습니다.')}
                    >
                        프로젝트 이동
                    </button>
                    <div className="project-edit-footer-actions">
                        <button type="button" className="project-edit-cancel-btn" onClick={onClose}>
                            취소
                        </button>
                        <button
                            type="button"
                            className="project-edit-save-btn"
                            onClick={handleSave}
                            disabled={loading || !name.trim()}
                        >
                            저장
                        </button>
                    </div>
                </footer>
            </div>
        </div>
    );
};

export default EditProjectModal;
