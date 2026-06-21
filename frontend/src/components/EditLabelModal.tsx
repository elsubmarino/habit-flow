import { useEffect, useRef, useState } from 'react';
import { fetchLabelById } from '../api/labelApi';
import { displayLabelName } from '../api/labelMappers';
import type { EntityId } from '../api/types';
import type { Label } from '../store/habitSlice';
import {
    getLabelColorName,
    LABEL_COLORS,
    normalizeLabelColor,
} from '../utils/labelColors';
import { ChevronDownIcon, CloseIcon, HelpCircleIcon } from './icons';
import type { LabelFormPayload } from './AddLabelModal';

const NAME_MAX = 60;

interface EditLabelModalProps {
    label: Label;
    onClose: () => void;
    onSave: (id: EntityId, payload: LabelFormPayload) => void;
}

const EditLabelModal: React.FC<EditLabelModalProps> = ({ label, onClose, onSave }) => {
    const [name, setName] = useState(displayLabelName(label.name));
    const [color, setColor] = useState(normalizeLabelColor(label.color));
    const [favorite, setFavorite] = useState(false);
    const [loading, setLoading] = useState(true);
    const [colorOpen, setColorOpen] = useState(false);
    const colorRef = useRef<HTMLDivElement>(null);

    useEffect(() => {
        let cancelled = false;
        setLoading(true);
        void fetchLabelById(label.id)
            .then(detail => {
                if (cancelled) return;
                setName(displayLabelName(detail.name));
                setColor(normalizeLabelColor(detail.color));
                setFavorite(Boolean(detail.favorite));
            })
            .catch(() => undefined)
            .finally(() => {
                if (!cancelled) setLoading(false);
            });
        return () => {
            cancelled = true;
        };
    }, [label.id]);

    useEffect(() => {
        const onKey = (e: KeyboardEvent) => {
            if (e.key === 'Escape') onClose();
        };
        window.addEventListener('keydown', onKey);
        return () => window.removeEventListener('keydown', onKey);
    }, [onClose]);

    useEffect(() => {
        const closeOnOutside = (e: MouseEvent) => {
            if (colorRef.current && !colorRef.current.contains(e.target as Node)) {
                setColorOpen(false);
            }
        };
        document.addEventListener('mousedown', closeOnOutside);
        return () => document.removeEventListener('mousedown', closeOnOutside);
    }, []);

    const handleSave = () => {
        if (!name.trim()) return;
        onSave(label.id, { name: name.trim(), color, favorite });
        onClose();
    };

    return (
        <div className="modal-overlay" onClick={onClose}>
            <div className="project-edit-modal label-form-modal" onClick={e => e.stopPropagation()}>
                <header className="project-edit-header">
                    <div className="project-edit-title-wrap">
                        <h2 className="project-edit-title">라벨 편집</h2>
                        <button type="button" className="project-edit-help-btn" aria-label="도움말">
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
                                    onClick={() => setColorOpen(open => !open)}
                                >
                                    <span className="project-edit-select-leading">
                                        <span className="project-edit-color-dot" style={{ background: color }} />
                                        <span>{getLabelColorName(color)}</span>
                                    </span>
                                    <ChevronDownIcon />
                                </button>
                                {colorOpen && (
                                    <ul className="project-edit-dropdown" role="listbox">
                                        {LABEL_COLORS.map(option => (
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
                    </div>
                )}

                <footer className="project-edit-footer label-form-footer">
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

export default EditLabelModal;
