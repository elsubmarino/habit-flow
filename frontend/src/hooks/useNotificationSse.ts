import { useEffect } from 'react';
import { subscribeNotificationStream } from '../api/notificationSse';
import { useAppDispatch } from '../store/hooks';
import { pushNotification } from '../store/notificationsSlice';

const RECONNECT_DELAY_MS = 3000;

export function useNotificationSse(enabled: boolean) {
    const dispatch = useAppDispatch();

    useEffect(() => {
        if (!enabled) return undefined;

        const controller = new AbortController();
        let reconnectTimer: number | undefined;
        let disposed = false;

        const connect = () => {
            void subscribeNotificationStream(
                {
                    onNotification: dto => {
                        dispatch(pushNotification(dto));
                    },
                },
                controller.signal,
            )
                .catch(() => undefined)
                .finally(() => {
                    if (disposed || controller.signal.aborted) return;
                    reconnectTimer = window.setTimeout(connect, RECONNECT_DELAY_MS);
                });
        };

        connect();

        return () => {
            disposed = true;
            controller.abort();
            if (reconnectTimer != null) {
                window.clearTimeout(reconnectTimer);
            }
        };
    }, [dispatch, enabled]);
}
