import { useCallback, useEffect, useLayoutEffect, useRef } from 'react';
import type { NavItem } from '../components/Sidebar';
import type { AppDispatch } from '../store';
import { setActiveView, setSelectedLabel, setSelectedProject } from '../store/habitSlice';
import {
    buildAppPath,
    defaultAppPath,
    isOAuthCallbackPath,
    isProjectInvitePath,
    parseAppPath,
} from '../utils/appRoutes';

interface UseAppUrlSyncParams {
    activeNav: NavItem;
    setActiveNav: (nav: NavItem) => void;
    showProjectsBrowse: boolean;
    setShowProjectsBrowse: (value: boolean) => void;
    showNotifications: boolean;
    setShowNotifications: (value: boolean) => void;
    selectedProjectId: import('../api/types').EntityId | null;
    selectedLabelId: import('../api/types').EntityId | null;
    dispatch: AppDispatch;
}

export function useAppUrlSync({
    activeNav,
    setActiveNav,
    showProjectsBrowse,
    setShowProjectsBrowse,
    showNotifications,
    setShowNotifications,
    selectedProjectId,
    selectedLabelId,
    dispatch,
}: UseAppUrlSyncParams) {
    const applyingFromUrl = useRef(false);
    const hydrated = useRef(false);

    const applyLocationFromUrl = useCallback(() => {
        const pathname = window.location.pathname;
        if (isOAuthCallbackPath(pathname) || isProjectInvitePath(pathname)) return;

        const location = parseAppPath(pathname);
        applyingFromUrl.current = true;

        switch (location.kind) {
            case 'notifications':
                setShowNotifications(true);
                setShowProjectsBrowse(false);
                setActiveNav('today');
                dispatch(setSelectedProject(null));
                dispatch(setSelectedLabel(null));
                break;
            case 'nav':
                setShowNotifications(false);
                setShowProjectsBrowse(false);
                setActiveNav(location.nav);
                dispatch(setActiveView(location.nav));
                dispatch(setSelectedProject(null));
                dispatch(setSelectedLabel(null));
                break;
            case 'labelsBrowse':
                setShowNotifications(false);
                setShowProjectsBrowse(false);
                setActiveNav('filters');
                dispatch(setActiveView('filters'));
                dispatch(setSelectedProject(null));
                dispatch(setSelectedLabel(null));
                break;
            case 'label':
                setShowNotifications(false);
                setShowProjectsBrowse(false);
                setActiveNav('filters');
                dispatch(setActiveView('filters'));
                dispatch(setSelectedProject(null));
                dispatch(setSelectedLabel(location.labelId));
                break;
            case 'projectsBrowse':
                setShowNotifications(false);
                setShowProjectsBrowse(true);
                dispatch(setSelectedProject(null));
                dispatch(setSelectedLabel(null));
                break;
            case 'project':
                setShowNotifications(false);
                setShowProjectsBrowse(false);
                dispatch(setSelectedProject(location.projectId));
                break;
            case 'unknown':
            case 'oauth':
                break;
        }

        queueMicrotask(() => {
            applyingFromUrl.current = false;
        });
    }, [
        dispatch,
        setActiveNav,
        setActiveView,
        setSelectedLabel,
        setSelectedProject,
        setShowNotifications,
        setShowProjectsBrowse,
    ]);

    useLayoutEffect(() => {
        const pathname = window.location.pathname;
        if (isOAuthCallbackPath(pathname) || isProjectInvitePath(pathname)) return;

        if (pathname === '/' || pathname === '') {
            window.history.replaceState(null, '', defaultAppPath());
        }

        applyLocationFromUrl();
        hydrated.current = true;
    }, [applyLocationFromUrl]);

    useEffect(() => {
        const onPopState = () => applyLocationFromUrl();
        window.addEventListener('popstate', onPopState);
        return () => window.removeEventListener('popstate', onPopState);
    }, [applyLocationFromUrl]);

    useEffect(() => {
        if (!hydrated.current || applyingFromUrl.current) return;
        if (isOAuthCallbackPath(window.location.pathname)) return;

        const nextPath = buildAppPath({
            activeNav,
            showProjectsBrowse,
            selectedProjectId,
            selectedLabelId,
            showNotifications,
        });

        if (window.location.pathname !== nextPath) {
            window.history.pushState(null, '', nextPath);
        }
    }, [
        activeNav,
        showProjectsBrowse,
        selectedProjectId,
        selectedLabelId,
        showNotifications,
    ]);
}
