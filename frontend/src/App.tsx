import { useCallback, useEffect, useLayoutEffect, useMemo, useRef, useState } from 'react';
import { useAppDispatch, useAppSelector } from './store/hooks';
import {
    addLabel,
    addProject,
    deleteLabel,
    deleteProject,
    fetchHabits,
    fetchProjectHabits,
    fetchMoreHabits,
    fetchFavorites,
    jumpToUpcomingWeek,
    setUpcomingAnchorDate,
    ensureUpcomingDay,
    fetchMoreUpcomingDay,
    fetchLabels,
    fetchMoreLabels,
    fetchNavTaskCounts,
    fetchProjects,
    invalidateSidebarAggregates,
    setActiveView,
    setSelectedLabel,
    setSelectedProject,
    updateLabel,
    updateProject,
    checkHabit,
    type ApiView,
    type Habit,
    type Label,
    type Project,
    habitRowKey,
} from './store/habitSlice';
import HabitItem from './components/HabitItem';
import AddHabitForm, { type AddHabitFormHandle } from './components/AddHabitForm';
import Sidebar, { type NavItem } from './components/Sidebar';
import LabelsBrowseView from './components/LabelsBrowseView';
import AddLabelModal from './components/AddLabelModal';
import EditLabelModal from './components/EditLabelModal';
import { displayLabelName } from './api/labelMappers';
import AddProjectModal from './components/AddProjectModal';
import EditProjectModal from './components/EditProjectModal';
import ProjectShareModal from './components/ProjectShareModal';
import ProjectsBrowseView from './components/ProjectsBrowseView';
import UpcomingTaskList from './components/UpcomingTaskList';
import OverdueTasksSection from './components/OverdueTasksSection';
import TaskDetailModal from './components/TaskDetailModal';
import TaskCompleteToast from './components/TaskCompleteToast';
import { useTaskCompleteToast } from './hooks/useTaskCompleteToast';
import InlineAddTaskButton from './components/InlineAddTaskButton';
import type { TaskRowLayout } from './components/HabitItem';
import SearchModal from './components/SearchModal';
import LoginScreen from './components/LoginScreen';
import NotificationsView from './components/NotificationsView';
import ActivityLogView from './components/ActivityLogView';
import ViewMenuButton from './components/ViewMenuButton';
import { fetchNotifications, selectUnreadCount } from './store/notificationsSlice';
import { BellIcon, PanelToggleIcon } from './components/icons';
import { saveUserProfile } from './utils/userProfile';
import { bootstrapAuthFromCallback, restoreAuthSession } from './api/authBootstrap';
import { clearStoredTokens, onAuthLogout } from './api/client';
import { fetchMember, logoutMember } from './api/memberApi';
import OAuthRedirectHandler from './components/OAuthRedirectHandler';
import { clearHabitError } from './store/habitSlice';
import { useAppUrlSync } from './hooks/useAppUrlSync';
import { useInfiniteScroll } from './hooks/useInfiniteScroll';
import { parseAppPath } from './utils/appRoutes';
import { formatSectionDate, formatTodayHeader, toISODate } from './utils/date';
import {
    filterHabits,
    groupHabits,
    loadViewPreferences,
    saveViewPreferences,
    type ViewPreferences,
} from './utils/viewPreferences';
import { useNotificationSse } from './hooks/useNotificationSse';
import { useQuickAddShortcut } from './hooks/useQuickAddShortcut';
import './App.css';

const NAV_META: Record<NavItem, { title: string; subtitle?: string }> = {
    inbox: { title: '관리함' },
    today: { title: '오늘', subtitle: formatTodayHeader() },
    upcoming: { title: '다음' },
    filters: { title: '라벨' },
    report: { title: '보고' },
};

function toApiView(nav: NavItem): ApiView {
    if (nav === 'filters' || nav === 'report') return 'all';
    return nav;
}

function getInitialNav(): NavItem {
    const location = parseAppPath(window.location.pathname);
    if (location.kind === 'nav') return location.nav;
    if (location.kind === 'labelsBrowse' || location.kind === 'label') return 'filters';
    return 'today';
}

