import React, { useEffect, useMemo, useRef, useState } from 'react';
import {
    BellIcon,
    CheckIcon,
    ChevronDownIcon,
    ChevronUpIcon,
    FilterIcon,
    InboxIcon,
    PanelToggleIcon,
    PlusIcon,
    TodayIcon,
    ReportIcon,
    SearchIcon,
    UpcomingIcon,
} from './icons';
import {
    buildSidebarFavorites,
    normalizeFavoriteTargetType,
    readFavoriteTargetId,
    resolveFavoriteLabel,
    resolveFavoriteProject,
} from '../api/favoriteMappers';
import type { FavoriteDto, EntityId } from '../api/types';
import type { Label, Project } from '../store/habitSlice';
import type { ReorderProjectRequest } from '../utils/projectSortOrder';
import { useUserProfile } from '../hooks/useUserProfile';
import UserMenuDropdown from './UserMenuDropdown';
import LabelListRow from './LabelListRow';
import ProjectListRow from './ProjectListRow';
import SortableProjectList from './SortableProjectList';

export type NavItem = 'inbox' | 'today' | 'upcoming' | 'filters' | 'report';

interface SidebarProps {
    activeNav: NavItem;
    inboxTaskCount: number;
    todayTaskCount: number;
    projects: Project[];
    labels: Label[];
    favorites: FavoriteDto[];
    selectedProjectId: EntityId | null;
    selectedLabelId: EntityId | null;
    projectsBrowseActive: boolean;
    projectsListExpanded: boolean;
    favoritesListExpanded: boolean;
    onNavChange: (nav: NavItem) => void;
    onProjectSelect: (projectId: EntityId) => void;
    onLabelSelect: (labelId: EntityId) => void;
    onEditLabel: (label: Label) => void;
    onDeleteLabel: (labelId: EntityId) => void;
    onProjectsBrowse: () => void;
    onAddProject: () => void;
    onEditProject: (project: Project) => void;
    onShareProject: (project: Project) => void;
    onDeleteProject: (projectId: EntityId) => void;
    onReorderProject?: (request: ReorderProjectRequest) => void;
    onToggleProjectsList: () => void;
    onToggleFavoritesList: () => void;
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
    inboxTaskCount,
    todayTaskCount,
    projects,
    labels,
    favorites,
    selectedProjectId,
    selectedLabelId,
    projectsBrowseActive,
    projectsListExpanded,
    favoritesListExpanded,
    onNavChange,
    onProjectSelect,
    onLabelSelect,
    onEditLabel,
    onDeleteLabel,
    onProjectsBrowse,
    onAddProject,
    onEditProject,
    onShareProject,
    onDeleteProject,
    onReorderProject,
    onToggleProjectsList,
    onToggleFavoritesList,
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
    const [projectsHeaderHover, setProjectsHeaderHover] = useState(false);
    const [favoritesHeaderHover, setFavoritesHeaderHover] = useState(false);
    const userMenuRef = useRef<HTMLDivElement>(null);
    const profile = useUserProfile();
    const projectById = new Map(projects.map(project => [project.id, project]));
    const labelById = new Map(labels.map(label => [label.id, label]));
    const sidebarFavorites = useMemo(
        () => buildSidebarFavorites(favorites, projects, labels),
        [favorites, projects, labels],
    );

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
        { id: 'inbox', label: '관리함', icon: <InboxIcon /> },
        { id: 'today', label: '오늘', icon: <TodayIcon /> },
        { id: 'upcoming', label: '다음', icon: <UpcomingIcon /> },
        { id: 'filters', label: '라벨', icon: <FilterIcon /> },
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
                </div>

                <nav className="sidebar-nav">
                    <ul>
                        <li>
                            <button
                                type="button"
                                className="nav-item"
                                onClick={onSearchClick}
                                aria-label="검색 (Ctrl K)"
                            >
                                <span className="nav-icon"><SearchIcon /></span>
                                <span className="nav-label">검색</span>
                                <kbd className="sidebar-kbd">Ctrl K</kbd>
                            </button>
                        </li>
                        {navItems.map(item => {
                            const count = item.id === 'inbox'
                                ? inboxTaskCount
                                : item.id === 'today'
                                    ? todayTaskCount
                                    : 0;
                            const showCount = item.id !== 'upcoming' && count > 0;

                            return (
                                <li key={item.id}>
                                    <button
                                        type="button"
                                        className={`nav-item ${activeNav === item.id && !selectedProjectId && !selectedLabelId && !projectsBrowseActive ? 'active' : ''}`}
                                        onClick={() => onNavChange(item.id)}
                                    >
                                        <span className="nav-icon">{item.icon}</span>
                                        <span className="nav-label">{item.label}</span>
                                        {showCount && (
                                            <span className="nav-count">{count}</span>
                                        )}
                                    </button>
                                </li>
                            );
                        })}
                    </ul>
                </nav>

