import { useState } from 'react';

interface LoginScreenProps {
    onLogin: (email: string) => void;
}

const LoginScreen: React.FC<LoginScreenProps> = ({ onLogin }) => {
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');

    const handleSubmit = (e: React.FormEvent) => {
        e.preventDefault();
        if (!email.trim() || !password.trim()) return;
        onLogin(email.trim());
    };

    return (
        <div className="login-page">
            <section className="login-panel">
                <div className="login-brand">
                    <span className="login-brand-icon">▤</span>
                    <span>todoist</span>
                </div>

                <h1 className="login-title">다시 오신 걸 환영합니다!</h1>

                <button type="button" className="oauth-btn">G 구글로 계속 진행</button>
                <button type="button" className="oauth-btn">f 페이스북으로 계속 진행</button>
                <button type="button" className="oauth-btn"> 애플로 계속 진행</button>

                <form className="login-form" onSubmit={handleSubmit}>
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

                    <button type="submit" className="login-submit">로그인</button>
                </form>

                <p className="login-footnote">계정이 없습니까? 가입하세요</p>
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
