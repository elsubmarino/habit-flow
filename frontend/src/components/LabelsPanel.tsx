import React, { useState } from 'react';
import type { Label } from '../store/habitSlice';

interface LabelsPanelProps {
    labels: Label[];
    selectedLabelId: number | null;
    onSelect: (id: number | null) => void;
    onAdd: (name: string) => void;
    onDelete: (id: number) => void;
}

const LabelsPanel: React.FC<LabelsPanelProps> = ({
    labels,
    selectedLabelId,
    onSelect,
    onAdd,
    onDelete,
}) => {
    const [newName, setNewName] = useState('');

    const handleAdd = () => {
        if (!newName.trim()) return;
        onAdd(newName.trim());
        setNewName('');
    };

    return (
        <div className="labels-panel">
            <h2 className="section-label">라벨</h2>
            <p className="panel-hint">Todoist처럼 @라벨로 작업을 분류합니다. 라벨을 클릭하면 해당 작업만 표시됩니다.</p>

            <div className="label-add-row">
                <input
                    className="modal-input"
                    placeholder="새 라벨 (예: 업무)"
                    value={newName}
                    onChange={e => setNewName(e.target.value)}
                    onKeyDown={e => e.key === 'Enter' && handleAdd()}
                />
                <button type="button" className="submit-btn" onClick={handleAdd}>라벨 추가</button>
            </div>

            <ul className="label-manage-list">
                {labels.map(label => (
                    <li key={label.id} className="label-manage-item">
                        <button
                            type="button"
                            className={`label-filter-btn ${selectedLabelId === label.id ? 'active' : ''}`}
                            onClick={() => onSelect(selectedLabelId === label.id ? null : label.id)}
                        >
                            <span className="label-chip-dot" style={{ background: label.color }} />
                            <span>{label.name}</span>
                            <span className="label-count">{label.taskCount}</span>
                        </button>
                        <button
                            type="button"
                            className="delete-btn"
                            onClick={() => {
                                if (window.confirm(`"${label.name}" 라벨을 삭제할까요?`)) {
                                    onDelete(label.id);
                                }
                            }}
                        >
                            삭제
                        </button>
                    </li>
                ))}
                {labels.length === 0 && (
                    <li className="panel-empty">라벨이 없습니다. 위에서 추가해 보세요.</li>
                )}
            </ul>
        </div>
    );
};

export default LabelsPanel;