function App() {
    const dispatch = useAppDispatch();
    const {
        list: habits,
        projects,
        labels,
        favorites,
        status,
        loadMoreStatus,
        tasksHasNext,
        overdueList,
        upcomingAnchorDate,
        upcomingWeekStartIso,
        upcomingJumpStatus,
        upcomingDays,
        upcomingDayCounts,
        upcomingSummaryStatus,
        labelsLoadMoreStatus,
        labelsHasNext,
        labelsStatus,
        error: habitError,
        selectedProjectId,
        selectedLabelId,
        selectedProjectDetail,
        inboxTaskCount,
        todayTaskCount,
    } = useAppSelector(state => state.habits);

    const initialLocation = parseAppPath(window.location.pathname);
    const [activeNav, setActiveNav] = useState<NavItem>(getInitialNav);
    const [showNotifications, setShowNotifications] = useState(
        () => initialLocation.kind === 'notifications',
    );
    const [sidebarCollapsed, setSidebarCollapsed] = useState(
        () => localStorage.getItem('habitflow.sidebarCollapsed') === 'true',
    );
    const [viewPrefs, setViewPrefs] = useState<ViewPreferences>(loadViewPreferences);
    const notificationItems = useAppSelector(state => state.notifications.items);
    const unreadNotificationCount = selectUnreadCount(notificationItems);
    const [showAddProjectModal, setShowAddProjectModal] = useState(false);
    const [showAddLabelModal, setShowAddLabelModal] = useState(false);
    const [editingProject, setEditingProject] = useState<Project | null>(null);
    const [sharingProject, setSharingProject] = useState<Project | null>(null);
    const [editingLabel, setEditingLabel] = useState<Label | null>(null);
    const [showProjectsBrowse, setShowProjectsBrowse] = useState(
        () => initialLocation.kind === 'projectsBrowse',
    );
    const [projectsListExpanded, setProjectsListExpanded] = useState(
        () => localStorage.getItem('habitflow.projectsListExpanded') !== 'false',
    );
    const [favoritesListExpanded, setFavoritesListExpanded] = useState(
        () => localStorage.getItem('habitflow.favoritesListExpanded') !== 'false',
    );
    const [showSearchModal, setShowSearchModal] = useState(false);
    const [selectedTaskId, setSelectedTaskId] = useState<number | null>(null);
    const [isAuthenticated, setIsAuthenticated] = useState<boolean>(() => bootstrapAuthFromCallback());
    const [sessionRestorePending, setSessionRestorePending] = useState(
        () => !bootstrapAuthFromCallback(),
    );

    useEffect(() => {
        if (!sessionRestorePending) return;

        let cancelled = false;
        void restoreAuthSession().then(restored => {
            if (cancelled) return;
            if (restored) {
                setIsAuthenticated(true);
            }
            setSessionRestorePending(false);
        });

        return () => {
            cancelled = true;
        };
    }, [sessionRestorePending]);

    useEffect(() => onAuthLogout(() => {
        setIsAuthenticated(false);
        setShowNotifications(false);
    }), []);

    const addFormRef = useRef<AddHabitFormHandle>(null);
    const mainPanelRef = useRef<HTMLElement>(null);
    const upcomingScrollTopRef = useRef<number | null>(null);
    const prevHabitsLengthRef = useRef(0);
    const prevShowLabelsBrowseRef = useRef(false);
    const prevShowProjectsBrowseRef = useRef(false);

    useQuickAddShortcut(() => addFormRef.current?.open());
    useNotificationSse(isAuthenticated);

    const {
        completedTaskId,
        showCompleteToast,
        dismissToast,
        isToastVisible,
    } = useTaskCompleteToast();

    const refreshSidebarCounts = useCallback(() => {
        void dispatch(invalidateSidebarAggregates({ projects: true, nav: true }));
    }, [dispatch]);

    const handleTaskCompleted = useCallback((habit: Habit) => {
        showCompleteToast(habit.id);
        refreshSidebarCounts();
    }, [showCompleteToast, refreshSidebarCounts]);

    const handleOpenHabit = useCallback((habit: Habit) => {
        setSelectedTaskId(habit.id);
    }, []);

    const handleOpenTaskId = useCallback((taskId: number) => {
        const habit = habits.find(h => h.id === taskId);
        if (habit) {
            handleOpenHabit(habit);
            return;
        }
        setSelectedTaskId(taskId);
    }, [habits, handleOpenHabit]);

    const handleTaskDeleted = useCallback((habitId: number) => {
        if (selectedTaskId === habitId) setSelectedTaskId(null);
        refreshSidebarCounts();
    }, [selectedTaskId, refreshSidebarCounts]);

    const handleUndoComplete = useCallback(async () => {
        if (completedTaskId == null) return;
        await dispatch(checkHabit({ habitId: completedTaskId, wasCompleted: true }));
        dismissToast();
        refreshSidebarCounts();
    }, [completedTaskId, dispatch, dismissToast, refreshSidebarCounts]);

    useAppUrlSync({
        activeNav,
        setActiveNav,
        showProjectsBrowse,
        setShowProjectsBrowse,
        showNotifications,
        setShowNotifications,
        selectedProjectId,
        selectedLabelId,
        dispatch,
    });

    const viewScrollKey = useMemo(
        () => [
            activeNav,
            selectedProjectId,
            selectedLabelId,
            showProjectsBrowse,
            showNotifications,
        ].join(':'),
        [activeNav, selectedProjectId, selectedLabelId, showProjectsBrowse, showNotifications],
    );

    useLayoutEffect(() => {
        if (!isAuthenticated) return;
        mainPanelRef.current?.scrollTo(0, 0);
    }, [viewScrollKey, isAuthenticated]);

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

    useEffect(() => {
        if (!isAuthenticated) return;

        dispatch(fetchFavorites());
        dispatch(fetchNavTaskCounts());
        dispatch(fetchNotifications());
        if (initialLocation.kind !== 'projectsBrowse') {
            dispatch(fetchProjects());
        }
        void fetchMember()
            .then(member => {
                saveUserProfile({
                    displayName: member.name ?? '사용자',
                    fullName: member.name ?? '사용자',
                    email: member.email ?? '',
                });
            })
            .catch(() => undefined);
    }, [dispatch, isAuthenticated]);

    const showLabelsBrowse = activeNav === 'filters' && !selectedLabelId && !selectedProjectId && !showProjectsBrowse;

    /** 화면(라우트) 진입 시 해당 뷰 데이터 재조회 */
    useEffect(() => {
        if (!isAuthenticated) return;
        if (showNotifications) return;
        if (activeNav === 'report') return;

        const enteredProjectsBrowse = showProjectsBrowse && !prevShowProjectsBrowseRef.current;
        prevShowProjectsBrowseRef.current = showProjectsBrowse;

        const enteredLabelsBrowse = showLabelsBrowse && !prevShowLabelsBrowseRef.current;
        prevShowLabelsBrowseRef.current = showLabelsBrowse;

        if (showProjectsBrowse) {
            if (enteredProjectsBrowse) {
                dispatch(fetchProjects());
                dispatch(fetchFavorites());
            }
            return;
        }

        if (showLabelsBrowse) {
            if (enteredLabelsBrowse) {
                dispatch(fetchLabels());
            }
            return;
        }

        if (labels.length === 0) {
            dispatch(fetchLabels());
        }

        if (selectedProjectId != null) {
            dispatch(fetchProjectHabits(selectedProjectId));
            return;
        }

        const view = selectedLabelId != null ? 'all' : toApiView(activeNav);
        dispatch(fetchHabits({ view, projectId: null, labelId: selectedLabelId }));
    }, [
        dispatch,
        isAuthenticated,
        activeNav,
        selectedProjectId,
        selectedLabelId,
        showProjectsBrowse,
        showLabelsBrowse,
        showNotifications,
    ]);

    const handleNavChange = (nav: NavItem) => {
        setShowNotifications(false);
        setShowProjectsBrowse(false);
        setActiveNav(nav);
        dispatch(setActiveView(nav));
        dispatch(setSelectedProject(null));
        dispatch(setSelectedLabel(null));
    };

    const handleNotificationsClick = () => {
        setShowNotifications(prev => !prev);
        setShowProjectsBrowse(false);
        dispatch(setSelectedProject(null));
        dispatch(setSelectedLabel(null));
    };

    const handleLogout = () => {
        setIsAuthenticated(false);
        setShowNotifications(false);
        void logoutMember()
            .catch(() => undefined)
            .finally(() => {
                clearStoredTokens();
            });
    };

    const toggleSidebar = () => {
        setSidebarCollapsed(prev => {
            const next = !prev;
            localStorage.setItem('habitflow.sidebarCollapsed', String(next));
            return next;
        });
    };

    const handleProjectsBrowse = () => {
        setShowNotifications(false);
        setShowProjectsBrowse(true);
        dispatch(setSelectedProject(null));
        dispatch(setSelectedLabel(null));
    };

    const handleProjectSelect = (projectId: number) => {
        setShowNotifications(false);
        setShowProjectsBrowse(false);
        dispatch(setSelectedProject(projectId));
    };

    const handleLabelsBrowse = () => {
        setShowNotifications(false);
        setShowProjectsBrowse(false);
        setActiveNav('filters');
        dispatch(setActiveView('filters'));
        dispatch(setSelectedLabel(null));
        dispatch(setSelectedProject(null));
    };

    const handleLabelSelect = (labelId: number | null) => {
        setShowNotifications(false);
        setShowProjectsBrowse(false);
        if (labelId != null) {
            setActiveNav('filters');
            dispatch(setActiveView('filters'));
        }
        dispatch(setSelectedLabel(labelId));
        dispatch(setSelectedProject(null));
    };

    const handleLabelOpen = (labelId: number) => {
        handleLabelSelect(labelId);
    };

    const handleEditLabel = (label: Label) => {
        setEditingLabel(label);
    };

    const handleDeleteLabel = (labelId: number) => {
        const label = labels.find(l => l.id === labelId);
        if (!label) return;
        if (!window.confirm(`"${displayLabelName(label.name)}" 라벨을 삭제할까요?`)) return;
        dispatch(deleteLabel(labelId));
        if (selectedLabelId === labelId) {
            handleLabelsBrowse();
        }
    };

    const toggleProjectsList = () => {
        setProjectsListExpanded(prev => {
            const next = !prev;
            localStorage.setItem('habitflow.projectsListExpanded', String(next));
            return next;
        });
    };

    const toggleFavoritesList = () => {
        setFavoritesListExpanded(prev => {
            const next = !prev;
            localStorage.setItem('habitflow.favoritesListExpanded', String(next));
            return next;
        });
    };

    const handleEditProject = (project: Project) => {
        setEditingProject(project);
    };

    const handleShareProject = (project: Project) => {
        setSharingProject(project);
    };

    const handleDeleteProject = (projectId: number) => {
        const project = projects.find(p => p.id === projectId);
        if (!project) return;
        if (!window.confirm(`"${project.name}" 프로젝트를 삭제할까요?\n작업은 프로젝트 없이 유지됩니다.`)) {
            return;
        }
        dispatch(deleteProject(projectId));
        if (selectedProjectId === projectId) {
            setShowProjectsBrowse(true);
        }
    };

    const handleViewPrefsChange = (prefs: ViewPreferences) => {
        setViewPrefs(prefs);
        saveViewPreferences(prefs);
    };

    const paginatedTaskView =
        (activeNav === 'today' || activeNav === 'inbox')
        && selectedProjectId == null
        && selectedLabelId == null;

    const displayHabits = useMemo(() => {
        let list = filterHabits(habits, viewPrefs, {
            preserveOrder:
                (paginatedTaskView || selectedProjectId != null)
                && viewPrefs.grouping === 'none',
        });
        if (!viewPrefs.showCompleted) {
            list = list.filter(h => !h.completedToday);
        }
        return list;
    }, [habits, viewPrefs, paginatedTaskView, selectedProjectId]);

    const meta = selectedLabelId
        ? {
            title: displayLabelName(labels.find(l => l.id === selectedLabelId)?.name ?? '라벨'),
            subtitle: '라벨',
        }
        : selectedProjectId
            ? {
                title: selectedProjectDetail?.name
                    ?? projects.find(p => p.id === selectedProjectId)?.name
                    ?? '프로젝트',
                subtitle: '프로젝트',
            }
            : NAV_META[activeNav];

    const showReport = activeNav === 'report' && !selectedProjectId && !selectedLabelId && !showProjectsBrowse;
    const showProjectsPanel = showProjectsBrowse && !selectedProjectId && !selectedLabelId;
    const showLabelsPanel = showLabelsBrowse;
    const showTodaySections = activeNav === 'today' && !selectedProjectId && !selectedLabelId;
    const showUpcomingGrouped = activeNav === 'upcoming' && !selectedProjectId && !selectedLabelId;
    const showInboxList = activeNav === 'inbox' && !selectedProjectId && !selectedLabelId;
    const showTaskPagination =
        showTodaySections || showInboxList || selectedProjectId != null;
    const showLabelsPagination = showLabelsPanel && labelsHasNext;

    const headerTaskCount = showTodaySections
        ? todayTaskCount
        : showInboxList
            ? inboxTaskCount
            : null;

    const handleLoadMoreTasks = useCallback(() => {
        if (!tasksHasNext || loadMoreStatus === 'loading' || upcomingJumpStatus === 'loading') return;

        if (showUpcomingGrouped && mainPanelRef.current) {
            upcomingScrollTopRef.current = mainPanelRef.current.scrollTop;
        }

        if (selectedProjectId != null) {
            dispatch(fetchMoreHabits('all'));
            return;
        }

        const view = selectedLabelId != null
            ? 'all'
            : toApiView(activeNav);
        if (view !== 'today' && view !== 'upcoming' && view !== 'inbox') return;
        dispatch(fetchMoreHabits(view));
    }, [dispatch, activeNav, selectedProjectId, selectedLabelId, tasksHasNext, loadMoreStatus, upcomingJumpStatus, showUpcomingGrouped]);

    useLayoutEffect(() => {
        if (!showUpcomingGrouped || !mainPanelRef.current) return;
        if (upcomingScrollTopRef.current != null && habits.length > prevHabitsLengthRef.current) {
            mainPanelRef.current.scrollTop = upcomingScrollTopRef.current;
            upcomingScrollTopRef.current = null;
        }
        prevHabitsLengthRef.current = habits.length;
    }, [habits.length, showUpcomingGrouped]);

    const upcomingSelectedDate = upcomingAnchorDate ?? toISODate(new Date());

    const handleEnsureUpcomingDay = useCallback((dateKey: string) => {
        dispatch(ensureUpcomingDay(dateKey));
    }, [dispatch]);

    const handleLoadMoreUpcomingDay = useCallback((dateKey: string) => {
        dispatch(fetchMoreUpcomingDay(dateKey));
    }, [dispatch]);

    const handleLoadMoreLabels = useCallback(() => {
        if (!labelsHasNext || labelsLoadMoreStatus === 'loading') return;
        dispatch(fetchMoreLabels());
    }, [dispatch, labelsHasNext, labelsLoadMoreStatus]);

    const loadMoreSentinelRef = useInfiniteScroll(
        status !== 'loading'
            && upcomingJumpStatus !== 'loading'
            && (paginatedTaskView || selectedProjectId != null)
            && tasksHasNext,
        tasksHasNext,
        loadMoreStatus === 'loading',
        handleLoadMoreTasks,
        mainPanelRef,
    );

    const labelsLoadMoreSentinelRef = useInfiniteScroll(
        labelsStatus !== 'loading' && showLabelsPanel && labelsHasNext,
        labelsHasNext,
        labelsLoadMoreStatus === 'loading',
        handleLoadMoreLabels,
        mainPanelRef,
    );
    const taskRowLayout: TaskRowLayout = selectedProjectId
        ? 'project'
        : activeNav === 'upcoming'
            ? 'upcoming'
            : 'list';

    const renderTaskList = (list: typeof displayHabits) => (
        <ul className="task-list">
            {list.map(habit => (
                <HabitItem
                    key={habitRowKey(habit)}
                    habit={habit}
                    layout={taskRowLayout}
                    onOpenDetails={handleOpenHabit}
                    onOpenProject={handleProjectSelect}
                    onTaskCompleted={handleTaskCompleted}
                    onTaskDeleted={handleTaskDeleted}
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
                    {overdueList.length > 0 && (
                        <OverdueTasksSection
                            habits={overdueList}
                            layout="list"
                            onOpenDetails={handleOpenHabit}
                            onOpenProject={handleProjectSelect}
                            onTaskCompleted={handleTaskCompleted}
                            onTaskDeleted={handleTaskDeleted}
                        />
                    )}
                    {renderTaskList(displayHabits)}
                    <InlineAddTaskButton onClick={() => addFormRef.current?.open()} />
                </>
            );
        }

        if (displayHabits.length === 0) {
            if (selectedProjectId != null && status !== 'loading') {
                return (
                    <div className="empty-state">
                        <p className="empty-title">이 프로젝트에 작업이 없습니다</p>
                        <p className="empty-desc">아래에서 작업을 추가해 보세요.</p>
                    </div>
                );
            }
            return null;
        }

        if (selectedProjectId != null) {
            if (viewPrefs.grouping !== 'none') {
                return <>{renderGroupedTasks(displayHabits)}</>;
            }
            return renderTaskList(displayHabits);
        }

        if (showUpcomingGrouped && viewPrefs.grouping === 'none') {
            return (
                <UpcomingTaskList
                    habits={displayHabits}
                    overdueHabits={overdueList}
                    upcomingDays={upcomingDays}
                    upcomingDayCounts={upcomingDayCounts}
                    upcomingSummaryStatus={upcomingSummaryStatus}
                    selectedDate={upcomingSelectedDate}
                    weekStartIso={upcomingWeekStartIso}
                    jumpStatus={upcomingJumpStatus}
                    scrollContainerRef={mainPanelRef}
                    onJumpToDate={iso => dispatch(jumpToUpcomingWeek(iso))}
                    onSelectDate={iso => dispatch(setUpcomingAnchorDate(iso))}
                    onEnsureDay={handleEnsureUpcomingDay}
                    onLoadMoreDay={handleLoadMoreUpcomingDay}
                    onOpenDetails={handleOpenHabit}
                    onOpenProject={handleProjectSelect}
                    onAddTask={() => addFormRef.current?.open()}
                    onTaskCompleted={handleTaskCompleted}
                    onTaskDeleted={handleTaskDeleted}
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

    if (sessionRestorePending) {
        return null;
    }

    if (!isAuthenticated) {
        return (
            <>
                <OAuthRedirectHandler onSuccess={handleAuthSuccess} />
                <LoginScreen onLoginSuccess={handleAuthSuccess} />
            </>
        );
    }

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
                inboxTaskCount={inboxTaskCount}
                todayTaskCount={todayTaskCount}
                projects={projects}
                labels={labels}
                favorites={favorites}
                selectedProjectId={selectedProjectId}
                selectedLabelId={selectedLabelId}
                projectsBrowseActive={showProjectsBrowse}
                projectsListExpanded={projectsListExpanded}
                favoritesListExpanded={favoritesListExpanded}
                onNavChange={handleNavChange}
                onProjectSelect={handleProjectSelect}
                onLabelSelect={handleLabelOpen}
                onEditLabel={handleEditLabel}
                onDeleteLabel={handleDeleteLabel}
                onProjectsBrowse={handleProjectsBrowse}
                onAddProject={() => setShowAddProjectModal(true)}
                onEditProject={handleEditProject}
                onShareProject={handleShareProject}
                onDeleteProject={handleDeleteProject}
                onToggleProjectsList={toggleProjectsList}
                onToggleFavoritesList={toggleFavoritesList}
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
                ref={mainPanelRef}
                className={`main-panel ${showNotifications ? 'main-panel-notifications' : ''} ${showReport ? 'main-panel-report' : ''}`}
            >
                {collapsedChrome}
                {showNotifications ? (
                    <NotificationsView
                        onOpenTask={id => {
                            handleOpenTaskId(id);
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
                        onOpenTask={handleOpenTaskId}
                    />
                ) : showProjectsPanel ? (
                    <ProjectsBrowseView
                        projects={projects}
                        onSelectProject={handleProjectSelect}
                        onAddProject={() => setShowAddProjectModal(true)}
                        onEditProject={handleEditProject}
                        onDeleteProject={handleDeleteProject}
                    />
                ) : showLabelsPanel ? (
                    <>
                        <LabelsBrowseView
                            labels={labels}
                            loading={labelsStatus === 'loading'}
                            onSelectLabel={handleLabelOpen}
                            onAddLabel={() => setShowAddLabelModal(true)}
                            onEditLabel={handleEditLabel}
                            onDeleteLabel={handleDeleteLabel}
                        />
                        {showLabelsPagination && (
                            <div ref={labelsLoadMoreSentinelRef} className="tasks-scroll-sentinel" aria-hidden="true">
                                {labelsLoadMoreStatus === 'loading' && (
                                    <p className="tasks-scroll-loading">불러오는 중…</p>
                                )}
                            </div>
                        )}
                    </>
                ) : (
                <>
                <header className="view-header">
                    <div className="view-header-main">
                        <div className="view-header-text">
                            {selectedProjectId && (
                                <button
                                    type="button"
                                    className="view-breadcrumb"
                                    onClick={handleProjectsBrowse}
                                >
                                    프로젝트 /
                                </button>
                            )}
                            {selectedLabelId && (
                                <button
                                    type="button"
                                    className="view-breadcrumb"
                                    onClick={handleLabelsBrowse}
                                >
                                    라벨 /
                                </button>
                            )}
                            <h1 className="view-title">{meta.title}</h1>
                            {meta.subtitle && !selectedProjectId && !selectedLabelId && (
                                <p className="view-subtitle">{meta.subtitle}</p>
                            )}
                        </div>
                        {!showLabelsPanel && (
                            <ViewMenuButton
                                preferences={viewPrefs}
                                labels={labels}
                                onChange={handleViewPrefsChange}
                            />
                        )}
                    </div>
                    {headerTaskCount != null && headerTaskCount > 0 && (
                        <p className="task-summary today-task-count">
                            <span className="today-count-icon">✓</span>
                            {headerTaskCount} 작업
                        </p>
                    )}
                </header>

                <section className={`task-section ${showUpcomingGrouped ? 'task-section-upcoming' : ''} ${selectedProjectId ? 'task-section-project' : ''}`}>
                    {status === 'loading' && <p className="status-message">불러오는 중…</p>}

                    {status !== 'loading' && displayHabits.length === 0 && overdueList.length === 0 && !showLabelsPanel && !showReport && !showProjectsPanel && (
                        <div className="empty-state">
                            <p className="empty-title">할 일이 없습니다</p>
                            <p className="empty-desc">
                                <kbd>Q</kbd> 키를 누르거나 아래에서 작업을 추가해 보세요.
                            </p>
                        </div>
                    )}

                    {renderTaskContent()}

                    {selectedProjectId && !showTodaySections && !showUpcomingGrouped && (
                        <InlineAddTaskButton onClick={() => addFormRef.current?.open()} />
                    )}

                    <AddHabitForm
                        ref={addFormRef}
                        view={activeNav}
                        projectId={selectedProjectId}
                        labelId={selectedLabelId}
                        hideTrigger={showTodaySections || showUpcomingGrouped || !!selectedProjectId}
                    />

                    {showTaskPagination && tasksHasNext && (
                        <div ref={loadMoreSentinelRef} className="tasks-scroll-sentinel" aria-hidden="true">
                            {loadMoreStatus === 'loading' && (
                                <p className="tasks-scroll-loading">불러오는 중…</p>
                            )}
                        </div>
                    )}
                </section>
                </>
                )}
            </main>

            {showAddProjectModal && (
                <AddProjectModal
                    onClose={() => setShowAddProjectModal(false)}
                    onAdd={(name, color) => {
                        void dispatch(addProject({ name, color }));
                    }}
                />
            )}

            {sharingProject && (
                <ProjectShareModal
                    project={sharingProject}
                    onClose={() => setSharingProject(null)}
                />
            )}

            {editingProject && (
                <EditProjectModal
                    project={editingProject}
                    allProjects={projects}
                    onClose={() => setEditingProject(null)}
                    onSave={payload => {
                        void dispatch(updateProject(payload));
                    }}
                />
            )}

            {showAddLabelModal && (
                <AddLabelModal
                    onClose={() => setShowAddLabelModal(false)}
                    onAdd={payload => {
                        void dispatch(addLabel(payload));
                    }}
                />
            )}

            {editingLabel && (
                <EditLabelModal
                    label={editingLabel}
                    onClose={() => setEditingLabel(null)}
                    onSave={(id, payload) => {
                        void dispatch(updateLabel({ id, ...payload }));
                    }}
                />
            )}

            {selectedTaskId != null && (
                <TaskDetailModal
                    taskId={selectedTaskId}
                    onClose={() => setSelectedTaskId(null)}
                    onTaskCompleted={handleTaskCompleted}
                />
            )}

            {isToastVisible && (
                <TaskCompleteToast
                    onUndo={() => void handleUndoComplete()}
                    onClose={dismissToast}
                />
            )}

            {showSearchModal && (
                <SearchModal
                    habits={habits}
                    projects={projects}
                    labels={labels}
                    onClose={() => setShowSearchModal(false)}
                    onSelectHabit={handleOpenHabit}
                    onSelectProject={id => handleProjectSelect(id)}
                    onSelectLabel={id => handleLabelSelect(id)}
                />
            )}
        </div>
        </>
    );
}

export default App;
