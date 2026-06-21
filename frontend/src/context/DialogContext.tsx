import {
    createContext,
    useCallback,
    useContext,
    useMemo,
    useRef,
    useState,
    type ReactNode,
} from 'react';
import AppDialog, { type AppDialogVariant } from '../components/AppDialog';

interface AlertOptions {
    title?: string;
    variant?: AppDialogVariant;
    confirmLabel?: string;
}

interface ConfirmOptions {
    title?: string;
    message: string;
    confirmLabel?: string;
    cancelLabel?: string;
    variant?: AppDialogVariant;
}

interface DialogState {
    title?: string;
    message: string;
    variant: AppDialogVariant;
    confirmLabel: string;
    cancelLabel: string;
    showCancel: boolean;
}

interface DialogContextValue {
    showAlert: (message: string, options?: AlertOptions) => Promise<void>;
    showErrorAlert: (message: string, options?: Omit<AlertOptions, 'variant'>) => Promise<void>;
    confirm: (options: ConfirmOptions) => Promise<boolean>;
}

const DialogContext = createContext<DialogContextValue | null>(null);

export function DialogProvider({ children }: { children: ReactNode }) {
    const [dialog, setDialog] = useState<DialogState | null>(null);
    const pendingRef = useRef<((confirmed: boolean) => void) | null>(null);

    const closeDialog = useCallback((confirmed: boolean) => {
        pendingRef.current?.(confirmed);
        pendingRef.current = null;
        setDialog(null);
    }, []);

    const showAlert = useCallback((message: string, options?: AlertOptions) => {
        const trimmed = message.trim();
        if (!trimmed) return Promise.resolve();

        return new Promise<void>(resolve => {
            pendingRef.current = () => resolve();
            setDialog({
                title: options?.title ?? '알림',
                message: trimmed,
                variant: options?.variant ?? 'default',
                confirmLabel: options?.confirmLabel ?? '확인',
                cancelLabel: '취소',
                showCancel: false,
            });
        });
    }, []);

    const showErrorAlert = useCallback((message: string, options?: Omit<AlertOptions, 'variant'>) => {
        return showAlert(message, { ...options, variant: 'warning' });
    }, [showAlert]);

    const confirm = useCallback((options: ConfirmOptions) => {
        const trimmed = options.message.trim();
        if (!trimmed) return Promise.resolve(false);

        return new Promise<boolean>(resolve => {
            pendingRef.current = resolve;
            setDialog({
                title: options.title ?? '확인',
                message: trimmed,
                variant: options.variant ?? 'default',
                confirmLabel: options.confirmLabel ?? '확인',
                cancelLabel: options.cancelLabel ?? '취소',
                showCancel: true,
            });
        });
    }, []);

    const value = useMemo(
        () => ({ showAlert, showErrorAlert, confirm }),
        [showAlert, showErrorAlert, confirm],
    );

    return (
        <DialogContext.Provider value={value}>
            {children}
            {dialog && (
                <AppDialog
                    title={dialog.title}
                    message={dialog.message}
                    variant={dialog.variant}
                    confirmLabel={dialog.confirmLabel}
                    cancelLabel={dialog.cancelLabel}
                    showCancel={dialog.showCancel}
                    onConfirm={() => closeDialog(true)}
                    onCancel={() => closeDialog(false)}
                />
            )}
        </DialogContext.Provider>
    );
}

export function useDialog() {
    const ctx = useContext(DialogContext);
    if (!ctx) {
        throw new Error('useDialog must be used within DialogProvider');
    }
    return ctx;
}
