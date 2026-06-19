interface AppToastProps {
    message: string;
    kind?: 'error' | 'info';
    onClose: () => void;
}

const AppToast: React.FC<AppToastProps> = ({ message, kind = 'info', onClose }) => (
    <div
        className={`app-toast app-toast-${kind}`}
        role={kind === 'error' ? 'alert' : 'status'}
        aria-live="assertive"
    >
        <span className="app-toast-message">{message}</span>
        <button type="button" className="app-toast-close" aria-label="닫기" onClick={onClose}>
            ×
        </button>
    </div>
);

export default AppToast;
