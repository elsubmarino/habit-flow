import { useEffect, useRef, useState } from 'react';
import { getLabelColorName, LABEL_COLORS } from '../utils/labelColors';
import { ChevronDownIcon, CloseIcon, HelpCircleIcon } from './icons';

const NAME_MAX = 60;

export interface LabelFormPayload {
    name: string;
    color: string;
    favorite: boolean;
}

interface AddLabelModalProps {
    onClose: () => void;
    onAdd: (payload: LabelFormPayload) => void;
}

const AddLabelModal: React.FC<AddLabelModalProps> = ({ onClose, onAdd }) => {
    const [name, setName] = useState('');
    const [color, setColor] = useState(LABEL_COLORS[0].hex);
    const [favorite, setFavorite] = useState(false);
    const [colorOpen, setColorOpen] = useState(false);
    const colorRef = useRef<HTMLDivElement>(null);

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

    const handleAdd = () => {
        if (!name.trim()) return;
        onAdd({ name: name.trim(), color, favorite });
        onClose();
    };

    return (
        <div className="modal-overlay" onClick={onClose}>
            <div className="project-edit-modal label-form-modal" onClick={e => e.stopPropagation()}>
                <header className="project-edit-header">
                    <div className="project-edit-title-wrap">
                        <h2 className="project-edit-title">라벨 추가</h2>
                        <button type="button" className="project-edit-help-btn" aria-label="도움말">
                            <HelpCircleIcon />
                        </button>
                    </div>
                    <button type="button" className="project-edit-close-btn" aria-label="닫기" onClick={onClose}>
                        <CloseIcon />
                    </button>
                </header>

                <div className="project-edit-body">
                    <label className="project-edit-field">
                        <span className="project-edit-label">이름</span>
                        <div className="project-edit-name-wrap">
                            <input
                                className="project-edit-input"
                                placeholder="라벨 이름"
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

                <footer className="project-edit-footer label-form-footer">
                    <div className="project-edit-footer-actions">
                        <button type="button" className="project-edit-cancel-btn" onClick={onClose}>
                            취소
                        </button>
                        <button
                            type="button"
                            className="project-edit-save-btn"
                            onClick={handleAdd}
                            disabled={!name.trim()}
                        >
                            추가
                        </button>
                    </div>
                </footer>
            </div>
        </div>
    );
};

export default AddLabelModal;
