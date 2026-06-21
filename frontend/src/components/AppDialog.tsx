import { useEffect } from 'react';

export type AppDialogVariant = 'default' | 'danger' | 'warning';

export interface AppDialogProps {
    title?: string;
    message: string;
    variant?: AppDialogVariant;
    confirmLabel?: string;
    cancelLabel?: string;
    showCancel?: boolean;
    onConfirm: () => void;
    onCancel: () => void;
}

const AppDialog: React.FC<AppDialogProps> = ({
    title,
    message,
    variant = 'default',
    confirmLabel = '확인',
    cancelLabel = '취소',
    showCancel = false,
    onConfirm,
    onCancel,
}) => {
    useEffect(() => {
        const onKeyDown = (event: KeyboardEvent) => {
            if (event.key === 'Escape') {
                onCancel();
            }
        };
        window.addEventListener('keydown', onKeyDown);
        return () => window.removeEventListener('keydown', onKeyDown);
    }, [onCancel]);

    const icon = variant === 'danger' ? '!' : variant === 'warning' ? 'i' : null;

    return (
        <div className="app-dialog-overlay" onClick={onCancel}>
            <div
                className={`app-dialog app-dialog-${variant}`}
                role="alertdialog"
                aria-modal="true"
                aria-labelledby="app-dialog-title"
                aria-describedby="app-dialog-message"
                onClick={event => event.stopPropagation()}
            >
                {icon && (
                    <div className="app-dialog-icon" aria-hidden="true">
                        {icon}
                    </div>
                )}
                <div className="app-dialog-body">
                    {title && (
                        <h2 id="app-dialog-title" className="app-dialog-title">
                            {title}
                        </h2>
                    )}
                    <p id="app-dialog-message" className="app-dialog-message">
                        {message}
                    </p>
                </div>
                <div className="app-dialog-actions">
                    {showCancel && (
                        <button
                            type="button"
                            className="app-dialog-btn app-dialog-btn-secondary"
                            onClick={onCancel}
                        >
                            {cancelLabel}
                        </button>
                    )}
                    <button
                        type="button"
                        className={`app-dialog-btn app-dialog-btn-primary${variant === 'danger' ? ' danger' : ''}`}
                        onClick={onConfirm}
                        autoFocus
                    >
                        {confirmLabel}
                    </button>
                </div>
            </div>
        </div>
    );
};

export default AppDialog;
