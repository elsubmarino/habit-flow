import { useState } from 'react';
import { getApiErrorMessage } from '../api/apiError';
import { setStoredTokens } from '../api/client';
import {
    loginMember,
    sendAuthCode,
    signUpMember,
    verifyAuthCode,
} from '../api/memberApi';
import {
    isProjectInvitePath,
    peekPendingInviteToken,
} from '../utils/projectInvite';

interface LoginScreenProps {
    onLoginSuccess: () => void;
}

type StatusMessage = { type: 'error' | 'success'; text: string };

const EMAIL_PATTERN = /^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,6}$/;

const LoginScreen: React.FC<LoginScreenProps> = ({ onLoginSuccess }) => {
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [name, setName] = useState('');
    const [authCode, setAuthCode] = useState('');
    const [emailVerified, setEmailVerified] = useState(false);
    const [codeSent, setCodeSent] = useState(false);
    const [mode, setMode] = useState<'login' | 'signup'>('login');
    const [status, setStatus] = useState<StatusMessage | null>(null);
    const [loading, setLoading] = useState(false);
    const [sendingCode, setSendingCode] = useState(false);
    const [verifyingCode, setVerifyingCode] = useState(false);

    const startGoogleLogin = () => {
        window.location.href = 'http://localhost:8080/oauth2/authorization/google';
    };

    const resetEmailVerification = () => {
        setAuthCode('');
        setEmailVerified(false);
        setCodeSent(false);
    };

    const switchMode = (next: 'login' | 'signup') => {
        setMode(next);
        setStatus(null);
        resetEmailVerification();
    };

    const handleEmailChange = (value: string) => {
        setEmail(value);
        if (emailVerified || codeSent) {
            resetEmailVerification();
        }
    };

    const handleSendAuthCode = async () => {
        const trimmedEmail = email.trim();
        if (!trimmedEmail) {
            setStatus({ type: 'error', text: '이메일을 입력해 주세요.' });
            return;
        }
        if (!EMAIL_PATTERN.test(trimmedEmail)) {
            setStatus({ type: 'error', text: '올바른 이메일 형식이 아닙니다.' });
            return;
        }

        setSendingCode(true);
        setStatus(null);
        try {
            await sendAuthCode(trimmedEmail);
            setCodeSent(true);
            setEmailVerified(false);
            setAuthCode('');
            setStatus({
                type: 'success',
                text: '인증번호를 발송했습니다. 3분 이내에 입력해 주세요.',
            });
        } catch (err) {
            setStatus({
                type: 'error',
                text: getApiErrorMessage(err, '인증번호 발송에 실패했습니다.'),
            });
        } finally {
            setSendingCode(false);
        }
    };

    const handleVerifyAuthCode = async () => {
        const trimmedEmail = email.trim();
        const trimmedCode = authCode.trim();

        if (!trimmedEmail) {
            setStatus({ type: 'error', text: '이메일을 입력해 주세요.' });
            return;
        }
        if (!/^\d{6}$/.test(trimmedCode)) {
            setStatus({ type: 'error', text: '6자리 인증번호를 입력해 주세요.' });
            return;
        }

        setVerifyingCode(true);
        setStatus(null);
        try {
            await verifyAuthCode(trimmedEmail, trimmedCode);
            setEmailVerified(true);
            setStatus({ type: 'success', text: '이메일 인증이 완료되었습니다.' });
        } catch (err) {
            setStatus({
                type: 'error',
                text: getApiErrorMessage(err, '인증번호 확인에 실패했습니다.'),
            });
        } finally {
            setVerifyingCode(false);
        }
    };

    const handleSignup = async (e: React.FormEvent) => {
        e.preventDefault();
        const trimmedName = name.trim();
        const trimmedEmail = email.trim();
        const trimmedPassword = password.trim();

        if (!trimmedName) {
            setStatus({ type: 'error', text: '이름을 입력해 주세요.' });
            return;
        }
        if (!trimmedEmail) {
            setStatus({ type: 'error', text: '이메일을 입력해 주세요.' });
            return;
        }
        if (!emailVerified) {
            setStatus({ type: 'error', text: '이메일 인증을 완료해 주세요.' });
            return;
        }
        if (!trimmedPassword) {
            setStatus({ type: 'error', text: '패스워드를 입력해 주세요.' });
            return;
        }

        setLoading(true);
        setStatus(null);
        try {
            await signUpMember({
                email: trimmedEmail,
                password: trimmedPassword,
                name: trimmedName,
            });
            setStatus({
                type: 'success',
                text: '가입이 완료되었습니다. 이메일로 로그인해 주세요.',
            });
            setPassword('');
            resetEmailVerification();
            setMode('login');
        } catch (err) {
            setStatus({
                type: 'error',
                text: getApiErrorMessage(err, '가입에 실패했습니다.'),
            });
        } finally {
            setLoading(false);
        }
    };

    const handleEmailLogin = async (e: React.FormEvent) => {
        e.preventDefault();
        const trimmedEmail = email.trim();
        const trimmedPassword = password.trim();

        if (!trimmedEmail) {
            setStatus({ type: 'error', text: '이메일을 입력해 주세요.' });
            return;
        }
        if (!trimmedPassword) {
            setStatus({ type: 'error', text: '패스워드를 입력해 주세요.' });
            return;
        }

        setLoading(true);
        setStatus(null);
        try {
            const { accessToken } = await loginMember({
                email: trimmedEmail,
                password: trimmedPassword,
            });
            setStoredTokens(accessToken);
            onLoginSuccess();
        } catch (err) {
            setStatus({
                type: 'error',
                text: getApiErrorMessage(err, '로그인에 실패했습니다.'),
            });
        } finally {
            setLoading(false);
        }
    };

    const formBusy = loading || sendingCode || verifyingCode;
    const pendingProjectInvite =
        isProjectInvitePath(window.location.pathname) || peekPendingInviteToken() != null;

    return (
        <div className="login-page">
            <section className="login-panel">
                <div className="login-brand">
                    <span className="login-brand-icon">▤</span>
                    <span>HabitFlow</span>
                </div>

                <h1 className="login-title">
                    {pendingProjectInvite
                        ? '프로젝트 초대'
                        : mode === 'signup'
                            ? '회원가입'
                            : '다시 오신 걸 환영합니다!'}
                </h1>

                {pendingProjectInvite && (
                    <p className="login-invite-notice">
                        로그인 또는 가입을 완료하면 프로젝트 초대가 수락됩니다.
                    </p>
                )}

                <button type="button" className="oauth-btn" onClick={startGoogleLogin}>
                    G 구글로 계속 진행
                </button>

                {status && (
                    <p className={status.type === 'success' ? 'login-success' : 'login-error'}>
                        {status.text}
                    </p>
                )}

                <form
                    className="login-form"
                    onSubmit={mode === 'signup' ? handleSignup : handleEmailLogin}
                    noValidate
                >
                    {mode === 'signup' && (
                        <>
                            <label className="login-label" htmlFor="signup-name">이름</label>
                            <input
                                id="signup-name"
                                type="text"
                                className="login-input"
                                placeholder="이름 입력..."
                                value={name}
                                onChange={e => setName(e.target.value)}
                                autoComplete="name"
                                disabled={formBusy}
                            />
                        </>
                    )}

                    <label className="login-label" htmlFor="login-email">이메일</label>
                    <div className="login-email-row">
                        <input
                            id="login-email"
                            type="email"
                            className="login-input"
                            placeholder="이메일 입력..."
                            value={email}
                            onChange={e => handleEmailChange(e.target.value)}
                            autoComplete="email"
                            disabled={formBusy}
                        />
                        {mode === 'signup' && (
                            <button
                                type="button"
                                className="login-inline-btn"
                                onClick={() => void handleSendAuthCode()}
                                disabled={formBusy}
                            >
                                {sendingCode ? '발송 중…' : codeSent ? '재발송' : '인증번호 발송'}
                            </button>
                        )}
                    </div>

                    {mode === 'signup' && codeSent && (
                        <>
                            <label className="login-label" htmlFor="signup-auth-code">인증번호</label>
                            <div className="login-email-row">
                                <input
                                    id="signup-auth-code"
                                    type="text"
                                    inputMode="numeric"
                                    pattern="\d{6}"
                                    maxLength={6}
                                    className="login-input"
                                    placeholder="6자리 인증번호"
                                    value={authCode}
                                    onChange={e => {
                                        setAuthCode(e.target.value.replace(/\D/g, '').slice(0, 6));
                                        if (emailVerified) setEmailVerified(false);
                                    }}
                                    autoComplete="one-time-code"
                                    disabled={formBusy || emailVerified}
                                />
                                <button
                                    type="button"
                                    className="login-inline-btn"
                                    onClick={() => void handleVerifyAuthCode()}
                                    disabled={formBusy || emailVerified}
                                >
                                    {verifyingCode ? '확인 중…' : '인증 확인'}
                                </button>
                            </div>
                            {emailVerified && (
                                <p className="login-verified-badge">이메일 인증 완료</p>
                            )}
                        </>
                    )}

                    <label className="login-label" htmlFor="login-password">패스워드</label>
                    <input
                        id="login-password"
                        type="password"
                        className="login-input"
                        placeholder="패스워드 입력..."
                        value={password}
                        onChange={e => setPassword(e.target.value)}
                        autoComplete={mode === 'signup' ? 'new-password' : 'current-password'}
                        disabled={formBusy}
                    />
                    {mode === 'signup' && (
                        <p className="login-password-hint">
                            영문 대·소문자, 숫자, 특수문자(@$!%*?&)를 포함해 8~20자
                        </p>
                    )}

                    <button
                        type="submit"
                        className="login-submit"
                        disabled={formBusy || (mode === 'signup' && !emailVerified)}
                    >
                        {loading ? '처리 중…' : mode === 'signup' ? '가입하기' : '로그인'}
                    </button>
                </form>

                <p className="login-footnote">
                    {mode === 'login' ? (
                        <>
                            계정이 없습니까?{' '}
                            <button type="button" className="login-link-btn" onClick={() => switchMode('signup')}>
                                가입하세요
                            </button>
                        </>
                    ) : (
                        <>
                            이미 계정이 있습니까?{' '}
                            <button type="button" className="login-link-btn" onClick={() => switchMode('login')}>
                                로그인
                            </button>
                        </>
                    )}
                </p>
            </section>

            <section className="login-preview">
                <div className="preview-card">○ 3분기 예산 마무리</div>
                <div className="preview-card">○ 팀 미팅 일정 설정</div>
                <div className="preview-card">○ 퇴근 후 우유 구매</div>
            </section>
        </div>
    );
};

export default LoginScreen;
