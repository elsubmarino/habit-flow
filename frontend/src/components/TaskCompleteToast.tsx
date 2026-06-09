interface TaskCompleteToastProps {
    count?: number;
    onUndo: () => void;
    onClose: () => void;
}

const TaskCompleteToast: React.FC<TaskCompleteToastProps> = ({
    count = 1,
    onUndo,
    onClose,
}) => (
    <div className="task-complete-toast" role="status" aria-live="polite">
        <span className="task-complete-toast-message">
            {count}작업을 완료했습니다
        </span>
        <button type="button" className="task-complete-toast-undo" onClick={onUndo}>
            실행 취소
        </button>
        <button
            type="button"
            className="task-complete-toast-close"
            aria-label="닫기"
            onClick={onClose}
        >
            ×
        </button>
    </div>
);

export default TaskCompleteToast;
