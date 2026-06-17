import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import type { Project } from '../store/habitSlice';
import { fetchActivityLogs } from '../api/activityApi';
import type { ActivityLogDto, ActivityType } from '../api/types';
import { useInfiniteScroll } from '../hooks/useInfiniteScroll';
import { formatActivityDateHeader, formatActivityRelativeTime } from '../utils/activityLog';

type ProjectFilter = 'all' | number;
type ActivityFilter = 'all' | ActivityType;
type DateFilter = 'all' | 'today' | 'week';

interface ActivityLogViewProps {
    projects: Project[];
    onOpenTask?: (taskId: number) => void;
}

const ACTIVITY_FILTER_OPTIONS: { value: ActivityFilter; label: string }[] = [
    { value: 'all', label: '모든 활동' },
    { value: 'ADDED', label: '추가' },
    { value: 'COMPLETED', label: '완료' },
    { value: 'INVITED', label: '초대' },
    { value: 'MOVED', label: '이동' },
    { value: 'UPDATED', label: '수정' },
    { value: 'DELETED', label: '삭제' },
];

function activityLabel(type: ActivityType): string {
    switch (type) {
        case 'ADDED':
            return '추가했습니다';
        case 'COMPLETED':
            return '완료했습니다';
        case 'INVITED':
            return '초대했습니다';
        case 'MOVED':
            return '이동시켰습니다';
        case 'DELETED':
            return '삭제했습니다';
        case 'UPDATED':
        default:
            return '수정했습니다';
    }
}

function activityBadge(type: ActivityType): string {
    switch (type) {
        case 'ADDED':
            return '+';
        case 'COMPLETED':
            return '✓';
        case 'INVITED':
            return '@';
        case 'DELETED':
            return '−';
        case 'MOVED':
        case 'UPDATED':
        default:
            return '↻';
    }
}

function renderActivityText(entry: ActivityLogDto): React.ReactNode {
    if (entry.customMessage?.trim()) {
        return entry.customMessage;
    }

    return (
        <>
            <strong>{entry.userName}</strong>님이 활동을 {activityLabel(entry.activityType)}
            {entry.projectName && (
                <span className="activity-project">
                    {' '}
                    · {entry.projectName} #
                </span>
            )}
        </>
    );
}

