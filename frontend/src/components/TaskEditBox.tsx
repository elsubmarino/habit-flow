interface TaskEditBoxProps {
    name: string;
    description: string;
    onNameChange: (value: string) => void;
    onDescriptionChange: (value: string) => void;
    onCancel: () => void;
    onSave: () => void;
    saving?: boolean;
}

const TaskEditBox: React.FC<TaskEditBoxProps> = ({
    name,
    description,
    onNameChange,
    onDescriptionChange,
    onCancel,
    onSave,
    saving = false,
}) => (
    <div className="task-edit-box" onClick={e => e.stopPropagation()}>
        <input
            className="task-edit-title"
            value={name}
            onChange={e => onNameChange(e.target.value)}
            placeholder="작업 이름"
            autoFocus
            onKeyDown={e => {
                if (e.key === 'Enter' && !e.shiftKey) {
                    e.preventDefault();
                    onSave();
                }
                if (e.key === 'Escape') onCancel();
            }}
        />
        <textarea
            className="task-edit-desc"
            value={description}
            onChange={e => onDescriptionChange(e.target.value)}
            placeholder="설명"
            rows={4}
            onKeyDown={e => {
                if (e.key === 'Escape') onCancel();
            }}
        />
        <div className="task-edit-actions">
            <button type="button" className="task-edit-cancel-btn" onClick={onCancel} disabled={saving}>
                취소
            </button>
            <button
                type="button"
                className="task-edit-save-btn"
                onClick={onSave}
                disabled={saving || !name.trim()}
            >
                저장
            </button>
        </div>
    </div>
);

export default TaskEditBox;
