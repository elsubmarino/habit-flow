import { useState } from 'react';

interface AddProjectModalProps {
    onClose: () => void;
    onAdd: (name: string, color: string) => void;
}

const COLORS = ['#4073ff', '#299438', '#eb8909', '#ad46ff', '#db4c3f', '#808080'];

const AddProjectModal: React.FC<AddProjectModalProps> = ({ onClose, onAdd }) => {
    const [name, setName] = useState('');
    const [color, setColor] = useState(COLORS[0]);

    const handleAdd = () => {
        if (!name.trim()) return;
        onAdd(name.trim(), color);
        onClose();
    };

    return (
        <div className="modal-overlay" onClick={onClose}>
            <div className="add-project-modal" onClick={e => e.stopPropagation()}>
                <h2 className="add-project-title">프로젝트 이름</h2>
                <input
                    className="add-project-input"
                    placeholder="내 프로젝트"
                    value={name}
                    onChange={e => setName(e.target.value)}
                    onKeyDown={e => e.key === 'Enter' && handleAdd()}
                    autoFocus
                />
                <p className="add-project-label">색상</p>
                <div className="color-picker">
                    {COLORS.map(c => (
                        <button
                            key={c}
                            type="button"
                            className={`color-swatch ${color === c ? 'active' : ''}`}
                            style={{ background: c }}
                            onClick={() => setColor(c)}
                            aria-label={`색상 ${c}`}
                        />
                    ))}
                </div>
                <div className="add-project-actions">
                    <button type="button" className="cancel-btn" onClick={onClose}>
                        취소
                    </button>
                    <button type="button" className="submit-btn" onClick={handleAdd} disabled={!name.trim()}>
                        추가
                    </button>
                </div>
            </div>
        </div>
    );
};

export default AddProjectModal;