                {sidebarFavorites.length > 0 && (
                    <div
                        className="sidebar-section sidebar-favorites-section"
                        onMouseEnter={() => setFavoritesHeaderHover(true)}
                        onMouseLeave={() => setFavoritesHeaderHover(false)}
                    >
                        <div className="sidebar-projects-header">
                            <span className="sidebar-favorites-title">즐겨찾기</span>
                            {favoritesHeaderHover && (
                                <div className="sidebar-projects-actions">
                                    <button
                                        type="button"
                                        className="sidebar-projects-action-btn"
                                        aria-label={favoritesListExpanded ? '목록 숨기기' : '목록 펼치기'}
                                        onClick={onToggleFavoritesList}
                                    >
                                        {favoritesListExpanded ? <ChevronUpIcon /> : <ChevronDownIcon />}
                                    </button>
                                </div>
                            )}
                        </div>

                        {favoritesListExpanded && (
                            <ul className="project-list">
                                {sidebarFavorites.map(favorite => {
                                    const targetId = readFavoriteTargetId(favorite);
                                    const targetType = normalizeFavoriteTargetType(favorite.targetType);
                                    if (targetType === 'PROJECT') {
                                        const project = resolveFavoriteProject(
                                            favorite,
                                            projectById.get(targetId),
                                        );
                                        return (
                                            <ProjectListRow
                                                key={`fav-${favorite.id}`}
                                                project={project}
                                                variant="sidebar"
                                                active={selectedProjectId === project.id}
                                                onSelect={onProjectSelect}
                                                onEdit={() => {
                                                    const source = projectById.get(targetId);
                                                    if (source) onEditProject(source);
                                                }}
                                                onShare={
                                                    projectById.has(targetId)
                                                        ? () => onShareProject(projectById.get(targetId)!)
                                                        : undefined
                                                }
                                                onDelete={onDeleteProject}
                                            />
                                        );
                                    }

                                    const label = resolveFavoriteLabel(
                                        favorite,
                                        labelById.get(targetId),
                                    );
                                    return (
                                        <LabelListRow
                                            key={`fav-${favorite.id}`}
                                            label={label}
                                            active={selectedLabelId === label.id}
                                            onSelect={onLabelSelect}
                                            onEdit={() => {
                                                const source = labelById.get(targetId);
                                                if (source) onEditLabel(source);
                                            }}
                                            onDelete={onDeleteLabel}
                                        />
                                    );
                                })}
                            </ul>
                        )}
                    </div>
                )}

                <div
                    className="sidebar-section sidebar-projects-section"
                    onMouseEnter={() => setProjectsHeaderHover(true)}
                    onMouseLeave={() => setProjectsHeaderHover(false)}
                >
                    <div className="sidebar-projects-header">
                        <button
                            type="button"
                            className={`sidebar-projects-title-btn ${projectsBrowseActive && !selectedProjectId ? 'active' : ''}`}
                            onClick={onProjectsBrowse}
                        >
                            <span>프로젝트</span>
                            <span className="sidebar-projects-usage">
                                {projects.length}/5
                            </span>
                        </button>
                        {projectsHeaderHover && (
                            <div className="sidebar-projects-actions">
                                <button
                                    type="button"
                                    className="sidebar-projects-action-btn"
                                    aria-label="프로젝트 추가"
                                    onClick={e => {
                                        e.stopPropagation();
                                        onAddProject();
                                    }}
                                >
                                    <PlusIcon />
                                </button>
                                <button
                                    type="button"
                                    className="sidebar-projects-action-btn"
                                    aria-label={projectsListExpanded ? '목록 숨기기' : '목록 펼치기'}
                                    onClick={e => {
                                        e.stopPropagation();
                                        onToggleProjectsList();
                                    }}
                                >
                                    {projectsListExpanded ? <ChevronUpIcon /> : <ChevronDownIcon />}
                                </button>
                            </div>
                        )}
                    </div>

                    {projectsListExpanded && (
                        <SortableProjectList
                            projects={projects}
                            sortable={!!onReorderProject}
                            onReorder={onReorderProject}
                            onSelect={onProjectSelect}
                            onEdit={onEditProject}
                            onShare={onShareProject}
                            onDelete={onDeleteProject}
                        />
                    )}
                </div>
            </div>
        </aside>
    );
};

export default Sidebar;
