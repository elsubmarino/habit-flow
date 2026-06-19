import {
    createContext,
    useCallback,
    useContext,
    useEffect,
    useMemo,
    useRef,
    useState,
    type ReactNode,
} from 'react';
import AppToast from '../components/AppToast';

type ToastKind = 'error' | 'info';

interface ToastState {
    message: string;
    kind: ToastKind;
}

interface ToastContextValue {
    showErrorToast: (message: string) => void;
    showToast: (message: string, kind?: ToastKind) => void;
    dismissToast: () => void;
}

const ToastContext = createContext<ToastContextValue | null>(null);

const TOAST_DURATION_MS = 7000;

export function ToastProvider({ children }: { children: ReactNode }) {
    const [toast, setToast] = useState<ToastState | null>(null);
    const timerRef = useRef<ReturnType<typeof setTimeout> | null>(null);

    const dismissToast = useCallback(() => {
        if (timerRef.current) {
            clearTimeout(timerRef.current);
            timerRef.current = null;
        }
        setToast(null);
    }, []);

    const showToast = useCallback((message: string, kind: ToastKind = 'info') => {
        const trimmed = message.trim();
        if (!trimmed) return;

        if (timerRef.current) {
            clearTimeout(timerRef.current);
        }

        setToast({ message: trimmed, kind });
        timerRef.current = setTimeout(() => {
            setToast(null);
            timerRef.current = null;
        }, TOAST_DURATION_MS);
    }, []);

    const showErrorToast = useCallback((message: string) => {
        showToast(message, 'error');
    }, [showToast]);

    useEffect(() => () => {
        if (timerRef.current) clearTimeout(timerRef.current);
    }, []);

    const value = useMemo(
        () => ({ showErrorToast, showToast, dismissToast }),
        [showErrorToast, showToast, dismissToast],
    );

    return (
        <ToastContext.Provider value={value}>
            {children}
            {toast && (
                <AppToast
                    message={toast.message}
                    kind={toast.kind}
                    onClose={dismissToast}
                />
            )}
        </ToastContext.Provider>
    );
}

export function useToast() {
    const ctx = useContext(ToastContext);
    if (!ctx) {
        throw new Error('useToast must be used within ToastProvider');
    }
    return ctx;
}
