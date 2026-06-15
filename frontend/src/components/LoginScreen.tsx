import { useState } from 'react';
import { getApiErrorMessage } from '../api/apiError';
import { setStoredTokens } from '../api/client';
import { loginMember, signUpMember } from '../api/memberApi';

interface LoginScreenProps {
    onLoginSuccess: () => void;
}

type StatusMessage = { type: 'error' | 'success'; text: string };

const LoginScreen: React.FC<LoginScreenProps> = ({ onLoginSuccess }) => {
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [name, setName] = useState('');
    const [mode, setMode] = useState<'login' | 'signup'>('login');
    const [status, setStatus] = useState<StatusMessage | null>(null);
    const [loading, setLoading] = useState(false);

    const startGoogleLogin = () => {
        window.location.href = 'http://localhost:8080/oauth2/authorization/google';
    };

    const switchMode = (next: 'login' | 'signup') => {
        setMode(next);
        setStatus(null);
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
            const { accessToken, refreshToken } = await loginMember({
                email: trimmedEmail,
                password: trimmedPassword,
            });
            setStoredTokens(accessToken, refreshToken);
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

    return (
        <div className="login-page">
            <section className="login-panel">
                <div className="login-brand">
                    <span className="login-brand-icon">▤</span>
                    <span>HabitFlow</span>
                </div>

                <h1 className="login-title">다시 오신 걸 환영합니다!</h1>

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
                                disabled={loading}
                            />
                        </>
                    )}

                    <label className="login-label" htmlFor="login-email">이메일</label>
                    <input
                        id="login-email"
                        type="email"
                        className="login-input"
                        placeholder="이메일 입력..."
                        value={email}
                        onChange={e => setEmail(e.target.value)}
                        autoComplete="email"
                        disabled={loading}
                    />

                    <label className="login-label" htmlFor="login-password">패스워드</label>
                    <input
                        id="login-password"
                        type="password"
                        className="login-input"
                        placeholder="패스워드 입력..."
                        value={password}
                        onChange={e => setPassword(e.target.value)}
                        autoComplete={mode === 'signup' ? 'new-password' : 'current-password'}
                        disabled={loading}
                    />

                    <button type="submit" className="login-submit" disabled={loading}>
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
