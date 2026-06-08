import { useEffect } from 'react';
import { bootstrapAuthFromCallback, OAUTH_CALLBACK_PATH } from '../api/authBootstrap';

interface OAuthRedirectHandlerProps {
    onSuccess: () => void;
}

/** useEffect 보조: 마운트 후 콜백 경로에서 한 번 더 동기화 */
const OAuthRedirectHandler: React.FC<OAuthRedirectHandlerProps> = ({ onSuccess }) => {
    useEffect(() => {
        const onCallback =
            window.location.pathname === OAUTH_CALLBACK_PATH ||
            window.location.pathname.endsWith(OAUTH_CALLBACK_PATH);
        if (!onCallback) return;

        if (bootstrapAuthFromCallback()) {
            onSuccess();
        }
    }, [onSuccess]);

    return null;
};

export default OAuthRedirectHandler;
