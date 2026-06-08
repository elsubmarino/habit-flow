import { useEffect, useMemo, useRef, useState } from 'react';
import { useAppDispatch, useAppSelector } from './store/hooks';
import {
    addLabel,
    addProject,
    deleteLabel,
    deleteProject,
    fetchHabits,
    fetchLabels,
    fetchProjects,
    setActiveView,
    setSelectedLabel,
    setSelectedProject,
    type ApiView,
} from './store/habitSlice';
import HabitItem from './components/HabitItem';
import AddHabitForm, { type AddHabitFormHandle } from './components/AddHabitForm';
import Sidebar, { type NavItem } from './components/Sidebar';
import LabelsPanel from './components/LabelsPanel';
import ProjectModal from './components/ProjectModal';
import UpcomingTaskList from './components/UpcomingTaskList';
import TaskDetailModal from './components/TaskDetailModal';
import SearchModal from './components/SearchModal';
import LoginScreen from './components/LoginScreen';
import NotificationsView from './components/NotificationsView';
import ActivityLogView from './components/ActivityLogView';
import ViewMenuButton from './components/ViewMenuButton';
import { fetchNotifications, selectUnreadCount } from './store/notificationsSlice';
import { BellIcon, PanelToggleIcon } from './components/icons';
import { saveUserProfile } from './utils/userProfile';
import { clearStoredToken } from './api/client';
import { bootstrapAuthFromCallback } from './api/authBootstrap';
import { fetchMember } from './api/memberApi';
import OAuthRedirectHandler from './components/OAuthRedirectHandler';
import { clearHabitError, fetchHabitDetail } from './store/habitSlice';
import { formatSectionDate, formatTodayHeader } from './utils/date';
import {
    filterHabits,
    groupHabits,
    loadViewPreferences,
    saveViewPreferences,
    type ViewPreferences,
} from './utils/viewPreferences';
import { useQuickAddShortcut } from './hooks/useQuickAddShortcut';
import './App.css';

const NAV_META: Record<NavItem, { title: string; subtitle?: string }> = {
    inbox: { title: '받은 편지함' },
    today: { title: '오늘', subtitle: formatTodayHeader() },
    upcoming: { title: '다음 7일' },
    filters: { title: '필터 및 라벨' },
    report: { title: '보고' },
};

function toApiView(nav: NavItem): ApiView {
    if (nav === 'filters' || nav === 'report') return 'all';
    return nav;
}

