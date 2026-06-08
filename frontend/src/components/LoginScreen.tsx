import { useState } from 'react';
import { signUpMember } from '../api/memberApi';

interface LoginScreenProps {
    onLoginSuccess: () => void;
}

const LoginScreen: React.FC<LoginScreenProps> = () => {
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [name, setName] = useState('');
    const [mode, setMode] = useState<'login' | 'signup'>('login');
    const [error, setError] = useState<string | null>(null);
    const [loading, setLoading] = useState(false);

    const startGoogleLogin = () => {
        // Vite 프록시 대신 백엔드(8080)로 직접 이동 → Google redirect_uri가 8080으로 고정됨
        window.location.href = 'http://localhost:8080/oauth2/authorization/google';
    };

    const handleSignup = async (e: React.FormEvent) => {
        e.preventDefault();
        if (!email.trim() || !password.trim() || !name.trim()) return;
        setLoading(true);
        setError(null);
        try {
            await signUpMember({
                email: email.trim(),
                password: password.trim(),
                name: name.trim(),
            });
            setError('가입이 완료되었습니다. 로그인은 Google OAuth를 사용해 주세요. (이메일 로그인 API 없음)');
            setMode('login');
        } catch (err) {
            setError(
                err instanceof Error
                    ? err.message
                    : '가입에 실패했습니다. (백엔드: POST /api/members 는 인증이 필요할 수 있음)',
            );
        } finally {
            setLoading(false);
        }
    };

    const handleEmailLogin = (e: React.FormEvent) => {
        e.preventDefault();
        setError('이메일/비밀번호 로그인 API가 백엔드에 없습니다. Google로 로그인해 주세요.');
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

                {error && <p className="login-error">{error}</p>}

                <form
                    className="login-form"
                    onSubmit={mode === 'signup' ? handleSignup : handleEmailLogin}
                >
                    {mode === 'signup' && (
                        <>
                            <label className="login-label">이름</label>
                            <input
                                type="text"
                                className="login-input"
                                placeholder="이름 입력..."
                                value={name}
                                onChange={e => setName(e.target.value)}
                            />
                        </>
                    )}

                    <label className="login-label">이메일</label>
                    <input
                        type="email"
                        className="login-input"
                        placeholder="이메일 입력..."
                        value={email}
                        onChange={e => setEmail(e.target.value)}
                        autoComplete="email"
                    />

                    <label className="login-label">패스워드</label>
                    <input
                        type="password"
                        className="login-input"
                        placeholder="패스워드 입력..."
                        value={password}
                        onChange={e => setPassword(e.target.value)}
                        autoComplete="current-password"
                    />

                    <button type="submit" className="login-submit" disabled={loading}>
                        {mode === 'signup' ? '가입하기' : '로그인'}
                    </button>
                </form>

                <p className="login-footnote">
                    {mode === 'login' ? (
                        <>
                            계정이 없습니까?{' '}
                            <button type="button" className="login-link-btn" onClick={() => setMode('signup')}>
                                가입하세요
                            </button>
                        </>
                    ) : (
                        <>
                            이미 계정이 있습니까?{' '}
                            <button type="button" className="login-link-btn" onClick={() => setMode('login')}>
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
