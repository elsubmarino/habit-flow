import axios from 'axios';

export function getApiErrorMessage(err: unknown, fallback: string): string {
    if (axios.isAxiosError(err)) {
        const data = err.response?.data;
        if (typeof data === 'string' && data.trim()) return data;
        if (data && typeof data === 'object') {
            if ('message' in data && typeof data.message === 'string') return data.message;
            if ('error' in data && typeof data.error === 'string') return data.error;
        }
        if (err.response?.status === 401) {
            return '회원가입 요청이 거부되었습니다. 백엔드에서 POST /api/members 를 permitAll 로 열어야 합니다.';
        }
        if (err.response?.status === 409) {
            return '이미 사용 중인 이메일입니다.';
        }
        if (err.message) return err.message;
    }
    if (err instanceof Error && err.message) return err.message;
    return fallback;
}