const ActivityLogView: React.FC<ActivityLogViewProps> = ({ projects }) => {
    const scrollBodyRef = useRef<HTMLDivElement>(null);
    const [items, setItems] = useState<ActivityLogDto[]>([]);
    const [loading, setLoading] = useState(true);
    const [loadMoreStatus, setLoadMoreStatus] = useState<'idle' | 'loading' | 'failed'>('idle');
    const [hasNext, setHasNext] = useState(false);
    const [nextCursor, setNextCursor] = useState<number | null>(null);
    const [error, setError] = useState<string | null>(null);
    const [projectFilter, setProjectFilter] = useState<ProjectFilter>('all');
    const [activityFilter, setActivityFilter] = useState<ActivityFilter>('all');
    const [dateFilter, setDateFilter] = useState<DateFilter>('all');

    useEffect(() => {
        let active = true;
        setLoading(true);
        setLoadMoreStatus('idle');

        void fetchActivityLogs()
            .then(page => {
                if (!active) return;
                setItems(page.content);
                setHasNext(page.hasNext);
                setNextCursor(page.nextCursor);
                setError(null);
            })
            .catch(err => {
                if (!active) return;
                setError(err instanceof Error ? err.message : '활동 내역을 불러오지 못했습니다.');
                setItems([]);
                setHasNext(false);
                setNextCursor(null);
            })
            .finally(() => {
                if (active) setLoading(false);
            });

        return () => {
            active = false;
        };
    }, []);

    const handleLoadMore = useCallback(() => {
        if (nextCursor == null || loadMoreStatus === 'loading') return;

        setLoadMoreStatus('loading');
        void fetchActivityLogs(nextCursor)
            .then(page => {
                setItems(prev => {
                    const existingIds = new Set(prev.map(item => item.id));
                    const appended = page.content.filter(item => !existingIds.has(item.id));
                    return [...prev, ...appended];
                });
                setHasNext(page.hasNext);
                setNextCursor(page.nextCursor);
                setLoadMoreStatus('idle');
            })
            .catch(err => {
                setError(err instanceof Error ? err.message : '활동 내역을 더 불러오지 못했습니다.');
                setLoadMoreStatus('failed');
            });
    }, [loadMoreStatus, nextCursor]);

    const loadMoreSentinelRef = useInfiniteScroll(
        !loading && hasNext,
        hasNext,
        loadMoreStatus === 'loading',
        handleLoadMore,
        scrollBodyRef,
    );

    const filtered = useMemo(() => {
        const now = new Date();
        const todayKey = now.toISOString().slice(0, 10);
        const weekAgo = new Date(now.getTime() - 7 * 24 * 60 * 60 * 1000);

        return items.filter(entry => {
            if (projectFilter !== 'all') {
                const project = projects.find(p => p.id === projectFilter);
                if (project && entry.projectName !== project.name) return false;
            }
            if (activityFilter !== 'all' && entry.activityType !== activityFilter) return false;
            const created = new Date(entry.createdAt);
            if (dateFilter === 'today' && entry.createdAt.slice(0, 10) !== todayKey) return false;
            if (dateFilter === 'week' && created < weekAgo) return false;
            return true;
        });
    }, [items, projectFilter, activityFilter, dateFilter, projects]);

    const grouped = useMemo(() => {
        const map = new Map<string, ActivityLogDto[]>();
        for (const entry of filtered) {
            const key = entry.createdAt.slice(0, 10);
            if (!map.has(key)) map.set(key, []);
            map.get(key)!.push(entry);
        }
        return [...map.entries()].sort(([a], [b]) => b.localeCompare(a));
    }, [filtered]);

    const exportMarkdown = () => {
        const exportedAt = new Date().toLocaleString('ko-KR');
        const lines = [
            '# HabitFlow 활동 보고',
            '',
            `-보낸 시각: ${exportedAt}`,
            `- 총 ${filtered.length}건`,
            '',
        ];

        for (const [dateKey, entries] of grouped) {
            lines.push(`## ${formatActivityDateHeader(dateKey, entries.length)}`);
            lines.push('');
            for (const entry of entries) {
                const time = new Date(entry.createdAt).toLocaleTimeString('ko-KR', {
                    hour: '2-digit',
                    minute: '2-digit',
                });
                if (entry.customMessage?.trim()) {
                    lines.push(`- ${time} **${entry.userName}** — ${entry.customMessage}`);
                } else {
                    const project = entry.projectName ? ` · ${entry.projectName}` : '';
                    lines.push(
                        `- ${time} **${entry.userName}**님이 ${activityLabel(entry.activityType)}${project}`,
                    );
                }
            }
            lines.push('');
        }

        const blob = new Blob([lines.join('\n')], { type: 'text/markdown;charset=utf-8;' });
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = 'habitflow-activity.md';
        a.click();
        URL.revokeObjectURL(url);
    };

    return (
        <div className="activity-log-page">
            <header className="activity-log-header">
                <div className="activity-log-title-row">
                    <h1 className="activity-log-title">보고: 전체</h1>
                    <button type="button" className="activity-export-btn" onClick={exportMarkdown}>
                        보내기
                    </button>
                </div>

                <div className="activity-filters">
                    <label className="activity-filter">
                        <span className="activity-filter-icon" aria-hidden>▦</span>
                        <select disabled value="all">
                            <option value="all">모든 작업 영역</option>
                        </select>
                    </label>
                    <label className="activity-filter">
                        <span className="activity-filter-icon" aria-hidden>#</span>
                        <select
                            value={String(projectFilter)}
                            onChange={e => {
                                const v = e.target.value;
                                setProjectFilter(v === 'all' ? 'all' : Number(v));
                            }}
                        >
                            <option value="all">모든 프로젝트</option>
                            {projects.map(p => (
                                <option key={p.id} value={String(p.id)}>
                                    {p.name}
                                </option>
                            ))}
                        </select>
                    </label>
                    <label className="activity-filter">
                        <span className="activity-filter-icon" aria-hidden>≡</span>
                        <select
                            value={activityFilter}
                            onChange={e => setActivityFilter(e.target.value as ActivityFilter)}
                        >
                            {ACTIVITY_FILTER_OPTIONS.map(opt => (
                                <option key={opt.value} value={opt.value}>
                                    {opt.label}
                                </option>
                            ))}
                        </select>
                    </label>
                    <label className="activity-filter">
                        <span className="activity-filter-icon" aria-hidden>📅</span>
                        <select
                            value={dateFilter}
                            onChange={e => setDateFilter(e.target.value as DateFilter)}
                        >
                            <option value="all">모든 날짜</option>
                            <option value="today">오늘</option>
                            <option value="week">지난 7일</option>
                        </select>
                    </label>
                </div>
            </header>

            <div ref={scrollBodyRef} className="activity-log-body">
                {loading && <p className="activity-empty">불러오는 중…</p>}
                {error && <p className="activity-empty">{error}</p>}
                {!loading && !error && grouped.length === 0 ? (
                    <p className="activity-empty">활동 내역이 없습니다.</p>
                ) : (
                    grouped.map(([dateKey, entries]) => (
                        <section key={dateKey} className="activity-day-group">
                            <h2 className="activity-day-title">
                                {formatActivityDateHeader(dateKey, entries.length)}
                            </h2>
                            <ul className="activity-list">
                                {entries.map(entry => (
                                    <li key={entry.id} className="activity-row">
                                        <span className="activity-avatar-wrap">
                                            <span className="activity-avatar">
                                                {entry.userName.charAt(0).toUpperCase()}
                                            </span>
                                            <span
                                                className={`activity-badge badge-${entry.activityType.toLowerCase()}`}
                                                aria-hidden
                                            >
                                                {activityBadge(entry.activityType)}
                                            </span>
                                        </span>
                                        <div className="activity-content">
                                            <p className="activity-text">
                                                {renderActivityText(entry)}
                                            </p>
                                        </div>
                                        <time className="activity-time" dateTime={entry.createdAt}>
                                            {formatActivityRelativeTime(entry.createdAt)}
                                        </time>
                                    </li>
                                ))}
                            </ul>
                        </section>
                    ))
                )}
                {!loading && hasNext && (
                    <div ref={loadMoreSentinelRef} className="tasks-scroll-sentinel" aria-hidden="true">
                        {loadMoreStatus === 'loading' && (
                            <p className="tasks-scroll-loading">불러오는 중…</p>
                        )}
                    </div>
                )}
            </div>
        </div>
    );
};

export default ActivityLogView;
