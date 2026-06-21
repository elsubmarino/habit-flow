import { useCallback, useEffect, useRef, useState } from 'react';
import type { EntityId } from '../api/types';

const TOAST_DURATION_MS = 10_000;

export function useTaskCompleteToast() {
    const [completedTaskId, setCompletedTaskId] = useState<EntityId | null>(null);
    const timerRef = useRef<ReturnType<typeof setTimeout> | null>(null);

    const dismissToast = useCallback(() => {
        if (timerRef.current) {
            clearTimeout(timerRef.current);
            timerRef.current = null;
        }
        setCompletedTaskId(null);
    }, []);

    const showCompleteToast = useCallback((taskId: EntityId) => {
        if (timerRef.current) {
            clearTimeout(timerRef.current);
        }
        setCompletedTaskId(taskId);
        timerRef.current = setTimeout(() => {
            setCompletedTaskId(null);
            timerRef.current = null;
        }, TOAST_DURATION_MS);
    }, []);

    useEffect(() => () => {
        if (timerRef.current) clearTimeout(timerRef.current);
    }, []);

    return {
        completedTaskId,
        showCompleteToast,
        dismissToast,
        isToastVisible: completedTaskId != null,
    };
}
