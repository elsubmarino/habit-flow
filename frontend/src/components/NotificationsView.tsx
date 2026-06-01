import { useMemo, useState } from 'react';
import { useAppDispatch, useAppSelector } from '../store/hooks';
import {
    formatRelativeTime,
    markAllNotificationsRead,
    markNotificationRead,
    selectUnreadCount,
    type AppNotification,
} from '../store/notificationsSlice';

type Tab = 'all' | 'unread';

interface NotificationsViewProps {
    onOpenTask: (habitId: number) => void;
    onOpenProject: (projectId: number) => void;
}

const NotificationsView: React.FC<NotificationsViewProps> = ({ onOpenTask, onOpenProject }) => {
    const dispatch = useAppDispatch();
    const items = useAppSelector(state => state.notifications.items);
    const [tab, setTab] = useState<Tab>('all');

    const unreadCount = useMemo(() => selectUnreadCount(items), [items]);

    const visible = useMemo(() => {
        const list = tab === 'unread' ? items.filter(n => !n.read) : items;
        return [...list].sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime());
    }, [items, tab]);

    const renderMessage = (n: AppNotification) => {
        const project =
            n.projectName && n.projectId != null ? (
                <button type="button" className="notif-link" onClick={() => onOpenProject(n.projectId!)}>
                    {n.projectName}
                </button>
            ) : null;
        if (n.action === 'completed') {
            return (
                <>
                    <strong>{n.actor}</strong>님이 {project}에서 1작업을 완료했습니다
                </>
            );
        }
        return <span>{n.actor}</span>;
    };

    return (
        <div className="notifications-page">
            <header className="notifications-header">
                <h1>알림</h1>
                <button
                    type="button"
                    className="mark-all-read"
                    onClick={() => void dispatch(markAllNotificationsRead())}
                >
                    ✓✓ 모두 읽음으로 표시
                </button>
            </header>

            <div className="notifications-tabs">
                <button
                    type="button"
                    className={tab === 'all' ? 'active' : ''}
                    onClick={() => setTab('all')}
                >
                    전체
                </button>
                <button
                    type="button"
                    className={tab === 'unread' ? 'active' : ''}
                    onClick={() => setTab('unread')}
                >
                    읽지 않음 {unreadCount > 0 ? unreadCount : ''}
                </button>
            </div>

            <ul className="notifications-list">
                {visible.length === 0 ? (
                    <li className="notifications-empty">알림이 없습니다.</li>
                ) : (
                    visible.map(n => (
                        <li key={n.id} className={`notification-card ${n.read ? 'read' : 'unread'}`}>
                            {!n.read && <span className="notif-unread-bar" aria-hidden />}
                            <span className="notif-avatar-wrap">
                                <span className="notif-avatar" aria-hidden>
                                    🐱
                                </span>
                                {n.action === 'completed' && (
                                    <span className="notif-avatar-badge" aria-hidden>
                                        ✓
                                    </span>
                                )}
                            </span>
                            <div className="notif-body">
                                <p className="notif-message">{renderMessage(n)}</p>
                                <button
                                    type="button"
                                    className="notif-task"
                                    onClick={() => onOpenTask(n.taskId)}
                                >
                                    {n.taskName}
                                </button>
                                <span className="notif-time">{formatRelativeTime(n.createdAt)}</span>
                            </div>
                            <button
                                type="button"
                                className="notif-action"
                                aria-label="읽음 처리"
                                onClick={() => void dispatch(markNotificationRead(n.id))}
                            >
                                ○
                            </button>
                        </li>
                    ))
                )}
            </ul>
        </div>
    );
};

export default NotificationsView;
