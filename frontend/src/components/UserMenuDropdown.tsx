import { useEffect } from 'react';
import type { UserProfile } from '../utils/userProfile';

interface UserMenuDropdownProps {
    profile: UserProfile;
    onClose: () => void;
    onLogout: () => void;
    onOpenActivity?: () => void;
}

interface MenuItem {
    id: string;
    label: string;
    icon: string;
    shortcut?: string;
    highlight?: boolean;
    action?: () => void;
}

const UserMenuDropdown: React.FC<UserMenuDropdownProps> = ({
    profile,
    onClose,
    onLogout,
    onOpenActivity,
}) => {
    useEffect(() => {
        const onKey = (e: KeyboardEvent) => {
            if (e.key === 'Escape') onClose();
        };
        window.addEventListener('keydown', onKey);
        return () => window.removeEventListener('keydown', onKey);
    }, [onClose]);

    const items: MenuItem[] = [
        { id: 'settings', label: '설정', icon: '⚙', shortcut: 'O 그리고 S' },
        { id: 'team', label: '+ 팀 추가', icon: '＋' },
        {
            id: 'activity',
            label: '활동 내역',
            icon: '〰',
            shortcut: 'G 그리고 A',
            action: onOpenActivity,
        },
        {
            id: 'print',
            label: '프린트',
            icon: '🖨',
            shortcut: 'Ctrl P',
            action: () => window.print(),
        },
        { id: 'updates', label: '새 업데이트', icon: '📣' },
        { id: 'labs', label: '최신 시험 기능', icon: '🧪' },
        {
            id: 'pro',
            label: '무료로 프로 플랜 시험해 보기',
            icon: '★',
            highlight: true,
        },
    ];

    const handleItem = (item: MenuItem) => {
        item.action?.();
        onClose();
    };

    return (
        <div className="user-menu" role="menu">
            <div className="user-menu-header">
                <div className="user-menu-identity">
                    <span className="user-menu-fullname">{profile.fullName}</span>
                    <span className="user-menu-email">{profile.email}</span>
                </div>
                <span className="user-menu-karma">
                    {profile.karma} · {profile.plan}
                </span>
            </div>

            <ul className="user-menu-list">
                {items.map(item => (
                    <li key={item.id}>
                        <button
                            type="button"
                            className={`user-menu-item ${item.highlight ? 'highlight' : ''}`}
                            role="menuitem"
                            onClick={() => handleItem(item)}
                        >
                            <span className="user-menu-item-icon" aria-hidden>
                                {item.icon}
                            </span>
                            <span className="user-menu-item-label">{item.label}</span>
                            {item.shortcut && (
                                <span className="user-menu-shortcut">{item.shortcut}</span>
                            )}
                        </button>
                    </li>
                ))}
            </ul>

            <div className="user-menu-divider" />

            <button type="button" className="user-menu-item" onClick={onClose}>
                <span className="user-menu-item-icon" aria-hidden>
                    ↻
                </span>
                <span className="user-menu-item-label">동기화</span>
                <span className="user-menu-shortcut muted">5분 전에</span>
            </button>

            <button
                type="button"
                className="user-menu-item"
                role="menuitem"
                onClick={() => {
                    onClose();
                    onLogout();
                }}
            >
                <span className="user-menu-item-icon" aria-hidden>
                    ⎋
                </span>
                <span className="user-menu-item-label">로그아웃</span>
            </button>

            <footer className="user-menu-footer">
                <span>v1.0 (베타)</span>
                <button type="button" className="user-menu-footer-link" onClick={onClose}>
                    체인지로그
                </button>
            </footer>
        </div>
    );
};

export default UserMenuDropdown;