function App() {
    const dispatch = useAppDispatch();
    const {
        list: habits,
        projects,
        labels,
        status,
        error: habitError,
        selectedProjectId,
        selectedLabelId,
    } = useAppSelector(state => state.habits);

    const [activeNav, setActiveNav] = useState<NavItem>('today');
    const [showNotifications, setShowNotifications] = useState(false);
    const [sidebarCollapsed, setSidebarCollapsed] = useState(
        () => localStorage.getItem('habitflow.sidebarCollapsed') === 'true',
    );
    const [viewPrefs, setViewPrefs] = useState<ViewPreferences>(loadViewPreferences);
    const notificationItems = useAppSelector(state => state.notifications.items);
    const unreadNotificationCount = selectUnreadCount(notificationItems);
    const [showProjectModal, setShowProjectModal] = useState(false);
    const [showSearchModal, setShowSearchModal] = useState(false);
    const [selectedHabitId, setSelectedHabitId] = useState<number | null>(null);
    const [isAuthenticated, setIsAuthenticated] = useState<boolean>(() => bootstrapAuthFromCallback());
    const addFormRef = useRef<AddHabitFormHandle>(null);

    useQuickAddShortcut(() => addFormRef.current?.open());

    useEffect(() => {
        const onKey = (e: KeyboardEvent) => {
            if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 'k') {
                e.preventDefault();
                setShowSearchModal(true);
            }
        };
        window.addEventListener('keydown', onKey);
        return () => window.removeEventListener('keydown', onKey);
    }, []);

    const handleAuthSuccess = () => {
        setIsAuthenticated(true);
        void fetchMember()
            .then(member => {
                saveUserProfile({
                    displayName: member.name ?? '사용자',
                    fullName: member.name ?? '사용자',
                    email: member.email ?? '',
                });
            })
            .catch(() => undefined);
    };

    if (!isAuthenticated) {
        return (
            <>
                <OAuthRedirectHandler onSuccess={handleAuthSuccess} />
                <LoginScreen onLoginSuccess={handleAuthSuccess} />
            </>
        );
    }

    useEffect(() => {
        dispatch(fetchProjects());
        dispatch(fetchLabels());
        dispatch(fetchNotifications());
        void fetchMember()
            .then(member => {
                saveUserProfile({
                    displayName: member.name ?? '사용자',
                    fullName: member.name ?? '사용자',
                    email: member.email ?? '',
                });
            })
            .catch(() => undefined);
    }, [dispatch]);

    useEffect(() => {
        if (activeNav === 'report') return;
        const view = selectedLabelId != null ? 'all' : toApiView(activeNav);
        dispatch(fetchHabits({ view, projectId: selectedProjectId, labelId: selectedLabelId }));
    }, [dispatch, activeNav, selectedProjectId, selectedLabelId]);

    const handleNavChange = (nav: NavItem) => {
        setShowNotifications(false);
        setActiveNav(nav);
        dispatch(setActiveView(nav));
        dispatch(setSelectedProject(null));
        dispatch(setSelectedLabel(null));
    };

    const handleNotificationsClick = () => {
        setShowNotifications(prev => !prev);
        dispatch(setSelectedProject(null));
        dispatch(setSelectedLabel(null));
    };

    const handleLogout = () => {
        clearStoredToken();
        setIsAuthenticated(false);
        setShowNotifications(false);
    };

    useEffect(() => {
        if (selectedHabitId != null) {
            void dispatch(fetchHabitDetail(selectedHabitId));
        }
    }, [dispatch, selectedHabitId]);

    const toggleSidebar = () => {
        setSidebarCollapsed(prev => {
            const next = !prev;
            localStorage.setItem('habitflow.sidebarCollapsed', String(next));
            return next;
        });
    };

    const handleProjectSelect = (projectId: number | null) => {
        setShowNotifications(false);
        if (projectId != null) setActiveNav('today');
        dispatch(setSelectedProject(projectId));
        dispatch(setSelectedLabel(null));
    };

    const handleLabelSelect = (labelId: number | null) => {
        setShowNotifications(false);
        if (labelId != null) setActiveNav('filters');
        dispatch(setSelectedLabel(labelId));
        dispatch(setSelectedProject(null));
    };

    const handleViewPrefsChange = (prefs: ViewPreferences) => {
        setViewPrefs(prefs);
        saveViewPreferences(prefs);
    };

    const displayHabits = useMemo(() => {
        let list = filterHabits(habits, viewPrefs);
        if (!viewPrefs.showCompleted) {
            list = list.filter(h => !h.completedToday);
        }
        return list;
    }, [habits, viewPrefs]);

    const pending = useMemo(() => displayHabits.filter(h => !h.completedToday), [displayHabits]);
    const completedToday = useMemo(() => displayHabits.filter(h => h.completedToday), [displayHabits]);

    const meta = selectedLabelId
        ? { title: labels.find(l => l.id === selectedLabelId)?.name ?? '라벨' }
        : selectedProjectId
            ? { title: projects.find(p => p.id === selectedProjectId)?.name ?? '프로젝트' }
            : NAV_META[activeNav];

    const showReport = activeNav === 'report' && !selectedProjectId && !selectedLabelId;
    const showTodaySections = activeNav === 'today' && !selectedProjectId && !selectedLabelId;
    const showFiltersPanel = activeNav === 'filters' && !selectedLabelId;
    const showUpcomingGrouped = activeNav === 'upcoming' && !selectedProjectId && !selectedLabelId;
    const selectedHabit = selectedHabitId != null
        ? habits.find(h => h.id === selectedHabitId) ?? null
        : null;

    const renderTaskList = (list: typeof displayHabits) => (
        <ul className="task-list">
            {list.map(habit => (
                <HabitItem
                    key={habit.id}
                    habit={habit}
                    onOpenDetails={setSelectedHabitId}
                />
            ))}
        </ul>
    );

    const renderGroupedTasks = (list: typeof displayHabits) => {
        const groups = groupHabits(list, viewPrefs.grouping);
        return groups.map(group => {
            const title =
                viewPrefs.grouping === 'date' && group.key !== 'none'
                    ? formatSectionDate(group.key)
                    : group.title;
            return (
                <section key={group.key}>
                    {title ? <h2 className="section-label">{title}</h2> : null}
                    {renderTaskList(group.habits)}
                </section>
            );
        });
    };

    const renderTaskContent = () => {
        if (viewPrefs.layout === 'board') {
            return (
                <div className="view-layout-placeholder">
                    <p>보드 보기는 곧 지원됩니다.</p>
                </div>
            );
        }
        if (viewPrefs.layout === 'calendar') {
            return (
                <div className="view-layout-placeholder">
                    <p>캘린더 보기는 곧 지원됩니다.</p>
                </div>
            );
        }

        if (showTodaySections && viewPrefs.grouping === 'none') {
            return (
                <>
                    {pending.length > 0 && (
                        <>
                            <h2 className="section-label">작업</h2>
                            {renderTaskList(pending)}
                        </>
                    )}
                    {completedToday.length > 0 && (
                        <>
                            <h2 className="section-label completed-label">
                                완료됨 · {completedToday.length}
                            </h2>
                            <ul className="task-list completed-list">
                                {completedToday.map(habit => (
                                    <HabitItem
                                        key={habit.id}
                                        habit={habit}
                                        onOpenDetails={setSelectedHabitId}
                                    />
                                ))}
                            </ul>
                        </>
                    )}
                </>
            );
        }

        if (displayHabits.length === 0) return null;

        if (showUpcomingGrouped && viewPrefs.grouping === 'none') {
            return (
                <UpcomingTaskList
                    habits={displayHabits}
                    onOpenDetails={setSelectedHabitId}
                />
            );
        }

        if (viewPrefs.grouping !== 'none') {
            return <>{renderGroupedTasks(displayHabits)}</>;
        }

        return renderTaskList(displayHabits);
    };

    const collapsedChrome = sidebarCollapsed ? (
        <div className="main-chrome-bar">
            <button
                type="button"
                className="chrome-btn"
                aria-label="사이드바 펼치기"
                onClick={toggleSidebar}
            >
                <PanelToggleIcon />
            </button>
            <div className="main-chrome-spacer" />
            <button
                type="button"
                className={`chrome-btn ${showNotifications ? 'active' : ''}`}
                aria-label="알림"
                onClick={handleNotificationsClick}
            >
                <BellIcon />
                {unreadNotificationCount > 0 && (
                    <span className="sidebar-bell-badge">{unreadNotificationCount}</span>
                )}
            </button>
        </div>
    ) : null;

    return (
        <>
        <OAuthRedirectHandler onSuccess={handleAuthSuccess} />
        <div className={`app-shell ${sidebarCollapsed ? 'sidebar-collapsed' : ''}`}>
            {habitError && (
                <div className="api-error-banner" role="alert">
                    <span>{habitError}</span>
                    <button type="button" onClick={() => dispatch(clearHabitError())}>
                        닫기
                    </button>
                </div>
            )}
            <Sidebar
                activeNav={activeNav}
                projects={projects}
                labels={labels}
                selectedProjectId={selectedProjectId}
                selectedLabelId={selectedLabelId}
                onNavChange={handleNavChange}
                onProjectSelect={handleProjectSelect}
                onLabelSelect={handleLabelSelect}
                onManageProjects={() => setShowProjectModal(true)}
                onAddClick={() => addFormRef.current?.open()}
                onSearchClick={() => setShowSearchModal(true)}
                notificationsActive={showNotifications}
                unreadNotificationCount={unreadNotificationCount}
                onNotificationsClick={handleNotificationsClick}
                onToggleSidebar={toggleSidebar}
                onLogout={handleLogout}
                onOpenActivity={() => handleNavChange('report')}
            />

            <main
                className={`main-panel ${showNotifications ? 'main-panel-notifications' : ''} ${showReport ? 'main-panel-report' : ''}`}
            >
                {collapsedChrome}
                {showNotifications ? (
                    <NotificationsView
                        onOpenTask={id => {
                            setSelectedHabitId(id);
                            setShowNotifications(false);
                        }}
                        onOpenProject={id => {
                            handleProjectSelect(id);
                            setShowNotifications(false);
                        }}
                    />
                ) : showReport ? (
                    <ActivityLogView
                        projects={projects}
                        onOpenTask={id => setSelectedHabitId(id)}
                    />
                ) : (
                <>
                <header className="view-header">
                    <div className="view-header-main">
                        <div className="view-header-text">
                            <h1 className="view-title">{meta.title}</h1>
                            {meta.subtitle && <p className="view-subtitle">{meta.subtitle}</p>}
                        </div>
                        {!showFiltersPanel && (
                            <ViewMenuButton
                                preferences={viewPrefs}
                                labels={labels}
                                onChange={handleViewPrefsChange}
                            />
                        )}
                    </div>
                    {showTodaySections && displayHabits.length > 0 && (
                        <p className="task-summary">
                            {pending.length > 0
                                ? `${pending.length}개 남음`
                                : '오늘 할 일을 모두 완료했습니다 🎉'}
                        </p>
                    )}
                </header>

                <section className="task-section">
                    {status === 'loading' && <p className="status-message">불러오는 중…</p>}

                    {showFiltersPanel && (
                        <LabelsPanel
                            labels={labels}
                            selectedLabelId={selectedLabelId}
                            onSelect={handleLabelSelect}
                            onAdd={name => dispatch(addLabel({ name }))}
                            onDelete={id => {
                                dispatch(deleteLabel(id));
                                dispatch(fetchLabels());
                            }}
                        />
                    )}

                    {status !== 'loading' && displayHabits.length === 0 && !showFiltersPanel && !showReport && (
                        <div className="empty-state">
                            <p className="empty-title">할 일이 없습니다</p>
                            <p className="empty-desc">
                                <kbd>Q</kbd> 키를 누르거나 아래에서 작업을 추가해 보세요.
                            </p>
                        </div>
                    )}

                    {renderTaskContent()}

                    <AddHabitForm
                        ref={addFormRef}
                        view={activeNav}
                        projectId={selectedProjectId}
                        labelId={selectedLabelId}
                    />
                </section>
                </>
                )}
            </main>

            {showProjectModal && (
                <ProjectModal
                    projects={projects}
                    onClose={() => setShowProjectModal(false)}
                    onAdd={(name, color) => {
                        dispatch(addProject({ name, color }));
                        dispatch(fetchProjects());
                    }}
                    onDelete={id => {
                        dispatch(deleteProject(id));
                        dispatch(fetchProjects());
                        if (selectedProjectId === id) dispatch(setSelectedProject(null));
                    }}
                />
            )}

            {selectedHabit && (
                <TaskDetailModal
                    habit={selectedHabit}
                    onClose={() => setSelectedHabitId(null)}
                />
            )}

            {showSearchModal && (
                <SearchModal
                    habits={habits}
                    projects={projects}
                    labels={labels}
                    onClose={() => setShowSearchModal(false)}
                    onSelectHabit={id => setSelectedHabitId(id)}
                    onSelectProject={id => handleProjectSelect(id)}
                    onSelectLabel={id => handleLabelSelect(id)}
                />
            )}
        </div>
        </>
    );
}

export default App;
