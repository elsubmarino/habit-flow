import React, { useEffect, useRef, useState } from 'react';
import {
    BellIcon,
    CheckIcon,
    ChevronDownIcon,
    FilterIcon,
    InboxIcon,
    PanelToggleIcon,
    PlusIcon,
    TodayIcon,
    ReportIcon,
    UpcomingIcon,
} from './icons';
import type { Label, Project } from '../store/habitSlice';
import { getUserProfile } from '../utils/userProfile';
import UserMenuDropdown from './UserMenuDropdown';

export type NavItem = 'inbox' | 'today' | 'upcoming' | 'filters' | 'report';

interface SidebarProps {
    activeNav: NavItem;
    projects: Project[];
    labels: Label[];
    selectedProjectId: number | null;
    selectedLabelId: number | null;
    onNavChange: (nav: NavItem) => void;
    onProjectSelect: (projectId: number | null) => void;
    onLabelSelect: (labelId: number | null) => void;
    onManageProjects: () => void;
    onAddClick: () => void;
    onSearchClick: () => void;
    notificationsActive: boolean;
    unreadNotificationCount: number;
    onNotificationsClick: () => void;
    onToggleSidebar: () => void;
    onLogout: () => void;
    onOpenActivity?: () => void;
}

const Sidebar: React.FC<SidebarProps> = ({
    activeNav,
    projects,
    labels,
    selectedProjectId,
    selectedLabelId,
    onNavChange,
    onProjectSelect,
    onLabelSelect,
    onManageProjects,
    onAddClick,
    onSearchClick,
    notificationsActive,
    unreadNotificationCount,
    onNotificationsClick,
    onToggleSidebar,
    onLogout,
    onOpenActivity,
}) => {
    const [userMenuOpen, setUserMenuOpen] = useState(false);
    const userMenuRef = useRef<HTMLDivElement>(null);
    const profile = getUserProfile();

    useEffect(() => {
        if (!userMenuOpen) return;
        const onDocClick = (e: MouseEvent) => {
            if (userMenuRef.current && !userMenuRef.current.contains(e.target as Node)) {
                setUserMenuOpen(false);
            }
        };
        document.addEventListener('mousedown', onDocClick);
        return () => document.removeEventListener('mousedown', onDocClick);
    }, [userMenuOpen]);

    const navItems: { id: NavItem; label: string; icon: React.ReactNode }[] = [
        { id: 'inbox', label: '받은 편지함', icon: <InboxIcon /> },
        { id: 'today', label: '오늘', icon: <TodayIcon /> },
        { id: 'upcoming', label: '다음 7일', icon: <UpcomingIcon /> },
        { id: 'filters', label: '필터 및 라벨', icon: <FilterIcon /> },
        { id: 'report', label: '보고', icon: <ReportIcon /> },
    ];

    return (
        <aside className="sidebar">
            <div className="sidebar-inner">
                <div className="sidebar-header">
                    <div className="sidebar-top-row">
                        <div className="sidebar-user-wrap" ref={userMenuRef}>
                            <button
                                type="button"
                                className={`sidebar-user-btn ${userMenuOpen ? 'open' : ''}`}
                                aria-label="계정 메뉴"
                                aria-expanded={userMenuOpen}
                                aria-haspopup="menu"
                                onClick={() => setUserMenuOpen(open => !open)}
                            >
                                <span className="logo-icon"><CheckIcon /></span>
                                <span className="sidebar-user-name">{profile.displayName}</span>
                                <span className="sidebar-user-chevron">
                                    <ChevronDownIcon />
                                </span>
                            </button>
                            {userMenuOpen && (
                                <UserMenuDropdown
                                    profile={profile}
                                    onClose={() => setUserMenuOpen(false)}
                                    onLogout={onLogout}
                                    onOpenActivity={() => {
                                        setUserMenuOpen(false);
                                        onOpenActivity?.();
                                    }}
                                />
                            )}
                        </div>
                        <button
                            type="button"
                            className={`sidebar-bell-btn ${notificationsActive ? 'active' : ''}`}
                            aria-label="알림"
                            onClick={onNotificationsClick}
                        >
                            <BellIcon />
                            {unreadNotificationCount > 0 && (
                                <span className="sidebar-bell-badge">{unreadNotificationCount}</span>
                            )}
                        </button>
                        <button
                            type="button"
                            className="sidebar-panel-btn"
                            aria-label="사이드바 접기"
                            onClick={onToggleSidebar}
                        >
                            <PanelToggleIcon />
                        </button>
                    </div>
                    <button type="button" className="sidebar-add-btn" onClick={onAddClick} aria-label="작업 추가 (Q)">
                        <PlusIcon />
                        <span>작업 추가</span>
                        <kbd className="sidebar-kbd">Q</kbd>
                    </button>
                    <button type="button" className="sidebar-search-btn" onClick={onSearchClick} aria-label="검색">
                        <span>검색</span>
                        <kbd className="sidebar-kbd">Ctrl K</kbd>
                    </button>
                </div>

                <nav className="sidebar-nav">
                    <ul>
                        {navItems.map(item => (
                            <li key={item.id}>
                                <button
                                    type="button"
                                    className={`nav-item ${activeNav === item.id && !selectedProjectId && !selectedLabelId ? 'active' : ''}`}
                                    onClick={() => onNavChange(item.id)}
                                >
                                    <span className="nav-icon">{item.icon}</span>
                                    <span className="nav-label">{item.label}</span>
                                </button>
                            </li>
                        ))}
                    </ul>
                </nav>

                <div className="sidebar-section">
                    <div className="sidebar-section-header">
                        <h3 className="sidebar-section-title">내 프로젝트</h3>
                        <button type="button" className="section-action-btn" onClick={onManageProjects}>
                            관리
                        </button>
                    </div>
                    <ul className="project-list">
                        {projects.map(project => (
                            <li key={project.id}>
                                <button
                                    type="button"
                                    className={`project-item ${selectedProjectId === project.id ? 'active' : ''}`}
                                    onClick={() => onProjectSelect(
                                        selectedProjectId === project.id ? null : project.id,
                                    )}
                                >
                                    <span className="project-dot" style={{ background: project.color }} />
                                    <span>{project.name}</span>
                                    {project.taskCount > 0 && (
                                        <span className="project-count">{project.taskCount}</span>
                                    )}
                                </button>
                            </li>
                        ))}
                    </ul>
                </div>

                {labels.length > 0 && (
                    <div className="sidebar-section">
                        <h3 className="sidebar-section-title">라벨</h3>
                        <ul className="project-list">
                            {labels.map(label => (
                                <li key={label.id}>
                                    <button
                                        type="button"
                                        className={`project-item ${selectedLabelId === label.id ? 'active' : ''}`}
                                        onClick={() => onLabelSelect(
                                            selectedLabelId === label.id ? null : label.id,
                                        )}
                                    >
                                        <span className="project-dot" style={{ background: label.color }} />
                                        <span>{label.name}</span>
                                        {label.taskCount > 0 && (
                                            <span className="project-count">{label.taskCount}</span>
                                        )}
                                    </button>
                                </li>
                            ))}
                        </ul>
                    </div>
                )}
            </div>
        </aside>
    );
};

export default Sidebar;
