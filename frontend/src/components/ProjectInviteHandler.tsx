import { useEffect, useRef } from 'react';
import { getApiErrorMessage } from '../api/apiError';
import { acceptProjectInvitation } from '../api/projectApi';
import { useToast } from '../context/ToastContext';
import { useAppDispatch } from '../store/hooks';
import { fetchProjects } from '../store/habitSlice';
import { APP_ROUTES } from '../utils/appRoutes';
import {
    consumePendingInviteToken,
    getProjectInviteTokenFromLocation,
    isProjectInvitePath,
    savePendingInviteToken,
} from '../utils/projectInvite';

interface ProjectInviteHandlerProps {
    isAuthenticated: boolean;
    onAccepted: () => void;
}

const ProjectInviteHandler: React.FC<ProjectInviteHandlerProps> = ({
    isAuthenticated,
    onAccepted,
}) => {
    const dispatch = useAppDispatch();
    const { showToast, showErrorToast } = useToast();
    const processingRef = useRef(false);

    useEffect(() => {
        if (!isProjectInvitePath(window.location.pathname)) return;
        const token = getProjectInviteTokenFromLocation();
        if (token) {
            savePendingInviteToken(token);
        }
    }, []);

    useEffect(() => {
        if (!isAuthenticated || processingRef.current) return;

        const token = isProjectInvitePath(window.location.pathname)
            ? getProjectInviteTokenFromLocation()
            : consumePendingInviteToken();
        if (!token) return;

        processingRef.current = true;
        void acceptProjectInvitation(token)
            .then(async () => {
                await dispatch(fetchProjects());
                window.history.replaceState(null, '', APP_ROUTES.projects);
                showToast('프로젝트 초대를 수락했습니다.');
                onAccepted();
            })
            .catch(err => {
                showErrorToast(getApiErrorMessage(err, '초대 수락에 실패했습니다.'));
                window.history.replaceState(null, '', APP_ROUTES.today);
            })
            .finally(() => {
                processingRef.current = false;
            });
    }, [dispatch, isAuthenticated, onAccepted, showErrorToast, showToast]);

    return null;
};

export default ProjectInviteHandler;
