import { useCallback, useMemo, useState } from 'react';
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
    const projects = useAppSelector(state => state.habits.projects);
    const [tab, setTab] = useState<Tab>('all');

    const unreadCount = useMemo(() => selectUnreadCount(items), [items]);

    const resolveProjectName = useCallback((projectId: number | null) => {
        if (projectId == null) return '프로젝트';
        return projects.find(p => p.id === projectId)?.name ?? `프로젝트 #${projectId}`;
    }, [projects]);

    const visible = useMemo(() => {
        const list = tab === 'unread' ? items.filter(n => !n.read) : items;
        return [...list].sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime());
    }, [items, tab]);

    const renderMessage = (n: AppNotification) => {
        if (n.customMessage) {
            return <span>{n.customMessage}</span>;
        }

        const projectName = resolveProjectName(n.projectId);
        const projectLink = n.projectId != null ? (
            <button type="button" className="notif-link" onClick={() => onOpenProject(n.projectId!)}>
                {projectName}
            </button>
        ) : null;

        if (n.action === 'invited') {
            return (
                <>
                    <strong>{n.actor}</strong>님이 {projectLink ?? projectName}에 초대했습니다
                </>
            );
        }
        if (n.action === 'completed') {
            return (
                <>
                    <strong>{n.actor}</strong>님이 {projectLink}에서 1작업을 완료했습니다
                </>
            );
        }
        return <span>{n.actor}</span>;
    };

    const renderTargetLabel = (n: AppNotification) => {
        if (n.notificationType === 'PROJECT') {
            return resolveProjectName(n.projectId);
        }
        return n.taskName;
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
                        <li key={n.dedupeKey} className={`notification-card ${n.read ? 'read' : 'unread'}`}>
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
                                {n.action === 'invited' && (
                                    <span className="notif-avatar-badge invited" aria-hidden>
                                        +
                                    </span>
                                )}
                            </span>
                            <div className="notif-body">
                                <p className="notif-message">{renderMessage(n)}</p>
                                {n.customMessage ? null : n.notificationType === 'PROJECT' && n.projectId != null ? (
                                    <button
                                        type="button"
                                        className="notif-task"
                                        onClick={() => onOpenProject(n.projectId!)}
                                    >
                                        {renderTargetLabel(n)}
                                    </button>
                                ) : n.taskId != null ? (
                                    <button
                                        type="button"
                                        className="notif-task"
                                        onClick={() => onOpenTask(n.taskId!)}
                                    >
                                        {renderTargetLabel(n)}
                                    </button>
                                ) : (
                                    <span className="notif-task">{renderTargetLabel(n)}</span>
                                )}
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
