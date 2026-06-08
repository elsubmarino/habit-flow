interface InlineAddTaskButtonProps {
    onClick: () => void;
}

const InlineAddTaskButton: React.FC<InlineAddTaskButtonProps> = ({ onClick }) => (
    <button type="button" className="inline-add-task-btn" onClick={onClick}>
        + 작업 추가
    </button>
);

export default InlineAddTaskButton;
